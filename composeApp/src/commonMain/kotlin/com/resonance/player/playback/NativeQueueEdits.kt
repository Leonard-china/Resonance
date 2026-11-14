package com.resonance.player.playback

/** Replace only the ranges around the native current item, preserving its decoder and position. */
data class NativeQueueEdit<T>(val from: Int, val until: Int, val items: List<T>)

fun <T> nativeQueueEdits(current: List<T>, desired: List<T>, currentIndex: Int,
    sameItem: (T, T) -> Boolean, sameCurrentItem: (T, T) -> Boolean = sameItem,
): List<NativeQueueEdit<T>> {
    val playing = current.getOrNull(currentIndex) ?: return emptyList()
    val targetIndex = desired.indexOfFirst { sameCurrentItem(it, playing) }
    if (targetIndex < 0) return emptyList() // A pending explicit selection owns this transition.
    val edits = mutableListOf<NativeQueueEdit<T>>()
    fun replace(from: Int, until: Int, items: List<T>) {
        val old = current.subList(from, until)
        if (old.size != items.size || old.indices.any { !sameItem(old[it], items[it]) })
            edits += NativeQueueEdit(from, until, items)
    }
    // Update the suffix first so prefix changes cannot invalidate its indices.
    replace(currentIndex + 1, current.size, desired.drop(targetIndex + 1))
    replace(0, currentIndex, desired.take(targetIndex))
    return edits
}
