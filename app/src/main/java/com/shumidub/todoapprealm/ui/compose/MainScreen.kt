package com.shumidub.todoapprealm.ui.compose

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.shumidub.todoapprealm.Tabs
import com.shumidub.todoapprealm.ui.theme.Todo100Theme
import com.shumidub.todoapprealm.ui.theme.paletteForGroup

/** A full-screen category view request: which group + which folder to open at. */
private data class DetailTarget(val group: Int, val folderId: Long)

/** Title per task group. Group 3 is the Indigo "Notes" tab; legacy Notes page is hidden. */
private fun titleForGroup(group: Int): String = when (group) {
    0 -> "Tasks 1"; 1 -> "Tasks 2"; 2 -> "Tasks 3"; 3 -> "Notes"; else -> "Tab $group"
}

/**
 * Two-level shell:
 *  - **Group level** — a [HorizontalPager] over the 4 task groups, each showing its category
 *    list. Swiping left/right switches group.
 *  - **Category level** — tapping a category opens [CategoryDetailScreen] full-screen, itself a
 *    pager over that group's categories (swipe to switch category). Back returns to the list.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val pageCount = Tabs.GROUP_COUNT // 4 task groups, no legacy Notes page
    val pagerState = rememberPagerState(initialPage = 0) { pageCount }
    val currentGroup by remember { derivedStateOf { pagerState.currentPage } }
    var showSync by remember { mutableStateOf(false) }
    var detail by remember { mutableStateOf<DetailTarget?>(null) }

    val themeGroup = detail?.group ?: currentGroup

    Todo100Theme(palette = paletteForGroup(themeGroup)) {
        val palette = paletteForGroup(themeGroup)
        val open = detail
        if (open != null) {
            CategoryDetailScreen(group = open.group, startFolderId = open.folderId, onBack = { detail = null })
        } else {
            Scaffold(
                containerColor = palette.bg,
                topBar = {
                    TopAppBar(
                        title = { Text(titleForGroup(pagerState.currentPage)) },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = palette.systemBar,
                            titleContentColor = palette.text,
                        ),
                        actions = {
                            IconButton(onClick = { showSync = true }) {
                                Icon(Icons.Default.Sync, contentDescription = "Бэкап / синхронизация", tint = palette.text)
                            }
                        },
                    )
                },
            ) { innerPadding ->
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                ) { page ->
                    TasksScreen(group = page, onOpenCategory = { folderId -> detail = DetailTarget(page, folderId) })
                }
            }
            if (showSync) {
                SyncDialog(palette = palette, onDismiss = { showSync = false })
            }
        }
    }
}
