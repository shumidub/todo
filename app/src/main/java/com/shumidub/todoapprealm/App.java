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
        // Deliberate debt: the whole app reads and writes Realm on the main thread
        // (controllers are synchronous statics, adapters bind managed objects directly).
        // These flags suppress Realm's thread guards so that works. The cost is that a
        // large query/write can block the UI — acceptable at the current data scale, but
        // the real fix is moving reads to background threads / async queries, not flipping
        // these off (that would crash every existing call site).
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

        rebindContainers();
    }

    /**
     * Re-point the static container references at the current Realm state. Call after a
     * restore replaces the {@link RealmFoldersContainer} so the old (now-invalidated)
     * references don't leak into the UI. Does NOT re-run Realm.init / re-create the
     * activity — the UI is refreshed separately.
     */
    public static void rebindContainers(){
        App.initRealm();
        realmFoldersContainer = realm.where(RealmFoldersContainer.class).findFirst();
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


}
