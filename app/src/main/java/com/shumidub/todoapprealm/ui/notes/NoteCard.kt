package com.shumidub.todoapprealm.ui.notes

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteCard(
    text: String,
    onClick: (() -> Unit)?,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .combinedClickable(
                onClick = onClick ?: {},
                onLongClick = onLongClick,
                enabled = true,
            ),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(12.dp),
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
