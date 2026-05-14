package com.shumidub.todoapprealm.ui.tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Accent = Color(0xFFE47C5D)

@Composable
fun AddTaskBottomBar(
    form: AddTaskForm,
    onTextChange: (String) -> Unit,
    onCountClick: () -> Unit,
    onMaxAccumulationClick: () -> Unit,
    onPriorityClick: () -> Unit,
    onCyclingClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            OutlinedTextField(
                value = form.text,
                onValueChange = onTextChange,
                placeholder = { Text("New task") },
                modifier = Modifier.weight(1f),
                singleLine = true,
            )
            ChipButton(
                text = form.countValue.toString(),
                accent = form.countValue > 1,
                onClick = onCountClick,
            )
            ChipButton(
                text = form.maxAccumulation.toString(),
                accent = form.maxAccumulation > 1,
                onClick = onMaxAccumulationClick,
            )
            ChipButton(
                text = priorityLabel(form.priority),
                accent = form.priority > 0,
                onClick = onPriorityClick,
            )
            ChipButton(
                text = "⟳",
                accent = form.isCycling,
                onClick = onCyclingClick,
            )
        }
    }
}

@Composable
private fun ChipButton(text: String, accent: Boolean, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.width(48.dp),
    ) {
        Text(
            text = text,
            color = if (accent) Accent else MaterialTheme.colorScheme.onPrimaryContainer,
            fontSize = 16.sp,
        )
    }
}

private fun priorityLabel(priority: Int): String =
    if (priority <= 0) "!" else "!".repeat(priority)
