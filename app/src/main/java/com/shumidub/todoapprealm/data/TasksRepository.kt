package com.shumidub.todoapprealm.data

import com.shumidub.todoapprealm.App
import com.shumidub.todoapprealm.realmcontrollers.taskcontroller.FolderTaskRealmController
import com.shumidub.todoapprealm.realmcontrollers.taskcontroller.TasksRealmController
import com.shumidub.todoapprealm.realmmodel.task.TaskObject
import io.realm.Realm
import io.realm.RealmChangeListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Thin Kotlin interop layer over the existing Java Realm controllers (Phase 1).
 *
 * Reads: a [Flow] of **detached** [FolderDto] snapshots. We register a single
 * [RealmChangeListener] on the process-wide UI-thread Realm; every committed transaction
 * re-reads the group and emits a fresh immutable snapshot. No managed Realm object escapes
 * (invariant G1); a restore that swaps the container simply triggers another emit (G2).
 *
 * Writes: delegate straight to the existing static controllers (which run synchronous
 * main-thread transactions). The change listener then drives the next emission.
 *
 * Realm stays on the main thread (App config: allowQueriesOnUiThread / allowWritesOnUiThread);
 * the schema version is never touched here.
 */
object TasksRepository {

    /** Detached folders (with their tasks) for a task group (0..3), reactive to any Realm commit. */
    fun foldersFlow(group: Int): Flow<List<FolderDto>> = callbackFlow {
        val realm: Realm? = App.realm
        if (realm == null) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }
        val listener = RealmChangeListener<Realm> { trySend(readGroup(group)) }
        realm.addChangeListener(listener)
        trySend(readGroup(group)) // initial snapshot
        awaitClose { realm.removeChangeListener(listener) }
    }

    /** Reads the group's folders + tasks into plain DTOs. Main-thread; managed objects stay local. */
    private fun readGroup(group: Int): List<FolderDto> {
        val container = App.realmFoldersContainer ?: return emptyList()
        val folders = container.tasksListForGroup(group) ?: return emptyList()
        return folders.mapNotNull { f ->
            if (f == null || !f.isValid) return@mapNotNull null
            val tasks = TasksRealmController.getTasks(f.id).map { it.toDto() }
            FolderDto(id = f.id, name = f.name ?: "", isDaily = f.isDaily, tasks = tasks)
        }
    }

    /**
     * Add a task to a folder, mirroring the legacy bottom add-task panel toggles
     * (points / max / priority / cycling). Defaults match a plain one-tap checkbox task.
     */
    fun addTask(
        folderId: Long,
        text: String,
        count: Int = 1,
        max: Int = 1,
        cycling: Boolean = false,
        priority: Int = 0,
    ) {
        val t = text.trim()
        if (t.isEmpty()) return
        TasksRealmController.addTask(t, count, max, cycling, priority, folderId)
    }

    fun setDone(taskId: Long, done: Boolean) {
        val task = TasksRealmController.getTask(taskId) ?: return
        TasksRealmController.setTaskDoneOrParticullaryDone(task, done)
    }

    fun deleteTask(taskId: Long) = TasksRealmController.deleteTask(taskId)

    fun addFolder(group: Int, name: String) {
        val n = name.trim()
        if (n.isEmpty()) return
        FolderTaskRealmController.addFolder(n, false, group)
    }
}

private fun TaskObject.toDto() = TaskDto(
    id = id,
    text = text ?: "",
    done = isDone,
    priority = priority,
    countValue = countValue,
    maxAccumulation = maxAccumulation,
    countAccumulation = countAccumulation,
    isCycling = isCycling,
    sectionId = sectionId,
    position = position,
    taskFolderId = taskFolderId,
)
