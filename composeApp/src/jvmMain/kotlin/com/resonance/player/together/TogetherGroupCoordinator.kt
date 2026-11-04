package com.resonance.player.together

/**
 * In-memory room state machine for a future authenticated multi-member transport.
 * This class does not authenticate callers, encrypt or transmit anything; the transport must
 * bind [memberId] to a verified device identity before invoking it.
 */
internal class TogetherGroupCoordinator(private val capacity: Int = 8) {
    init { require(capacity in 3..16) }

    data class Member(val id: String, val name: String, val ready: Boolean = false)
    data class Track(val id: String, val sourceMemberId: String, val title: String, val artist: String, val durationMs: Long)
    data class Snapshot(
        val revision: Long,
        val members: List<Member>,
        val queue: List<Track>,
        val currentTrackId: String?,
        val playing: Boolean,
        val progress: Float,
    )
    sealed interface Action {
        data object Play : Action
        data object Pause : Action
        data object Next : Action
        data object Previous : Action
        data class Seek(val fraction: Float) : Action
        data class Select(val trackId: String) : Action
        data class Append(val track: Track) : Action
        data class Remove(val trackId: String) : Action
        data class Move(val trackId: String, val destination: Int) : Action
    }
    enum class Outcome { Applied, Duplicate, MissingSequence, NotMember, Invalid }
    data class Result(val outcome: Outcome, val snapshot: Snapshot)

    private val members = linkedMapOf<String, Member>()
    // Retain sequence floors for departed identities so a rejoin cannot replay old commands.
    private val sequenceFloors = mutableMapOf<String, Long>()
    private val queue = mutableListOf<Track>()
    private var currentTrackId: String? = null
    private var playing = false
    private var progress = 0f
    private var revision = 0L

    /** Caller must already have authenticated the device and obtained the required join consent. */
    @Synchronized fun admitVerified(id: String, name: String): Boolean {
        if (!ID.matches(id) || name.length > 40 || name.any(::isControl)) return false
        if (members.containsKey(id)) return true
        if (members.size >= capacity || (id !in sequenceFloors && sequenceFloors.size >= MAX_IDENTITIES)) return false
        val next = Math.addExact(revision, 1)
        members[id] = Member(id, name.ifBlank { "朋友" })
        sequenceFloors.putIfAbsent(id, 0L)
        revision = next
        return true
    }

    /** Updates the readiness state of an admitted member. */
    @Synchronized fun setMemberReady(id: String, ready: Boolean): Boolean {
        val existing = members[id] ?: return false
        if (existing.ready == ready) return true
        members[id] = existing.copy(ready = ready)
        revision = Math.addExact(revision, 1)
        return true
    }

    /** A member may leave itself; binding the request to its verified identity is mandatory. */
    @Synchronized fun leaveVerified(id: String): Boolean {
        if (id !in members) return false
        val next = Math.addExact(revision, 1)
        members.remove(id)
        revision = next
        return true
    }

    /** Removes pending tracks contributed by a departing member if they are not cached. */
    @Synchronized fun removeMemberTracks(memberId: String, shouldRemove: (Track) -> Boolean): List<Track> {
        val removed = mutableListOf<Track>()
        val iterator = queue.iterator()
        var currentChanged = false
        while (iterator.hasNext()) {
            val track = iterator.next()
            if (track.sourceMemberId == memberId && shouldRemove(track)) {
                iterator.remove()
                removed.add(track)
                if (currentTrackId == track.id) {
                    currentChanged = true
                }
            }
        }
        if (currentChanged) {
            currentTrackId = queue.firstOrNull()?.id
            progress = 0f
            if (currentTrackId == null) playing = false
        }
        if (removed.isNotEmpty()) {
            revision = Math.addExact(revision, 1)
        }
        return removed
    }

    @Synchronized fun snapshot(): Snapshot = Snapshot(revision, members.values.toList(), queue.toList(), currentTrackId, playing, progress)

