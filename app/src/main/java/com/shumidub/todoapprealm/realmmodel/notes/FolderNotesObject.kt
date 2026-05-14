package com.shumidub.todoapprealm.realmmodel.notes

import io.realm.kotlin.ext.realmListOf
import io.realm.kotlin.types.RealmList
import io.realm.kotlin.types.RealmObject

class FolderNotesObject : RealmObject {
    var id: Long = 0
    var name: String? = null
    var notesObjectRealmList: RealmList<NoteObject> = realmListOf()
}
