package com.shumidub.todoapprealm.data

import com.shumidub.todoapprealm.App
import com.shumidub.todoapprealm.realmcontrollers.taskcontroller.FolderTaskRealmController
import com.shumidub.todoapprealm.realmcontrollers.taskcontroller.SectionsRealmController
import com.shumidub.todoapprealm.realmcontrollers.taskcontroller.TasksRealmController
import com.shumidub.todoapprealm.realmmodel.task.SectionObject
import com.shumidub.todoapprealm.realmmodel.task.TaskObject
import io.realm.Realm
import io.realm.RealmChangeListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * Thin Kotlin interop layer over the existing Java Realm controllers (Phase 1).
 *
 * Reads: a [Flow] of **detached** [GroupUiState] snapshots. A single [RealmChangeListener]
 * on the process-wide UI-thread Realm re-reads + re-emits on every committed transaction.
 * No managed Realm object escapes (invariant G1); a restore that swaps the container simply
 * triggers another emit (G2).
 *
 * Writes: delegate straight to the existing static controllers (synchronous main-thread
 * transactions). The change listener then drives the next emission. Realm stays on the main
 * thread; the schema version is never touched here.
 */
object TasksRepository {

    /** Pinged after a restore swaps the container, to force the flows to re-read (gap G2). */
    private val restoreSignal = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    fun notifyRestored() { restoreSignal.tryEmit(Unit) }

    /** Detached state for a task group (0..3) plus the global day-score, reactive to any commit. */
    fun groupFlow(group: Int): Flow<GroupUiState> = callbackFlow {
        val realm: Realm? = App.realm
        if (realm == null) {
            trySend(GroupUiState(emptyList(), 0))
            awaitClose { }
            return@callbackFlow
        }
        val listener = RealmChangeListener<Realm> { trySend(readState(group)) }
        realm.addChangeListener(listener)
        val job = launch { restoreSignal.collect { trySend(readState(group)) } }
        trySend(readState(group)) // initial snapshot
        awaitClose { realm.removeChangeListener(listener); job.cancel() }
    }

    private fun readState(group: Int): GroupUiState =
        GroupUiState(folders = readGroup(group), dayScope = computeDayScope())

    /** Reads the group's folders (tasks + sections) into plain DTOs. Main-thread; objects stay local. */
    private fun readGroup(group: Int): List<FolderDto> {
        val container = App.realmFoldersContainer ?: return emptyList()
        val folders = container.tasksListForGroup(group) ?: return emptyList()
        return folders.mapNotNull { f ->
            if (f == null || !f.isValid) return@mapNotNull null
            val tasks = TasksRealmController.getTasks(f.id).map { it.toDto() }
            val sections = SectionsRealmController.getSections(f.id).map { it.toDto() }
            FolderDto(id = f.id, name = f.name ?: "", isDaily = f.isDaily, tasks = tasks, sections = sections)
        }
    }

    private fun computeDayScope(): Int {
        App.setDayScopeValue()
        return App.dayScope
    }

    /** All folders across every group, for the editor's category picker. */
    fun allFolderRefs(): List<FolderRef> =
        FolderTaskRealmController.getAllFolders().mapNotNull { f ->
            if (f == null || !f.isValid) null
            else FolderRef(id = f.id, name = f.name ?: "", group = FolderTaskRealmController.getFolderGroup(f))
        }

    // ---- task writes ----

    /**
     * Add a task to a folder, mirroring the legacy bottom add-task panel toggles
     * (points / max / priority / cycling). Defaults match a plain one-tap checkbox task.
     */
    fun addTask(folderId: Long, text: String, count: Int = 1, max: Int = 1, cycling: Boolean = false, priority: Int = 0) {
        val t = text.trim()
        if (t.isEmpty()) return
        TasksRealmController.addTask(t, count, max, cycling, priority, folderId)
    }

