package com.shumidub.todoapprealm.realmcontrollers.reportcontroller

import com.shumidub.todoapprealm.App
import com.shumidub.todoapprealm.realmmodel.RealmFoldersContainer
import com.shumidub.todoapprealm.realmmodel.report.ReportObject
import io.realm.kotlin.ext.query

object ReportRealmController {

    fun getReportList(): List<ReportObject> =
        App.realm.query<RealmFoldersContainer>().first().find()
            ?.reportObjectList
            ?.toList()
            .orEmpty()

    fun getReport(id: Long): ReportObject? =
        App.realm.query<ReportObject>("id == $0", id).first().find()

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
        val newId = getValidId()
        App.realm.writeBlocking {
            val container = query<RealmFoldersContainer>().first().find() ?: return@writeBlocking
            val report = copyToRealm(ReportObject().apply {
                this.id = newId
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
            })
            container.reportObjectList.add(report)
        }
        return newId
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
        App.realm.writeBlocking {
            val report = query<ReportObject>("id == $0", id).first().find() ?: return@writeBlocking
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
        App.realm.writeBlocking {
            val report = query<ReportObject>("id == $0", id).first().find() ?: return@writeBlocking
            delete(report)
        }
    }

    private fun getValidId(): Long {
        var id = System.currentTimeMillis()
        while (App.realm.query<ReportObject>("id == $0", id).first().find() != null) {
            id++
        }
        return id
    }
}
