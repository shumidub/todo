package com.shumidub.todoapprealm.ui.compose

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.UnfoldLess
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.shumidub.todoapprealm.data.FolderDto
import com.shumidub.todoapprealm.data.ReorderEntry
import com.shumidub.todoapprealm.data.SectionDto
import com.shumidub.todoapprealm.data.TabNames
import com.shumidub.todoapprealm.data.TaskDto
import com.shumidub.todoapprealm.ui.theme.TabPalette
import com.shumidub.todoapprealm.ui.theme.paletteForGroup
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

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
    val palette = paletteForGroup(group)
    var showAddFolder by remember { mutableStateOf(false) }

    // Local order so the list can shuffle live during a drag; resets on each Realm emission.
    var cards by remember(state.folders) { mutableStateOf(state.folders) }
    val lazyState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(lazyState) { from, to ->
        cards = cards.toMutableList().apply { add(to.index, removeAt(from.index)) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (cards.isEmpty()) {
            EmptyState(palette = palette, onAddFolder = { showAddFolder = true })
        } else {
            LazyColumn(
                state = lazyState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                items(cards, key = { it.id }) { folder ->
                    ReorderableItem(reorderState, key = folder.id) { isDragging ->
                        CategoryCard(
                            folder = folder, palette = palette,
                            modifier = Modifier
                                .longPressDraggableHandle(onDragStopped = { vm.reorderFolders(cards.map { it.id }) })
                                .then(if (isDragging) Modifier.shadow(6.dp, RoundedCornerShape(3.dp)) else Modifier),
                            onClick = { onOpenCategory(folder.id) },
                        )
                    }
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

    // Action mode (contextual bar): a long-press selects a task/section header for deletion.
    var selection by remember { mutableStateOf<Selection?>(null) }
    BackHandler { if (selection != null) selection = null else onBack() }
    LaunchedEffect(folders.isEmpty()) { if (folders.isEmpty()) onBack() }
    if (folders.isEmpty()) return

    val startIndex = remember(startFolderId, folders.size) {
        folders.indexOfFirst { it.id == startFolderId }.coerceIn(0, folders.size - 1)
    }
    val pagerState = rememberPagerState(initialPage = startIndex) { folders.size }
    val currentPage = pagerState.currentPage.coerceIn(0, folders.size - 1)
    val currentFolder = folders.getOrNull(currentPage)
    LaunchedEffect(currentPage) { selection = null }

    var editingTaskId by remember { mutableStateOf<Long?>(null) }
    var menuOpen by remember { mutableStateOf(false) }
    var dialog by remember { mutableStateOf<FolderDialog?>(null) }

    val barColors = TopAppBarDefaults.topAppBarColors(
        containerColor = palette.systemBar,
        titleContentColor = palette.text,
        navigationIconContentColor = palette.text,
        actionIconContentColor = palette.text,
    )

    Scaffold(
        containerColor = palette.bg,
        topBar = {
            val sel = selection
            if (sel != null) {
                // Contextual action bar: cancel + delete the selected task/section.
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = { selection = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Отмена", tint = palette.text)
                        }
                    },
                    title = { Text("", color = palette.text) },
                    colors = barColors,
                    actions = {
                        if (sel.isSection) {
                            val sec = folders.flatMap { it.sections }.firstOrNull { it.id == sel.id }
                            if (sec != null) {
                                IconButton(onClick = { vm.setSectionCollapsedByDefault(sel.id, !sec.collapsedByDefault) }) {
                                    Icon(
                                        if (sec.collapsedByDefault) Icons.Default.UnfoldLess else Icons.Default.UnfoldMore,
                                        contentDescription = "Сворачивать при запуске",
                                        tint = palette.text,
                                    )
                                }
                            }
                        }
                        IconButton(onClick = {
                            if (sel.isSection) vm.deleteSection(sel.id) else vm.deleteTask(sel.id)
                            selection = null
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Удалить", tint = palette.text)
                        }
                    },
                )
            } else {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад", tint = palette.text)
                        }
                    },
                    title = { Text(currentFolder?.name?.ifBlank { "Без названия" } ?: "", color = palette.text) },
                    colors = barColors,
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
            }
        },
    ) { inner ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize().padding(inner),
        ) { page ->
            folders.getOrNull(page)?.let { folder ->
                FolderTasksPage(
                    folder = folder, group = group, palette = palette, vm = vm,
                    selection = selection, onSelect = { selection = it },
                    onEditTask = { editingTaskId = it },
                )
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
private fun FolderTasksPage(
    folder: FolderDto,
    group: Int,
    palette: TabPalette,
    vm: TasksViewModel,
    selection: Selection?,
    onSelect: (Selection?) -> Unit,
    onEditTask: (Long) -> Unit,
) {
    // Done tasks are hidden until the footer is tapped.
    var showDone by remember(folder.id) { mutableStateOf(false) }
    val doneCount = folder.tasks.count { it.done }
    // Local, mutable copy of the visible rows so the reorderable list can shuffle live during a
    // drag; it resets whenever Realm re-emits this folder or the show-done toggle flips.
    var rows by remember(folder, showDone) { mutableStateOf(buildSheetRows(folder, showDone)) }
    var draggedKey by remember(folder.id) { mutableStateOf<String?>(null) }

    val lazyState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(lazyState) { from, to ->
        rows = rows.toMutableList().apply { add(to.index, removeAt(from.index)) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
    ) {
        if (rows.isEmpty() && doneCount == 0) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
                Text("Нет задач", color = palette.inputText.copy(alpha = 0.6f))
            }
        } else {
            LazyColumn(
                state = lazyState,
                modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(rows, key = { rowKey(it) }) { row ->
                    ReorderableItem(reorderState, key = rowKey(row)) { isDragging ->
                        val selected = selection?.matches(row) == true
                        // The drag handle is the sole long-press owner: long-press picks the row up
                        // for dragging AND selects it for the action bar. A real drag consumes the
                        // gesture, so the plain tap-to-edit click only fires on a short tap.
                        val handle = Modifier.longPressDraggableHandle(
                            onDragStarted = { draggedKey = rowKey(row); onSelect(selectionOf(row)) },
                            onDragStopped = {
                                val (outer, inner) = resolveReorder(rows, draggedKey)
                                vm.applyReorder(folder.id, outer, inner)
                                draggedKey = null
                            },
                        )
                        when (row) {
                            is SheetRow.Header -> SectionHeaderRow(
                                section = row.section, palette = palette,
                                doneSum = folder.tasks.filter { it.sectionId == row.section.id }.sumOf { it.countAccumulation * it.countValue },
                                allSum = folder.tasks.filter { it.sectionId == row.section.id }.sumOf { it.maxAccumulation * it.countValue },
                                modifier = handle
                                    .then(if (isDragging) Modifier.shadow(2.dp, RoundedCornerShape(6.dp)) else Modifier)
                                    .background(if (selected) palette.accent.copy(alpha = 0.18f) else Color.Transparent, RoundedCornerShape(6.dp)),
                                onToggle = { vm.setSectionCollapsed(row.section.id, !row.section.currentlyCollapsed) },
                            )
                            is SheetRow.Item -> Card(
                                modifier = Modifier
                                    .padding(start = if (row.task.sectionId != 0L) 12.dp else 0.dp)
                                    .then(handle).fillMaxWidth()
                                    .then(if (isDragging) Modifier.shadow(4.dp, RoundedCornerShape(3.dp)) else Modifier),
                                shape = RoundedCornerShape(3.dp),
                                colors = CardDefaults.cardColors(containerColor = palette.surface),
                                border = if (selected) BorderStroke(2.dp, palette.accent) else null,
                            ) {
                                TaskRow(
                                    task = row.task, group = group, palette = palette,
                                    onToggle = { id, done -> vm.toggleDone(id, done) },
                                    onClick = { onEditTask(row.task.id) },
                                )
                            }
                            is SheetRow.Empty -> Box(
                                modifier = Modifier.fillMaxWidth().height(44.dp).padding(start = 16.dp),
                                contentAlignment = Alignment.CenterStart,
                            ) {
                                Text("Перетащите сюда", color = palette.inputText.copy(alpha = 0.35f), fontSize = 12.sp)
                            }
                        }
                    }
                }
                if (doneCount > 0) {
                    item(key = "done-footer") {
                        DoneFooter(count = doneCount, showDone = showDone, palette = palette) { showDone = !showDone }
                    }
                }
            }
        }
        // Bottom panel: a full-width band in a slightly darker shade of the tab background.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(palette.surfaceMuted)
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            AddTaskPanel(folderId = folder.id, group = group, palette = palette, vm = vm)
        }
    }
}

// ---- Category list pieces ----

@Composable
private fun CategoryCard(folder: FolderDto, palette: TabPalette, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val done = folder.tasks.sumOf { it.countAccumulation * it.countValue }
    val all = folder.tasks.sumOf { it.maxAccumulation * it.countValue }
    Card(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(3.dp),
        colors = CardDefaults.cardColors(containerColor = palette.surface),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = folder.name.ifBlank { "Без названия" },
                color = palette.inputText, fontWeight = FontWeight.Normal,
                modifier = Modifier.weight(1f),
            )
            Text(text = "$done/$all", color = palette.counter, fontWeight = FontWeight.Normal, fontSize = 13.sp)
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

    /** Drop-target placeholder shown under an expanded section that has no visible tasks. */
    data class Empty(val section: SectionDto) : SheetRow
}

/** Action-mode target: a long-pressed task or section header, identified by kind + id. */
private data class Selection(val isSection: Boolean, val id: Long)

private fun selectionOf(row: SheetRow): Selection = when (row) {
    is SheetRow.Header -> Selection(isSection = true, id = row.section.id)
    is SheetRow.Item -> Selection(isSection = false, id = row.task.id)
    is SheetRow.Empty -> Selection(isSection = false, id = -1L)
}

private fun Selection.matches(row: SheetRow): Boolean = when (row) {
    is SheetRow.Header -> isSection && id == row.section.id
    is SheetRow.Item -> !isSection && id == row.task.id
    is SheetRow.Empty -> false
}

/** Stable LazyColumn / reorderable key per row (header vs task can share an id space). */
private fun rowKey(row: SheetRow): String = when (row) {
    is SheetRow.Header -> "h${row.section.id}"
    is SheetRow.Item -> "t${row.task.id}"
    is SheetRow.Empty -> "e${row.section.id}"
}

/**
 * Resolve a folder's reordered visible rows into persistence instructions for
 * [TasksViewModel.applyReorder]. Each task's new container is the section whose **expanded**
 * header is nearest above it, but only the dragged task may *change* container — a non-dragged
 * task keeps its existing [TaskDto.sectionId], so an unrelated free task that merely sits under
 * a section header is not silently absorbed into it (and a done free task parked at the bottom
 * is never re-homed). A collapsed header acts as a free-zone boundary.
 *
 * @return the outer-space order (section headers + free tasks) and the per-section member order.
 */
private fun resolveReorder(
    rows: List<SheetRow>,
    draggedKey: String?,
): Pair<List<ReorderEntry>, Map<Long, List<Long>>> {
    val outer = ArrayList<ReorderEntry>()
    val inner = LinkedHashMap<Long, MutableList<Long>>()
    var header: SectionDto? = null
    for (row in rows) {
        when (row) {
            is SheetRow.Header -> {
                header = row.section
                outer.add(ReorderEntry(isSection = true, id = row.section.id))
            }
            is SheetRow.Item -> {
                val task = row.task
                val h = header
                val dragged = rowKey(row) == draggedKey
                val container = when {
                    h == null || h.currentlyCollapsed -> 0L
                    dragged -> h.id
                    task.sectionId == h.id -> h.id
                    else -> 0L
                }
                if (container == 0L) outer.add(ReorderEntry(isSection = false, id = task.id))
                else inner.getOrPut(container) { ArrayList() }.add(task.id)
            }
            is SheetRow.Empty -> {
                // Placeholder for an empty section: ensure the section still gets restamped (so a
                // task dragged out of it leaves it empty), but adds no task. Keeps `header`.
                inner.getOrPut(row.section.id) { ArrayList() }
            }
        }
    }
    return outer to inner
}

/** Sections interleaved with free tasks by outer position; section tasks (done sink) shown
 *  when expanded; done free tasks last. */
private fun buildSheetRows(folder: FolderDto, showDone: Boolean): List<SheetRow> {
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
                    val secTasks = tasks.filter { it.sectionId == e.section.id && (showDone || !it.done) }
                        .sortedWith(compareBy({ it.done }, { it.position }))
                    secTasks.forEach { rows.add(SheetRow.Item(it)) }
                    if (secTasks.isEmpty()) rows.add(SheetRow.Empty(e.section))
                }
            }
            e.task != null -> rows.add(SheetRow.Item(e.task))
        }
    }
    if (showDone) freeDone.forEach { rows.add(SheetRow.Item(it)) }
    return rows
}

