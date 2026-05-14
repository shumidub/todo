package com.shumidub.todoapprealm.ui.main

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shumidub.todoapprealm.ui.notes.NotesMode
import com.shumidub.todoapprealm.ui.notes.NotesScreen
import com.shumidub.todoapprealm.ui.notes.NotesViewModel
import com.shumidub.todoapprealm.ui.reports.ReportsScreen
import com.shumidub.todoapprealm.ui.reports.ReportsViewModel
import com.shumidub.todoapprealm.ui.tasks.TasksScreen
import com.shumidub.todoapprealm.ui.tasks.TasksViewModel
import com.shumidub.todoapprealm.ui.theme.TodoTheme
import kotlinx.coroutines.launch

private const val PAGE_NOTES = 0
private const val PAGE_TASKS = 1
private const val PAGE_REPORTS = 2
private const val EXIT_TIMEOUT_MS = 2000L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onExitApp: () -> Unit,
    onShowToast: (String) -> Unit,
) {
    val mainViewModel: MainViewModel = hiltViewModel()
    val notesViewModel: NotesViewModel = hiltViewModel()
    val tasksViewModel: TasksViewModel = hiltViewModel()
    val reportsViewModel: ReportsViewModel = hiltViewModel()

    val pagerState = rememberPagerState(initialPage = PAGE_TASKS) { 3 }
    val tasksSheetState = rememberStandardBottomSheetState(
        initialValue = SheetValue.PartiallyExpanded,
        skipHiddenState = true,
    )
    val scope = rememberCoroutineScope()

    val dayScope by mainViewModel.dayScope.collectAsStateWithLifecycle()
    val notesState by notesViewModel.state.collectAsStateWithLifecycle()
    val tasksState by tasksViewModel.state.collectAsStateWithLifecycle()

    val tasksExpanded = tasksSheetState.currentValue == SheetValue.Expanded
    val tasksTitle = if (tasksExpanded) {
        tasksState.folders.getOrNull(tasksState.selectedFolderIndex)?.name ?: "Tasks"
    } else "Tasks"

    val (title, showBack) = computeAppBar(
        page = pagerState.currentPage,
        notesMode = notesState.mode,
        tasksTitle = tasksTitle,
    )

    var lastBackPressMs by rememberSaveable { mutableLongStateOf(0L) }
    BackHandler(enabled = true) {
        val now = System.currentTimeMillis()
        val handled = when (pagerState.currentPage) {
            PAGE_NOTES -> {
                if (notesState.mode is NotesMode.Notes) {
                    notesViewModel.backToFolders(); true
                } else false
            }
            PAGE_TASKS -> {
                if (tasksSheetState.currentValue == SheetValue.Expanded) {
                    scope.launch { tasksSheetState.partialExpand() }
                    true
                } else false
            }
            else -> false
        }
        if (!handled) {
            if (now - lastBackPressMs < EXIT_TIMEOUT_MS) {
                onExitApp()
            } else {
                lastBackPressMs = now
                onShowToast("For exit press again")
            }
        }
    }

    TodoTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(title) },
                    navigationIcon = {
                        if (showBack) {
                            IconButton(onClick = onBackNavClick(
                                page = pagerState.currentPage,
                                onNotesBack = notesViewModel::backToFolders,
                                onTasksBack = { scope.launch { tasksSheetState.partialExpand() } },
                            )) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                )
                            }
                        }
                    },
                    actions = {
                        Text(
                            text = dayScope.toString(),
                            modifier = Modifier.padding(horizontal = 12.dp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        IconButton(onClick = {
                            when (pagerState.currentPage) {
                                PAGE_NOTES -> notesViewModel.openAdd()
                                PAGE_TASKS -> if (!tasksExpanded) tasksViewModel.openAddFolder()
                                PAGE_REPORTS -> reportsViewModel.openAdd()
                            }
                        }) {
                            Icon(Icons.Default.Add, contentDescription = "Add")
                        }
                    },
                )
            },
        ) { padding ->
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                userScrollEnabled = !tasksExpanded,
            ) { page ->
                when (page) {
                    PAGE_NOTES -> NotesScreen(viewModel = notesViewModel)
                    PAGE_TASKS -> TasksScreen(
                        bottomSheetState = tasksSheetState,
                        viewModel = tasksViewModel,
                    )
                    PAGE_REPORTS -> ReportsScreen(viewModel = reportsViewModel)
                }
            }
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage != PAGE_TASKS &&
            tasksSheetState.currentValue == SheetValue.Expanded
        ) {
            tasksSheetState.partialExpand()
        }
    }
}

private fun computeAppBar(
    page: Int,
    notesMode: NotesMode,
    tasksTitle: String,
): Pair<String, Boolean> = when (page) {
    PAGE_NOTES -> when (notesMode) {
        NotesMode.Folders -> "Notes" to false
        is NotesMode.Notes -> notesMode.folderName to true
    }
    PAGE_TASKS -> tasksTitle to (tasksTitle != "Tasks")
    PAGE_REPORTS -> "Reports" to false
    else -> "Todo" to false
}

private fun onBackNavClick(
    page: Int,
    onNotesBack: () -> Unit,
    onTasksBack: () -> Unit,
): () -> Unit = when (page) {
    PAGE_NOTES -> onNotesBack
    PAGE_TASKS -> onTasksBack
    else -> ({})
}
