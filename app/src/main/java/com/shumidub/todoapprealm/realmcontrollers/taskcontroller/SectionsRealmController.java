package com.shumidub.todoapprealm.realmcontrollers.taskcontroller;

import com.shumidub.todoapprealm.App;
import com.shumidub.todoapprealm.realmmodel.task.SectionObject;
import com.shumidub.todoapprealm.realmmodel.task.TaskObject;

import java.util.ArrayList;
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
        App.initRealm();
        return App.realm.where(SectionObject.class)
                .equalTo("parentFolderId", folderId)
                .findAll()
                .sort("position", Sort.ASCENDING);
    }

    public static SectionObject getSection(long sectionId) {
        App.initRealm();
        return App.realm.where(SectionObject.class).equalTo("id", sectionId).findFirst();
    }

    // ---------- Mutations ----------

    public static SectionObject addSection(long folderId, String name,
                                           boolean collapsedByDefault, int position) {
        App.initRealm();
        final String trimmed = name == null ? "" : name.trim();
        if (trimmed.isEmpty() || trimmed.length() > 40) {
            throw new IllegalArgumentException("Section name must be 1..40 chars");
        }
        final long id = getIdForNextValue();
        App.realm.executeTransaction(r -> {
            SectionObject s = r.createObject(SectionObject.class, id);
            s.setName(trimmed);
            s.setCollapsedByDefault(collapsedByDefault);
            s.setCurrentlyCollapsed(collapsedByDefault);
            s.setParentFolderId(folderId);
            s.setPosition(position);
        });
        SectionObject managed = App.realm.where(SectionObject.class).equalTo("id", id).findFirst();
        compactPositions(folderId);
        return managed;
    }

    public static void editSection(SectionObject s, String name, boolean collapsedByDefault) {
        if (s == null || !s.isValid()) return;
        final String trimmed = name == null ? "" : name.trim();
        if (trimmed.isEmpty() || trimmed.length() > 40) {
            throw new IllegalArgumentException("Section name must be 1..40 chars");
        }
        App.initRealm();
        App.realm.executeTransaction(r -> {
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
        App.initRealm();
        final long folderId = s.getParentFolderId();
        final long sectionId = s.getId();
        App.realm.executeTransaction(r -> {
            RealmResults<TaskObject> inSection = r.where(TaskObject.class)
                    .equalTo("taskFolderId", folderId)
                    .equalTo("sectionId", sectionId)
                    .findAll();
            for (TaskObject t : inSection) {
                t.setSectionId(0);
            }
            s.deleteFromRealm();
        });
        compactPositions(folderId);
    }

    public static void setCurrentlyCollapsed(SectionObject s, boolean collapsed) {
        if (s == null || !s.isValid()) return;
        App.initRealm();
        App.realm.executeTransaction(r -> s.setCurrentlyCollapsed(collapsed));
    }

    public static void moveTaskToSection(TaskObject task, long newSectionId, int newPosition) {
        if (task == null || !task.isValid()) return;
        App.initRealm();
        App.realm.executeTransaction(r -> {
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
        App.initRealm();
        App.realm.executeTransaction(r -> {
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
     * Re-stamp positions to a contiguous 0..N-1 in current sorted order.
     * Outer space (sections + free tasks) is compacted independently from each inner space.
     */
    public static void compactPositions(long folderId) {
        App.initRealm();
        App.realm.executeTransaction(r -> {
            // Outer space: sections + free tasks (sectionId == 0).
            List<SectionObject> sections = new ArrayList<>(
                    r.where(SectionObject.class)
                            .equalTo("parentFolderId", folderId)
                            .findAll()
                            .sort("position", Sort.ASCENDING));
            List<TaskObject> freeTasks = new ArrayList<>(
                    r.where(TaskObject.class)
                            .equalTo("taskFolderId", folderId)
                            .equalTo("sectionId", 0L)
                            .findAll()
                            .sort("position", Sort.ASCENDING));

            // Merge by current position; stable order keeps existing relative order on ties.
            // Build interleaved outer list.
            List<Object> outer = new ArrayList<>(sections.size() + freeTasks.size());
            int si = 0, ti = 0;
            while (si < sections.size() && ti < freeTasks.size()) {
                if (sections.get(si).getPosition() <= freeTasks.get(ti).getPosition()) {
                    outer.add(sections.get(si++));
                } else {
                    outer.add(freeTasks.get(ti++));
                }
            }
            while (si < sections.size()) outer.add(sections.get(si++));
            while (ti < freeTasks.size()) outer.add(freeTasks.get(ti++));
            for (int i = 0; i < outer.size(); i++) {
                Object it = outer.get(i);
                if (it instanceof SectionObject) ((SectionObject) it).setPosition(i);
                else ((TaskObject) it).setPosition(i);
            }

            // Inner spaces: per section, re-stamp tasks in that section.
            for (SectionObject s : sections) {
                List<TaskObject> inner = new ArrayList<>(
                        r.where(TaskObject.class)
                                .equalTo("taskFolderId", folderId)
                                .equalTo("sectionId", s.getId())
                                .findAll()
                                .sort("done", Sort.ASCENDING, "position", Sort.ASCENDING));
                for (int i = 0; i < inner.size(); i++) {
                    inner.get(i).setPosition(i);
                }
            }
        });
    }

    /** Next outer position for a new section/task appended at the end. */
    public static int nextOuterPosition(long folderId) {
        App.initRealm();
        Number maxSec = App.realm.where(SectionObject.class)
                .equalTo("parentFolderId", folderId).max("position");
        Number maxTask = App.realm.where(TaskObject.class)
                .equalTo("taskFolderId", folderId)
                .equalTo("sectionId", 0L)
                .max("position");
        int a = maxSec == null ? -1 : maxSec.intValue();
        int b = maxTask == null ? -1 : maxTask.intValue();
        return Math.max(a, b) + 1;
    }

    private static long getIdForNextValue() {
        App.initRealm();
        long id = System.currentTimeMillis();
        while (App.realm.where(SectionObject.class).equalTo("id", id).findFirst() != null) {
            id++;
        }
        return id;
    }
}
