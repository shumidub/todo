package com.shumidub.todoapprealm.ui.tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shumidub.todoapprealm.data.tasks.FolderTaskSnapshot
import com.shumidub.todoapprealm.ui.theme.TodoTheme
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    bottomSheetState: SheetState = rememberStandardBottomSheetState(
        initialValue = SheetValue.PartiallyExpanded,
        skipHiddenState = true,
    ),
    viewModel: TasksViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val scaffoldState = rememberBottomSheetScaffoldState(bottomSheetState = bottomSheetState)

    TodoTheme {
        BottomSheetScaffold(
            scaffoldState = scaffoldState,
            sheetPeekHeight = 72.dp,
            sheetContent = {
                TaskSheetContent(state = state, viewModel = viewModel)
            },
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
            ) {
                FoldersList(
                    state = state,
                    onFolderClick = { folder ->
                        if (state.addForm.text.isNotBlank()) {
                            viewModel.submitAddTask(folder.id)
                        } else {
                            val index = state.folders.indexOfFirst { it.id == folder.id }
                            if (index >= 0) viewModel.selectFolder(index)
                            scope.launch { bottomSheetState.expand() }
                        }
                    },
                    onFolderLongClick = viewModel::openFolderActions,
                    onReorder = viewModel::reorderFolder,
                    modifier = Modifier.weight(1f),
                )
                AddTaskBottomBar(
                    form = state.addForm,
                    onTextChange = viewModel::onAddFormTextChange,
                    onCountClick = viewModel::cycleCount,
                    onMaxAccumulationClick = viewModel::cycleMaxAccumulation,
                    onPriorityClick = viewModel::cyclePriority,
                    onCyclingClick = viewModel::toggleCycling,
                )
            }
        }

        TasksDialogs(state = state, viewModel = viewModel)
    }
}

