package com.shumidub.todoapprealm.realmmodel.task

import com.shumidub.todoapprealm.realmmodel.RealmInteger
import io.realm.kotlin.ext.realmListOf
import io.realm.kotlin.types.RealmList
import io.realm.kotlin.types.RealmObject

class TaskObject : RealmObject {
    var id: Long = 0
    var text: String? = null
    var done: Boolean = false
    var taskFolderId: Long = 0
    var priority: Int = 0
    var lastDoneDate: Int = 0
    var isCycling: Boolean = false
    var countValue: Int = 0
    var maxAccumulation: Int = 0
    var countAccumulation: Int = 0
    var dateCountAccumulation: RealmList<RealmInteger> = realmListOf()
}
