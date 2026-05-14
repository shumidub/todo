package com.shumidub.todoapprealm.ui.reports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shumidub.todoapprealm.data.report.ReportSnapshot

@Composable
fun FullSizeReportDialog(report: ReportSnapshot, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = if (report.isWeekReport) "Week ${report.weekNumber}" else report.date,
                    fontSize = 18.sp,
                )
                Text(
                    text = report.countOfDay.toString(),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                RatingsBlock(report)
                if (report.reportText.isNotBlank()) {
                    Text(
                        text = report.reportText,
                        modifier = Modifier.padding(top = 12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("OK") }
        },
    )
}

@Composable
private fun RatingsBlock(report: ReportSnapshot) {
    RatingRow("Soul", report.soulRating)
    RatingRow("Health", report.healthRating)
    RatingRow("Phinance", report.phinanceRating)
    RatingRow("English", report.englishRating)
    RatingRow("Social", report.socialRating)
    RatingRow("Familly", report.famillyRating)
}

@Composable
private fun RatingRow(label: String, rating: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text("★".repeat(rating.coerceIn(0, 5)), color = MaterialTheme.colorScheme.primary)
    }
}
