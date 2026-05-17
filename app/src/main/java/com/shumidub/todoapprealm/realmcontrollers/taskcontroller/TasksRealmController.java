package com.shumidub.todoapprealm.realmcontrollers.taskcontroller;

import android.util.Log;
import com.shumidub.todoapprealm.App;
import com.shumidub.todoapprealm.realmmodel.task.FolderTaskObject;
import com.shumidub.todoapprealm.realmmodel.task.TaskObject;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import io.reactivex.annotations.NonNull;
import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmResults;
import io.realm.Sort;

/**
 * Created by Артем on 21.12.2017.
 */

public class TasksRealmController {

    //GET TASKS
    /** get all tasks, without folder id !!! if folder if == null !!! do not happend */
    public static List<TaskObject> getTasks(){
        App.initRealm();



        return App.realm.where(TaskObject.class).findAll()
                .sort("done", Sort.ASCENDING, "id",Sort.ASCENDING);
    }

    /** get not done tasks , without folder id !!! if folder if == null !!! do not happend*/
    public static List<TaskObject> getNotDoneTasks(){
        App.initRealm();
        return App.realm.where(TaskObject.class)
                .equalTo("done", false)
                .findAll()
                .sort("done", Sort.ASCENDING, "id",Sort.ASCENDING);
    }

    /** get done tasks , without folder id !!! if folder if == null !!! do not happend */
    public static List<TaskObject> getDoneTasks(){
        App.initRealm();
        return App.realm.where(TaskObject.class)
                .equalTo("done", true)
                .findAll()
                .sort("done", Sort.ASCENDING, "id",Sort.ASCENDING);
    }

    /** done and not done tasks but where countAccumulation more than 0
     * use for reset daily count value
     */
    public static List<TaskObject> getDoneAndPartiallyDoneTasks(){
        App.initRealm();
        return App.realm.where(TaskObject.class)
                .notEqualTo("countAccumulation", 0)
                .findAll()
                .sort("done", Sort.ASCENDING, "id",Sort.ASCENDING);
    }

    /** get tasks by folder id*/
    public static RealmResults<TaskObject> getTasks(long folderId){
        App.initRealm();
        //todo changed!!! new need test + need think and add about sort


//       TaskObject task = App.realm.where(TaskObject.class).findFirst();
//       rlto.indexOf(task);

   // task-002: sort by (done ASC, position ASC) so drag-order is preserved per section/free
   return getFolderTasksRealmListFromFolder(folderId).sort(
           new String[]{"done", "position"},
           new Sort[]{Sort.ASCENDING, Sort.ASCENDING});
    }

    /** get not done tasks by id*/
    public static List<TaskObject> getNotDoneTasks(long folderId){
        App.initRealm();
        return getFolderTasksRealmListFromFolder(folderId)
                .where().equalTo("done", false).findAll()
                .sort("position", Sort.ASCENDING);
    }

    /** get done tasks by id*/
    public static List<TaskObject> getDoneTasks(long folderId){
        App.initRealm();
        return getFolderTasksRealmListFromFolder(folderId)
                .where().equalTo("done", true).findAll()
                .sort("position", Sort.ASCENDING);
    }

    /** get done and not done tasks but where countAccumulation more than 0 */
    public static List<TaskObject> getDoneAndPartiallyDoneTasks(long folderId){
        App.initRealm();
        return App.realm.where(TaskObject.class)
                .equalTo("taskFolderId", folderId)
                .notEqualTo("countAccumulation", 0)
                .findAll()
                .sort("done", Sort.ASCENDING, "id",Sort.ASCENDING);
    }

    //SINGLE TASK
    /** get task by id*/
    public static TaskObject getTask(long idTask){
        App.initRealm();
        return App.realm.where(TaskObject.class).equalTo("id", idTask).findFirst();
    }

    /** add task*/
    public static  void addTask(String text, int count, int maxAccumulation, boolean cycling, int priority, long taskFolderId ){
        App.initRealm();
        App.realm.executeTransaction((transaction) -> {
            TaskObject task = App.realm.createObject(TaskObject.class);
            long id = getIdForNextValue();
            task.setId(id);
            task.setText(text);
            task.setLastDoneDate(0);
            task.setPriority(priority);
            task.setTaskFolderId(taskFolderId);
            task.setCountValue(count);
            task.setMaxAccumulation(maxAccumulation);
            task.setCountAccumulation(0);
            task.setCycling(cycling);
            // task-002: stamp position so the new task lands at the end of the outer list.
            task.setSectionId(0);
            task.setPosition(SectionsRealmController.nextOuterPosition(taskFolderId));
            FolderTaskRealmController.getFolder(taskFolderId).folderTasks.add(task);
//          App.realm.insert(task);
        });
    }

