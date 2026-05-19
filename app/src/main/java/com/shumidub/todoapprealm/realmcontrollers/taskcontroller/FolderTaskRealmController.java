package com.shumidub.todoapprealm.realmcontrollers.taskcontroller;

import android.util.Log;

import com.shumidub.todoapprealm.App;
import com.shumidub.todoapprealm.realmmodel.task.FolderTaskObject;
import com.shumidub.todoapprealm.realmmodel.RealmFoldersContainer;
import com.shumidub.todoapprealm.realmmodel.task.TaskObject;

import java.util.ArrayList;
import java.util.Iterator;

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

    /** Get folders for tab 0 (legacy callers). */
    public static RealmList<FolderTaskObject> getFoldersList(){
        return getFoldersList(0);
    }

    /** Get folders for a given tab. group=0 → Tasks1, group=1 → Tasks2, group=2 → Tasks3. */
    public static RealmList<FolderTaskObject> getFoldersList(int group){
        App.initRealm();
        switch (group) {
            case 1: return App.folderOfTasksList2FromContainer;
            case 2: return App.folderOfTasksList3FromContainer;
            default: return App.folderOfTasksListFromContainer;
        }
    }

    /** Tab index (0, 1, or 2) the folder lives on. -1 if not in any container list. */
    public static int getFolderGroup(FolderTaskObject folder){
        if (folder == null) return -1;
        if (App.folderOfTasksListFromContainer != null
                && App.folderOfTasksListFromContainer.contains(folder)) return 0;
        if (App.folderOfTasksList2FromContainer != null
                && App.folderOfTasksList2FromContainer.contains(folder)) return 1;
        if (App.folderOfTasksList3FromContainer != null
                && App.folderOfTasksList3FromContainer.contains(folder)) return 2;
        return -1;
    }

    /** All folders across all tabs (Tasks1 first, then Tasks2, then Tasks3). */
    public static java.util.List<FolderTaskObject> getAllFolders(){
        App.initRealm();
        java.util.List<FolderTaskObject> all = new ArrayList<>();
        if (App.folderOfTasksListFromContainer != null) all.addAll(App.folderOfTasksListFromContainer);
        if (App.folderOfTasksList2FromContainer != null) all.addAll(App.folderOfTasksList2FromContainer);
        if (App.folderOfTasksList3FromContainer != null) all.addAll(App.folderOfTasksList3FromContainer);
        return all;
    }

    /** get folder by id */
    public static FolderTaskObject getFolder(long listId){
        return getFoldersQuery().equalTo("id", listId).findFirst();
    }

    /** add folder to tab 0 (legacy). */
    public static long addFolder(String name, boolean isDaily){
        return addFolder(name, isDaily, 0);
    }

    /** add folder to a specific tab. */
    public static long addFolder(String name, boolean isDaily, int group){
        long id = getIdForNextValue();
        App.initRealm();
        App.realm.executeTransaction((transaction) -> {
            FolderTaskObject folder = App.realm.createObject(FolderTaskObject.class);
            folder.setId(id);
            folder.setName(name);
            folder.setDaily(isDaily);
            getFoldersList(group).add(folder);
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

    /** Move folder to target tab. Appends to the destination list and removes from the other. */
    public static void moveFolderToGroup(FolderTaskObject folder, int targetGroup){
        if (folder == null) return;
        int current = getFolderGroup(folder);
        if (current == targetGroup) return;
        App.initRealm();
        App.realm.executeTransaction((r) -> {
            if (App.folderOfTasksListFromContainer != null) App.folderOfTasksListFromContainer.remove(folder);
            if (App.folderOfTasksList2FromContainer != null) App.folderOfTasksList2FromContainer.remove(folder);
            if (App.folderOfTasksList3FromContainer != null) App.folderOfTasksList3FromContainer.remove(folder);
            getFoldersList(targetGroup).add(folder);
        });
    }

    /** delete folder by folderobject */
    public static void deleteFolder(FolderTaskObject folderObject){
        App.initRealm();
        long folderId = folderObject.getId();

        Runnable body = () -> {
            // For every task currently in this folder: either fully delete it
            // (this folder was its only category) or detach it from this folder
            // (the task survives in its remaining categories).
            RealmList<TaskObject> tasks = folderObject.getTasks();
            if (tasks != null) {
                ArrayList<TaskObject> snapshot = new ArrayList<>(tasks);
                for (TaskObject task : snapshot) detachOrDeleteTaskFromFolder(task, folderId);
            }
            // Catch direct-orphan tasks whose primary still points to this folder
            // but which somehow aren't in the folder's task list.
            ArrayList<TaskObject> orphans = new ArrayList<>(
                    App.realm.where(TaskObject.class).equalTo("taskFolderId", folderId).findAll());
            for (TaskObject task : orphans) detachOrDeleteTaskFromFolder(task, folderId);

            if (App.folderOfTasksListFromContainer != null) {
                App.folderOfTasksListFromContainer.remove(folderObject);
            }
            if (App.folderOfTasksList2FromContainer != null) {
                App.folderOfTasksList2FromContainer.remove(folderObject);
            }
            if (App.folderOfTasksList3FromContainer != null) {
                App.folderOfTasksList3FromContainer.remove(folderObject);
            }
            folderObject.deleteFromRealm();
            App.realm.where(FolderTaskObject.class).equalTo("id", folderId).findAll().deleteAllFromRealm();
        };

        if (App.realm.isInTransaction()) {
            body.run();
        } else {
            realm.executeTransaction((transaction) -> body.run());
        }
    }

    /** Must run inside a Realm transaction. */
    private static void detachOrDeleteTaskFromFolder(TaskObject task, long folderId) {
        if (task == null || !task.isValid()) return;

        // Build the list of categories this task should keep
        java.util.List<Long> remaining = new ArrayList<>();
        if (task.getTaskFolderId() != folderId) remaining.add(task.getTaskFolderId());
        RealmList<Long> extras = task.getExtraFolderIds();
        if (extras != null) {
            for (Long id : extras) {
                if (id != null && id != folderId && !remaining.contains(id)) remaining.add(id);
            }
        }

        if (remaining.isEmpty()) {
            if (task.getDateCountAccumulation() != null) task.getDateCountAccumulation().clear();
            if (extras != null) extras.clear();
            task.deleteFromRealm();
            return;
        }

        task.setTaskFolderId(remaining.get(0));
        if (task.getExtraFolderIds() == null) {
            task.setExtraFolderIds(new RealmList<>());
        }
        RealmList<Long> managedExtras = task.getExtraFolderIds();
        managedExtras.clear();
        for (int i = 1; i < remaining.size(); i++) managedExtras.add(remaining.get(i));
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