    @Synchronized fun currentTrack(): Track? = queue.firstOrNull { it.id == currentTrackId }

    @Synchronized fun markTrackAvailable(trackId: String) {
        if (queue.any { it.id == trackId }) revision = Math.addExact(revision, 1)
    }

    /** Reflects confirmed coordinator playback rather than assuming a requested command succeeded. */
    @Synchronized fun reflectPlayback(trackId: String?, nowPlaying: Boolean, nowProgress: Float): Boolean {
        if (!nowProgress.isFinite()) return false
        val current = trackId?.takeIf { id -> queue.any { it.id == id } }
        // A removed native item can still be sampled before its queued replacement is applied.
        if (trackId != null && current == null && queue.isNotEmpty()) return false
        val nextProgress = nowProgress.coerceIn(0f, 1f)
        if (currentTrackId == current && playing == nowPlaying && kotlin.math.abs(progress - nextProgress) <= 0.05f) return false
        val nextRevision = Math.addExact(revision, 1)
        currentTrackId = current
        playing = nowPlaying
        progress = nextProgress
        revision = nextRevision
        return true
    }

    /** Reflects the coordinator device's existing playback queue without discarding guest entries. */
    @Synchronized fun syncLocalQueue(memberId: String, tracks: List<Track>): Boolean {
        if (memberId !in members || tracks.size > MAX_TRACKS || tracks.any { it.sourceMemberId != memberId } ||
            tracks.map { it.id }.distinct().size != tracks.size || tracks.any {
                !TRACK_ID.matches(it.id) || it.title.isBlank() || it.title.length > 128 || it.artist.length > 80 ||
                    it.title.any(::isControl) || it.artist.any(::isControl) || it.durationMs !in 1L..MAX_DURATION_MS
            }) return false
        val contributed = queue.filter { it.sourceMemberId != memberId }
        val ids = tracks.mapTo(mutableSetOf()) { it.id }
        if (contributed.any { it.id in ids } || tracks.size + contributed.size > MAX_TRACKS) return false
        val next = tracks + contributed
        if (queue == next) return false
        val nextRevision = Math.addExact(revision, 1)
        queue.clear()
        queue.addAll(next)
        if (queue.none { it.id == currentTrackId }) {
            currentTrackId = queue.firstOrNull()?.id
            progress = 0f
            if (currentTrackId == null) playing = false
        }
        revision = nextRevision
        return true
    }

    @Synchronized fun setPlaybackState(nowPlaying: Boolean, nowProgress: Float) {
        if (playing != nowPlaying || kotlin.math.abs(progress - nowProgress) > 0.05f) {
            playing = nowPlaying
            progress = nowProgress.coerceIn(0f, 1f)
            revision = Math.addExact(revision, 1)
        }
    }

    /** Internal playback projection: existing foreign entries keep their verified source identity. */
    @Synchronized fun syncPlayableQueue(localId: String, tracks: List<Track>, pendingIds: Set<String>): Boolean {
        if (localId !in members || tracks.map { it.id }.distinct().size != tracks.size) return false
        if (tracks.any { track ->
                !TRACK_ID.matches(track.id) || track.title.isBlank() || track.title.length > 128 || track.artist.length > 80 ||
                    track.title.any(::isControl) || track.artist.any(::isControl) || track.durationMs !in 1L..MAX_DURATION_MS ||
                    (track.sourceMemberId != localId && queue.none { it.id == track.id && it.sourceMemberId == track.sourceMemberId })
            }) return false
        val next = tracks + queue.filter { it.id in pendingIds && tracks.none { t -> t.id == it.id } }
        if (next.size > MAX_TRACKS || next == queue) return false
        val oldIndex = queue.indexOfFirst { it.id == currentTrackId }.coerceAtLeast(0)
        val nextRevision = Math.addExact(revision, 1)
        queue.clear(); queue.addAll(next)
        if (queue.none { it.id == currentTrackId }) {
            currentTrackId = queue.getOrNull(oldIndex.coerceAtMost(queue.lastIndex))?.id
            progress = 0f
            if (currentTrackId == null) playing = false
        }
        revision = nextRevision
        return true
    }

