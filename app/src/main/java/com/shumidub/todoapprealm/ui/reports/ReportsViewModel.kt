package com.shumidub.todoapprealm.ui.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shumidub.todoapprealm.data.report.ReportRepository
import com.shumidub.todoapprealm.data.report.ReportSnapshot
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val repository: ReportRepository,
) : ViewModel() {

    private val dialog = MutableStateFlow<ReportDialog>(ReportDialog.None)

    val state: StateFlow<ReportsState> = combine(repository.reports, dialog) { reports, dlg ->
        ReportsState(reports = reports, dialog = dlg)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, ReportsState())

    fun refresh() = repository.refresh()

    fun openAdd() {
        dialog.value = ReportDialog.Add
    }

    fun openEdit(id: Long) {
        val report = repository.getReport(id) ?: return
        dialog.value = ReportDialog.Edit(report)
    }

    fun openFullSize(id: Long) {
        val report = repository.getReport(id) ?: return
        dialog.value = ReportDialog.FullSize(report)
    }

    fun openActions(id: Long) {
        dialog.value = ReportDialog.Actions(id)
    }

    fun openDelete(id: Long) {
        dialog.value = ReportDialog.Delete(id)
    }

    fun dismissDialog() {
        dialog.value = ReportDialog.None
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
    ) {
        repository.addReport(
            date, dayCount, textReport,
            soulRating, healthRating, phinanceRating,
            englishRating, socialRating, famillyRating,
            isWeekReport, weekNumber,
        )
        dismissDialog()
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
        repository.editReport(
            id, date, dayCount, textReport,
            soulRating, healthRating, phinanceRating,
            englishRating, socialRating, famillyRating,
            weekNumber,
        )
        dismissDialog()
    }

    fun deleteReport(id: Long) {
        repository.deleteReport(id)
        dismissDialog()
    }
}

data class ReportsState(
    val reports: List<ReportSnapshot> = emptyList(),
    val dialog: ReportDialog = ReportDialog.None,
)

sealed interface ReportDialog {
    data object None : ReportDialog
    data object Add : ReportDialog
    data class Edit(val report: ReportSnapshot) : ReportDialog
    data class FullSize(val report: ReportSnapshot) : ReportDialog
    data class Actions(val id: Long) : ReportDialog
    data class Delete(val id: Long) : ReportDialog
}
