package com.shumidub.todoapprealm.data

import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.FirebaseDatabase
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import com.shumidub.todoapprealm.App
import com.shumidub.todoapprealm.realmmodel.RealmFoldersContainer
import com.shumidub.todoapprealm.realmmodel.task.SectionObject
import com.shumidub.todoapprealm.realmmodel.task.TaskObject
import com.shumidub.todoapprealm.sync.FileWritter
import io.realm.RealmList

/**
 * Kotlin backup/sync layer for the Compose host: JSON + Firebase backup/restore, driven
 * via callbacks (no Activity coupling — the legacy MainActivity/sync-util classes are gone).
 *
 * Realm is main-thread only, so JSON export/restore run synchronously on the caller's
 * (main) thread, exactly like the original. After a restore the container reference is
 * rebound and [TasksRepository.notifyRestored] re-emits the live screens (gap G2).
 * Schema version is never touched; old backups stay restorable (extraFolderIds normalized).
 */
/** Snapshot of the Firebase backup history, for display in the sync dialog. */
data class BackupInfo(val count: Int, val lastTs: Long?, val sizeBytes: Long)

object SyncManager {

    private fun gson(): Gson = GsonBuilder().setPrettyPrinting().create()

    // ---- JSON (Downloads/REALM_BD_JSON.txt) ----

    /**
     * Serialize the container + sections into one tree. SectionObject lives in a separate
     * Realm table (not in the container's object graph), so it must be attached explicitly
     * under "sections" — otherwise sections are lost on restore and tasks with a sectionId
     * become orphaned (invisible). Returns null if there's nothing to back up.
     */
    private fun serializeWithSections(): MutableMap<String, Any?>? {
        val container = App.realm.where(RealmFoldersContainer::class.java).findFirst() ?: return null
        val g = Gson()
        @Suppress("UNCHECKED_CAST")
        val map = g.fromJson(g.toJson(App.realm.copyFromRealm(container)), Map::class.java) as MutableMap<String, Any?>
        val sections = App.realm.copyFromRealm(App.realm.where(SectionObject::class.java).findAll())
        map["sections"] = g.fromJson(g.toJson(sections), Any::class.java)
        return map
    }

