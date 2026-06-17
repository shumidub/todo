package com.shumidub.todoapprealm.ui.compose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.shumidub.todoapprealm.data.TasksRepository

/**
 * Sole launcher activity — hosts the full Jetpack Compose UI
 * (see docs/COMPOSE-MIGRATION-PLAN.md). The legacy Fragment-based `MainActivity` it
 * replaced has been removed; this is now the only Activity in the app.
 */
class ComposeHostActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // Sections open per their default each launch (matches legacy app start).
        TasksRepository.resetCollapseStatesAtStart()
        setContent {
            MainScreen()
        }
    }

    override fun onResume() {
        super.onResume()
        // Reset cycling tasks not completed today (legacy FolderSlidingPanelFragment.onResume).
        TasksRepository.runDailyResetIfNeeded()
    }
}