@Composable
private fun SectionHeaderRow(section: SectionDto, palette: TabPalette, doneSum: Int, allSum: Int, modifier: Modifier = Modifier, onToggle: () -> Unit) {
    Row(
        modifier = modifier.fillMaxWidth().clickable(onClick = onToggle).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (section.currentlyCollapsed) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
            contentDescription = null, tint = palette.inputText,
        )
        Spacer(Modifier.width(4.dp))
        Text(section.name, color = palette.inputText, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        Text("$doneSum/$allSum", color = palette.inputText.copy(alpha = 0.6f), fontSize = 14.sp)
        Spacer(Modifier.width(8.dp))
    }
}

/** Footer at the very end of a folder's list: how many tasks are done; tap to show/hide them. */
@Composable
private fun DoneFooter(count: Int, showDone: Boolean, palette: TabPalette, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (showDone) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = null, tint = palette.inputText.copy(alpha = 0.7f),
        )
        Spacer(Modifier.width(4.dp))
        Text("Выполнено: $count", color = palette.inputText.copy(alpha = 0.7f), fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun TaskRow(task: TaskDto, group: Int, palette: TabPalette, modifier: Modifier = Modifier, onToggle: (Long, Boolean) -> Unit, onClick: () -> Unit) {
    Row(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick).padding(start = 4.dp, top = 7.dp, bottom = 7.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (group != 3) {
            Spacer(Modifier.width(4.dp))
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
        // Points + accumulation/max (e.g. "2 1/5" = 2 points each, done 1 of 5 times) then the
        // checkbox on the right edge. Accent border when cyclic, grey otherwise; a small strip
        // under it marks a task that lives in more than one category.
        if (group != 3) {
            Spacer(Modifier.width(8.dp))
            Text("${task.countValue}", color = palette.inputText.copy(alpha = 0.6f), fontSize = 12.sp)
            Spacer(Modifier.width(4.dp))
            Text("${task.countAccumulation}/${task.maxAccumulation}", color = palette.inputText.copy(alpha = 0.6f), fontSize = 12.sp)
            Box(contentAlignment = Alignment.Center) {
                Checkbox(
                    checked = task.done,
                    onCheckedChange = { checked -> onToggle(task.id, checked) },
                    modifier = Modifier.requiredSize(28.dp),
                    colors = CheckboxDefaults.colors(
                        checkedColor = palette.accent,
                        uncheckedColor = if (task.isCycling) palette.accent else palette.inputText.copy(alpha = 0.5f),
                        checkmarkColor = palette.surface,
                    ),
                )
                if (task.extraFolderIds.isNotEmpty()) {
                    Box(
                        Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 1.dp)
                            .width(16.dp)
                            .height(2.dp)
                            .background(
                                if (task.isCycling) palette.accent else palette.inputText.copy(alpha = 0.5f),
                                RoundedCornerShape(2.dp),
                            ),
                    )
                }
            }
        }
    }
}

@Composable
private fun AddTaskPanel(folderId: Long, group: Int, palette: TabPalette, vm: TasksViewModel) {
    var text by remember(folderId) { mutableStateOf("") }
    var count by remember(folderId) { mutableStateOf(1) }
    var max by remember(folderId) { mutableStateOf(1) }
    var cycling by remember(folderId) { mutableStateOf(false) }

    fun submit() {
        if (text.isNotBlank()) {
            vm.addTask(folderId, text, count, max, cycling, 0)
            text = ""; count = 1; max = 1; cycling = false
        }
    }

    Column {
        // Controls row: point (×) / repeat (/) / cycling (↻) chips for tasks, plus the add button —
        // all the same fixed size (CtrlWidth × CtrlHeight).
        Row(
            modifier = Modifier.padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (group != 3) {
                ToggleChip("×$count", active = count > 1, palette = palette, compact = true) { count = cycle1to10(count) }
                ToggleChip("/$max", active = max > 1, palette = palette, compact = true) { max = cycle1to10(max) }
                ToggleChip("", active = cycling, palette = palette, compact = true, icon = Icons.Default.Refresh) { cycling = !cycling }
            }
            Spacer(Modifier.weight(1f))
            Button(
                onClick = { submit() },
                modifier = Modifier.width(CtrlWidth).height(CtrlHeight),
                contentPadding = PaddingValues(0.dp),
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.buttonColors(containerColor = palette.accent),
            ) {
                Icon(Icons.Default.Add, contentDescription = "Добавить", tint = Color.White)
            }
        }
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            placeholder = { Text(if (group == 3) "Новая заметка" else "Новая задача") },
            maxLines = 7,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = palette.surface,
                unfocusedContainerColor = palette.surface,
                focusedTextColor = palette.inputText,
                unfocusedTextColor = palette.inputText,
                focusedBorderColor = palette.accent,
                unfocusedBorderColor = Color.Transparent,
                cursorColor = palette.accent,
                focusedPlaceholderColor = palette.inputText.copy(alpha = 0.5f),
                unfocusedPlaceholderColor = palette.inputText.copy(alpha = 0.5f),
            ),
        )
    }
}

