package com.shumidub.todoapprealm.realmcontrollers.taskcontroller;

import com.shumidub.todoapprealm.App;
import com.shumidub.todoapprealm.Tabs;
import com.shumidub.todoapprealm.realmcontrollers.RealmDb;
import com.shumidub.todoapprealm.realmmodel.task.FolderTaskObject;
import com.shumidub.todoapprealm.realmmodel.RealmFoldersContainer;
import com.shumidub.todoapprealm.realmmodel.task.TaskObject;

import java.util.ArrayList;
import java.util.Iterator;

import io.realm.RealmList;

/**
 * Created by Артем on 24.12.2017.
 */

public class FolderTaskRealmController {

    /** Get folders for tab 0 (legacy callers). */
    public static RealmList<FolderTaskObject> getFoldersList(){
        return getFoldersList(0);
    }

    /** Get folders for a given task group (0..3). */
    public static RealmList<FolderTaskObject> getFoldersList(int group){
        RealmDb.realm();
        return App.realmFoldersContainer.tasksListForGroup(group);
    }

    /** Task group (0..3) the folder lives on. -1 if not in any container list. */
    public static int getFolderGroup(FolderTaskObject folder){
        if (folder == null) return -1;
        for (int g = 0; g < Tabs.GROUP_COUNT; g++) {
            RealmList<FolderTaskObject> list = getFoldersList(g);
            if (list != null && list.contains(folder)) return g;
        }
        return -1;
    }

    /** All folders across all tabs, in group order. */
    public static java.util.List<FolderTaskObject> getAllFolders(){
        java.util.List<FolderTaskObject> all = new ArrayList<>();
        for (int g = 0; g < Tabs.GROUP_COUNT; g++) {
            RealmList<FolderTaskObject> list = getFoldersList(g);
            if (list != null) all.addAll(list);
        }
        return all;
    }

    /** get folder by id */
    public static FolderTaskObject getFolder(long listId){
        return RealmDb.findById(FolderTaskObject.class, listId);
    }

    /** add folder to tab 0 (legacy). */
    public static long addFolder(String name, boolean isDaily){
        return addFolder(name, isDaily, 0);
    }

    /** add folder to a specific tab. */
    public static long addFolder(String name, boolean isDaily, int group){
        long id = getIdForNextValue();
        RealmDb.write(() -> {
            FolderTaskObject folder = RealmDb.realm().createObject(FolderTaskObject.class);
            folder.setId(id);
            folder.setName(name);
            folder.setDaily(isDaily);
            getFoldersList(group).add(folder);
        });
        return id;
    }

    /** edit folder by folderobject */
    public static long editFolder(FolderTaskObject folder, String name, boolean isDaily){
        RealmDb.write(() -> {
            folder.setName(name);
            folder.setDaily(isDaily);
        });
        return folder.getId();
    }

    /** edit folder by id */
    public static long editFolder(long id, String name, boolean isDaily){
        FolderTaskObject folder = getFolder(id);
        return editFolder(folder, name, isDaily);
    }

    /** Move folder to target tab. Appends to the destination list and removes from the other. */
    public static void moveFolderToGroup(FolderTaskObject folder, int targetGroup){
        if (folder == null) return;
        int current = getFolderGroup(folder);
        if (current == targetGroup) return;
        RealmDb.write(() -> {
            removeFromAllGroups(folder);
            getFoldersList(targetGroup).add(folder);
        });
    }

    /** Reorder this tab's folders to match the given id order (drag-and-drop). */
    public static void reorderFolders(int group, java.util.List<Long> orderedIds){
        if (orderedIds == null) return;
        RealmDb.write(() -> {
            RealmList<FolderTaskObject> list = getFoldersList(group);
            if (list == null) return;
            for (int target = 0; target < orderedIds.size() && target < list.size(); target++) {
                long id = orderedIds.get(target);
                int cur = -1;
                for (int i = target; i < list.size(); i++) {
                    FolderTaskObject f = list.get(i);
                    if (f != null && f.getId() == id) { cur = i; break; }
                }
                if (cur > target) list.move(cur, target);
            }
        });
    }

    /** delete folder by folderobject */
    public static void deleteFolder(FolderTaskObject folderObject){
        long folderId = folderObject.getId();

        RealmDb.write(() -> {
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
                    RealmDb.realm().where(TaskObject.class).equalTo("taskFolderId", folderId).findAll());
            for (TaskObject task : orphans) detachOrDeleteTaskFromFolder(task, folderId);

            removeFromAllGroups(folderObject);
            folderObject.deleteFromRealm();
            RealmDb.realm().where(FolderTaskObject.class).equalTo("id", folderId).findAll().deleteAllFromRealm();
        });
    }

    /** Must run inside a Realm transaction. */
    private static void removeFromAllGroups(FolderTaskObject folder) {
        for (int g = 0; g < Tabs.GROUP_COUNT; g++) {
            RealmList<FolderTaskObject> list = getFoldersList(g);
            if (list != null) list.remove(folder);
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
        deleteFolder(getFolder(idList));
    }

    /** folder is valid */
    public static boolean folderIsExist(FolderTaskObject list){
        return list.isValid();
    }

    /** folder is exist and valid */
    public static boolean folderIsExist(long idList){
        FolderTaskObject folder = getFolder(idList);
        return folder != null && folder.isValid();
    }

    /** folders is not exist, haven,t any folder*/
    public static boolean listOfFolderIsEmpty(){
        return RealmDb.realm().where(FolderTaskObject.class).findAll().isEmpty();
    }

    /** containre of folders is exist*/
    public static boolean containerOfFolderIsExist(){
        return RealmDb.realm().where(RealmFoldersContainer.class).findFirst() != null;
    }

    /** get unique id*/
    static long getIdForNextValue() {
        return RealmDb.newUniqueId(FolderTaskObject.class);
    }
}
