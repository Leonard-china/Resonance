package com.resonance.player.platform

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.resonance.player.playback.nativeQueueEdits

internal fun updateNativePlaybackQueue(player: Player, selectedId: String, desired: List<MediaItem>) {
    if (player.currentMediaItem?.mediaId != selectedId) return
    val current = (0 until player.mediaItemCount).map(player::getMediaItemAt)
    nativeQueueEdits(current, desired, player.currentMediaItemIndex,
        sameItem = { left, right -> left.mediaId == right.mediaId && left.localConfiguration?.uri == right.localConfiguration?.uri },
        // Controller snapshots may omit local configuration; the stable media ID owns playback.
        sameCurrentItem = { left, right -> left.mediaId == right.mediaId },
    ).forEach { edit -> player.replaceMediaItems(edit.from, edit.until, edit.items) }
}
