package com.shumidub.todoapprealm.ui.activity.main

import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.shumidub.todoapprealm.ui.main.MainScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
        )
        setContent {
            MainScreen(
                onExitApp = ::finish,
                onShowToast = ::showToast,
            )
        }
    }

    private fun showToast(text: String) {
        if (isFinishing) return
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
    }
}
