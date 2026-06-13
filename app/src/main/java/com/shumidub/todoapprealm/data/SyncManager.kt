package com.shumidub.todoapprealm.data

import android.content.ContentResolver
import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.shumidub.todoapprealm.App
import com.shumidub.todoapprealm.realmcontrollers.ContainersControllers.ContainersRealmController
import com.shumidub.todoapprealm.realmmodel.RealmFoldersContainer
import com.shumidub.todoapprealm.realmmodel.task.TaskObject
import com.shumidub.todoapprealm.sync.FileWritter
import io.realm.RealmList

/**
 * Kotlin backup/sync layer for the Compose host — the same JSON + Firebase logic as the
 * legacy [com.shumidub.todoapprealm.sync.JsonSyncUtil] / [FirebaseSyncUtil] but decoupled
 * from `MainActivity` (returns/reports via callbacks instead of casting the activity).
 *
 * Realm is main-thread only, so JSON export/restore run synchronously on the caller's
 * (main) thread, exactly like the original. After a restore the container reference is
 * rebound and [TasksRepository.notifyRestored] re-emits the live screens (gap G2).
 * Schema version is never touched; old backups stay restorable (extraFolderIds normalized).
 */
object SyncManager {

    private fun gson(): Gson = GsonBuilder().setPrettyPrinting().create()

    // ---- JSON (Downloads/REALM_BD_JSON.txt) ----

    /** Serialize the whole container to Downloads. Returns a user-facing message. */
    fun exportToDownloads(): String {
        App.initRealm()
        val container = App.realm.where(RealmFoldersContainer::class.java).findFirst()
            ?: return "Нечего сохранять"
        val json = gson().toJson(App.realm.copyFromRealm(container))
        FileWritter.saveFile(json)
        return if (FileWritter.isBackupExist()) "Сохранено в Downloads (REALM_BD_JSON.txt)" else "Ошибка сохранения"
    }

    fun restoreFromUri(uri: Uri?, resolver: ContentResolver): String {
        if (uri == null) return "Файл не выбран"
        val json = try {
            resolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        } catch (e: Exception) {
            null
        }
        if (json.isNullOrBlank()) return "Файл пуст или нечитаем"
        return restoreFromJson(json)
    }

    /** Replace the whole DB from a JSON string (SAF picker + Firebase import share this). */
    fun restoreFromJson(json: String): String {
        if (json.isBlank()) return "Пустой бэкап"
        App.initRealm()
        try {
            App.realm.executeTransaction { realm ->
                ContainersRealmController.deleteFromRealmAllContainers()
                val restored = gson().fromJson(json, RealmFoldersContainer::class.java)
                realm.insertOrUpdate(restored)
                // Backups made before multi-category support don't carry extraFolderIds.
                for (t in realm.where(TaskObject::class.java).findAll()) {
                    if (t.extraFolderIds == null) t.extraFolderIds = RealmList()
                }
            }
        } catch (e: Exception) {
            return "Ошибка восстановления: ${e.message}"
        }
        // Re-point the static container refs the UI reads, then re-emit (gap G2).
        App.rebindContainers()
        TasksRepository.notifyRestored()
        return "Восстановлено!"
    }

    // ---- Firebase (users/{uid}/backup) ----

    private fun auth(): FirebaseAuth? = try { FirebaseAuth.getInstance() } catch (t: Throwable) { null }

    fun firebaseAvailable(): Boolean = auth() != null
    fun isSignedIn(): Boolean = auth()?.currentUser != null
    fun currentEmail(): String? = auth()?.currentUser?.email
    fun signOut() { auth()?.signOut() }

    fun signIn(email: String, password: String, register: Boolean, cb: (Boolean, String) -> Unit) {
        val a = auth() ?: run { cb(false, "Firebase не настроен"); return }
        if (email.isBlank() || password.length < 6) {
            cb(false, "Введите email и пароль (≥6 символов)")
            return
        }
        val task = if (register) a.createUserWithEmailAndPassword(email.trim(), password)
        else a.signInWithEmailAndPassword(email.trim(), password)
        task.addOnCompleteListener { t ->
            if (t.isSuccessful) cb(true, "Вход выполнен: ${currentEmail()}")
            else cb(false, t.exception?.message ?: "Ошибка авторизации")
        }
    }

    fun uploadToFirebase(cb: (Boolean, String) -> Unit) {
        val user = auth()?.currentUser ?: run { cb(false, "Не выполнен вход"); return }
        App.initRealm()
        val container = App.realm.where(RealmFoldersContainer::class.java).findFirst()
            ?: run { cb(false, "Нечего выгружать"); return }
        val tree: Any = try {
            val g = Gson()
            g.fromJson(g.toJson(App.realm.copyFromRealm(container)), Any::class.java)
        } catch (e: Exception) {
            cb(false, "Ошибка сериализации: ${e.message}"); return
        }
        val payload = hashMapOf<String, Any>("backup" to tree, "updatedAt" to ServerValue.TIMESTAMP)
        FirebaseDatabase.getInstance().getReference("users").child(user.uid)
            .updateChildren(payload)
            .addOnCompleteListener { t ->
                if (t.isSuccessful) cb(true, "Выгружено в облако")
                else cb(false, t.exception?.message ?: "Ошибка выгрузки")
            }
    }

    fun downloadFromFirebase(cb: (Boolean, String) -> Unit) {
        val user = auth()?.currentUser ?: run { cb(false, "Не выполнен вход"); return }
        FirebaseDatabase.getInstance().getReference("users").child(user.uid).child("backup").get()
            .addOnCompleteListener { t ->
                if (!t.isSuccessful) {
                    cb(false, t.exception?.message ?: "Ошибка загрузки"); return@addOnCompleteListener
                }
                val tree = t.result?.value
                if (tree == null) { cb(false, "В облаке нет бэкапа"); return@addOnCompleteListener }
                val json = try { Gson().toJson(tree) } catch (e: Exception) {
                    cb(false, "Ошибка разбора: ${e.message}"); return@addOnCompleteListener
                }
                cb(true, restoreFromJson(json))
            }
    }
}
