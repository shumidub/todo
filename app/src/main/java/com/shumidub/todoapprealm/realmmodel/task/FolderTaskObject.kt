package com.shumidub.todoapprealm.realmmodel.task

import io.realm.RealmList
import io.realm.RealmObject

open class FolderTaskObject : RealmObject() {
    var id: Long = 0
    var name: String? = null
    var folderTasks: RealmList<TaskObject> = RealmList()
    var isDaily: Boolean = false
}