    public static  void editTask(TaskObject task, String text, @NonNull int count, @NonNull int maxAccumulation, @NonNull boolean cycling, @NonNull int priority ){
        App.initRealm();
        App.realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                if (!text.isEmpty()) task.setText(text);
                task.setPriority(priority);
                task.setCountValue(count);
                task.setMaxAccumulation(maxAccumulation);
                task.setCycling(cycling);
            }
        });
    }

    public static void setTaskDoneOrParticullaryDone(TaskObject task, boolean done){
        App.initRealm();
        App.realm.executeTransaction((transaction)-> {
            if(done == false){
                task.setDone(done);
                task.clearDateCountAccumulation();
                task.setLastDoneDate(0);
            }

            if (done) {
                Calendar cal = Calendar.getInstance();
                int date = Integer.valueOf("" + cal.get(Calendar.DAY_OF_YEAR) + cal.get(Calendar.YEAR));

                task.addDateCountAccumulation(date);
                task.setLastDoneDate(date);

                if (task.getCountAccumulation() >= task.getMaxAccumulation()){
                    task.setDone(done);
                }
            }
        });
    }

    /**delete task*/
    public static void deleteTask(TaskObject task) {
        App.initRealm();
        long taskId = task.getId();
        String taskText = task.getText();

        Runnable body = () -> {
            // remove the task from every folder that references it (primary + extras)
            for (FolderTaskObject folder : App.realm.where(FolderTaskObject.class).findAll()) {
                if (folder.getTasks() != null && folder.getTasks().contains(task)) {
                    folder.getTasks().remove(task);
                }
            }
            if (task.isValid()) {
                task.getDateCountAccumulation().clear();
                if (task.getExtraFolderIds() != null) task.getExtraFolderIds().clear();
                task.deleteFromRealm();
            }
        };

        if (App.realm.isInTransaction()) {
            body.run();
        } else {
            App.realm.executeTransaction((transaction) -> body.run());
        }

        if (App.realm.where(TaskObject.class).equalTo("id", taskId).findFirst() == null){
            Log.d("DEBUG_TAG", "TASK: " + taskText + " id:" + taskId + " DELETED" );
        }else{
            Log.d("DEBUG_TAG", "TASK: " + taskText + " id:" + taskId + " NOT DELETED !!!" );
        }
    }

    /**delete task by id*/
    public static void deleteTask(long id){
        deleteTask(App.realm.where(TaskObject.class).equalTo("id", id).findFirst());
    }

    /**
     * @deprecated task-002: use {@link SectionsRealmController#reorderItems} which honours
     * section membership and the new {@code position} field. Kept for legacy callers in sync code.
     */
    @Deprecated
    public static void changeOrder(long folderId, TaskObject taskObjectTarget , TaskObject taskObjectTargetPosition){
        RealmList<TaskObject> taskList = getFolderTasksRealmListFromFolder(folderId);
        int from = taskList.indexOf(taskObjectTarget);
        int to  = taskList.indexOf(taskObjectTargetPosition);
        taskList.add(to, taskList.remove(from));


        //todo не сбрасывается from он становится равен предыдущему to?
        Log.d("DTAG488", String.format("onMove: from %d  to %d ", from, to));

    }

    /** get unique id*/
    private static long getIdForNextValue(){
        long id =  System.currentTimeMillis();
        App.initRealm();
        while ((App.realm.where(TaskObject.class).equalTo("id", id)).findFirst()!=null){
            id ++;
        }
        return id;
    }

    @SuppressWarnings("All")
    public static RealmList<TaskObject> getFolderTasksRealmListFromFolder (long folderId){
        return ((FolderTaskObject) App.realm.where(FolderTaskObject.class)
                .equalTo("id", folderId)
                .findFirst())
                .folderTasks;
    }

    /** All folder ids this task belongs to — primary first, then extras. Never empty. */
    public static List<Long> getCategoryIds(TaskObject task) {
        List<Long> ids = new ArrayList<>();
        ids.add(task.getTaskFolderId());
        RealmList<Long> extras = task.getExtraFolderIds();
        if (extras != null) {
            for (Long id : extras) {
                if (id != null && id != task.getTaskFolderId() && !ids.contains(id)) ids.add(id);
            }
        }
        return ids;
    }

    /**
     * Set the full list of folders this task belongs to.
     * The first id in {@code folderIds} becomes the primary {@code taskFolderId};
     * the rest are stored as extras. Existing folders not in the list lose the task.
     */
    public static void setTaskCategories(TaskObject task, List<Long> folderIds) {
        if (folderIds == null || folderIds.isEmpty()) return;
        App.initRealm();

        // de-dupe preserving order
        Set<Long> ordered = new LinkedHashSet<>(folderIds);
        List<Long> finalIds = new ArrayList<>(ordered);

        App.realm.executeTransaction((r) -> {
            long newPrimary = finalIds.get(0);
            Set<Long> newSet = new HashSet<>(finalIds);

            // remove from folders no longer assigned
            for (FolderTaskObject folder : App.realm.where(FolderTaskObject.class).findAll()) {
                long fid = folder.getId();
                boolean contains = folder.getTasks() != null && folder.getTasks().contains(task);
                if (contains && !newSet.contains(fid)) {
                    folder.getTasks().remove(task);
                }
            }

            // add to folders newly assigned
            for (Long fid : finalIds) {
                FolderTaskObject folder = FolderTaskRealmController.getFolder(fid);
                if (folder == null) continue;
                if (folder.getTasks() == null) continue;
                if (!folder.getTasks().contains(task)) folder.getTasks().add(task);
            }

            task.setTaskFolderId(newPrimary);

            if (task.getExtraFolderIds() == null) {
                task.setExtraFolderIds(new RealmList<>());
            }
            RealmList<Long> extras = task.getExtraFolderIds();
            extras.clear();
            for (int i = 1; i < finalIds.size(); i++) {
                extras.add(finalIds.get(i));
            }
        });
    }

    public static void setTaskPriority(TaskObject taskObject, int priority){
        if (priority >= 0 && priority <= 3) {
            App.initRealm();
            App.realm.executeTransaction((r) -> taskObject.setPriority(priority));
        }
    }


}
