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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.shumidub.todoapprealm.data.report.ReportSnapshot
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private data class ReportForm(
    val date: String,
    val countValue: String,
    val text: String,
    val soul: Int,
    val health: Int,
    val phinance: Int,
    val english: Int,
    val social: Int,
    val familly: Int,
    val isWeek: Boolean,
)

@Composable
fun AddReportDialog(
    onDismiss: () -> Unit,
    onConfirm: (
        date: String,
        dayCount: Int,
        text: String,
        soul: Int, health: Int, phinance: Int,
        english: Int, social: Int, familly: Int,
        isWeekReport: Boolean, weekNumber: Int,
    ) -> Unit,
) {
    val today = remember { SimpleDateFormat("dd.MM.yyyy", Locale.US).format(Date()) }
    val currentWeek = remember { Calendar.getInstance().get(Calendar.WEEK_OF_YEAR) }

    var form by remember {
        mutableStateOf(
            ReportForm(
                date = today,
                countValue = "0",
                text = "",
                soul = 0, health = 0, phinance = 0,
                english = 0, social = 0, familly = 0,
                isWeek = false,
            )
        )
    }

    ReportFormDialog(
        title = "Add new report",
        positiveText = "ADD",
        form = form,
        onFormChange = { form = it },
        showWeekToggle = true,
        currentWeek = currentWeek,
        today = today,
        onDismiss = onDismiss,
        onConfirm = { validated ->
            val count = validated.countValue.toInt()
            val date: String
            val weekNumber: Int
            if (validated.isWeek) {
                weekNumber = validated.date.toInt()
                val cal = Calendar.getInstance()
                date = "${cal.get(Calendar.DATE)}.${cal.get(Calendar.MONTH)}.${cal.get(Calendar.YEAR)}"
            } else {
                date = validated.date
                weekNumber = currentWeek
            }
            onConfirm(
                date, count, validated.text,
                validated.soul, validated.health, validated.phinance,
                validated.english, validated.social, validated.familly,
                validated.isWeek, weekNumber,
            )
        },
    )
}

@Composable
fun EditReportDialog(
    report: ReportSnapshot,
    onDismiss: () -> Unit,
    onConfirm: (
        date: String,
        dayCount: Int,
        text: String,
        soul: Int, health: Int, phinance: Int,
        english: Int, social: Int, familly: Int,
        weekNumber: Int,
    ) -> Unit,
) {
    val currentWeek = remember { Calendar.getInstance().get(Calendar.WEEK_OF_YEAR) }
    val today = remember { SimpleDateFormat("dd.MM.yyyy", Locale.US).format(Date()) }

    var form by remember {
        mutableStateOf(
            ReportForm(
                date = if (report.isWeekReport) report.weekNumber.toString() else report.date,
                countValue = report.countOfDay.toString(),
                text = report.reportText,
                soul = report.soulRating,
                health = report.healthRating,
                phinance = report.phinanceRating,
                english = report.englishRating,
                social = report.socialRating,
                familly = report.famillyRating,
                isWeek = report.isWeekReport,
            )
        )
    }

    ReportFormDialog(
        title = "Edit report",
        positiveText = "EDIT",
        form = form,
        onFormChange = { form = it },
        showWeekToggle = false,
        currentWeek = currentWeek,
        today = today,
        onDismiss = onDismiss,
        onConfirm = { validated ->
            val count = validated.countValue.toInt()
            val date: String
            val weekNumber: Int
            if (validated.isWeek) {
                weekNumber = validated.date.toInt()
                val cal = Calendar.getInstance()
                date = "${cal.get(Calendar.DATE)}.${cal.get(Calendar.MONTH)}.${cal.get(Calendar.YEAR)}"
            } else {
                date = validated.date
                weekNumber = currentWeek
            }
            onConfirm(
                date, count, validated.text,
                validated.soul, validated.health, validated.phinance,
                validated.english, validated.social, validated.familly,
                weekNumber,
            )
        },
    )
}

@Composable
private fun ReportFormDialog(
    title: String,
    positiveText: String,
    form: ReportForm,
    onFormChange: (ReportForm) -> Unit,
    showWeekToggle: Boolean,
    currentWeek: Int,
    today: String,
    onDismiss: () -> Unit,
    onConfirm: (ReportForm) -> Unit,
) {
    var dateError by remember { mutableStateOf<String?>(null) }
    var countError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = form.date,
                    onValueChange = { onFormChange(form.copy(date = it)) },
                    label = { Text(if (form.isWeek) "Week number" else "Date") },
                    isError = dateError != null,
                    supportingText = { dateError?.let { Text(it) } },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = form.countValue,
                    onValueChange = { onFormChange(form.copy(countValue = it.filter { ch -> ch.isDigit() })) },
                    label = { Text(if (form.isWeek) "Week count" else "Day count") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = countError != null,
                    supportingText = { countError?.let { Text(it) } },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                )
                OutlinedTextField(
                    value = form.text,
                    onValueChange = { onFormChange(form.copy(text = it)) },
                    label = { Text("Report text") },
                    minLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                )

                RatingSlider("Soul", form.soul) { onFormChange(form.copy(soul = it)) }
                RatingSlider("Health", form.health) { onFormChange(form.copy(health = it)) }
                RatingSlider("Phinance", form.phinance) { onFormChange(form.copy(phinance = it)) }
                RatingSlider("English", form.english) { onFormChange(form.copy(english = it)) }
                RatingSlider("Social", form.social) { onFormChange(form.copy(social = it)) }
                RatingSlider("Familly", form.familly) { onFormChange(form.copy(familly = it)) }

                if (showWeekToggle) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Week report",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Switch(
                            checked = form.isWeek,
                            onCheckedChange = { checked ->
                                onFormChange(
                                    form.copy(
                                        isWeek = checked,
                                        date = if (checked) currentWeek.toString() else today,
                                    )
                                )
                            },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val (dErr, cErr) = validate(form, currentWeek)
                dateError = dErr
                countError = cErr
                if (dErr == null && cErr == null) {
                    onConfirm(form)
                }
            }) { Text(positiveText) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun RatingSlider(label: String, value: Int, onChange: (Int) -> Unit) {
    var v by remember(value) { mutableIntStateOf(value) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, modifier = Modifier.weight(0.3f), style = MaterialTheme.typography.bodySmall)
        Slider(
            value = v.toFloat(),
            onValueChange = { v = it.toInt() },
            onValueChangeFinished = { onChange(v) },
            valueRange = 0f..5f,
            steps = 4,
            modifier = Modifier.weight(0.7f),
        )
        Text(v.toString(), style = MaterialTheme.typography.bodySmall)
    }
}

private fun validate(form: ReportForm, currentWeek: Int): Pair<String?, String?> {
    val dateError: String? = when {
        form.date.isEmpty() -> "Should be filled"
        !form.isWeek && form.date.length != 10 -> "Not valid date"
        form.isWeek && form.date.toIntOrNull().let { it != currentWeek && it != currentWeek - 1 } ->
            "Not valid week number"
        else -> null
    }
    val countError: String? = when {
        form.countValue.isEmpty() -> "Should be filled"
        (form.countValue.toIntOrNull() ?: Int.MAX_VALUE) >= 500 -> "Count value too match"
        else -> null
    }
    return dateError to countError
}