/** Shared size for the add-panel controls: the toggle chips and the add button all match. */
private val CtrlWidth = 60.dp
private val CtrlHeight = 32.dp

@Composable
private fun ToggleChip(label: String, active: Boolean, palette: TabPalette, compact: Boolean = false, icon: ImageVector? = null, onClick: () -> Unit) {
    val bg = if (active) palette.accent else palette.inputText.copy(alpha = 0.10f)
    val fg = if (active) palette.surface else palette.inputText.copy(alpha = 0.7f)
    if (compact) {
        // On the darker bottom band: the tab's own colour when unselected, accent when active.
        val cbg = if (active) palette.accent else palette.bg
        val cfg = if (active) Color.White else palette.text
        Box(
            modifier = Modifier
                .width(CtrlWidth).height(CtrlHeight)
                .background(cbg, RoundedCornerShape(6.dp))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            if (icon != null) Icon(icon, contentDescription = null, tint = cfg, modifier = Modifier.size(18.dp))
            else Text(text = label, color = cfg, fontWeight = FontWeight.Medium, fontSize = 13.sp)
        }
    } else {
        Box(
            modifier = Modifier
                .background(bg, RoundedCornerShape(8.dp))
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            if (icon != null) Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(20.dp))
            else Text(text = label, color = fg, fontWeight = FontWeight.Medium)
        }
    }
}