@Composable
private fun FoldersList(
    state: TasksState,
    onFolderClick: (FolderTaskSnapshot) -> Unit,
    onFolderLongClick: (Long) -> Unit,
    onReorder: (from: Int, to: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.folders.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "No folders yet",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }
    val lazyListState = rememberLazyListState()
    val reorderable = rememberReorderableLazyListState(lazyListState) { from, to ->
        onReorder(from.index, to.index)
    }
    LazyColumn(
        state = lazyListState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        items(state.folders, key = { it.id }) { folder ->
            ReorderableItem(reorderable, key = folder.id) { isDragging ->
                FolderTaskCard(
                    folder = folder,
                    taskCount = state.tasksByFolder[folder.id]?.count { !it.isDone } ?: 0,
                    onClick = { onFolderClick(folder) },
                    onLongClick = { onFolderLongClick(folder.id) },
                    isDragging = isDragging,
                    dragHandleModifier = Modifier.draggableHandle(),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskSheetContent(state: TasksState, viewModel: TasksViewModel) {
    if (state.folders.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Create a folder first",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    val pagerState = rememberPagerState(
        initialPage = state.selectedFolderIndex,
        pageCount = { state.folders.size },
    )
    LaunchedEffect(state.selectedFolderIndex, state.folders.size) {
        if (state.selectedFolderIndex < state.folders.size &&
            pagerState.currentPage != state.selectedFolderIndex
        ) {
            pagerState.scrollToPage(state.selectedFolderIndex)
        }
    }
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            viewModel.selectFolder(page)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        val title = state.folders.getOrNull(state.selectedFolderIndex)?.name ?: "Tasks"
        Text(
            text = title,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            val folder = state.folders[page]
            TasksPage(
                folderId = folder.id,
                tasks = state.tasksByFolder[folder.id].orEmpty(),
                showDoneTasks = state.showDoneTasks,
                onToggleShowDone = viewModel::toggleShowDoneTasks,
                onTaskCheckedChange = viewModel::toggleTaskDone,
                onPriorityClick = viewModel::cycleTaskPriority,
                onTaskLongClick = viewModel::openTaskActions,
                onReorder = { from, to -> viewModel.reorderTask(folder.id, from, to) },
            )
        }
    }
}

@Composable
private fun TasksPage(
    folderId: Long,
    tasks: List<com.shumidub.todoapprealm.data.tasks.TaskSnapshot>,
    showDoneTasks: Boolean,
    onToggleShowDone: () -> Unit,
    onTaskCheckedChange: (taskId: Long, done: Boolean) -> Unit,
    onPriorityClick: (taskId: Long, current: Int) -> Unit,
    onTaskLongClick: (taskId: Long) -> Unit,
    onReorder: (from: Int, to: Int) -> Unit,
) {
    val notDone = tasks.filter { !it.isDone }
    val done = tasks.filter { it.isDone }
    val visible = if (showDoneTasks) notDone + done else notDone

    val lazyListState = rememberLazyListState()
    val reorderable = rememberReorderableLazyListState(lazyListState) { from, to ->
        onReorder(from.index, to.index)
    }

    LazyColumn(
        state = lazyListState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
    ) {
        if (visible.isEmpty() && done.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "No tasks yet",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        items(visible, key = { it.id }) { task ->
            ReorderableItem(reorderable, key = task.id) { isDragging ->
                TaskCard(
                    task = task,
                    onCheckedChange = { onTaskCheckedChange(task.id, it) },
                    onPriorityClick = { onPriorityClick(task.id, task.priority) },
                    onClick = { onTaskLongClick(task.id) },
                    onLongClick = { onTaskLongClick(task.id) },
                    isDragging = isDragging,
                    dragHandleModifier = Modifier.draggableHandle(),
                )
            }
        }
        if (done.isNotEmpty()) {
            item(key = "done-footer") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    TextButton(onClick = onToggleShowDone) {
                        val label = if (showDoneTasks) "Hide done" else "Done ${done.size} tasks"
                        Text(label)
                    }
                }
            }
        }
    }
}

@Composable
private fun TasksDialogs(state: TasksState, viewModel: TasksViewModel) {
    when (val dialog = state.dialog) {
        TasksDialog.None -> Unit

        TasksDialog.AddFolder -> FolderTaskDialog(
            title = "Add folder",
            positiveText = "Add",
            initialName = "",
            initialDaily = false,
            onDismiss = viewModel::dismissDialog,
            onConfirm = viewModel::addFolder,
        )

        is TasksDialog.FolderActions -> ActionsDialog(
            title = "Folder",
            onEdit = { viewModel.openEditFolder(dialog.folderId) },
            onDelete = { viewModel.openDeleteFolder(dialog.folderId) },
            onDismiss = viewModel::dismissDialog,
        )

        is TasksDialog.EditFolder -> FolderTaskDialog(
            title = "Edit folder",
            positiveText = "Save",
            initialName = dialog.folder.name,
            initialDaily = dialog.folder.isDaily,
            onDismiss = viewModel::dismissDialog,
            onConfirm = { name, isDaily -> viewModel.saveFolder(dialog.folder.id, name, isDaily) },
        )

        is TasksDialog.DeleteFolder -> AlertDialog(
            onDismissRequest = viewModel::dismissDialog,
            title = { Text("Delete folder") },
            text = { Text("Are you sure?") },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmDeleteFolder(dialog.id) }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissDialog) { Text("Cancel") }
            },
        )

        is TasksDialog.TaskActions -> ActionsDialog(
            title = "Task",
            onEdit = { viewModel.openEditTask(dialog.taskId) },
            onDelete = { viewModel.openDeleteTask(dialog.taskId) },
            onDismiss = viewModel::dismissDialog,
        )

        is TasksDialog.EditTask -> EditTaskDialog(
            task = dialog.task,
            onDismiss = viewModel::dismissDialog,
            onConfirm = { text, count, max, cycling, priority ->
                viewModel.saveTask(dialog.task.id, text, count, max, cycling, priority)
            },
        )

        is TasksDialog.DeleteTask -> AlertDialog(
            onDismissRequest = viewModel::dismissDialog,
            title = { Text("Delete task") },
            text = { Text("Are you sure?") },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmDeleteTask(dialog.id) }) { Text("Delete") }
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

