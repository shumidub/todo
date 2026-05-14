package com.shumidub.todoapprealm.realmmodel.notes

import io.realm.RealmList
import io.realm.RealmObject

open class FolderNotesObject : RealmObject() {
    var id: Long = 0
    var name: String? = null
    var notesObjectRealmList: RealmList<NoteObject> = RealmList()
}
