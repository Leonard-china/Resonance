package com.resonance.player.platform

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract

internal fun exportDirectoryUri(): Uri = DocumentsContract.buildDocumentUri(
    "com.android.externalstorage.documents", "primary:${Environment.DIRECTORY_DOWNLOADS}/Resonance",
)

/** Only a system directory browser receives the fixed export-directory URI, with no URI grant. */
internal fun openAndroidExportDirectory(context: Context, location: String): Boolean {
    val uri = exportDirectoryUri()
    if (location != uri.toString()) return false
    val intent = Intent(Intent.ACTION_VIEW).setDataAndType(uri, DocumentsContract.Document.MIME_TYPE_DIR)
        // A reused FilesActivity can keep its last directory instead of honoring the new URI.
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NEW_DOCUMENT or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
    val preferred = listOf("com.android.documentsui", "com.google.android.documentsui")
    val browsers = context.packageManager.queryIntentActivities(
        intent, PackageManager.MATCH_DEFAULT_ONLY or PackageManager.GET_RESOLVED_FILTER,
    ).filter { resolved ->
        val system = resolved.activityInfo.applicationInfo.flags and
            (ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
        val directoryType = resolved.filter?.typesIterator()?.asSequence()
            ?.any { it == DocumentsContract.Document.MIME_TYPE_DIR } == true
        system && (resolved.activityInfo.packageName in preferred || directoryType)
    }.sortedBy { resolved -> preferred.indexOf(resolved.activityInfo.packageName).let { if (it < 0) preferred.size else it } }
    return browsers.any { resolved ->
        runCatching {
            context.startActivity(Intent(intent).setComponent(ComponentName(resolved.activityInfo.packageName, resolved.activityInfo.name)))
            true
        }.getOrDefault(false)
    }
}
