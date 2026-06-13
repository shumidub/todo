package com.shumidub.todoapprealm.ui.compose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

/**
 * Compose entry point for the Jetpack Compose migration (see docs/COMPOSE-MIGRATION-PLAN.md).
 *
 * Kept SEPARATE from the legacy [com.shumidub.todoapprealm.ui.activity.main.MainActivity]
 * during the transition: the launcher still opens the old, fully-working Fragment UI, while
 * this activity hosts the growing Compose skeleton. Launch it for verification with:
 *
 *   adb shell am start -n com.shumidub.todoapprealm.alpha8/com.shumidub.todoapprealm.ui.compose.ComposeHostActivity
 *
 * In Phase 5 (cleanup) this becomes the launcher and MainActivity is deleted.
 */
class ComposeHostActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            MainScreen()
        }
    }
}
