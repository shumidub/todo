package com.shumidub.todoapprealm.ui.compose

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import com.shumidub.todoapprealm.data.SectionDto
import com.shumidub.todoapprealm.data.TaskDto
import com.shumidub.todoapprealm.ui.theme.TabPalette
import com.shumidub.todoapprealm.ui.theme.paletteForGroup

@Composable
private fun groupVm(group: Int): TasksViewModel = viewModel(
    key = "tasks-$group",
    factory = viewModelFactory { initializer { TasksViewModel(group) } },
)

/**
 * Category LIST for a group (0..3): the day score + folder cards. Tapping a card opens that
 * category full-screen ([CategoryDetailScreen]) via [onOpenCategory]. Swiping left/right here
 * (handled by the outer pager in [MainScreen]) switches groups.
 */
@Composable
fun TasksScreen(group: Int, onOpenCategory: (Long) -> Unit) {
    val vm = groupVm(group)
    val state by vm.state.collectAsStateWithLifecycle()
    val folders = state.folders
    val palette = paletteForGroup(group)
    var showAddFolder by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (folders.isEmpty()) {
            EmptyState(palette = palette, onAddFolder = { showAddFolder = true })
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item { DayScoreHeader(dayScope = state.dayScope, palette = palette) }
                items(folders, key = { it.id }) { folder ->
                    CategoryCard(folder = folder, palette = palette, onClick = { onOpenCategory(folder.id) })
                }
                item { AddCategoryButton(palette = palette, onClick = { showAddFolder = true }) }
            }
        }
    }

    if (showAddFolder) {
        TextEntryDialog(
            title = "Новая категория", initial = "", palette = palette, confirmLabel = "Создать",
            onConfirm = { name -> vm.addFolder(name); showAddFolder = false },
            onDismiss = { showAddFolder = false },
        )
    }
}

