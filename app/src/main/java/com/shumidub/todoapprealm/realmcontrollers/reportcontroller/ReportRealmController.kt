package com.shumidub.todoapprealm.realmcontrollers.reportcontroller

import com.shumidub.todoapprealm.App
import com.shumidub.todoapprealm.realmmodel.report.ReportObject
import io.realm.RealmList

object ReportRealmController {

    fun getReportList(): RealmList<ReportObject> {
        App.initRealm()
        return App.realmFoldersContainer?.reportObjectList ?: RealmList()
    }

    fun getReport(id: Long): ReportObject? {
        App.initRealm()
        return App.realm.where(ReportObject::class.java).equalTo("id", id).findFirst()
    }

    fun addReport(
        date: String,
        dayCount: Int,
        textReport: String,
        soulRating: Int,
        healthRating: Int,
        phinanceRating: Int,
        englishRating: Int,
        socialRating: Int,
        famillyRating: Int,
        isWeekReport: Boolean,
        weekNumber: Int,
    ): Long {
        val id = getValidId()
        App.initRealm()
        App.realm.executeTransaction { realm ->
            val report = realm.createObject(ReportObject::class.java).apply {
                this.id = id
                this.date = date
                this.countOfDay = dayCount
                this.reportText = textReport
                this.soulRating = soulRating
                this.healthRating = healthRating
                this.phinanceRating = phinanceRating
                this.englishRating = englishRating
                this.socialRating = socialRating
                this.famillyRating = famillyRating
                this.isWeekReport = isWeekReport
                this.weekNumber = weekNumber
            }
            App.realmFoldersContainer?.reportObjectList?.add(report)
        }
        return id
    }

    fun editReport(
        id: Long,
        date: String,
        dayCount: Int,
        textReport: String,
        soulRating: Int,
        healthRating: Int,
        phinanceRating: Int,
        englishRating: Int,
        socialRating: Int,
        famillyRating: Int,
        weekNumber: Int,
    ) {
        App.initRealm()
        val report = getReport(id) ?: return
        App.realm.executeTransaction {
            report.date = date
            report.countOfDay = dayCount
            report.reportText = textReport
            report.soulRating = soulRating
            report.healthRating = healthRating
            report.phinanceRating = phinanceRating
            report.englishRating = englishRating
            report.socialRating = socialRating
            report.famillyRating = famillyRating
            report.weekNumber = weekNumber
        }
    }

    fun delReport(id: Long) {
        App.initRealm()
        val report = getReport(id) ?: return
        val deletion: () -> Unit = {
            App.realmFoldersContainer?.reportObjectList?.remove(report)
            report.deleteFromRealm()
            App.realm.where(ReportObject::class.java)
                .equalTo("id", id)
                .findAll()
                .deleteAllFromRealm()
        }
        if (App.realm.isInTransaction) deletion() else App.realm.executeTransaction { deletion() }
    }

    private fun getValidId(): Long {
        App.initRealm()
        var id = System.currentTimeMillis()
        while (App.realm.where(ReportObject::class.java).equalTo("id", id).findFirst() != null) {
            id++
        }
        return id
    }
}
