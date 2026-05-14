package com.shumidub.todoapprealm.ui.reports

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shumidub.todoapprealm.data.report.ReportSnapshot
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private val Count100 = Color(0xFF0E9E00)
private val Count80 = Color(0xFFF7E51F)
private val Count60 = Color(0xFFF2641D)
private val Count0 = Color(0xFFD20808)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReportCard(
    report: ReportSnapshot,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        colors = CardDefaults.cardColors(
            containerColor = if (report.isWeekReport) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Header(report)
            RatingRow(report)
            if (report.reportText.isNotBlank()) {
                Text(
                    text = report.reportText,
                    modifier = Modifier.padding(top = 8.dp),
                    maxLines = 7,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
private fun Header(report: ReportSnapshot) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = formatDate(report),
            fontSize = 17.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(end = 8.dp),
        )
        if (report.isWeekReport) {
            Text(
                text = "Week count",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 8.dp),
            )
        }
        Text(
            text = report.countOfDay.toString(),
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = countColor(report),
        )
    }
}

@Composable
private fun RatingRow(report: ReportSnapshot) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        RatingBadge("Soul", report.soulRating)
        RatingBadge("Health", report.healthRating)
        RatingBadge("$", report.phinanceRating)
        RatingBadge("Eng", report.englishRating)
        RatingBadge("Soc", report.socialRating)
        RatingBadge("Fam", report.famillyRating)
    }
}

@Composable
private fun RatingBadge(label: String, rating: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = "★".repeat(rating.coerceIn(0, 5)), fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
    }
}

private fun formatDate(report: ReportSnapshot): String {
    if (report.isWeekReport) return "Week ${report.weekNumber}"
    return runCatching {
        val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.US)
        val parsed = sdf.parse(report.date) ?: return report.date
        val calendar = Calendar.getInstance().apply { time = parsed }
        "Day ${calendar.get(Calendar.DAY_OF_YEAR)} (${report.date})"
    }.getOrDefault(report.date)
}

private fun countColor(report: ReportSnapshot): Color {
    val count = report.countOfDay
    return if (report.isWeekReport) {
        when {
            count >= 50 -> Count100
            count >= 40 -> Count80
            count >= 30 -> Count60
            else -> Count0
        }
    } else {
        when {
            count >= 100 -> Count100
            count >= 80 -> Count80
            count >= 60 -> Count60
            else -> Count0
        }
    }
}