    /** Edit an existing task's text + params (does not change done state). */
    fun editTask(taskId: Long, text: String, count: Int, max: Int, cycling: Boolean, priority: Int) {
        val task = TasksRealmController.getTask(taskId) ?: return
        TasksRealmController.editTask(task, text, count, max, cycling, priority)
    }

    fun setDone(taskId: Long, done: Boolean) {
        val task = TasksRealmController.getTask(taskId) ?: return
        TasksRealmController.setTaskDoneOrParticullaryDone(task, done)
    }

    fun deleteTask(taskId: Long) = TasksRealmController.deleteTask(taskId)

    /** Replace the set of folders a task belongs to (first id becomes primary). */
    fun setCategories(taskId: Long, folderIds: List<Long>) {
        val task = TasksRealmController.getTask(taskId) ?: return
        if (folderIds.isEmpty()) return
        TasksRealmController.setTaskCategories(task, folderIds)
    }

    // ---- folder writes ----

    fun addFolder(group: Int, name: String) {
        val n = name.trim()
        if (n.isEmpty()) return
        FolderTaskRealmController.addFolder(n, false, group)
    }

    fun editFolder(folderId: Long, name: String, isDaily: Boolean) {
        val n = name.trim()
        if (n.isEmpty()) return
        FolderTaskRealmController.editFolder(folderId, n, isDaily)
    }

    fun deleteFolder(folderId: Long) = FolderTaskRealmController.deleteFolder(folderId)

    fun moveFolderToGroup(folderId: Long, targetGroup: Int) {
        val folder = FolderTaskRealmController.getFolder(folderId) ?: return
        FolderTaskRealmController.moveFolderToGroup(folder, targetGroup)
    }

    // ---- section writes ----

    fun addSection(folderId: Long, name: String) {
        val n = name.trim()
        if (n.isEmpty() || n.length > 40) return
        SectionsRealmController.addSection(folderId, n, false, SectionsRealmController.nextOuterPosition(folderId))
    }

    fun editSection(sectionId: Long, name: String) {
        val s = SectionsRealmController.getSection(sectionId) ?: return
        val n = name.trim()
        if (n.isEmpty() || n.length > 40) return
        SectionsRealmController.editSection(s, n, s.isCollapsedByDefault)
    }

    fun deleteSection(sectionId: Long) {
        val s = SectionsRealmController.getSection(sectionId) ?: return
        SectionsRealmController.deleteSection(s)
    }

    fun setSectionCollapsed(sectionId: Long, collapsed: Boolean) {
        val s = SectionsRealmController.getSection(sectionId) ?: return
        SectionsRealmController.setCurrentlyCollapsed(s, collapsed)
    }

    // ---- daily lifecycle (replaces FolderSlidingPanelFragment.onResume / App start) ----

    private var lastResetDate = 0

    /** Reset cycling tasks not completed today. Cheap no-op if the date hasn't changed. */
    fun runDailyResetIfNeeded() {
        if (App.realm == null) return
        val today = todayDate()
        if (today == lastResetDate) return
        val snapshot = ArrayList(TasksRealmController.getDoneAndPartiallyDoneTasks())
        for (t in snapshot) {
            if (t != null && t.isValid && t.isCycling && t.lastDoneDate != today) {
                TasksRealmController.setTaskDoneOrParticullaryDone(t, false)
            }
        }
        lastResetDate = today
    }

    /** Clear any per-session manual collapse state so sections open per their default. */
    fun resetCollapseStatesAtStart() {
        if (App.realm == null) return
        SectionsRealmController.resetAllCollapseStates()
    }

    private fun todayDate(): Int {
        val cal = Calendar.getInstance()
        return ("" + cal.get(Calendar.DAY_OF_YEAR) + cal.get(Calendar.YEAR)).toInt()
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
    extraFolderIds = extraFolderIds?.filterNotNull() ?: emptyList(),
)

private fun SectionObject.toDto() = SectionDto(
    id = id,
    name = name ?: "",
    currentlyCollapsed = isCurrentlyCollapsed,
    collapsedByDefault = isCollapsedByDefault,
    position = position,
)
