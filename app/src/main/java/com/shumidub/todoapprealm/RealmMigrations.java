package com.shumidub.todoapprealm;

import io.realm.DynamicRealm;
import io.realm.FieldAttribute;
import io.realm.RealmMigration;
import io.realm.RealmSchema;

public class RealmMigrations implements RealmMigration {

    public static final long SCHEMA_VERSION = 3;

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
