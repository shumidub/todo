package com.shumidub.todoapprealm;

import io.realm.DynamicRealm;
import io.realm.DynamicRealmObject;
import io.realm.FieldAttribute;
import io.realm.RealmList;
import io.realm.RealmMigration;
import io.realm.RealmObjectSchema;
import io.realm.RealmResults;
import io.realm.RealmSchema;

public class RealmMigrations implements RealmMigration {

    public static final long SCHEMA_VERSION = 4;

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
