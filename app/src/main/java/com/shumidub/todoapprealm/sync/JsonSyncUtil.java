package com.shumidub.todoapprealm.sync;

import android.app.Activity;
import android.content.Intent;
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

//        Intent sendIntent = new Intent();
//        sendIntent.setAction(Intent.ACTION_SEND);
//        sendIntent.putExtra(Intent.EXTRA_TEXT, json);
//        sendIntent.setType("text/plain");
//        activity.startActivity(sendIntent);

    }

    public void realmBdFromJson(){



        if (!jsonIsExist()){
            ((MainActivity)activity).showToast("Backup file (REALM_BD_JSON.txt) not found on Download folder");
            return;
        } else {


            String json = FileWritter.readFile();

            Log.d("DTAG44444", "realmBdFromJson: " + json);

//            if (!TextUtils.isEmpty(json)) {
                GsonBuilder builder = new GsonBuilder().setPrettyPrinting();
                Gson gson = builder.create();

                App.initRealm();

                App.realm.executeTransaction((transaction) -> {


                    ContainersRealmController.deleteFromRealmAllContainers();

                    RealmFoldersContainer realmFoldersContainer2 = gson.fromJson(json, RealmFoldersContainer.class);

//                    realmFoldersContainer
//                            .setFolderOfNotesList(
//                                    realmFoldersContainer2
//                                            .getFolderOfNotesList());
//
//                    realmFoldersContainer
//                            .setFolderOfTasksList(
//                                    realmFoldersContainer2
//                                            .getFolderOfTasksList());

                    App.realm.insertOrUpdate(realmFoldersContainer2);

                    App.realmFoldersContainer = App.realm.where(RealmFoldersContainer.class).findFirst();
                    normalizeExtraFolderIds();

                    Log.d("DTAG44444", "realm container count =  "
                            + App.realm.where(RealmFoldersContainer.class).findAll().size());


//                    realmFoldersContainer2 = null;


//                    App.realm.copyToRealm(realmFoldersContainer);

                });


                ((MainActivity)activity).showToast("Restored!");

                App.getApp().onCreate();


                //todo !!!!!!!!!!!!

//                ((MainActivity) activity).finishAndRemoveTask();
                ((MainActivity) activity).finish();

                ((MainActivity) activity).resetAllView();

                App.getApp().onCreate();
                Intent intent = new Intent(activity, MainActivity.class);
                activity.startActivity(intent);



//                ((MainActivity) activity).onCreateActions();



//            }else{
//                ((MainActivity)activity).showToast("File is empty!");
//            }



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

        GsonBuilder builder = new GsonBuilder().setPrettyPrinting();
        Gson gson = builder.create();

        App.initRealm();

        App.realm.executeTransaction((transaction) -> {
            ContainersRealmController.deleteFromRealmAllContainers();
            RealmFoldersContainer restored = gson.fromJson(json, RealmFoldersContainer.class);
            App.realm.insertOrUpdate(restored);
            App.realmFoldersContainer = App.realm.where(RealmFoldersContainer.class).findFirst();
            normalizeExtraFolderIds();
            Log.d("DTAG44444", "realm container count = "
                    + App.realm.where(RealmFoldersContainer.class).findAll().size());
        });

        ((MainActivity)activity).showToast("Restored!");

        App.getApp().onCreate();
        ((MainActivity)activity).finish();
        ((MainActivity)activity).resetAllView();
        App.getApp().onCreate();
        Intent intent = new Intent(activity, MainActivity.class);
        activity.startActivity(intent);
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