/**
 * Full-screen view of a group's categories. A [HorizontalPager] over the group's folders —
 * swipe left/right to switch category — opened at [startFolderId]. Each page shows that
 * category's tasks (sections + free) and the bottom add-task panel; the top bar carries the
 * category name, a back arrow, and the folder menu (rename / move / delete / add section).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDetailScreen(group: Int, startFolderId: Long, onBack: () -> Unit) {
    val vm = groupVm(group)
    val state by vm.state.collectAsStateWithLifecycle()
    val folders = state.folders
    val palette = paletteForGroup(group)

    BackHandler(onBack = onBack)
    LaunchedEffect(folders.isEmpty()) { if (folders.isEmpty()) onBack() }
    if (folders.isEmpty()) return

    val startIndex = remember(startFolderId, folders.size) {
        folders.indexOfFirst { it.id == startFolderId }.coerceIn(0, folders.size - 1)
    }
    val pagerState = rememberPagerState(initialPage = startIndex) { folders.size }
    val currentPage = pagerState.currentPage.coerceIn(0, folders.size - 1)
    val currentFolder = folders.getOrNull(currentPage)

    var editingTaskId by remember { mutableStateOf<Long?>(null) }
    var menuOpen by remember { mutableStateOf(false) }
    var dialog by remember { mutableStateOf<FolderDialog?>(null) }

    Scaffold(
        containerColor = palette.bg,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад", tint = palette.text)
                    }
                },
                title = { Text(currentFolder?.name?.ifBlank { "Без названия" } ?: "", color = palette.text) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = palette.systemBar,
                    titleContentColor = palette.text,
                    navigationIconContentColor = palette.text,
                ),
                actions = {
                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Меню", tint = palette.text)
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            if (group != 3) {
                                DropdownMenuItem(text = { Text("Добавить секцию") }, onClick = { menuOpen = false; dialog = FolderDialog.AddSection })
                            }
                            DropdownMenuItem(text = { Text("Переименовать") }, onClick = { menuOpen = false; dialog = FolderDialog.Rename })
                            DropdownMenuItem(text = { Text("Переместить") }, onClick = { menuOpen = false; dialog = FolderDialog.Move })
                            DropdownMenuItem(text = { Text("Удалить категорию") }, onClick = { menuOpen = false; dialog = FolderDialog.Delete })
                        }
                    }
                },
            )
        },
    ) { inner ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize().padding(inner),
        ) { page ->
            folders.getOrNull(page)?.let { folder ->
                FolderTasksPage(folder = folder, group = group, palette = palette, vm = vm, onEditTask = { editingTaskId = it })
            }
        }
    }

    val editing = folders.flatMap { it.tasks }.firstOrNull { it.id == editingTaskId }
    if (editing != null) {
        TaskEditorDialog(task = editing, group = group, palette = palette, vm = vm, onDismiss = { editingTaskId = null })
    }

    if (currentFolder != null) {
        when (dialog) {
            FolderDialog.Rename -> TextEntryDialog(
                title = "Переименовать", initial = currentFolder.name, palette = palette, confirmLabel = "Сохранить",
                onConfirm = { vm.editFolder(currentFolder.id, it, currentFolder.isDaily); dialog = null }, onDismiss = { dialog = null },
            )
            FolderDialog.AddSection -> TextEntryDialog(
                title = "Новая секция", initial = "", palette = palette, confirmLabel = "Создать",
                onConfirm = { vm.addSection(currentFolder.id, it); dialog = null }, onDismiss = { dialog = null },
            )
            FolderDialog.Delete -> ConfirmDialog(
                title = "Удалить категорию?", message = "«${currentFolder.name}» и все её задачи будут удалены.",
                onConfirm = { vm.deleteFolder(currentFolder.id); dialog = null }, onDismiss = { dialog = null },
            )
            FolderDialog.Move -> MoveGroupDialog(
                currentGroup = group,
                onPick = { g -> vm.moveFolderToGroup(currentFolder.id, g); dialog = null },
                onDismiss = { dialog = null },
            )
            null -> {}
        }
    }
}

/** One full-screen category page: its tasks (sections + free) scroll above a pinned add-task panel. */
@Composable
private fun FolderTasksPage(folder: FolderDto, group: Int, palette: TabPalette, vm: TasksViewModel, onEditTask: (Long) -> Unit) {
    val rows = remember(folder) { buildSheetRows(folder) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .imePadding(),
    ) {
        if (rows.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("Нет задач", color = palette.inputText.copy(alpha = 0.6f))
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(vertical = 8.dp)) {
                items(
                    rows,
                    key = { row -> if (row is SheetRow.Header) "h${row.section.id}" else "t${(row as SheetRow.Item).task.id}" },
                ) { row ->
                    when (row) {
                        is SheetRow.Header -> SectionHeaderRow(
                            section = row.section, palette = palette,
                            onToggle = { vm.setSectionCollapsed(row.section.id, !row.section.currentlyCollapsed) },
                            onDelete = { vm.deleteSection(row.section.id) },
                        )
                        is SheetRow.Item -> TaskRow(
                            task = row.task, group = group, palette = palette,
                            onToggle = { id, done -> vm.toggleDone(id, done) },
                            onClick = { onEditTask(row.task.id) },
                            onDelete = { vm.deleteTask(row.task.id) },
                        )
                    }
                }
            }
        }
        HorizontalDivider(color = palette.inputText.copy(alpha = 0.12f))
        AddTaskPanel(folderId = folder.id, group = group, palette = palette, vm = vm)
        Spacer(Modifier.height(8.dp))
    }
}

// ---- Category list pieces ----

@Composable
private fun DayScoreHeader(dayScope: Int, palette: TabPalette) {
    Text(
        text = "Очки за день: $dayScope",
        color = palette.text,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(start = 4.dp, bottom = 2.dp),
    )
}

@Composable
private fun CategoryCard(folder: FolderDto, palette: TabPalette, onClick: () -> Unit) {
    val done = folder.tasks.sumOf { it.countAccumulation * it.countValue }
    val all = folder.tasks.sumOf { it.maxAccumulation * it.countValue }
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = palette.surface),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = folder.name.ifBlank { "Без названия" },
                color = palette.inputText, fontWeight = FontWeight.Bold,
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

// ---- task rows / sections / add panel ----

private sealed interface SheetRow {
    data class Header(val section: SectionDto) : SheetRow
    data class Item(val task: TaskDto) : SheetRow
}

/** Sections interleaved with free tasks by outer position; section tasks (done sink) shown
 *  when expanded; done free tasks last. */
