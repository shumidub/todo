package com.shumidub.todoapprealm.ui.reports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shumidub.todoapprealm.ui.theme.TodoTheme

@Composable
fun ReportsScreen(viewModel: ReportsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    TodoTheme {
        Scaffold(
            floatingActionButton = {
                FloatingActionButton(onClick = viewModel::openAdd) {
                    Icon(Icons.Default.Add, contentDescription = "Add report")
                }
            },
        ) { padding ->
            if (state.reports.isEmpty()) {
                EmptyState(padding)
            } else {
                ReportsList(
                    state = state,
                    onClick = viewModel::openFullSize,
                    onLongClick = viewModel::openActions,
                    contentPadding = padding,
                )
            }
        }

        when (val dialog = state.dialog) {
            ReportDialog.None -> Unit

            ReportDialog.Add -> AddReportDialog(
                onDismiss = viewModel::dismissDialog,
                onConfirm = viewModel::addReport,
            )

            is ReportDialog.Edit -> EditReportDialog(
                report = dialog.report,
                onDismiss = viewModel::dismissDialog,
                onConfirm = { date, count, text, soul, health, phin, eng, soc, fam, week ->
                    viewModel.editReport(
                        id = dialog.report.id,
                        date = date,
                        dayCount = count,
                        textReport = text,
                        soulRating = soul,
                        healthRating = health,
                        phinanceRating = phin,
                        englishRating = eng,
                        socialRating = soc,
                        famillyRating = fam,
                        weekNumber = week,
                    )
                },
            )

            is ReportDialog.FullSize -> FullSizeReportDialog(
                report = dialog.report,
                onDismiss = viewModel::dismissDialog,
            )

            is ReportDialog.Actions -> ReportActionsDialog(
                onEdit = { viewModel.openEdit(dialog.id) },
                onDelete = { viewModel.openDelete(dialog.id) },
                onDismiss = viewModel::dismissDialog,
            )

            is ReportDialog.Delete -> DeleteReportDialog(
                onDismiss = viewModel::dismissDialog,
                onConfirm = { viewModel.deleteReport(dialog.id) },
            )
        }
    }
}

@Composable
private fun EmptyState(padding: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "No reports yet",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ReportsList(
    state: ReportsState,
    onClick: (Long) -> Unit,
    onLongClick: (Long) -> Unit,
    contentPadding: PaddingValues,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 8.dp,
            end = 8.dp,
            top = contentPadding.calculateTopPadding() + 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 96.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        items(state.reports, key = { it.id }) { report ->
            ReportCard(
                report = report,
                onClick = { onClick(report.id) },
                onLongClick = { onLongClick(report.id) },
            )
        }
    }
}
