package com.shumidub.todoapprealm;

import android.app.Application;
import android.content.Context;
import android.os.Build;
import androidx.annotation.RequiresApi;
import androidx.multidex.MultiDex;
import android.util.Log;

import com.shumidub.todoapprealm.realmcontrollers.ContainersControllers.ContainersRealmController;
import com.shumidub.todoapprealm.realmcontrollers.notescontroller.FolderNotesRealmController;
import com.shumidub.todoapprealm.realmcontrollers.reportcontroller.ReportRealmController;
import com.shumidub.todoapprealm.realmcontrollers.taskcontroller.FolderTaskRealmController;
import com.shumidub.todoapprealm.realmcontrollers.taskcontroller.TasksRealmController;
import com.shumidub.todoapprealm.realmmodel.task.FolderTaskObject;
import com.shumidub.todoapprealm.realmmodel.RealmFoldersContainer;
import com.shumidub.todoapprealm.realmmodel.RealmInteger;
import com.shumidub.todoapprealm.realmmodel.task.TaskObject;
import com.shumidub.todoapprealm.realmmodel.notes.FolderNotesObject;
import com.shumidub.todoapprealm.ui.fragment.task_section.folder_panel_sliding_fragment.fragment.FolderSlidingPanelFragment;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmList;
import io.realm.RealmModel;

import static com.shumidub.todoapprealm.realmcontrollers.taskcontroller.TasksRealmController.addTask;
import static com.shumidub.todoapprealm.realmcontrollers.taskcontroller.TasksRealmController.getTasks;

/**
 * Created by Артем on 19.12.2017.
 *
 */

public class App extends Application {

    static App mApp;

    public static Realm realm;
    public static RealmFoldersContainer realmFoldersContainer;
    public static RealmList<FolderTaskObject> folderOfTasksListFromContainer;
    public static RealmList<FolderTaskObject> folderOfTasksList2FromContainer;
    public static RealmList<FolderTaskObject> folderOfTasksList3FromContainer;
    public static RealmList<FolderNotesObject> folderOfNotesContainerList;
    public static FolderSlidingPanelFragment folderSlidingPanelFragment;
    public static final java.util.List<FolderSlidingPanelFragment> folderSlidingPanelFragments = new java.util.ArrayList<>();


    RealmModel gettedFolderObject;

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


//        addContent(100, 1000);


        if(BuildConfig.DEBUG && FolderTaskRealmController.listOfFolderIsEmpty()) {
            Log.d("DTAG", "onCreate: ");
//            addContent(100, 500);  //= 50 000 notes, tasks and reports
        }
//        else {


//            for (RealmModel folderTask : folderOfTasksListFromContainer){
//
//                List<String> tasks = new ArrayList<>();
//
//                RealmList<TaskObject> taskObjects = ((FolderTaskObject) folderTask).getTasks();
//
//                for (TaskObject t : taskObjects){
//                    tasks.add(t.getText().toString());
//                }
//
//
//                Log.d("DTAG77", "onCreate: folderOfTasks name = "
//                        + ((FolderTaskObject)folderTask).getName() +
//                        " TASKS =  " + tasks );
//            }





//        }
    }

    public static App getApp(){
        return mApp;
    }

    public static void initRealm() {
        if (realm == null) realm = Realm.getDefaultInstance();
    }

    public static void closeRealm(){
        realm = null;
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

        folderOfTasksListFromContainer = realmFoldersContainer.folderOfTasksList;
        folderOfTasksList2FromContainer = realmFoldersContainer.folderOfTasksList2;
        folderOfTasksList3FromContainer = realmFoldersContainer.folderOfTasksList3;
        folderOfNotesContainerList = realmFoldersContainer.folderOfNotesList;
    }


//    private void addContent(int numberFolders, int numberObjects) {
//
//        initRealm();
//
//
//
//        for (int i = 0; i < numberFolders; i++){
//
//            long idFolderTask = FolderTaskRealmController.addFolder("folder " + i, i/2==0);
//            Log.d("DTAG", "addContent: 1    " + i);
//
//
//            for (int i2 = 0; i2 < numberObjects; i2++) {
//                addTask("Notes " + i2, 3, 4, true, 2, idFolderTask);
//                Log.d("DTAG", "addContent: 2    " + i2);
//            }
//
//            long idFolderNote = FolderNotesRealmController.addFolderNote("Note " + i);
//            Log.d("DTAG", "addContent: 3    " + i);
//
//            for (int i2 = 0; i2 < numberObjects; i2++) {
//                FolderNotesRealmController.addNote(idFolderNote, "note " + i2);
//                Log.d("DTAG", "addContent: 4    " + i2);
//            }
//
//
//            for (int i2 = 0; i2 < numberObjects; i2++) {
//                ReportRealmController.addReport("23.03.2012", 100,
//                        "TEXT REPORT #" + i,10,10,
//                        10,9,8,
//                        7,i/3!=0,13);
//                Log.d("DTAG", "addContent: 5    " + i + " " + i2);
//            }
//
//
//
//
//        }


//
//        realm.executeTransaction((Realm realm) -> {
//
//            final FolderTaskObject folderObject
//                    = realm.createObject(FolderTaskObject.class);
//            folderObject.setId(System.currentTimeMillis());
//            folderObject.setName("folderObject 1 daily");
//            folderObject.setDaily(true);
//
//            final FolderTaskObject folderObject2
//                    = realm.createObject(FolderTaskObject.class);
//            folderObject2.setId(System.currentTimeMillis() + 5646465);
//            folderObject2.setName("folderObject 2 not daily");
//            folderObject.setDaily(false);
//
//
//            folderOfTasksListFromContainer.add(folderObject);
//            folderOfTasksListFromContainer.add(folderObject2);
//
//           gettedFolderObject = folderOfTasksListFromContainer.get(0);

//        });
//
//
//        long folderId = ((FolderTaskObject)gettedFolderObject).getId();
//        String folderName = ((FolderTaskObject)gettedFolderObject).getName();
//
//        for (int i = 0; i < 20; i++) {
//            TasksRealmController.addTask("t " + i , 1, 1,
//                    false, 1, folderId);
//            Log.d("DTAG77", "addContent: t " + i + " folder" + folderName );
//        }




//    }

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
