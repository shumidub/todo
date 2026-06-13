package com.shumidub.todoapprealm.sync;

import android.app.Activity;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.shumidub.todoapprealm.App;
import com.shumidub.todoapprealm.realmcontrollers.ContainersControllers.ContainersRealmController;
import com.shumidub.todoapprealm.realmmodel.RealmFoldersContainer;
import com.shumidub.todoapprealm.realmmodel.task.TaskObject;
import com.shumidub.todoapprealm.ui.activity.main.MainActivity;

import io.realm.RealmList;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by A.shumidub on 19.03.18.
 *
 */

public class JsonSyncUtil {

    Activity activity;

    public JsonSyncUtil(Activity activity){
        App.initRealm();
        this.activity = activity;
    }

    public void realmBdToJson(){

        Log.d("DTAG444", "realmBdToJson: ");

        GsonBuilder builder = new GsonBuilder().setPrettyPrinting();
        Gson gson = builder.create();
        String json = gson.toJson(App.realm.copyFromRealm(App.realm.where(RealmFoldersContainer.class).findFirst()));




        FileWritter.saveFile(json);


        if (jsonIsExist()){
            ((MainActivity)activity).showToast("Saved to Download folder as REALM_BD_JSON.txt!");
        } else {
            ((MainActivity)activity).showToast("Error!");
        }
    }

    public boolean jsonIsExist(){
        return  FileWritter.isBackupExist();
    }

    /** Ensure every managed TaskObject has a non-null extraFolderIds list.
     *  Backups produced before multi-category support don't carry the field. */
    private static void normalizeExtraFolderIds() {
        for (TaskObject t : App.realm.where(TaskObject.class).findAll()) {
            if (t.getExtraFolderIds() == null) t.setExtraFolderIds(new RealmList<>());
        }
    }


    public void realmBdFromJsonUri(Uri uri){
        if (uri == null){
            ((MainActivity)activity).showToast("Backup not picked");
            return;
        }

        String json = readJsonFromUri(uri);
        if (TextUtils.isEmpty(json)){
            ((MainActivity)activity).showToast("Picked file is empty or unreadable");
            return;
        }

        realmBdFromJsonString(json);
    }

    /** Restore the whole DB from a JSON string (used by both the SAF picker and the
     *  Firebase import). Replaces current data, then restarts the activity. */
    public void realmBdFromJsonString(String json){
        if (TextUtils.isEmpty(json)){
            ((MainActivity)activity).showToast("Backup is empty");
            return;
        }

        GsonBuilder builder = new GsonBuilder().setPrettyPrinting();
        Gson gson = builder.create();

        App.initRealm();

        App.realm.executeTransaction((transaction) -> {
            ContainersRealmController.deleteFromRealmAllContainers();
            RealmFoldersContainer restored = gson.fromJson(json, RealmFoldersContainer.class);
            App.realm.insertOrUpdate(restored);
            normalizeExtraFolderIds();
            Log.d("DTAG44444", "realm container count = "
                    + App.realm.where(RealmFoldersContainer.class).findAll().size());
        });

        // The restore replaced the RealmFoldersContainer, invalidating the static
        // references the UI holds. Re-point them and refresh the live screens in place —
        // no manual Application.onCreate() / activity relaunch needed.
        App.rebindContainers();
        ((MainActivity)activity).refreshAfterRestore();
        ((MainActivity)activity).showToast("Restored!");
    }

    private String readJsonFromUri(Uri uri){
        try (InputStream is = activity.getContentResolver().openInputStream(uri);
             ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
            if (is == null) return "";
            byte[] chunk = new byte[8192];
            int read;
            while ((read = is.read(chunk)) != -1) buffer.write(chunk, 0, read);
            return buffer.toString("UTF-8");
        } catch (IOException e) {
            Log.e("JsonSyncUtil", "readJsonFromUri failed", e);
            return "";
        }
    }

}
