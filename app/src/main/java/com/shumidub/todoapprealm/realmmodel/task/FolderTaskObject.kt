package com.shumidub.todoapprealm.realmmodel.task

import io.realm.kotlin.ext.realmListOf
import io.realm.kotlin.types.RealmList
import io.realm.kotlin.types.RealmObject

class FolderTaskObject : RealmObject {
    var id: Long = 0
    var name: String? = null
    var folderTasks: RealmList<TaskObject> = realmListOf()
    var isDaily: Boolean = false
}
