package com.shumidub.todoapprealm.realmmodel.notes

import io.realm.kotlin.types.RealmObject

class NoteObject : RealmObject {
    var id: Long = 0
    var idFolder: Long = 0
    var text: String? = null
}
