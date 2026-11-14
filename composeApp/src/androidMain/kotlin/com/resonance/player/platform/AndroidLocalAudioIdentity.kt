package com.resonance.player.platform

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.MediaStore.Audio.Media
import com.resonance.player.model.Track

/** Match SAF and MediaStore only when Android identifies the same local file. */
internal class AndroidLocalAudioIdentity(private val context: Context) {
    private val mediaIdsByPath by lazy {
        val id = Media._ID
        val path = Media.DATA
        runCatching {
            buildMap {
                context.contentResolver.query(Media.EXTERNAL_CONTENT_URI,
                    arrayOf(id, path), null, null, null)?.use { cursor ->
                    while (cursor.moveToNext()) cursor.getString(1)?.let { put(it, cursor.getLong(0)) }
                }
            }
        }.getOrDefault(emptyMap())
    }

    private val mediaIds = mutableMapOf<String, Long?>()

    private fun mediaId(source: String): Long? {
        if (source in mediaIds) return mediaIds[source]
        return resolveMediaId(source).also { mediaIds[source] = it }
    }

    private fun resolveMediaId(source: String): Long? {
        val uri = Uri.parse(source)
        val parsed = mediaRow(uri)
        if (parsed != null) return parsed
        if (!DocumentsContract.isDocumentUri(context, uri)) return null
        if (uri.authority == "com.android.externalstorage.documents") {
            val documentId = DocumentsContract.getDocumentId(uri)
            val relative = documentId.substringAfter("primary:", "")
            if (documentId.startsWith("primary:") && relative.isNotEmpty() &&
                relative.split('/').none { it.isEmpty() || it == "." || it == ".." }) {
                mediaIdsByPath["${Environment.getExternalStorageDirectory().absolutePath}/$relative"]?.let { return it }
            }
        }
        if (Build.VERSION.SDK_INT < 29) return null
        return runCatching { MediaStore.getMediaUri(context, uri)?.let(::mediaRow) }.getOrNull()
    }

    private fun mediaRow(uri: Uri): Long? {
        if (uri.scheme != "content" || uri.authority != "media") return null
        val segments = uri.pathSegments
        if (segments.firstOrNull()?.let { it == "external" || it == "external_primary" } != true) return null
        if (segments.drop(1).dropLast(1) !in listOf(listOf("audio", "media"), listOf("file"))) return null
        return runCatching { ContentUris.parseId(uri) }.getOrNull()?.takeIf { it >= 0L }
    }

    fun key(track: Track): String {
        val source = track.sourceUri ?: return "track:${track.id}"
        return mediaId(source)?.let { "media:$it" } ?: "track:${track.id}"
    }

    fun merge(tracks: List<Track>, favorites: Set<String>, activeId: String? = null): AndroidLocalCatalog {
        val aliases = mutableMapOf<String, String>()
        val merged = tracks.associateBy(Track::id).values.groupBy(::key).values.map { group ->
            val selected = group.firstOrNull { it.id == activeId }
                // Keep a previously imported document identity when it later appears in MediaStore.
                ?: group.last()
            group.filterNot { it.id == selected.id }.forEach { aliases[it.id] = selected.id }
            // The folder grant may have been revoked while this file is still readable through MediaStore.
            val indexedSource = group.firstNotNullOfOrNull { track ->
                track.sourceUri?.takeIf { mediaRow(Uri.parse(it)) != null }
            }
            selected.copy(sourceUri = indexedSource ?: selected.sourceUri,
                isFavorite = group.any { it.id in favorites || it.isFavorite })
        }
        return AndroidLocalCatalog(merged, aliases)
    }
}

internal data class AndroidLocalCatalog(val tracks: List<Track>, val aliases: Map<String, String>) {
    fun resolve(id: String): String = aliases[id] ?: id

    fun equivalentIds(id: String): Set<String> {
        val canonical = resolve(id)
        return aliases.filterValues { it == canonical }.keys + canonical
    }
}
