package com.shumidub.todoapprealm.ui.notes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shumidub.todoapprealm.data.notes.FolderNoteSnapshot
import com.shumidub.todoapprealm.ui.theme.TodoTheme
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun NotesScreen(viewModel: NotesViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    TodoTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            NotesBody(
                state = state,
                onFolderClick = viewModel::openFolder,
                onFolderLongClick = viewModel::openFolderActions,
                onNoteLongClick = viewModel::openNoteActions,
                onReorderFolder = viewModel::reorderFolder,
                onReorderNote = viewModel::reorderNote,
            )
        }

        NotesDialogs(state = state, viewModel = viewModel)
    }
}

@Composable
private fun NotesBody(
    state: NotesState,
    onFolderClick: (FolderNoteSnapshot) -> Unit,
    onFolderLongClick: (Long) -> Unit,
    onNoteLongClick: (Long) -> Unit,
    onReorderFolder: (from: Int, to: Int) -> Unit,
    onReorderNote: (from: Int, to: Int) -> Unit,
) {
    when (state.mode) {
        NotesMode.Folders -> {
            if (state.folders.isEmpty()) {
                EmptyState("No folders yet")
            } else {
                ReorderableNoteList(
                    items = state.folders,
                    keyFor = { it.id },
                    textFor = { it.name },
                    onClick = { onFolderClick(it) },
                    onLongClick = { onFolderLongClick(it.id) },
                    onReorder = onReorderFolder,
                )
            }
        }

        is NotesMode.Notes -> {
            if (state.notes.isEmpty()) {
                EmptyState("No notes in this folder")
            } else {
                ReorderableNoteList(
                    items = state.notes,
                    keyFor = { it.id },
                    textFor = { it.text },
                    onClick = null,
                    onLongClick = { onNoteLongClick(it.id) },
                    onReorder = onReorderNote,
                )
            }
        }
    }
}

@Composable
private fun <T : Any> ReorderableNoteList(
    items: List<T>,
    keyFor: (T) -> Any,
    textFor: (T) -> String,
    onClick: ((T) -> Unit)?,
    onLongClick: (T) -> Unit,
    onReorder: (from: Int, to: Int) -> Unit,
) {
    val lazyListState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
        onReorder(from.index, to.index)
    }
    LazyColumn(
        state = lazyListState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        items(items, key = keyFor) { item ->
            ReorderableItem(reorderableState, key = keyFor(item)) { isDragging ->
                NoteCard(
                    text = textFor(item),
                    onClick = onClick?.let { { it(item) } },
                    onLongClick = { onLongClick(item) },
                    isDragging = isDragging,
                    dragHandleModifier = Modifier.draggableHandle(),
                )
            }
        }
    }
}

@Composable
private fun NotesDialogs(state: NotesState, viewModel: NotesViewModel) {
    when (val dialog = state.dialog) {
        NotesDialog.None -> Unit

        NotesDialog.AddFolder -> NoteTextDialog(
            title = "Add folder",
            initialText = "",
            positiveText = "Add",
            label = "Folder name",
            onDismiss = viewModel::dismissDialog,
            onConfirm = viewModel::addFolder,
        )

        is NotesDialog.AddNote -> NoteTextDialog(
            title = "Add note",
            initialText = "",
            positiveText = "Add",
            label = "Note text",
            onDismiss = viewModel::dismissDialog,
            onConfirm = { viewModel.addNote(dialog.folderId, it) },
        )

        is NotesDialog.EditFolder -> NoteTextDialog(
            title = "Edit folder",
            initialText = dialog.folder.name,
            positiveText = "Edit",
            label = "Folder name",
            onDismiss = viewModel::dismissDialog,
            onConfirm = { viewModel.saveFolder(dialog.folder.id, it) },
        )

        is NotesDialog.EditNote -> NoteTextDialog(
            title = "Edit note",
            initialText = dialog.note.text,
            positiveText = "Edit",
            label = "Note text",
            onDismiss = viewModel::dismissDialog,
            onConfirm = { viewModel.saveNote(dialog.note.id, it) },
        )

        is NotesDialog.FolderActions -> ActionsDialog(
            title = "Folder",
            onEdit = { viewModel.openEditFolder(dialog.folderId) },
            onDelete = { viewModel.openDeleteFolder(dialog.folderId) },
            onDismiss = viewModel::dismissDialog,
        )

        is NotesDialog.NoteActions -> ActionsDialog(
            title = "Note",
            onEdit = { viewModel.openEditNote(dialog.noteId) },
            onDelete = { viewModel.openDeleteNote(dialog.noteId) },
            onDismiss = viewModel::dismissDialog,
        )

        is NotesDialog.DeleteFolder -> AlertDialog(
            onDismissRequest = viewModel::dismissDialog,
            title = { Text("Delete") },
            text = { Text("Are you sure?") },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmDeleteFolder(dialog.id) }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissDialog) { Text("Cancel") }
            },
        )

        is NotesDialog.DeleteNote -> AlertDialog(
            onDismissRequest = viewModel::dismissDialog,
            title = { Text("Delete") },
            text = { Text("Are you sure?") },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmDeleteNote(dialog.id) }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissDialog) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun ActionsDialog(
    title: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onEdit, modifier = Modifier.fillMaxWidth()) {
                    Text("Edit", modifier = Modifier.fillMaxWidth())
                }
                TextButton(onClick = onDelete, modifier = Modifier.fillMaxWidth()) {
                    Text("Delete", modifier = Modifier.fillMaxWidth())
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun EmptyState(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
