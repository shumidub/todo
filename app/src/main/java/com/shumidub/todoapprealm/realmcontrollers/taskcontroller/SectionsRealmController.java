package com.shumidub.todoapprealm.realmcontrollers.taskcontroller;

import com.shumidub.todoapprealm.realmcontrollers.RealmDb;
import com.shumidub.todoapprealm.realmmodel.task.FolderTaskObject;
import com.shumidub.todoapprealm.realmmodel.task.SectionObject;
import com.shumidub.todoapprealm.realmmodel.task.TaskObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import io.realm.RealmResults;
import io.realm.Sort;

/**
 * Realm controller for {@link SectionObject} (task-002).
 *
 * <p><b>Position model invariant</b> (see task-002 design §3):
 * <ul>
 *   <li><b>Outer space</b>: ordered list of mixed items inside one folder where each item is
 *       either a {@code SectionObject} or a {@code TaskObject} with {@code sectionId == 0}.
 *       Their {@code position} field is the dense index in this outer list.</li>
 *   <li><b>Inner space</b>: per section S, ordered list of tasks where
 *       {@code sectionId == S.id}. Those tasks' {@code position} field is the dense index
 *       inside that section.</li>
 * </ul>
 *
 * <p>Storage uses {@code FolderTaskObject.folderTasks} RealmList for membership, but ordering
 * for display is driven exclusively by {@code position}.
 */
public final class SectionsRealmController {

    private SectionsRealmController() { /* utility */ }

    // ---------- Reads ----------

    public static RealmResults<SectionObject> getSections(long folderId) {
        return RealmDb.realm().where(SectionObject.class)
                .equalTo("parentFolderId", folderId)
                .findAll()
                .sort("position", Sort.ASCENDING);
    }

    public static SectionObject getSection(long sectionId) {
        return RealmDb.findById(SectionObject.class, sectionId);
    }

    // ---------- Mutations ----------

    public static SectionObject addSection(long folderId, String name,
                                           boolean collapsedByDefault, int position) {
        final String trimmed = name == null ? "" : name.trim();
        if (trimmed.isEmpty() || trimmed.length() > 40) {
            throw new IllegalArgumentException("Section name must be 1..40 chars");
        }
        final long id = getIdForNextValue();
        RealmDb.write(r -> {
            SectionObject s = r.createObject(SectionObject.class, id);
            s.setName(trimmed);
            s.setCollapsedByDefault(collapsedByDefault);
            s.setCurrentlyCollapsed(collapsedByDefault);
            s.setParentFolderId(folderId);
            s.setPosition(position);
        });
        SectionObject managed = RealmDb.findById(SectionObject.class, id);
        compactPositions(folderId);
        return managed;
    }

    public static void editSection(SectionObject s, String name, boolean collapsedByDefault) {
        if (s == null || !s.isValid()) return;
        final String trimmed = name == null ? "" : name.trim();
        if (trimmed.isEmpty() || trimmed.length() > 40) {
            throw new IllegalArgumentException("Section name must be 1..40 chars");
        }
        RealmDb.write(r -> {
            s.setName(trimmed);
            s.setCollapsedByDefault(collapsedByDefault);
        });
    }

    /**
     * Delete a section. Tasks inside become "free" (sectionId=0), keeping their relative
     * positions. The header slot is removed and outer positions are compacted.
     */
    public static void deleteSection(SectionObject s) {
        if (s == null || !s.isValid()) return;
        final long folderId = s.getParentFolderId();
        final long sectionId = s.getId();
        RealmDb.write(r -> {
            // Members placed in this section (in THIS folder) become free; uses per-category
            // section so a multi-category task is only freed in the folder the section belongs to.
            for (TaskObject t : folderMembers(folderId)) {
                if (TasksRealmController.effectiveSection(t, folderId) == sectionId) {
                    TasksRealmController.setEffectiveSection(t, folderId, 0L);
                }
            }
            s.deleteFromRealm();
        });
        compactPositions(folderId);
    }

    /** Distinct, valid members of a folder's task list (the membership source of truth). */
    private static List<TaskObject> folderMembers(long folderId) {
        FolderTaskObject folder = RealmDb.findById(FolderTaskObject.class, folderId);
        LinkedHashMap<Long, TaskObject> byId = new LinkedHashMap<>();
        if (folder != null && folder.getTasks() != null) {
            for (TaskObject t : folder.getTasks()) {
                if (t != null && t.isValid()) byId.put(t.getId(), t);
            }
        }
        return new ArrayList<>(byId.values());
    }

