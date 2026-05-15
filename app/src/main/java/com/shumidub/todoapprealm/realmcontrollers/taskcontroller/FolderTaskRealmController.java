package com.shumidub.todoapprealm.realmcontrollers.taskcontroller;

import android.util.Log;

import com.shumidub.todoapprealm.App;
import com.shumidub.todoapprealm.realmmodel.task.FolderTaskObject;
import com.shumidub.todoapprealm.realmmodel.RealmFoldersContainer;
import com.shumidub.todoapprealm.realmmodel.task.TaskObject;

import java.util.ArrayList;

import io.realm.RealmList;
import io.realm.RealmQuery;
import io.realm.RealmResults;
import static com.shumidub.todoapprealm.App.realm;

/**
 * Created by Артем on 24.12.2017.
 */

public class FolderTaskRealmController {

    private static RealmQuery<FolderTaskObject> foldersQuery;
    private static RealmResults<FolderTaskObject> folders;

    /** Get all folder */
    public static RealmList<FolderTaskObject> getFoldersList(){
        App.initRealm();
        return App.folderOfTasksListFromContainer;
    }

    /** get folder by id */
    public static FolderTaskObject getFolder(long listId){
        return getFoldersQuery().equalTo("id", listId).findFirst();
    }

    /** add folder */
    public static long addFolder(String name, boolean isDaily){
        long id = getIdForNextValue();
        App.initRealm();
        App.realm.executeTransaction((transaction) -> {
                FolderTaskObject folder = App.realm.createObject(FolderTaskObject.class);
                folder.setId(id);
                folder.setName(name);
                folder.setDaily(isDaily);
//              App.realm.insert(folder);
                App.folderOfTasksListFromContainer.add(folder);
        });
        return id;
    }

    /** edit folder by folderobject */
    public static long editFolder(FolderTaskObject folder, String name, boolean isDaily){
        App.initRealm();
        realm.executeTransaction((transaction)-> {
            folder.setName(name);
            folder.setDaily(isDaily);
        });
        return folder.getId();
    }

    /** edit folder by id */
    public static long editFolder(long id, String name, boolean isDaily){
        App.initRealm();
        FolderTaskObject folder = getFolder(id);
        return editFolder(folder, name, isDaily);
    }

    /** delete folder by folderobject */
    public static void deleteFolder(FolderTaskObject folderObject){
        App.initRealm();
        RealmList<TaskObject> realmList = folderObject.getTasks();

        long folderId = folderObject.getId();

        if (App.realm.isInTransaction()){


                realmList.deleteAllFromRealm();
                App.realm.where(TaskObject.class).equalTo("taskFolderId", folderId).findAll().deleteAllFromRealm();


//            ArrayList<Long> arrayList = new ArrayList<>();
//            for (int i = 0; i<App.folderOfTasksListFromContainer.size(); i++){
//                arrayList.add(App.folderOfTasksListFromContainer.get(i).getId());
//            }
//            Log.d("DTAG24257", "BEFORE DELETING - folderIdArray = : " + arrayList.toString());


                App.folderOfTasksListFromContainer.remove(folderObject);

//            arrayList.clear();
//            for (int i = 0; i<App.folderOfTasksListFromContainer.size(); i++){
//                arrayList.add(App.folderOfTasksListFromContainer.get(i).getId());
//            }
//            Log.d("DTAG24257", "AFTER DELETING - folderIdArray = : " + arrayList.toString());


                folderObject.deleteFromRealm();

                App.realm.where(FolderTaskObject.class).equalTo("id", folderId).findAll().deleteAllFromRealm();



        }else {

            realm.executeTransaction((transaction) -> {
                realmList.deleteAllFromRealm();
                App.realm.where(TaskObject.class).equalTo("taskFolderId", folderId).findAll().deleteAllFromRealm();


//            ArrayList<Long> arrayList = new ArrayList<>();
//            for (int i = 0; i<App.folderOfTasksListFromContainer.size(); i++){
//                arrayList.add(App.folderOfTasksListFromContainer.get(i).getId());
//            }
//            Log.d("DTAG24257", "BEFORE DELETING - folderIdArray = : " + arrayList.toString());


                App.folderOfTasksListFromContainer.remove(folderObject);

//            arrayList.clear();
//            for (int i = 0; i<App.folderOfTasksListFromContainer.size(); i++){
//                arrayList.add(App.folderOfTasksListFromContainer.get(i).getId());
//            }
//            Log.d("DTAG24257", "AFTER DELETING - folderIdArray = : " + arrayList.toString());


                folderObject.deleteFromRealm();

                App.realm.where(FolderTaskObject.class).equalTo("id", folderId).findAll().deleteAllFromRealm();


            });

        }
    }

    /** delete folder by id */
    public static void deleteFolder(long idList){
        App.initRealm();
        FolderTaskObject list = getFoldersQuery().equalTo("id", idList).findFirst();
        deleteFolder(list);
    }

    /** folder is valid */
    public static boolean folderIsExist(FolderTaskObject list){
        App.initRealm();
        return list.isValid();
    }

    /** folder is exist and valid */
    public static boolean folderIsExist(long idList){
        App.initRealm();
        if ( realm.where(FolderTaskObject.class).equalTo("id", idList).findFirst() == null){
            return false;
        }else {
            return realm.where(FolderTaskObject.class).equalTo("id", idList).findFirst().isValid();
        }
    }

    /** folders is not exist, haven,t any folder*/
    public static boolean listOfFolderIsEmpty(){
        App.initRealm();
        return (realm.where(FolderTaskObject.class).findAll() == null
                || realm.where(FolderTaskObject.class).findAll().size() == 0);
    }

    /** containre of folders is exist*/
    public static boolean containerOfFolderIsExist(){
        App.initRealm();
        return (realm.where(RealmFoldersContainer.class).findFirst() != null);
    }

    /** Промежуточный запрос */
    private static RealmQuery<FolderTaskObject> getFoldersQuery(){
        App.initRealm();
        foldersQuery = realm.where(FolderTaskObject.class);
        return foldersQuery;
    }

    /** get unique id*/
    static long getIdForNextValue() {
        long id =  System.currentTimeMillis();
        App.initRealm();
        while ((App.realm.where(FolderTaskObject.class).equalTo("id", id)).findFirst()!=null){
            id ++;
        }
        return id;
    }
}
