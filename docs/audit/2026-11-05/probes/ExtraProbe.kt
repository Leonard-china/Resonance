package qa
import com.resonance.player.platform.*
import com.resonance.player.model.*
import java.nio.file.*
import java.nio.file.attribute.PosixFilePermissions
import kotlinx.coroutines.runBlocking
import org.json.JSONObject

fun main() = runBlocking {
    val root=Files.createTempDirectory("resonance-qa-extra-")
    val previous=System.getProperty("RESONANCE_DATA")
    System.setProperty("RESONANCE_DATA",root.resolve("data").toString())
    val store=DesktopLibraryStore(root.resolve("data"))
    val services=DesktopPlatformServices()
    try {
        val a=Files.writeString(root.resolve("a.mp3"),"generated-a")
        val b=Files.writeString(root.resolve("b.mp3"),"generated-b")
        val art=Files.writeString(root.resolve("album.jpg"),"generated-shared-art")
        val ta=Track("a","A","Test","Album","1:00",0,sourceUri=a.toString(),artworkPath=art.toString())
        val tb=ta.copy(id="b",title="B",sourceUri=b.toString())
        store.save(listOf(ta,tb))
        val deletion=services.deleteLocalTrack(ta)
        val shared=JSONObject().put("deleteSucceeded",deletion.success).put("remainingAudioExists",Files.isRegularFile(b)).put("sharedArtworkExists",Files.exists(art)).put("remainingTrackStillReferencesArtwork",store.load().single().artworkPath==art.toString())
        val locked=Files.createDirectories(root.resolve("locked"))
        val c=Files.writeString(locked.resolve("c.mp3"),"generated-undeletable")
        val tc=ta.copy(id="c",title="C",sourceUri=c.toString(),artworkPath=null)
        store.save(listOf(tc,tb))
        Files.setPosixFilePermissions(locked,PosixFilePermissions.fromString("r-x------"))
        val failed=try { services.deleteLocalTrack(tc) } finally { Files.setPosixFilePermissions(locked,PosixFilePermissions.fromString("rwx------")) }
        val falseSuccess=JSONObject().put("reportedSuccess",failed.success).put("fileDeleted",failed.fileDeleted).put("originalAudioStillExists",Files.exists(c)).put("trackStillInLibrary",store.load().any{it.id==tc.id}).put("message",failed.message)
        println(JSONObject().put("sharedArtworkDeletion",shared).put("failedDeleteRemovesRecord",falseSuccess).toString(2))
    } finally {
        services.close()
        if(previous==null)System.clearProperty("RESONANCE_DATA") else System.setProperty("RESONANCE_DATA",previous)
        require(root.fileName.toString().startsWith("resonance-qa-extra-"))
        root.toFile().deleteRecursively()
    }
}