    public static void setCurrentlyCollapsed(SectionObject s, boolean collapsed) {
        if (s == null || !s.isValid()) return;
        RealmDb.write(r -> s.setCurrentlyCollapsed(collapsed));
    }

    /**
     * Reset every section's {@code currentlyCollapsed} back to its
     * {@code collapsedByDefault} preference. Called once at app start
     * so manual collapse/expand from the previous session does not persist.
     */
    public static void resetAllCollapseStates() {
        RealmDb.write(r -> {
            for (SectionObject s : r.where(SectionObject.class).findAll()) {
                if (s.isCurrentlyCollapsed() != s.isCollapsedByDefault()) {
                    s.setCurrentlyCollapsed(s.isCollapsedByDefault());
                }
            }
        });
    }

    public static void moveTaskToSection(TaskObject task, long newSectionId, int newPosition) {
        if (task == null || !task.isValid()) return;
        RealmDb.write(r -> {
            task.setSectionId(newSectionId);
            task.setPosition(newPosition);
        });
        compactPositions(task.getTaskFolderId());
    }

    public static final class ItemMove {
        public enum Kind { SECTION, TASK }
        public final Kind kind;
        public final long id;
        public final int newPosition;
        /** -1 means unchanged (tasks only). */
        public final long newSectionId;

        public ItemMove(Kind kind, long id, int newPosition, long newSectionId) {
            this.kind = kind;
            this.id = id;
            this.newPosition = newPosition;
            this.newSectionId = newSectionId;
        }
    }

    /** Apply drag-n-drop moves atomically, then re-stamp positions. */
    public static void reorderItems(long folderId, List<ItemMove> moves) {
        if (moves == null || moves.isEmpty()) return;
        RealmDb.write(r -> {
            for (ItemMove m : moves) {
                if (m.kind == ItemMove.Kind.SECTION) {
                    SectionObject s = r.where(SectionObject.class).equalTo("id", m.id).findFirst();
                    if (s != null) s.setPosition(m.newPosition);
                } else {
                    TaskObject t = r.where(TaskObject.class).equalTo("id", m.id).findFirst();
                    if (t != null) {
                        t.setPosition(m.newPosition);
                        if (m.newSectionId != -1L) t.setSectionId(m.newSectionId);
                    }
                }
            }
        });
        compactPositions(folderId);
    }

    /**
     * Restamp positions of all tasks in a single container (inner section or outer-space free-zone)
     * from an explicit ordered list of task IDs. Avoids position-tie issues that
     * {@link #reorderItems} + {@link #compactPositions} fall into when two tasks momentarily
     * share the same {@code position} value.
     *
     * <p>If {@code sectionId == 0} this restamps free-zone tasks (sections in the same folder
     * keep their positions; final {@link #compactPositions} call interleaves them deterministically).
     *
     * <p>The dragged task's {@code sectionId} is set to {@code sectionId} if it differs.
     */
    public static void rearrangeTasksInContainer(long folderId, long sectionId, List<Long> orderedTaskIds) {
        if (orderedTaskIds == null) return;
        RealmDb.write(r -> {
            for (int i = 0; i < orderedTaskIds.size(); i++) {
                TaskObject t = r.where(TaskObject.class).equalTo("id", orderedTaskIds.get(i)).findFirst();
                if (t == null) continue;
                TasksRealmController.setEffectiveSection(t, folderId, sectionId);
                TasksRealmController.setEffectivePosition(t, folderId, i);
            }
        });
        compactPositions(folderId);
    }

