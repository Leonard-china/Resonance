package com.resonance.player

import androidx.test.platform.app.InstrumentationRegistry
import android.provider.DocumentsContract
import android.content.ContextWrapper
import android.content.Intent
import com.resonance.player.platform.exportDirectoryUri
import com.resonance.player.platform.openAndroidExportDirectory
import org.junit.Assert.*
import org.junit.Test

class AndroidExportLocationTest {
    @Test fun folderLaunchUsesAFreshDocumentTaskAndCarriesNoStorageGrant() {
        val base = InstrumentationRegistry.getInstrumentation().targetContext
        var launched: Intent? = null
        val context = object : ContextWrapper(base) {
            override fun startActivity(intent: Intent) { launched = intent }
        }
        assertTrue(openAndroidExportDirectory(context, exportDirectoryUri().toString()))
        val intent = requireNotNull(launched)
        assertEquals(Intent.ACTION_VIEW, intent.action)
        assertEquals(exportDirectoryUri(), intent.data)
        assertEquals(DocumentsContract.Document.MIME_TYPE_DIR, intent.type)
        val taskFlags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NEW_DOCUMENT or Intent.FLAG_ACTIVITY_MULTIPLE_TASK
        assertEquals(taskFlags, intent.flags and taskFlags)
        assertEquals(0, intent.flags and (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION))
        assertNotNull(intent.component)
    }

    @Test fun directoryUriTargetsTheExportSubfolderAndOtherLocationsAreRejected() {
        val uri = exportDirectoryUri()
        assertEquals("content", uri.scheme)
        assertEquals("com.android.externalstorage.documents", uri.authority)
        assertEquals("primary:Download/Resonance", DocumentsContract.getDocumentId(uri))
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertFalse(openAndroidExportDirectory(context, "file:///sdcard/Download"))
        assertFalse(openAndroidExportDirectory(context, "content://untrusted.example/document/other"))
    }
}
