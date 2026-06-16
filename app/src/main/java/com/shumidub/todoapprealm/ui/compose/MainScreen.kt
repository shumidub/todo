package com.shumidub.todoapprealm.ui.compose

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shumidub.todoapprealm.data.TabNames
import com.shumidub.todoapprealm.data.TasksRepository
import com.shumidub.todoapprealm.ui.theme.Todo100Theme
import com.shumidub.todoapprealm.ui.theme.paletteForGroup

/** A full-screen category view request: which group + which folder to open at. */
private data class DetailTarget(val group: Int, val folderId: Long)

/** Package of the external app opened by tapping the day-score (Memento Database). */
const val DAY_SCORE_APP_PACKAGE = "com.luckydroid.droidbase"

/** Launch the external day-score app; show a toast if it isn't installed. */
fun launchDayScoreApp(context: Context) {
    val intent = context.packageManager.getLaunchIntentForPackage(DAY_SCORE_APP_PACKAGE)
        ?.apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
    if (intent != null) context.startActivity(intent)
    else Toast.makeText(context, "Memento Database не установлена", Toast.LENGTH_SHORT).show()
}

/**
 * Two-level shell:
 *  - **Group level** — a [HorizontalPager] over the 4 task groups, each showing its category
 *    list. Swiping left/right switches group. The top bar carries the (renameable) tab title,
 *    the global day-score, and an overflow menu (sync / rename tab).
 *  - **Category level** — tapping a category opens [CategoryDetailScreen] full-screen, itself a
 *    pager over that group's categories (swipe to switch category). Back returns to the list.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val context = LocalContext.current
    // Visual tab order: Notes (group 3) sits leftmost, then Tasks 1/2/3. Page index → group id
    // goes through groupOrder; the app still opens on Tasks 1 (group 0) by default.
    val groupOrder = remember { listOf(3, 0, 1, 2) }
    val pageCount = groupOrder.size
    val startPage = remember { groupOrder.indexOf(0) }
    val pagerState = rememberPagerState(initialPage = startPage) { pageCount }
    val currentGroup by remember { derivedStateOf { groupOrder[pagerState.currentPage] } }
    var showSync by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf(false) }
    var tabNames by remember { mutableStateOf(TabNames.load(context)) }
    var detail by remember { mutableStateOf<DetailTarget?>(null) }
    val dayScore by remember { TasksRepository.dayScoreFlow() }.collectAsStateWithLifecycle(initialValue = 0)

    val themeGroup = detail?.group ?: currentGroup

    Todo100Theme(palette = paletteForGroup(themeGroup)) {
        val palette = paletteForGroup(themeGroup)
        val open = detail
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                containerColor = palette.bg,
                topBar = {
                    TopAppBar(
                        title = { Text(tabNames.getOrElse(currentGroup) { "Tab $currentGroup" }) },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = palette.systemBar,
                            titleContentColor = palette.text,
                            actionIconContentColor = palette.text,
                        ),
                        actions = {
                            Text(
                                text = dayScore.toString(),
                                color = palette.text,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable { launchDayScoreApp(context) },
                            )
                            Spacer(Modifier.width(4.dp))
                            Box {
                                IconButton(onClick = { menuOpen = true }) {
                                    Icon(Icons.Default.MoreVert, contentDescription = "Меню", tint = palette.text)
                                }
                                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                    DropdownMenuItem(
                                        text = { Text("Синхронизация") },
                                        onClick = { menuOpen = false; showSync = true },
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Переименовать таб") },
                                        onClick = { menuOpen = false; renaming = true },
                                    )
                                }
                            }
                        },
                    )
                },
            ) { innerPadding ->
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                ) { page ->
                    val g = groupOrder[page]
                    TasksScreen(group = g, onOpenCategory = { folderId -> detail = DetailTarget(g, folderId) })
                }
            }
            // Detail overlays the list; pulling it down reveals the list underneath.
            if (open != null) {
                CategoryDetailScreen(group = open.group, startFolderId = open.folderId, onBack = { detail = null })
            }
            if (showSync) {
                SyncDialog(palette = palette, onDismiss = { showSync = false })
            }
            if (renaming) {
                var value by remember { mutableStateOf(tabNames.getOrElse(currentGroup) { "" }) }
                AlertDialog(
                    onDismissRequest = { renaming = false },
                    title = { Text("Переименовать таб") },
                    text = {
                        OutlinedTextField(value = value, onValueChange = { value = it }, singleLine = true)
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            if (value.isNotBlank()) {
                                TabNames.save(context, currentGroup, value)
                                tabNames = TabNames.load(context)
                            }
                            renaming = false
                        }) { Text("Сохранить") }
                    },
                    dismissButton = { TextButton(onClick = { renaming = false }) { Text("Отмена") } },
                    containerColor = Color.White,
                )
            }
        }
    }
}
