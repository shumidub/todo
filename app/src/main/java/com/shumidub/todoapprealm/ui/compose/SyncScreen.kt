package com.shumidub.todoapprealm.ui.compose

import android.app.Activity
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.shumidub.todoapprealm.data.SyncManager
import com.shumidub.todoapprealm.sync.LocalSyncUtil
import com.shumidub.todoapprealm.ui.theme.TabPalette

private fun toast(ctx: Context, msg: String) = Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()

/**
 * Backup / sync dialog for the Compose host: JSON export to Downloads, restore from a
 * SAF-picked file, share as text, and Firebase upload/download with email/password auth.
 * All logic lives in [SyncManager]; this is just the UI. JSON ops are synchronous (Realm is
 * main-thread); Firebase ops report via Toast from their async callbacks.
 */
@Composable
fun SyncDialog(palette: TabPalette, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val activity = context as? Activity

    var signedInEmail by remember { mutableStateOf(SyncManager.currentEmail()) }
    var showAuth by remember { mutableStateOf(false) }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) toast(context, SyncManager.restoreFromUri(uri, context.contentResolver))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Бэкап / синхронизация") },
        text = {
            Column {
                SyncRow("Сохранить в файл") { toast(context, SyncManager.exportToDownloads()) }
                SyncRow("Восстановить из файла") { importLauncher.launch(arrayOf("*/*")) }
                SyncRow("Поделиться текстом") {
                    activity?.let { LocalSyncUtil(it).putAllRealmDbAsMessage() }
                    onDismiss()
                }
                if (SyncManager.firebaseAvailable()) {
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider(color = palette.inputText.copy(alpha = 0.12f))
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = signedInEmail?.let { "Аккаунт: $it" } ?: "Вход не выполнен",
                        color = palette.inputText.copy(alpha = 0.7f),
                    )
                    SyncRow(if (signedInEmail != null) "Сменить аккаунт" else "Войти в облако") {
                        if (signedInEmail != null) { SyncManager.signOut(); signedInEmail = null }
                        showAuth = true
                    }
                    SyncRow("Выгрузить в облако") {
                        SyncManager.uploadToFirebase { _, msg -> toast(context, msg) }
                    }
                    SyncRow("Загрузить из облака") {
                        SyncManager.downloadFromFirebase { _, msg -> toast(context, msg) }
                        onDismiss()
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Закрыть") } },
        containerColor = Color.White,
    )

    if (showAuth) {
        FirebaseAuthDialog(
            palette = palette,
            onSignedIn = { signedInEmail = SyncManager.currentEmail(); showAuth = false },
            onDismiss = { showAuth = false },
        )
    }
}

@Composable
private fun SyncRow(label: String, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Text(label, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun FirebaseAuthDialog(palette: TabPalette, onSignedIn: () -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    fun go(register: Boolean) {
        SyncManager.signIn(email, password, register) { ok, msg ->
            if (ok) { toast(context, msg); onSignedIn() } else message = msg
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Вход в Firebase") },
        text = {
            Column {
                OutlinedTextField(
                    value = email, onValueChange = { email = it }, singleLine = true,
                    label = { Text("Email") }, modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = password, onValueChange = { password = it }, singleLine = true,
                    label = { Text("Пароль") }, visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (message.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(message, color = palette.accent)
                }
            }
        },
        confirmButton = { TextButton(onClick = { go(false) }) { Text("Войти") } },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { go(true) }) { Text("Регистрация") }
                TextButton(onClick = onDismiss) { Text("Отмена") }
            }
        },
        containerColor = Color.White,
    )
}
