package com.shumidub.todoapprealm.data.report

import com.shumidub.todoapprealm.realmcontrollers.reportcontroller.ReportRealmController
import com.shumidub.todoapprealm.realmmodel.report.ReportObject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReportRepository @Inject constructor() {

    private val _reports = MutableStateFlow<List<ReportSnapshot>>(emptyList())
    val reports: StateFlow<List<ReportSnapshot>> = _reports.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        _reports.value = ReportRealmController.getReportList().map { it.toSnapshot() }
    }

    fun getReport(id: Long): ReportSnapshot? =
        ReportRealmController.getReport(id)?.toSnapshot()

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
        val id = ReportRealmController.addReport(
            date, dayCount, textReport,
            soulRating, healthRating, phinanceRating,
            englishRating, socialRating, famillyRating,
            isWeekReport, weekNumber,
        )
        refresh()
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
        ReportRealmController.editReport(
            id, date, dayCount, textReport,
            soulRating, healthRating, phinanceRating,
            englishRating, socialRating, famillyRating,
            weekNumber,
        )
        refresh()
    }

    fun deleteReport(id: Long) {
        ReportRealmController.delReport(id)
        refresh()
    }

    private fun ReportObject.toSnapshot(): ReportSnapshot = ReportSnapshot(
        id = id,
        date = date.orEmpty(),
        countOfDay = countOfDay,
        reportText = reportText.orEmpty(),
        soulRating = soulRating,
        healthRating = healthRating,
        phinanceRating = phinanceRating,
        englishRating = englishRating,
        socialRating = socialRating,
        famillyRating = famillyRating,
        weekNumber = weekNumber,
        isWeekReport = isWeekReport,
    )
}
