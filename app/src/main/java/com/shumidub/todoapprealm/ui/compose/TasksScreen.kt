package com.shumidub.todoapprealm.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.shumidub.todoapprealm.data.FolderDto
import com.shumidub.todoapprealm.data.TaskDto
import com.shumidub.todoapprealm.ui.theme.TabPalette
import com.shumidub.todoapprealm.ui.theme.paletteForGroup

/**
 * Tasks UI for a group (0..3), modelled on the legacy folder-panel UX (docs/specs/folder-panel.md):
 * the screen shows **categories** (folders), not tasks. Tapping a category opens its tasks **from
 * the bottom** (a Material 3 [ModalBottomSheet]); new tasks are added from the bottom add-task panel
 * inside that sheet, with the points / max / priority / cycling toggles (hidden on the Notes group).
 *
 * Reads the existing Realm DB through [TasksViewModel] (detached DTO snapshots, invariant G1).
 * The custom 24dp-peek AnchoredDraggable panel, drag-reorder, sections and folder action-mode are
 * the remaining full-Phase-2 work; this delivers the core category → tasks-from-bottom → add loop.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(group: Int) {
    val vm: TasksViewModel = viewModel(
        key = "tasks-$group",
        factory = viewModelFactory { initializer { TasksViewModel(group) } },
    )
    val folders by vm.folders.collectAsStateWithLifecycle()
    val palette = paletteForGroup(group)

    var selectedId by remember { mutableStateOf<Long?>(null) }
    var showAddFolder by remember { mutableStateOf(false) }

    // If the open folder disappears (deleted/restored), close the sheet.
    LaunchedEffect(folders, selectedId) {
        if (selectedId != null && folders.none { it.id == selectedId }) selectedId = null
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (folders.isEmpty()) {
            EmptyState(palette = palette, onAddFolder = { showAddFolder = true })
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(folders, key = { it.id }) { folder ->
                    CategoryCard(folder = folder, palette = palette, onClick = { selectedId = folder.id })
                }
                item { AddCategoryButton(palette = palette, onClick = { showAddFolder = true }) }
            }
        }
    }

    val selected = folders.firstOrNull { it.id == selectedId }
    if (selected != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { selectedId = null },
            sheetState = sheetState,
            containerColor = palette.surface,
        ) {
            FolderSheet(folder = selected, group = group, palette = palette, vm = vm)
        }
    }

    if (showAddFolder) {
        AddFolderDialog(
            palette = palette,
            onConfirm = { name -> vm.addFolder(name); showAddFolder = false },
            onDismiss = { showAddFolder = false },
        )
    }
}

// ---- Category list ----

@Composable
private fun CategoryCard(folder: FolderDto, palette: TabPalette, onClick: () -> Unit) {
    val done = folder.tasks.sumOf { it.countAccumulation * it.countValue }
    val all = folder.tasks.sumOf { it.maxAccumulation * it.countValue }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = palette.surface),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = folder.name.ifBlank { "Без названия" },
                color = palette.inputText,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            Text(text = "$done/$all", color = palette.counter, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun AddCategoryButton(palette: TabPalette, onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Icon(Icons.Default.Add, contentDescription = null, tint = palette.text)
        Spacer(Modifier.width(6.dp))
        Text("Добавить категорию", color = palette.text)
    }
}

@Composable
private fun EmptyState(palette: TabPalette, onAddFolder: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Пока нет категорий", color = palette.text)
        Spacer(Modifier.height(12.dp))
        TextButton(onClick = onAddFolder) {
            Icon(Icons.Default.Add, contentDescription = null, tint = palette.accent)
            Spacer(Modifier.width(6.dp))
            Text("Добавить категорию", color = palette.accent)
        }
    }
}

// ---- Bottom sheet: a category's tasks + add panel ----

@Composable
private fun FolderSheet(folder: FolderDto, group: Int, palette: TabPalette, vm: TasksViewModel) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .navigationBarsPadding()
            .imePadding(),
    ) {
        Text(
            text = folder.name.ifBlank { "Без названия" },
            color = palette.inputText,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        HorizontalDivider(color = palette.inputText.copy(alpha = 0.12f))

        if (folder.tasks.isEmpty()) {
            Text(
                text = "Нет задач",
                color = palette.inputText.copy(alpha = 0.6f),
                modifier = Modifier.padding(vertical = 16.dp),
            )
        } else {
            LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                items(folder.tasks, key = { it.id }) { task ->
                    TaskRow(
                        task = task,
                        palette = palette,
                        onToggle = { id, done -> vm.toggleDone(id, done) },
                        onDelete = { id -> vm.deleteTask(id) },
                    )
                }
            }
        }

        HorizontalDivider(color = palette.inputText.copy(alpha = 0.12f))
        AddTaskPanel(folderId = folder.id, group = group, palette = palette, vm = vm)
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun TaskRow(task: TaskDto, palette: TabPalette, onToggle: (Long, Boolean) -> Unit, onDelete: (Long) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(
            checked = task.done,
            onCheckedChange = { checked -> onToggle(task.id, checked) },
            colors = CheckboxDefaults.colors(
                checkedColor = palette.accent,
                uncheckedColor = palette.inputText.copy(alpha = 0.5f),
                checkmarkColor = palette.surface,
            ),
        )
        Text(
            text = task.text,
            color = palette.inputText,
            textDecoration = if (task.done) TextDecoration.LineThrough else TextDecoration.None,
            modifier = Modifier.weight(1f),
        )
        if (task.maxAccumulation > 1) {
            Text(
                text = "${task.countAccumulation}/${task.maxAccumulation}",
                color = palette.inputText.copy(alpha = 0.6f),
            )
            Spacer(Modifier.width(4.dp))
        }
        IconButton(onClick = { onDelete(task.id) }) {
            Icon(Icons.Default.Delete, contentDescription = "Удалить", tint = palette.inputText.copy(alpha = 0.5f))
        }
    }
}

@Composable
private fun AddTaskPanel(folderId: Long, group: Int, palette: TabPalette, vm: TasksViewModel) {
    var text by remember(folderId) { mutableStateOf("") }
    // Add-task toggles (legacy bottom panel). Hidden on the Notes group (3).
    var count by remember(folderId) { mutableStateOf(1) }
    var max by remember(folderId) { mutableStateOf(1) }
    var priority by remember(folderId) { mutableStateOf(0) }
    var cycling by remember(folderId) { mutableStateOf(false) }

    fun submit() {
        if (text.isNotBlank()) {
            vm.addTask(folderId, text, count, max, cycling, priority)
            text = ""
            count = 1; max = 1; priority = 0; cycling = false
        }
    }

    Column {
        if (group != 3) {
            Row(
                modifier = Modifier.padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ToggleChip("×$count", active = count > 1, palette = palette) { count = if (count >= 10) 1 else count + 1 }
                ToggleChip("/$max", active = max > 1, palette = palette) { max = if (max >= 10) 1 else max + 1 }
                ToggleChip("!$priority", active = priority > 0, palette = palette) { priority = if (priority >= 3) 0 else priority + 1 }
                ToggleChip("↻", active = cycling, palette = palette) { cycling = !cycling }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text("Новая задача") },
                singleLine = true,
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = palette.inputText,
                    unfocusedTextColor = palette.inputText,
                    focusedBorderColor = palette.accent,
                    unfocusedBorderColor = palette.inputText.copy(alpha = 0.3f),
                    cursorColor = palette.accent,
                    focusedPlaceholderColor = palette.inputText.copy(alpha = 0.5f),
                    unfocusedPlaceholderColor = palette.inputText.copy(alpha = 0.5f),
                ),
            )
            IconButton(onClick = { submit() }) {
                Icon(Icons.Default.Add, contentDescription = "Добавить", tint = palette.accent)
            }
        }
    }
}

@Composable
private fun ToggleChip(label: String, active: Boolean, palette: TabPalette, onClick: () -> Unit) {
    val bg = if (active) palette.accent else palette.inputText.copy(alpha = 0.10f)
    val fg = if (active) palette.surface else palette.inputText.copy(alpha = 0.7f)
    Box(
        modifier = Modifier
            .background(bg, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(text = label, color = fg, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun AddFolderDialog(palette: TabPalette, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новая категория") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = { Text("Название") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    cursorColor = palette.accent,
                    focusedBorderColor = palette.accent,
                ),
            )
        },
        confirmButton = { TextButton(onClick = { if (name.isNotBlank()) onConfirm(name) }) { Text("Создать") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
        containerColor = Color.White,
    )
}
