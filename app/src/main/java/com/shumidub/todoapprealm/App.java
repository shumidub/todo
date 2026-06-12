package com.shumidub.todoapprealm;

import android.app.Application;
import android.content.Context;
import android.os.Build;
import androidx.annotation.RequiresApi;
import androidx.multidex.MultiDex;

import com.shumidub.todoapprealm.realmcontrollers.taskcontroller.FolderTaskRealmController;
import com.shumidub.todoapprealm.realmcontrollers.taskcontroller.TasksRealmController;
import com.shumidub.todoapprealm.realmmodel.RealmFoldersContainer;
import com.shumidub.todoapprealm.realmmodel.RealmInteger;
import com.shumidub.todoapprealm.realmmodel.task.TaskObject;
import com.shumidub.todoapprealm.realmmodel.notes.FolderNotesObject;
import com.shumidub.todoapprealm.ui.fragment.task_section.folder_panel_sliding_fragment.fragment.FolderSlidingPanelFragment;

import java.util.Calendar;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmList;

/**
 * Created by Артем on 19.12.2017.
 *
 */

public class App extends Application {

    static App mApp;

    public static Realm realm;
    public static RealmFoldersContainer realmFoldersContainer;
    public static RealmList<FolderNotesObject> folderOfNotesContainerList;
    public static FolderSlidingPanelFragment folderSlidingPanelFragment;
    public static final java.util.List<FolderSlidingPanelFragment> folderSlidingPanelFragments = new java.util.ArrayList<>();

    public static int dayScope;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }


    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onCreate() {
        super.onCreate();

        mApp = this;

        Realm.init(this);
        Realm.setDefaultConfiguration(new RealmConfiguration.Builder()
                .schemaVersion(RealmMigrations.SCHEMA_VERSION)
                .migration(new RealmMigrations())
                .allowWritesOnUiThread(true)
                .allowQueriesOnUiThread(true)
                .build());
        initRealm();
        initContainers();
    }

    public static App getApp(){
        return mApp;
    }

    /**
     * The app keeps a single UI-thread Realm open for the whole process lifetime:
     * the static RealmLists below stay valid only while this instance is open,
     * so it must never be closed or re-acquired (each extra getDefaultInstance()
     * call would leak a reference-counted instance).
     */
    public static void initRealm() {
        if (realm == null) realm = Realm.getDefaultInstance();
    }

    private static void initContainers(){
        App.initRealm();
        realm.executeTransaction((realm) -> {
            if (!FolderTaskRealmController.containerOfFolderIsExist()){
                realmFoldersContainer = realm.createObject(RealmFoldersContainer.class);
            } else {
                realmFoldersContainer = realm.where(RealmFoldersContainer.class).findFirst();
            }

        });

        folderOfNotesContainerList = realmFoldersContainer.folderOfNotesList;
    }

    public static void setDayScopeValue(){
        // done and not done tasks but where countAccumulation more than 0
        List<TaskObject> allDoneAndParticullaryDoneTasks = TasksRealmController.getDoneAndPartiallyDoneTasks();

        int todayDate = Integer.valueOf("" + Calendar.getInstance().get(Calendar.DAY_OF_YEAR) +
                Calendar.getInstance().get(Calendar.YEAR));

        App.dayScope = 0;

        for (TaskObject task : allDoneAndParticullaryDoneTasks) {
            if (task.getLastDoneDate() == todayDate) {
                int equalDateCount = 0;
                for (RealmInteger realmInteger : task.getDateCountAccumulation()) {
                    if (realmInteger.getMyInteger() == todayDate) {
                        equalDateCount++;
                    }
                }
                App.dayScope = App.dayScope + task.getCountValue() * equalDateCount;
            }
        }
    }


    public static FolderSlidingPanelFragment getFolderSlidingPanelFragment(){
        return folderSlidingPanelFragment;
    }

}
