package com.shumidub.todoapprealm.sync;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import com.shumidub.todoapprealm.App;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class FileWritter {

    private static final String TAG = "FileWritter";
    private static final String FILE_NAME = "REALM_BD_JSON.txt";
    private static final String MIME_TYPE = "text/plain";

    public static void saveFile(String json) {
        Context ctx = App.getApp();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveViaMediaStore(ctx, json);
        } else {
            saveLegacy(json);
        }
    }

    public static String readFile() {
        Context ctx = App.getApp();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return readViaMediaStore(ctx);
        }
        return readLegacy();
    }

    public static boolean isBackupExist() {
        Context ctx = App.getApp();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return findDownloadUri(ctx) != null;
        }
        File file = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS), FILE_NAME);
        return file.exists();
    }

    private static void saveViaMediaStore(Context ctx, String json) {
        ContentResolver resolver = ctx.getContentResolver();
        Uri existing = findDownloadUri(ctx);
        Uri target;
        boolean newlyInserted = false;

        if (existing != null) {
            target = existing;
            ContentValues pending = new ContentValues();
            pending.put(MediaStore.Downloads.IS_PENDING, 1);
            resolver.update(target, pending, null, null);
        } else {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Downloads.DISPLAY_NAME, FILE_NAME);
            values.put(MediaStore.Downloads.MIME_TYPE, MIME_TYPE);
            values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
            values.put(MediaStore.Downloads.IS_PENDING, 1);
            target = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            newlyInserted = true;
        }

        if (target == null) {
            Log.e(TAG, "MediaStore insert returned null");
            return;
        }

        try (OutputStream os = resolver.openOutputStream(target, "wt")) {
            if (os == null) throw new IOException("openOutputStream returned null");
            os.write(json.getBytes("UTF-8"));
            os.flush();
        } catch (IOException e) {
            Log.e(TAG, "saveViaMediaStore failed", e);
            if (newlyInserted) resolver.delete(target, null, null);
            return;
        }

        ContentValues done = new ContentValues();
        done.put(MediaStore.Downloads.IS_PENDING, 0);
        resolver.update(target, done, null, null);
        Log.d(TAG, "Saved to Downloads via MediaStore: " + target);
    }

    private static String readViaMediaStore(Context ctx) {
        Uri uri = findDownloadUri(ctx);
        if (uri == null) return "";

        try (InputStream is = ctx.getContentResolver().openInputStream(uri);
             ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
            if (is == null) return "";
            byte[] chunk = new byte[8192];
            int read;
            while ((read = is.read(chunk)) != -1) {
                buffer.write(chunk, 0, read);
            }
            return buffer.toString("UTF-8");
        } catch (IOException e) {
            Log.e(TAG, "readViaMediaStore failed", e);
            return "";
        }
    }

    private static Uri findDownloadUri(Context ctx) {
        Uri collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI;
        String[] projection = {MediaStore.Downloads._ID};
        String selection = MediaStore.Downloads.DISPLAY_NAME + " = ? AND "
                + MediaStore.Downloads.RELATIVE_PATH + " LIKE ?";
        String[] args = {FILE_NAME, Environment.DIRECTORY_DOWNLOADS + "/%"};

        try (Cursor cursor = ctx.getContentResolver().query(
                collection, projection, selection, args, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Downloads._ID));
                return ContentUris.withAppendedId(collection, id);
            }
        }
        return null;
    }

    private static void saveLegacy(String json) {
        File file = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS), FILE_NAME);
        try (FileWriter writer = new FileWriter(file, false)) {
            writer.write(json);
            Log.d(TAG, "Saved to " + file);
        } catch (IOException e) {
            Log.e(TAG, "saveLegacy failed", e);
        }
    }

    private static String readLegacy() {
        File file = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS), FILE_NAME);
        if (!file.exists()) return "";

        byte[] bytes = new byte[(int) file.length()];
        try (FileInputStream in = new FileInputStream(file)) {
            int read = in.read(bytes);
            if (read != bytes.length) {
                Log.w(TAG, "readLegacy: short read " + read + "/" + bytes.length);
            }
        } catch (FileNotFoundException e) {
            return "";
        } catch (IOException e) {
            Log.e(TAG, "readLegacy failed", e);
            return "";
        }
        return new String(bytes);
    }
}