// ---- Task editor ----

@Composable
private fun TaskEditorDialog(task: TaskDto, group: Int, palette: TabPalette, vm: TasksViewModel, onDismiss: () -> Unit) {
    var text by remember(task.id) { mutableStateOf(task.text) }
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
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        properties = DialogProperties(usePlatformDefaultWidth = false),
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
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ToggleChip("×$count", active = count > 1, palette = palette) { count = cycle1to10(count) }
                        ToggleChip("/$max", active = max > 1, palette = palette) { max = cycle1to10(max) }
                        ToggleChip("!$priority", active = priority > 0, palette = palette) { priority = (priority + 1) % 4 }
                        ToggleChip("", active = cycling, palette = palette, icon = Icons.Default.Refresh) { cycling = !cycling }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text("Категории", color = palette.inputText.copy(alpha = 0.7f), fontWeight = FontWeight.Medium)
                allFolders.forEach { ref ->
                    val checked = selected.contains(ref.id)
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable {
                            if (checked) { if (selected.size > 1) selected.remove(ref.id) } else selected.add(ref.id)
                            vm.setCategories(task.id, selected.toList())
                        }.padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = checked, onCheckedChange = null, colors = CheckboxDefaults.colors(checkedColor = palette.accent))
                        Spacer(Modifier.width(8.dp))
                        Text("${ref.name} ${groupTag(ref.group)}", color = palette.inputText, fontSize = 17.sp)
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
    val names = TabNames.load(LocalContext.current)
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

private fun cycle1to10(v: Int): Int = if (v < 1) 1 else if (v >= 10) 1 else v + 1

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
