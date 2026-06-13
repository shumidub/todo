package com.shumidub.todoapprealm.ui.compose

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.shumidub.todoapprealm.Tabs
import com.shumidub.todoapprealm.ui.theme.Todo100Theme
import com.shumidub.todoapprealm.ui.theme.paletteForGroup

/**
 * Title per task group. Group 3 is the Indigo "Notes" tab (a Tasks-style list).
 * The legacy Notes page (old pager page 0) is intentionally NOT shown in the Compose UI.
 */
private fun titleForGroup(group: Int): String = when (group) {
    0 -> "Tasks 1"
    1 -> "Tasks 2"
    2 -> "Tasks 3"
    3 -> "Notes"
    else -> "Tab $group"
}

/**
 * Root composable for the Compose UI. One page per task group (0..3) — the legacy Notes
 * page is hidden per the migration. Theme + system bars track the settled page; each page
 * renders its real [TasksScreen] in its own palette.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val pageCount = Tabs.GROUP_COUNT // 4 task groups, no legacy Notes page
    val pagerState = rememberPagerState(initialPage = 0) { pageCount }
    val currentGroup by remember { derivedStateOf { pagerState.currentPage } }

    Todo100Theme(palette = paletteForGroup(currentGroup)) {
        val palette = paletteForGroup(currentGroup)
        Scaffold(
            containerColor = palette.bg,
            topBar = {
                TopAppBar(
                    title = { Text(titleForGroup(pagerState.currentPage)) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = palette.systemBar,
                        titleContentColor = palette.text,
                    ),
                )
            },
        ) { innerPadding ->
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) { page ->
                // page index maps directly to task group (0..3)
                TasksScreen(group = page)
            }
        }
    }
}