    /**
     * Outer-space restamp from an explicit ordered list of {@link ItemMove} entries
     * (SECTION + TASK, in the desired visual order). Each TASK entry is forced into
     * the free-zone ({@code sectionId = 0}). Positions become 0..N-1 in one transaction.
     * Use when drag moves a task across section boundaries into the free-zone — avoids
     * position ties that {@link #reorderItems} + {@link #compactPositions} cannot
     * disambiguate.
     */
    public static void rearrangeOuterSpace(long folderId, List<ItemMove> orderedEntries) {
        if (orderedEntries == null) return;
        RealmDb.write(r -> {
            for (int i = 0; i < orderedEntries.size(); i++) {
                ItemMove e = orderedEntries.get(i);
                if (e.kind == ItemMove.Kind.SECTION) {
                    SectionObject s = r.where(SectionObject.class).equalTo("id", e.id).findFirst();
                    if (s != null) s.setPosition(i);
                } else {
                    TaskObject t = r.where(TaskObject.class).equalTo("id", e.id).findFirst();
                    if (t != null) {
                        TasksRealmController.setEffectiveSection(t, folderId, 0L);
                        TasksRealmController.setEffectivePosition(t, folderId, i);
                    }
                }
            }
        });
        compactPositions(folderId);
    }

    /**
     * Re-stamp positions to a contiguous 0..N-1 in current sorted order.
     * Outer space (sections + free tasks) is compacted independently from each inner space.
     */
    public static void compactPositions(long folderId) {
        RealmDb.write(r -> {
            List<SectionObject> sections = new ArrayList<>(
                    r.where(SectionObject.class)
                            .equalTo("parentFolderId", folderId)
                            .findAll()
                            .sort("position", Sort.ASCENDING));
            // Membership is the folder's task list (a folder also holds tasks whose primary
            // category is elsewhere); ordering is the per-category effective position/section.
            List<TaskObject> members = folderMembers(folderId);

            // Outer space: free tasks (no section IN THIS folder), by effective position.
            List<TaskObject> freeTasks = new ArrayList<>();
            for (TaskObject t : members) {
                if (TasksRealmController.effectiveSection(t, folderId) == 0L) freeTasks.add(t);
            }
            freeTasks.sort((a, b) -> Integer.compare(
                    TasksRealmController.effectivePosition(a, folderId),
                    TasksRealmController.effectivePosition(b, folderId)));

            // Merge sections + free tasks by current position; stable on ties.
            List<Object> outer = new ArrayList<>(sections.size() + freeTasks.size());
            int si = 0, ti = 0;
            while (si < sections.size() && ti < freeTasks.size()) {
                int sp = sections.get(si).getPosition();
                int tp = TasksRealmController.effectivePosition(freeTasks.get(ti), folderId);
                if (sp <= tp) outer.add(sections.get(si++));
                else outer.add(freeTasks.get(ti++));
            }
            while (si < sections.size()) outer.add(sections.get(si++));
            while (ti < freeTasks.size()) outer.add(freeTasks.get(ti++));
            for (int i = 0; i < outer.size(); i++) {
                Object it = outer.get(i);
                if (it instanceof SectionObject) ((SectionObject) it).setPosition(i);
                else TasksRealmController.setEffectivePosition((TaskObject) it, folderId, i);
            }

            // Inner spaces: per section, re-stamp its members (done sinks to the bottom).
            for (SectionObject s : sections) {
                List<TaskObject> inner = new ArrayList<>();
                for (TaskObject t : members) {
                    if (TasksRealmController.effectiveSection(t, folderId) == s.getId()) inner.add(t);
                }
                inner.sort((a, b) -> {
                    if (a.isDone() != b.isDone()) return a.isDone() ? 1 : -1;
                    return Integer.compare(
                            TasksRealmController.effectivePosition(a, folderId),
                            TasksRealmController.effectivePosition(b, folderId));
                });
                for (int i = 0; i < inner.size(); i++) {
                    TasksRealmController.setEffectivePosition(inner.get(i), folderId, i);
                }
            }
        });
    }

    /** Next outer position for a new section/task appended at the end of this folder's free zone. */
    public static int nextOuterPosition(long folderId) {
        int max = -1;
        Number maxSec = RealmDb.realm().where(SectionObject.class)
                .equalTo("parentFolderId", folderId).max("position");
        if (maxSec != null) max = maxSec.intValue();
        for (TaskObject t : folderMembers(folderId)) {
            if (TasksRealmController.effectiveSection(t, folderId) == 0L) {
                max = Math.max(max, TasksRealmController.effectivePosition(t, folderId));
            }
        }
        return max + 1;
    }

    private static long getIdForNextValue() {
        return RealmDb.newUniqueId(SectionObject.class);
    }
}
