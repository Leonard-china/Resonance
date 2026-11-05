package com.resonance.player.application

import com.resonance.player.data.PersistenceQueue
import com.resonance.player.model.*
import com.resonance.player.platform.LibraryServices
import com.resonance.player.rematchCatalogTracks

/** Import transactions own the persistence barrier and reload the committed native catalog. */
class LibraryImportUseCase(private val library: LibraryServices, private val writes: PersistenceQueue) {
    data class Result(val report: ImportReport, val tracks: List<Track>?, val playlists: List<Playlist>?)
    suspend fun execute(convertToMp3: Boolean, paths: List<String>?, currentPlaylists: () -> List<Playlist>): Result {
        writes.run { }
        val report = if (paths == null) library.importMusic(convertToMp3) else library.importMusicPaths(paths, convertToMp3)
        if (report.tracks.isEmpty() && report.playlists.isEmpty()) return Result(report, null, null)
        val tracks = library.loadLibrary()
        val playlists = if (report.importedPackageCount > 0) library.loadPlaylists() else currentPlaylists()
        return Result(report, tracks, rematchCatalogTracks(playlists, tracks))
    }
}
