package com.shumidub.todoapprealm.data.realm

import com.shumidub.todoapprealm.realmmodel.RealmFoldersContainer
import com.shumidub.todoapprealm.realmmodel.RealmInteger
import com.shumidub.todoapprealm.realmmodel.notes.FolderNotesObject
import com.shumidub.todoapprealm.realmmodel.notes.NoteObject
import com.shumidub.todoapprealm.realmmodel.report.ReportObject
import com.shumidub.todoapprealm.realmmodel.task.FolderTaskObject
import com.shumidub.todoapprealm.realmmodel.task.TaskObject
import io.realm.kotlin.migration.AutomaticSchemaMigration
import io.realm.kotlin.types.BaseRealmObject
import kotlin.reflect.KClass

const val REALM_SCHEMA_VERSION: Long = 1

val REALM_SCHEMA: Set<KClass<out BaseRealmObject>> = setOf(
    RealmInteger::class,
    RealmFoldersContainer::class,
    TaskObject::class,
    FolderTaskObject::class,
    NoteObject::class,
    FolderNotesObject::class,
    ReportObject::class,
)

val todoRealmMigration = AutomaticSchemaMigration { _ ->
    // Schema is at version 1 — no transformations required. When a model
    // changes, bump REALM_SCHEMA_VERSION and add chained upgrades:
    //
    // if (migrationContext.oldRealm.schemaVersion() < 2) {
    //     migrationContext.enumerate("TaskObject") { _, newObject ->
    //         newObject?.set("notes", "")
    //     }
    // }
}
