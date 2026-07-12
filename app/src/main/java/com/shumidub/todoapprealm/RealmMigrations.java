package com.shumidub.todoapprealm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.realm.DynamicRealm;
import io.realm.DynamicRealmObject;
import io.realm.FieldAttribute;
import io.realm.RealmList;
import io.realm.RealmMigration;
import io.realm.RealmObjectSchema;
import io.realm.RealmResults;
import io.realm.RealmSchema;

public class RealmMigrations implements RealmMigration {

    public static final long SCHEMA_VERSION = 7;

    @Override
    public void migrate(DynamicRealm realm, long oldVersion, long newVersion) {
        RealmSchema schema = realm.getSchema();

        if (oldVersion < 2) {
            // add TaskObject.extraFolderIds (RealmList<Long>) for multi-category support
            schema.get("TaskObject")
                    .addRealmListField("extraFolderIds", Long.class);
        }

        if (oldVersion < 3) {
            // add RealmFoldersContainer.folderOfTasksList2 (second Tasks tab)
            schema.get("RealmFoldersContainer")
                    .addRealmListField("folderOfTasksList2", schema.get("FolderTaskObject"));
        }

        if (oldVersion < 4) {
            // ---- task-002: SectionObject schema ----
            RealmObjectSchema sectionSchema = schema.create("SectionObject")
                    .addField("id", long.class, FieldAttribute.PRIMARY_KEY)
                    .addField("name", String.class, FieldAttribute.REQUIRED)
                    .addField("collapsedByDefault", boolean.class)
                    .addField("currentlyCollapsed", boolean.class)
                    .addField("parentFolderId", long.class, FieldAttribute.INDEXED)
                    .addField("position", int.class);

            // ---- task-002: TaskObject.sectionId (default 0 = "no section") ----
            schema.get("TaskObject")
                    .addField("sectionId", long.class);

            // ---- task-002: TaskObject.position (backfilled below from RealmList index) ----
            schema.get("TaskObject")
                    .addField("position", int.class);

            // Backfill TaskObject.position per folder using the current FolderTaskObject.folderTasks
            // RealmList order. We must do this through DynamicRealm because RealmList ordering
            // isn't visible inside a per-record `transform`.
            RealmResults<DynamicRealmObject> folders =
                    realm.where("FolderTaskObject").findAll();
            for (DynamicRealmObject folder : folders) {
                RealmList<DynamicRealmObject> tasks = folder.getList("folderTasks");
                if (tasks == null) continue;
                for (int i = 0; i < tasks.size(); i++) {
                    DynamicRealmObject t = tasks.get(i);
                    if (t != null && t.isValid()) {
                        t.setInt("position", i);
                    }
                }
            }

            // ---- task-003: third Tasks tab folder list ----
            schema.get("RealmFoldersContainer")
                    .addRealmListField("folderOfTasksList3", schema.get("FolderTaskObject"));
        }

        if (oldVersion < 5) {
            // Notes tab (taskGroup 3) folder list
            schema.get("RealmFoldersContainer")
                    .addRealmListField("folderOfTasksList4", schema.get("FolderTaskObject"));
        }

        if (oldVersion < 6) {
            // ---- task-004: per-category ordering + section placement ----
            // Embedded TaskPlacement {folderId, position, sectionId}, one per folder a task
            // belongs to. NO per-record backfill: placements are materialized lazily when a task
            // becomes multi-category (see TasksRealmController), so existing tasks keep ordering
            // by their legacy position/sectionId until then — behavior is unchanged on upgrade.
            RealmObjectSchema placementSchema = schema.create("TaskPlacement")
                    .addField("folderId", long.class)
                    .addField("position", int.class)
                    .addField("sectionId", long.class);
            placementSchema.setEmbedded(true);
            schema.get("TaskObject")
                    .addRealmListField("placements", placementSchema);
        }

        if (oldVersion < 7) {
            // ---- task-005: TaskObject.id becomes a primary key ----
            // Before v7 `id` was non-unique. A multi-category task is a single shared row referenced
            // by several folders, but Gson tree-serializes it once per folder on backup, and the
            // restore's insertOrUpdate (no PK = plain insert) re-created one row per folder. Those
            // clones drifted out of sync, so tapping a checkbox / editing / deleting hit whichever
            // clone findFirst(id) returned — usually not the one on screen. Collapse every clone
            // group back to one shared row (union its folder memberships) BEFORE adding the PK,
            // otherwise addPrimaryKey() would throw on the duplicates.
            consolidateDuplicateTasks(realm);
            schema.get("TaskObject").addPrimaryKey("id");
        }
    }