private fun buildSheetRows(folder: FolderDto): List<SheetRow> {
    val tasks = folder.tasks
    val sections = folder.sections.sortedBy { it.position }
    val freeNotDone = tasks.filter { it.sectionId == 0L && !it.done }.sortedBy { it.position }
    val freeDone = tasks.filter { it.sectionId == 0L && it.done }.sortedBy { it.position }

    data class Outer(val pos: Int, val section: SectionDto?, val task: TaskDto?)
    val outer = buildList {
        sections.forEach { add(Outer(it.position, it, null)) }
        freeNotDone.forEach { add(Outer(it.position, null, it)) }
    }.sortedBy { it.pos }

    val rows = mutableListOf<SheetRow>()
    outer.forEach { e ->
        when {
            e.section != null -> {
                rows.add(SheetRow.Header(e.section))
                if (!e.section.currentlyCollapsed) {
                    tasks.filter { it.sectionId == e.section.id }
                        .sortedWith(compareBy({ it.done }, { it.position }))
                        .forEach { rows.add(SheetRow.Item(it)) }
                }
            }
            e.task != null -> rows.add(SheetRow.Item(e.task))
        }
    }
    freeDone.forEach { rows.add(SheetRow.Item(it)) }
    return rows
}

@Composable
private fun SectionHeaderRow(section: SectionDto, palette: TabPalette, onToggle: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (section.currentlyCollapsed) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
            contentDescription = null, tint = palette.inputText,
        )
        Spacer(Modifier.width(4.dp))
        Text(section.name, color = palette.inputText, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "Удалить секцию", tint = palette.inputText.copy(alpha = 0.4f))
        }
    }
}

@Composable
private fun TaskRow(task: TaskDto, group: Int, palette: TabPalette, onToggle: (Long, Boolean) -> Unit, onClick: () -> Unit, onDelete: (Long) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), verticalAlignment = Alignment.CenterVertically) {
        if (group != 3) {
            Checkbox(
                checked = task.done,
                onCheckedChange = { checked -> onToggle(task.id, checked) },
                colors = CheckboxDefaults.colors(
                    checkedColor = palette.accent,
                    uncheckedColor = palette.inputText.copy(alpha = 0.5f),
                    checkmarkColor = palette.surface,
                ),
            )
        } else {
            Spacer(Modifier.width(12.dp))
        }
        if (task.priority > 0) {
            Text("!".repeat(task.priority), color = palette.accent, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(4.dp))
        }
        Text(
            text = task.text,
            color = palette.inputText,
            textDecoration = if (task.done) TextDecoration.LineThrough else TextDecoration.None,
            modifier = Modifier.weight(1f),
        )
        if (task.maxAccumulation > 1) {
            Text("${task.countAccumulation}/${task.maxAccumulation}", color = palette.inputText.copy(alpha = 0.6f))
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
    var count by remember(folderId) { mutableStateOf(1) }
    var max by remember(folderId) { mutableStateOf(1) }
    var priority by remember(folderId) { mutableStateOf(0) }
    var cycling by remember(folderId) { mutableStateOf(false) }

    fun submit() {
        if (text.isNotBlank()) {
            vm.addTask(folderId, text, count, max, cycling, priority)
            text = ""; count = 1; max = 1; priority = 0; cycling = false
        }
    }

    Column {
        if (group != 3) {
            Row(modifier = Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ToggleChip("×$count", active = count > 1, palette = palette) { count = cycle1to9(count) }
                ToggleChip("/$max", active = max > 1, palette = palette) { max = cycle1to9(max) }
                ToggleChip("!$priority", active = priority > 0, palette = palette) { priority = (priority + 1) % 4 }
                ToggleChip("↻", active = cycling, palette = palette) { cycling = !cycling }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text(if (group == 3) "Новая заметка" else "Новая задача") },
                singleLine = true,
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                colors = fieldColors(palette),
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
    ) { Text(text = label, color = fg, fontWeight = FontWeight.Medium) }
}

// ---- Task editor ----

@Composable
private fun TaskEditorDialog(task: TaskDto, group: Int, palette: TabPalette, vm: TasksViewModel, onDismiss: () -> Unit) {
    var text by remember(task.id) { mutableStateOf(task.text) }
    var done by remember(task.id) { mutableStateOf(task.done) }
    var count by remember(task.id) { mutableStateOf(task.countValue.coerceAtLeast(1)) }
    var max by remember(task.id) { mutableStateOf(task.maxAccumulation.coerceAtLeast(1)) }
    var priority by remember(task.id) { mutableStateOf(task.priority) }
    var cycling by remember(task.id) { mutableStateOf(task.isCycling) }

    val allFolders = remember(task.id) { vm.allFolderRefs() }
    val selected = remember(task.id) {
        mutableStateListOf<Long>().apply {
            add(task.taskFolderId)
            task.extraFolderIds.forEach { if (!contains(it)) add(it) }
        }
    }

    AlertDialog(
        onDismissRequest = { vm.editTask(task.id, text, count, max, cycling, priority); onDismiss() },
        title = { Text(if (group == 3) "Заметка" else "Задача") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = text, onValueChange = { text = it },
                    modifier = Modifier.fillMaxWidth(), label = { Text("Текст") },
                    minLines = if (group == 3) 6 else 1, colors = fieldColors(palette),
                )
                if (group != 3) {
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = done,
                            onCheckedChange = { done = it; vm.toggleDone(task.id, it) },
                            colors = CheckboxDefaults.colors(checkedColor = palette.accent),
                        )
                        Text("Выполнено", color = palette.inputText)
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ToggleChip("×$count", active = count > 1, palette = palette) { count = cycle1to9(count) }
                        ToggleChip("/$max", active = max > 1, palette = palette) { max = cycle1to9(max) }
                        ToggleChip("!$priority", active = priority > 0, palette = palette) { priority = (priority + 1) % 4 }
                        ToggleChip("↻", active = cycling, palette = palette) { cycling = !cycling }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text("Категории", color = palette.inputText.copy(alpha = 0.7f), fontWeight = FontWeight.Medium)
                allFolders.forEach { ref ->
                    val checked = selected.contains(ref.id)
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable {
                            if (checked) { if (selected.size > 1) selected.remove(ref.id) } else selected.add(ref.id)
                        },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = checked, onCheckedChange = null, colors = CheckboxDefaults.colors(checkedColor = palette.accent))
                        Text("${ref.name} ${groupTag(ref.group)}", color = palette.inputText)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                vm.editTask(task.id, text, count, max, cycling, priority)
                vm.setCategories(task.id, selected.toList())
                onDismiss()
            }) { Text("Готово") }
        },
        dismissButton = { TextButton(onClick = { vm.deleteTask(task.id); onDismiss() }) { Text("Удалить", color = palette.accent) } },
        containerColor = Color.White,
    )
}