    /** Serialize the whole container to Downloads. Returns a user-facing message. */
    fun exportToDownloads(): String {
        App.initRealm()
        val tree = serializeWithSections() ?: return "Нечего сохранять"
        return if (FileWritter.saveFile(gson().toJson(tree))) "Сохранено в Downloads (REALM_BD_JSON.txt)"
        else "Ошибка сохранения"
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
                // Полная замена БД. realm.deleteAll() безопасен внутри транзакции — в отличие
                // от прежнего deleteFromRealmAllContainers(), который удалял объекты во время
                // итерации по live-RealmResults и открывал вложенные RealmDb.write().
                realm.deleteAll()
                val restored = gson().fromJson(json, RealmFoldersContainer::class.java)
                realm.insertOrUpdate(restored)
                // Sections sit beside the container under "sections" (separate Realm table),
                // so Gson's RealmFoldersContainer parse ignores them — restore them by hand.
                try {
                    val rootObj = JsonParser.parseString(json).asJsonObject
                    if (rootObj.has("sections") && rootObj.get("sections").isJsonArray) {
                        for (el in rootObj.getAsJsonArray("sections")) {
                            realm.insertOrUpdate(gson().fromJson(el, SectionObject::class.java))
                        }
                    }
                } catch (e: Exception) {
                    Log.w("SyncManager", "sections restore skipped: ${e.message}")
                }
                // Backups made before multi-category support don't carry extraFolderIds.
                for (t in realm.where(TaskObject::class.java).findAll()) {
                    if (t.extraFolderIds == null) t.extraFolderIds = RealmList()
                }
            }
        } catch (e: Exception) {
            Log.e("SyncManager", "restore failed", e)
            return "Ошибка восстановления: ${e.message}"
        }
        // Re-point the static container refs the UI reads, then re-emit (gap G2).
        App.rebindContainers()
        TasksRepository.notifyRestored()
        return "Восстановлено!"
    }

    // ---- Firebase (history: users/{uid}/timestamps + users/{uid}/snapshots/{ts}) ----
    //
    // Каждая выгрузка пишется новым снимком snapshots/{ts} (ts — System.currentTimeMillis),
    // а ts добавляется в список timestamps. Когда история превышает MAX_HISTORY,
    // удаляем самый старый ts и его снимок. Старый плоский users/{uid}/backup
    // мигрируется в историю при первой выгрузке.

    private const val MAX_HISTORY = 30

    private fun auth(): FirebaseAuth? = try { FirebaseAuth.getInstance() } catch (t: Throwable) { null }

    /** timestamps может лежать как массив или как объект — собираем отсортированный список Long. */
    private fun parseTimestamps(snap: DataSnapshot?): MutableList<Long> {
        val out = ArrayList<Long>()
        if (snap != null && snap.exists()) {
            for (child in snap.children) (child.value as? Number)?.let { out.add(it.toLong()) }
        }
        out.sort()
        return out
    }

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

    /**
     * Read backup-history stats from Firebase: number of snapshots, the latest snapshot's
     * timestamp, and the byte size of the JSON that "Загрузить из Firebase" would pull
     * (the backup node, or the latest snapshot if backup is absent). Returns null if not signed in.
     */
    fun fetchBackupInfo(cb: (BackupInfo?) -> Unit) {
        val user = auth()?.currentUser ?: run { cb(null); return }
        val root = FirebaseDatabase.getInstance().getReference("users").child(user.uid)
        val sizeOf = { tree: Any? ->
            if (tree == null) 0L
            else try { Gson().toJson(tree).toByteArray(Charsets.UTF_8).size.toLong() } catch (e: Exception) { 0L }
        }
        root.child("timestamps").get().addOnCompleteListener { t1 ->
            val list = if (t1.isSuccessful) parseTimestamps(t1.result) else emptyList()
            root.child("backup").get().addOnCompleteListener { tb ->
                val backup = if (tb.isSuccessful) tb.result?.value else null
                if (backup != null) {
                    cb(BackupInfo(list.size, list.lastOrNull(), sizeOf(backup))); return@addOnCompleteListener
                }
                val last = list.lastOrNull()
                if (last == null) { cb(BackupInfo(list.size, null, 0L)); return@addOnCompleteListener }
                root.child("snapshots").child(last.toString()).get().addOnCompleteListener { t2 ->
                    cb(BackupInfo(list.size, last, sizeOf(if (t2.isSuccessful) t2.result?.value else null)))
                }
            }
        }
    }

    fun uploadToFirebase(cb: (Boolean, String) -> Unit) {
        val user = auth()?.currentUser ?: run { cb(false, "Не выполнен вход"); return }
        App.initRealm()
        val tree: Any = try {
            serializeWithSections() ?: run { cb(false, "Нечего выгружать"); return }
        } catch (e: Exception) {
            cb(false, "Ошибка сериализации: ${e.message}"); return
        }
        val root = FirebaseDatabase.getInstance().getReference("users").child(user.uid)
        root.child("timestamps").get().addOnCompleteListener { t1 ->
            if (!t1.isSuccessful) { cb(false, t1.exception?.message ?: "Ошибка выгрузки"); return@addOnCompleteListener }
            val list = parseTimestamps(t1.result)
            var ts = System.currentTimeMillis()
            while (list.contains(ts)) ts++
            list.add(ts)
            val updates = HashMap<String, Any?>()
            while (list.size > MAX_HISTORY) {       // переполнение истории — выкидываем самый старый снимок
                val removed = list.removeAt(0)
                updates["snapshots/$removed"] = null
            }
            // backup + updatedAt — каноничный «последний» (его читает версия из main);
            // snapshots/{ts} + timestamps — история (до MAX_HISTORY). Пишем dual-write.
            updates["backup"] = tree
            updates["updatedAt"] = ts
            updates["snapshots/$ts"] = tree
            updates["timestamps"] = list
            root.updateChildren(updates).addOnCompleteListener { t2 ->
                if (t2.isSuccessful) cb(true, "Выгружено в Firebase")
                else cb(false, t2.exception?.message ?: "Ошибка выгрузки")
            }
        }
    }

    fun downloadFromFirebase(cb: (Boolean, String) -> Unit) {
        val user = auth()?.currentUser ?: run { cb(false, "Не выполнен вход"); return }
        val root = FirebaseDatabase.getInstance().getReference("users").child(user.uid)
        val restore = { tree: Any? ->
            if (tree == null) cb(false, "В Firebase нет бэкапа")
            else {
                val json = try { Gson().toJson(tree) } catch (e: Exception) { null }
                if (json == null) cb(false, "Ошибка разбора")
                else cb(true, restoreFromJson(json))
            }
        }
        // Каноничный последний — users/{uid}/backup (как в версии из main).
        root.child("backup").get().addOnCompleteListener { tb ->
            val backup = if (tb.isSuccessful) tb.result?.value else null
            if (backup != null) { restore(backup); return@addOnCompleteListener }
            // backup нет (например, данные только в истории) — берём последний снимок.
            root.child("timestamps").get().addOnCompleteListener { t1 ->
                if (!t1.isSuccessful) { cb(false, t1.exception?.message ?: "Ошибка загрузки"); return@addOnCompleteListener }
                val list = parseTimestamps(t1.result)
                if (list.isEmpty()) { cb(false, "В Firebase нет бэкапа"); return@addOnCompleteListener }
                root.child("snapshots").child(list.last().toString()).get().addOnCompleteListener { t2 ->
                    if (!t2.isSuccessful) { cb(false, t2.exception?.message ?: "Ошибка загрузки"); return@addOnCompleteListener }
                    restore(t2.result?.value)
                }
            }
        }
    }
}