    @Synchronized fun reset() {
        members.clear()
        sequenceFloors.clear()
        queue.clear()
        currentTrackId = null
        playing = false
        progress = 0f
        revision = 0L
    }

    /** Each member has identical permissions; per-member monotonic sequence prevents replay. */
    @Synchronized fun submitVerified(memberId: String, sequence: Long, action: Action): Result {
        if (memberId !in members) return result(Outcome.NotMember)
        val floor = sequenceFloors.getValue(memberId)
        if (sequence <= floor) return result(Outcome.Duplicate)
        if (!valid(memberId, action)) return result(Outcome.Invalid)
        val next = Math.addExact(revision, 1)
        when (action) {
            Action.Play -> playing = true
            Action.Pause -> playing = false
            Action.Next -> step(1)
            Action.Previous -> step(-1)
            is Action.Seek -> progress = action.fraction
            is Action.Select -> { currentTrackId = action.trackId; progress = 0f }
            is Action.Append -> {
                queue.add(action.track)
                if (currentTrackId == null) currentTrackId = action.track.id
            }
            is Action.Remove -> {
                val index = queue.indexOfFirst { it.id == action.trackId }
                queue.removeAt(index)
                if (currentTrackId == action.trackId) {
                    currentTrackId = queue.getOrNull(index.coerceAtMost(queue.lastIndex))?.id
                    progress = 0f
                    if (currentTrackId == null) playing = false
                }
            }
            is Action.Move -> {
                val index = queue.indexOfFirst { it.id == action.trackId }
                queue.add(action.destination, queue.removeAt(index))
            }
        }
        sequenceFloors[memberId] = sequence
        revision = next
        return result(Outcome.Applied)
    }

    private fun valid(memberId: String, action: Action): Boolean = when (action) {
        Action.Play -> currentTrackId != null && queue.isNotEmpty()
        Action.Next -> queue.indexOfFirst { it.id == currentTrackId }.let { it >= 0 && it < queue.lastIndex }
        Action.Previous -> queue.indexOfFirst { it.id == currentTrackId } > 0
        Action.Pause -> true
        is Action.Seek -> currentTrackId != null && action.fraction.isFinite() && action.fraction in 0f..1f
        is Action.Select -> queue.any { it.id == action.trackId }
        is Action.Append -> action.track.let {
            it.sourceMemberId == memberId && TRACK_ID.matches(it.id) && queue.none { existing -> existing.id == it.id } &&
                queue.size < MAX_TRACKS && it.title.isNotBlank() && it.title.length <= 128 && it.artist.length <= 80 &&
                it.title.none(::isControl) && it.artist.none(::isControl) && it.durationMs in 1L..MAX_DURATION_MS
        }
        is Action.Remove -> queue.any { it.id == action.trackId }
        is Action.Move -> queue.any { it.id == action.trackId } && action.destination in queue.indices
    }

    private fun step(direction: Int) {
        val current = queue.indexOfFirst { it.id == currentTrackId }.coerceAtLeast(0)
        val next = (current + direction).coerceIn(0, queue.lastIndex)
        currentTrackId = queue[next].id
        progress = 0f
    }

    private fun result(outcome: Outcome) = Result(outcome, snapshot())

    private fun isControl(value: Char) = value.code in 0..31 || value.code in 127..159

    companion object {
        private val ID = Regex("[a-f0-9]{32,64}")
        private val TRACK_ID = Regex("[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}")
        private const val MAX_TRACKS = 50
        private const val MAX_IDENTITIES = 128
        private const val MAX_DURATION_MS = 24L * 60 * 60 * 1000
    }
}
