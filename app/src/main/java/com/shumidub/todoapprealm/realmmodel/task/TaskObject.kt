package com.shumidub.todoapprealm.realmmodel.task

import com.shumidub.todoapprealm.realmmodel.RealmInteger
import io.realm.RealmList
import io.realm.RealmObject

open class TaskObject : RealmObject() {
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
    var dateCountAccumulation: RealmList<RealmInteger> = RealmList()

    fun clearDateCountAccumulation() {
        countAccumulation = 0
        dateCountAccumulation.clear()
    }

    fun addDateCountAccumulation(lastDateCount: Int) {
        if (dateCountAccumulation.size < maxAccumulation) {
            dateCountAccumulation.add(RealmInteger().apply { myInteger = lastDateCount })
        }
        countAccumulation = dateCountAccumulation.size
    }
}