// ---- shared dialogs ----

private enum class FolderDialog { Rename, AddSection, Delete, Move }

@Composable
private fun TextEntryDialog(title: String, initial: String, palette: TabPalette, confirmLabel: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var value by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value, onValueChange = { value = it }, singleLine = true,
                placeholder = { Text("Название") }, colors = fieldColors(palette),
            )
        },
        confirmButton = { TextButton(onClick = { if (value.isNotBlank()) onConfirm(value) }) { Text(confirmLabel) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
        containerColor = Color.White,
    )
}

@Composable
private fun ConfirmDialog(title: String, message: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) }, text = { Text(message) },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Удалить") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
        containerColor = Color.White,
    )
}

@Composable
private fun MoveGroupDialog(currentGroup: Int, onPick: (Int) -> Unit, onDismiss: () -> Unit) {
    val names = listOf("Tasks 1", "Tasks 2", "Tasks 3", "Notes")
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Переместить в…") },
        text = {
            Column {
                names.forEachIndexed { g, name ->
                    if (g != currentGroup) {
                        TextButton(onClick = { onPick(g) }, modifier = Modifier.fillMaxWidth()) {
                            Text(name, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
        containerColor = Color.White,
    )
}

// ---- helpers ----

private fun cycle1to9(v: Int): Int = if (v < 1) 1 else if (v >= 9) 1 else v + 1

private fun groupTag(group: Int): String = when (group) {
    0 -> "[T1]"; 1 -> "[T2]"; 2 -> "[T3]"; 3 -> "[N]"; else -> ""
}

@Composable
private fun fieldColors(palette: TabPalette) = OutlinedTextFieldDefaults.colors(
    focusedTextColor = palette.inputText,
    unfocusedTextColor = palette.inputText,
    focusedBorderColor = palette.accent,
    unfocusedBorderColor = palette.inputText.copy(alpha = 0.3f),
    cursorColor = palette.accent,
    focusedPlaceholderColor = palette.inputText.copy(alpha = 0.5f),
    unfocusedPlaceholderColor = palette.inputText.copy(alpha = 0.5f),
)