    /**
     * Collapse TaskObject rows that share an {@code id} into a single canonical row each, so the
     * field can become a primary key. For every duplicate group we:
     *   1. pick the canonical row (the most-progressed clone — highest countAccumulation, then done),
     *   2. re-point every folder's {@code folderTasks} list to the canonical row (deduped per folder),
     *   3. rebuild the canonical's {@code taskFolderId} (primary) + {@code extraFolderIds} from the
     *      union of folders that referenced any clone,
     *   4. delete the now-orphaned clones.
     * Tasks that already have a unique id are left untouched.
     */
    private static void consolidateDuplicateTasks(DynamicRealm realm) {
        RealmResults<DynamicRealmObject> allTasks = realm.where("TaskObject").findAll();

        // id -> canonical clone (the most-progressed one)
        Map<Long, DynamicRealmObject> canonical = new HashMap<>();
        for (DynamicRealmObject t : allTasks) {
            if (t == null || !t.isValid()) continue;
            long id = t.getLong("id");
            DynamicRealmObject cur = canonical.get(id);
            if (cur == null || isMoreComplete(t, cur)) canonical.put(id, t);
        }

        // id -> set of folder ids that referenced any clone of this task (membership union)
        Map<Long, Set<Long>> foldersForId = new HashMap<>();
        RealmResults<DynamicRealmObject> folders = realm.where("FolderTaskObject").findAll();
        for (DynamicRealmObject folder : folders) {
            long fid = folder.getLong("id");
            RealmList<DynamicRealmObject> list = folder.getList("folderTasks");
            if (list == null) continue;
            // Collect the canonical row for each distinct id present in this folder, preserving order.
            LinkedHashMap<Long, DynamicRealmObject> keep = new LinkedHashMap<>();
            for (DynamicRealmObject t : list) {
                if (t == null || !t.isValid()) continue;
                long id = t.getLong("id");
                Set<Long> set = foldersForId.get(id);
                if (set == null) { set = new LinkedHashSet<>(); foldersForId.put(id, set); }
                set.add(fid);
                if (!keep.containsKey(id)) keep.put(id, canonical.get(id));
            }
            // Re-point the folder list to the canonical rows only (drops clones + within-folder dupes).
            list.clear();
            for (DynamicRealmObject c : keep.values()) {
                if (c != null && c.isValid()) list.add(c);
            }
        }

        // Rebuild primary + extra folder ids on each canonical row from the membership union.
        for (Map.Entry<Long, DynamicRealmObject> e : canonical.entrySet()) {
            DynamicRealmObject c = e.getValue();
            if (c == null || !c.isValid()) continue;
            Set<Long> folderSet = foldersForId.get(e.getKey());
            if (folderSet == null || folderSet.isEmpty()) continue; // orphan task: leave as-is
            long primary = c.getLong("taskFolderId");
            if (!folderSet.contains(primary)) {
                primary = folderSet.iterator().next();
                c.setLong("taskFolderId", primary);
            }
            RealmList<Long> extras = c.getList("extraFolderIds", Long.class);
            extras.clear();
            for (Long fid : folderSet) {
                if (fid != null && fid != primary) extras.add(fid);
            }
        }

        // Delete the orphaned clones (every row that is not the canonical for its id).
        List<DynamicRealmObject> toDelete = new ArrayList<>();
        for (DynamicRealmObject t : allTasks) {
            if (t == null || !t.isValid()) continue;
            DynamicRealmObject c = canonical.get(t.getLong("id"));
            if (c != null && !t.equals(c)) toDelete.add(t);
        }
        for (DynamicRealmObject t : toDelete) {
            if (t.isValid()) t.deleteFromRealm();
        }
    }

    /** A clone is "more complete" if it has higher accumulation, or (tie) is marked done. */
    private static boolean isMoreComplete(DynamicRealmObject a, DynamicRealmObject b) {
        int ca = a.getInt("countAccumulation");
        int cb = b.getInt("countAccumulation");
        if (ca != cb) return ca > cb;
        return a.getBoolean("done") && !b.getBoolean("done");
    }

    @Override
    public int hashCode() {
        return RealmMigrations.class.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof RealmMigrations;
    }
}
