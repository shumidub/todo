package com.shumidub.todoapprealm.ui.tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shumidub.todoapprealm.data.tasks.TaskSnapshot

private val Accent = Color(0xFFE47C5D)

@Composable
fun EditTaskDialog(
    task: TaskSnapshot,
    onDismiss: () -> Unit,
    onConfirm: (
        text: String,
        countValue: Int,
        maxAccumulation: Int,
        isCycling: Boolean,
        priority: Int,
    ) -> Unit,
) {
    var text by remember(task.id) { mutableStateOf(task.text) }
    var count by remember(task.id) { mutableIntStateOf(task.countValue.coerceAtLeast(1)) }
    var maxAcc by remember(task.id) { mutableIntStateOf(task.maxAccumulation.coerceAtLeast(1)) }
    var priority by remember(task.id) { mutableIntStateOf(task.priority.coerceIn(0, 3)) }
    var cycling by remember(task.id) { mutableStateOf(task.isCycling) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit task") },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = {
                        text = it
                        if (error != null && it.isNotBlank()) error = null
                    },
                    label = { Text("Task") },
                    isError = error != null,
                    supportingText = { error?.let { Text(it) } },
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Chip(count.toString(), count > 1) { count = cycle(count) }
                    Chip(maxAcc.toString(), maxAcc > 1) { maxAcc = cycle(maxAcc) }
                    Chip(priorityLabel(priority), priority > 0) { priority = (priority + 1) % 4 }
                    Chip("⟳", cycling) { cycling = !cycling }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (text.isBlank()) {
                    error = "Should be filled"
                } else {
                    onConfirm(text, count, maxAcc, cycling, priority)
                }
            }) { Text("EDIT") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun Chip(text: String, accent: Boolean, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.width(56.dp)) {
        Text(
            text = text,
            color = if (accent) Accent else MaterialTheme.colorScheme.onSurface,
            fontSize = 16.sp,
        )
    }
}

private fun cycle(current: Int): Int = if (current < 9) current + 1 else 1
private fun priorityLabel(priority: Int): String =
    if (priority <= 0) "!" else "!".repeat(priority)
