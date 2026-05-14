package com.shumidub.todoapprealm.data.realm

import android.content.ContentValues
import android.content.Context
import android.provider.MediaStore
import android.util.Log
import io.realm.Realm
import io.realm.RealmConfiguration
import io.realm.exceptions.RealmMigrationNeededException
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object RealmBackup {

    private const val TAG = "RealmBackup"

    /**
     * Probes the Realm file with a no-migration config. If the on-disk schema
     * differs from the in-code schema (a migration would be needed), copies the
     * current file to the public Downloads directory before returning so the
     * caller can proceed with the real migration.
     *
     * Safe to call before any other Realm operation. Does nothing if the file
     * doesn't exist or already matches the current schema.
     */
    fun backupIfMigrationNeeded(context: Context) {
        val probe = RealmConfiguration.Builder()
            .schemaVersion(REALM_SCHEMA_VERSION)
            .build()
        try {
            Realm.getInstance(probe).use { /* schemas match — nothing to do */ }
        } catch (e: RealmMigrationNeededException) {
            backup(context, reason = "schema-mismatch")
        } catch (e: Throwable) {
            Log.w(TAG, "Probe failed; skipping backup", e)
        }
    }

    private fun backup(context: Context, reason: String) {
        val src = File(context.filesDir, Realm.DEFAULT_REALM_NAME)
        if (!src.exists()) return

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val displayName = "todoapprealm_${reason}_$timestamp.realm"

        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, displayName)
            put(MediaStore.Downloads.MIME_TYPE, "application/octet-stream")
            put(MediaStore.Downloads.IS_PENDING, 1)
        }

        val uri = context.contentResolver.insert(
            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            values,
        ) ?: run {
            Log.w(TAG, "MediaStore insert returned null; backup skipped")
            return
        }

        try {
            context.contentResolver.openOutputStream(uri)?.use { out ->
                src.inputStream().use { it.copyTo(out) }
            }
            context.contentResolver.update(
                uri,
                ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) },
                null,
                null,
            )
            Log.i(TAG, "Realm backed up to Downloads/$displayName")
        } catch (e: Throwable) {
            Log.e(TAG, "Backup write failed; deleting partial file", e)
            context.contentResolver.delete(uri, null, null)
        }
    }
}
