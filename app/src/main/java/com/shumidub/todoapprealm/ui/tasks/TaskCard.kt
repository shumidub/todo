package com.shumidub.todoapprealm.ui.tasks

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shumidub.todoapprealm.data.tasks.TaskSnapshot

private val Accent = Color(0xFFE47C5D)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TaskCard(
    task: TaskSnapshot,
    onCheckedChange: (Boolean) -> Unit,
    onPriorityClick: () -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDragging: Boolean = false,
    dragHandleModifier: Modifier? = null,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isDragging) 8.dp else 1.dp,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Checkbox(checked = task.isDone, onCheckedChange = onCheckedChange)
            Text(
                text = task.text,
                modifier = Modifier.weight(1f),
                color = if (task.isDone) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                textDecoration = if (task.isDone) TextDecoration.LineThrough else TextDecoration.None,
            )
            Text(
                text = priorityText(task.priority),
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .combinedClickable(onClick = onPriorityClick, onLongClick = onLongClick),
                color = if (task.priority > 0) Accent else MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 16.sp,
            )
            Text(
                text = task.countValue.toString(),
                modifier = Modifier.padding(horizontal = 4.dp),
                color = if (task.countValue > 1) Accent else MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 16.sp,
            )
            Text(
                text = "${task.countAccumulation}/${task.maxAccumulation}",
                modifier = Modifier.padding(horizontal = 4.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
            )
            if (dragHandleModifier != null) {
                Box(
                    modifier = dragHandleModifier
                        .padding(8.dp)
                        .size(20.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Drag to reorder",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private fun priorityText(priority: Int): String =
    if (priority <= 0) "!" else "!".repeat(priority)
