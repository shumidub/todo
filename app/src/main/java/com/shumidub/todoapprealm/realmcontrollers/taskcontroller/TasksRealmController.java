package com.shumidub.todoapprealm.realmcontrollers.taskcontroller;

import android.util.Log;
import com.shumidub.todoapprealm.realmcontrollers.RealmDb;
import com.shumidub.todoapprealm.realmmodel.task.FolderTaskObject;
import com.shumidub.todoapprealm.realmmodel.task.TaskObject;
import com.shumidub.todoapprealm.realmmodel.task.TaskPlacement;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import androidx.annotation.NonNull;
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
        return RealmDb.realm().where(TaskObject.class).findAll()
                .sort("done", Sort.ASCENDING, "id",Sort.ASCENDING);
    }

    /** get not done tasks , without folder id !!! if folder if == null !!! do not happend*/
    public static List<TaskObject> getNotDoneTasks(){
        return RealmDb.realm().where(TaskObject.class)
                .equalTo("done", false)
                .findAll()
                .sort("done", Sort.ASCENDING, "id",Sort.ASCENDING);
    }

    /** get done tasks , without folder id !!! if folder if == null !!! do not happend */
    public static List<TaskObject> getDoneTasks(){
        return RealmDb.realm().where(TaskObject.class)
                .equalTo("done", true)
                .findAll()
                .sort("done", Sort.ASCENDING, "id",Sort.ASCENDING);
    }

    /** done and not done tasks but where countAccumulation more than 0
     * use for reset daily count value
     */
    public static List<TaskObject> getDoneAndPartiallyDoneTasks(){
        return RealmDb.realm().where(TaskObject.class)
                .notEqualTo("countAccumulation", 0)
                .findAll()
                .sort("done", Sort.ASCENDING, "id",Sort.ASCENDING);
    }

    /** get tasks by folder id*/
    public static RealmResults<TaskObject> getTasks(long folderId){
        // task-002: sort by (done ASC, position ASC) so drag-order is preserved per section/free
        return getFolderTasksRealmListFromFolder(folderId).sort(
           new String[]{"done", "position"},
           new Sort[]{Sort.ASCENDING, Sort.ASCENDING});
    }

    /** get not done tasks by id*/
    public static List<TaskObject> getNotDoneTasks(long folderId){
        return getFolderTasksRealmListFromFolder(folderId)
                .where().equalTo("done", false).findAll()
                .sort("position", Sort.ASCENDING);
    }

    /** get done tasks by id*/
    public static List<TaskObject> getDoneTasks(long folderId){
        return getFolderTasksRealmListFromFolder(folderId)
                .where().equalTo("done", true).findAll()
                .sort("position", Sort.ASCENDING);
    }

    /** get done and not done tasks but where countAccumulation more than 0 */
    public static List<TaskObject> getDoneAndPartiallyDoneTasks(long folderId){
        return RealmDb.realm().where(TaskObject.class)
                .equalTo("taskFolderId", folderId)
                .notEqualTo("countAccumulation", 0)
                .findAll()
                .sort("done", Sort.ASCENDING, "id",Sort.ASCENDING);
    }

    //SINGLE TASK
    /** get task by id*/
    public static TaskObject getTask(long idTask){
        return RealmDb.findById(TaskObject.class, idTask);
    }

    /** add task*/
    public static  void addTask(String text, int count, int maxAccumulation, boolean cycling, int priority, long taskFolderId ){
        RealmDb.write(() -> {
            TaskObject task = RealmDb.realm().createObject(TaskObject.class);
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
        });
    }

    public static  void editTask(TaskObject task, String text, @NonNull int count, @NonNull int maxAccumulation, @NonNull boolean cycling, @NonNull int priority ){
        RealmDb.write(() -> {
            if (!text.isEmpty()) task.setText(text);
            task.setPriority(priority);
            task.setCountValue(count);
            task.setMaxAccumulation(maxAccumulation);
            task.setCycling(cycling);
        });
    }

    public static void setTaskDoneOrParticullaryDone(TaskObject task, boolean done){
        RealmDb.write(() -> {
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
        long taskId = task.getId();
        String taskText = task.getText();

        RealmDb.write(() -> {
            // remove the task from every folder that references it (primary + extras)
            for (FolderTaskObject folder : RealmDb.realm().where(FolderTaskObject.class).findAll()) {
                if (folder.getTasks() != null && folder.getTasks().contains(task)) {
                    folder.getTasks().remove(task);
                }
            }
            if (task.isValid()) {
                task.getDateCountAccumulation().clear();
                if (task.getExtraFolderIds() != null) task.getExtraFolderIds().clear();
                task.deleteFromRealm();
            }
        });

        if (RealmDb.findById(TaskObject.class, taskId) == null){
            Log.d("DEBUG_TAG", "TASK: " + taskText + " id:" + taskId + " DELETED" );
        }else{
            Log.d("DEBUG_TAG", "TASK: " + taskText + " id:" + taskId + " NOT DELETED !!!" );
        }
    }

    /**delete task by id*/
    public static void deleteTask(long id){
        deleteTask(RealmDb.findById(TaskObject.class, id));
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
        return RealmDb.newUniqueId(TaskObject.class);
    }

    @SuppressWarnings("All")
    public static RealmList<TaskObject> getFolderTasksRealmListFromFolder (long folderId){
        return RealmDb.findById(FolderTaskObject.class, folderId).folderTasks;
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

    // ---- per-category placements (task-004) ----

    /** True if the task belongs to more than one folder (and therefore uses placements). */
    public static boolean isMultiCategory(TaskObject task) {
        RealmList<Long> e = task.getExtraFolderIds();
        return e != null && !e.isEmpty();
    }

    /** The task's placement for {@code folderId}, or null if it has none (single-category task). */
    public static TaskPlacement placementFor(TaskObject task, long folderId) {
        RealmList<TaskPlacement> ps = task.getPlacements();
        if (ps != null) {
            for (TaskPlacement p : ps) {
                if (p != null && p.isValid() && p.getFolderId() == folderId) return p;
            }
        }
        return null;
    }

    /** Order index of {@code task} within {@code folderId}: per-category placement, or the legacy
     *  shared {@code position} as a fallback (single-category tasks / not-yet-materialized). */
    public static int effectivePosition(TaskObject task, long folderId) {
        TaskPlacement p = placementFor(task, folderId);
        return p != null ? p.getPosition() : task.getPosition();
    }

    /** Section of {@code task} within {@code folderId}: per-category placement, or the legacy
     *  {@code sectionId} for the primary folder (0 = free in any non-primary folder). */
    public static long effectiveSection(TaskObject task, long folderId) {
        TaskPlacement p = placementFor(task, folderId);
        if (p != null) return p.getSectionId();
        return folderId == task.getTaskFolderId() ? task.getSectionId() : 0L;
    }

    /** Write {@code task}'s order within {@code folderId} (placement when present/multi-category,
     *  else the legacy field). Must run inside a transaction. */
    public static void setEffectivePosition(TaskObject task, long folderId, int pos) {
        TaskPlacement p = placementFor(task, folderId);
        if (p != null) { p.setPosition(pos); return; }
        if (!isMultiCategory(task)) { task.setPosition(pos); return; }
        TaskPlacement np = RealmDb.realm().createEmbeddedObject(TaskPlacement.class, task, "placements");
        np.setFolderId(folderId);
        np.setSectionId(effectiveSection(task, folderId));
        np.setPosition(pos);
    }

    /** Write {@code task}'s section within {@code folderId} (placement when present/multi-category,
     *  else the legacy field). Must run inside a transaction. */
    public static void setEffectiveSection(TaskObject task, long folderId, long sectionId) {
        TaskPlacement p = placementFor(task, folderId);
        if (p != null) { p.setSectionId(sectionId); return; }
        if (!isMultiCategory(task)) { task.setSectionId(sectionId); return; }
        TaskPlacement np = RealmDb.realm().createEmbeddedObject(TaskPlacement.class, task, "placements");
        np.setFolderId(folderId);
        np.setPosition(effectivePosition(task, folderId));
        np.setSectionId(sectionId);
    }

    /** Distinct, valid members of a folder (its {@code folderTasks} list), ordered for display
     *  by (done, per-category position). The source of truth for membership is the list, not
     *  {@code taskFolderId} — a folder also holds tasks whose primary category is elsewhere. */
    public static List<TaskObject> getFolderMembers(long folderId) {
        FolderTaskObject folder = FolderTaskRealmController.getFolder(folderId);
        LinkedHashMap<Long, TaskObject> byId = new LinkedHashMap<>();
        if (folder != null && folder.getTasks() != null) {
            for (TaskObject t : folder.getTasks()) {
                if (t != null && t.isValid()) byId.put(t.getId(), t);
            }
        }
        List<TaskObject> list = new ArrayList<>(byId.values());
        list.sort((a, b) -> {
            if (a.isDone() != b.isDone()) return a.isDone() ? 1 : -1;
            return Integer.compare(effectivePosition(a, folderId), effectivePosition(b, folderId));
        });
        return list;
    }

    /**
     * Set the full list of folders this task belongs to.
     * The first id in {@code folderIds} becomes the primary {@code taskFolderId};
     * the rest are stored as extras. Existing folders not in the list lose the task.
     *
     * <p>Keeps per-category placements (task-004) in sync: a multi-category task gets exactly one
     * placement per assigned folder (created seeded at the end of that folder's free zone, the
     * primary's seeded from the legacy position/section); placements for dropped folders are
     * removed. A task that falls back to a single category sheds its placements and restores the
     * legacy {@code position}/{@code sectionId} from the surviving folder so its order is kept.
     */
    public static void setTaskCategories(TaskObject task, List<Long> folderIds) {
        if (folderIds == null || folderIds.isEmpty()) return;

        // de-dupe preserving order
        Set<Long> ordered = new LinkedHashSet<>(folderIds);
        List<Long> finalIds = new ArrayList<>(ordered);

        RealmDb.write(() -> {
            long newPrimary = finalIds.get(0);

            // Remove the task from EVERY folder, dropping ALL occurrences (a task may have been
            // added to a folder's list more than once — a plain remove() leaves a duplicate behind,
            // so unchecking a category wouldn't take effect). Then re-add it exactly once to each
            // assigned folder. This also de-duplicates the membership data.
            for (FolderTaskObject folder : RealmDb.realm().where(FolderTaskObject.class).findAll()) {
                RealmList<TaskObject> list = folder.getTasks();
                if (list == null) continue;
                while (list.remove(task)) { /* drop all occurrences */ }
            }
            for (Long fid : finalIds) {
                FolderTaskObject folder = FolderTaskRealmController.getFolder(fid);
                if (folder == null || folder.getTasks() == null) continue;
                folder.getTasks().add(task);
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

            // ---- placements (must run after extraFolderIds is set: isMultiCategory reads it) ----
            RealmList<TaskPlacement> placements = task.getPlacements();
            if (finalIds.size() == 1) {
                // Back to a single category: capture its current order/section into the legacy
                // fields, then drop placements so the fallback path drives ordering.
                if (placements != null && !placements.isEmpty()) {
                    int pos = effectivePosition(task, newPrimary);
                    long sec = effectiveSection(task, newPrimary);
                    placements.deleteAllFromRealm();
                    task.setPosition(pos);
                    task.setSectionId(sec);
                }
            } else {
                // Multi-category: drop placements for folders no longer assigned…
                if (placements != null) {
                    for (int i = placements.size() - 1; i >= 0; i--) {
                        if (!finalIds.contains(placements.get(i).getFolderId())) {
                            placements.deleteFromRealm(i);
                        }
                    }
                }
                // …and create a placement for any assigned folder still missing one.
                for (Long fid : finalIds) {
                    if (placementFor(task, fid) != null) continue;
                    int seedPos = (fid == newPrimary) ? task.getPosition()
                            : SectionsRealmController.nextOuterPosition(fid);
                    long seedSec = (fid == newPrimary) ? task.getSectionId() : 0L;
                    TaskPlacement np = RealmDb.realm()
                            .createEmbeddedObject(TaskPlacement.class, task, "placements");
                    np.setFolderId(fid);
                    np.setPosition(seedPos);
                    np.setSectionId(seedSec);
                }
            }
        });

        // Keep every affected folder's positions dense (nested write reuses the txn if any).
        for (Long fid : finalIds) SectionsRealmController.compactPositions(fid);
    }

    public static void setTaskPriority(TaskObject taskObject, int priority){
        if (priority >= 0 && priority <= 3) {
            RealmDb.write(() -> taskObject.setPriority(priority));
        }
    }


}
