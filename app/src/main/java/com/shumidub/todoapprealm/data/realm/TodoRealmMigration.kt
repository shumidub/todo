package com.shumidub.todoapprealm.data.realm

import io.realm.DynamicRealm
import io.realm.RealmMigration

const val REALM_SCHEMA_VERSION: Long = 1

class TodoRealmMigration : RealmMigration {
    override fun migrate(realm: DynamicRealm, oldVersion: Long, newVersion: Long) {
        // Schema is currently at version 1 — no transformations to apply.
        // When the model changes, bump REALM_SCHEMA_VERSION and add chained
        // upgrades here, e.g.:
        //
        // if (oldVersion < 2) {
        //     realm.schema.get("TaskObject")
        //         ?.addField("notes", String::class.java)
        // }
        // if (oldVersion < 3) { ... }
    }

    override fun hashCode(): Int = TodoRealmMigration::class.hashCode()
    override fun equals(other: Any?): Boolean = other is TodoRealmMigration
}
