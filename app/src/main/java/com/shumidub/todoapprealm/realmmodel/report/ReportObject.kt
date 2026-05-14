package com.shumidub.todoapprealm.realmmodel.report

import io.realm.kotlin.types.RealmObject

class ReportObject : RealmObject {
    var id: Long = 0
    var date: String? = null
    var countOfDay: Int = 0
    var reportText: String? = null
    var soulRating: Int = 0
    var healthRating: Int = 0
    var phinanceRating: Int = 0
    var englishRating: Int = 0
    var socialRating: Int = 0
    var famillyRating: Int = 0
    var weekNumber: Int = 0
    var isWeekReport: Boolean = false
}
