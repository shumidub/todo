package com.shumidub.todoapprealm.data.report

data class ReportSnapshot(
    val id: Long,
    val date: String,
    val countOfDay: Int,
    val reportText: String,
    val soulRating: Int,
    val healthRating: Int,
    val phinanceRating: Int,
    val englishRating: Int,
    val socialRating: Int,
    val famillyRating: Int,
    val weekNumber: Int,
    val isWeekReport: Boolean,
)
