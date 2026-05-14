package com.shumidub.todoapprealm.data.tasks

import com.shumidub.todoapprealm.App
import com.shumidub.todoapprealm.realmcontrollers.taskcontroller.FolderTaskRealmController
import com.shumidub.todoapprealm.realmcontrollers.taskcontroller.TasksRealmController
import com.shumidub.todoapprealm.realmmodel.task.FolderTaskObject
import com.shumidub.todoapprealm.realmmodel.task.TaskObject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TasksRepository @Inject constructor() {

    private val _folders = MutableStateFlow<List<FolderTaskSnapshot>>(emptyList())
    val folders: StateFlow<List<FolderTaskSnapshot>> = _folders.asStateFlow()

    private val _tasksByFolder = MutableStateFlow<Map<Long, List<TaskSnapshot>>>(emptyMap())
    val tasksByFolder: StateFlow<Map<Long, List<TaskSnapshot>>> = _tasksByFolder.asStateFlow()

    init {
        refreshFolders()
    }

    fun refreshFolders() {
        val list = FolderTaskRealmController.getFoldersList()?.toList().orEmpty()
        _folders.value = list.map { it.toSnapshot() }
        _tasksByFolder.value = list.associate { folder ->
            folder.id to TasksRealmController.getTasks(folder.id).map { it.toSnapshot() }
        }
    }

    fun refreshTasks(folderId: Long) {
        val tasks = TasksRealmController.getTasks(folderId).map { it.toSnapshot() }
        _tasksByFolder.value = _tasksByFolder.value.toMutableMap().apply {
            put(folderId, tasks)
        }
    }

    fun getFolder(id: Long): FolderTaskSnapshot? =
        FolderTaskRealmController.getFolder(id)?.toSnapshot()

    fun getTask(id: Long): TaskSnapshot? =
        TasksRealmController.getTask(id)?.toSnapshot()

    fun addFolder(name: String, isDaily: Boolean): Long {
        val id = FolderTaskRealmController.addFolder(name, isDaily)
        refreshFolders()
        return id
    }

    fun editFolder(id: Long, name: String, isDaily: Boolean) {
        FolderTaskRealmController.editFolder(id, name, isDaily)
        refreshFolders()
    }

    fun deleteFolder(id: Long) {
        FolderTaskRealmController.deleteFolder(id)
        refreshFolders()
    }

    fun reorderFolder(from: Int, to: Int) {
        val container = App.realmFoldersContainer ?: return
        val list = container.folderOfTasksList ?: return
        if (from !in list.indices || to !in list.indices) return
        App.initRealm()
        App.realm.executeTransaction {
            list.add(to, list.removeAt(from))
        }
        refreshFolders()
    }

    fun addTask(
        folderId: Long,
        text: String,
        countValue: Int,
        maxAccumulation: Int,
        isCycling: Boolean,
        priority: Int,
    ) {
        TasksRealmController.addTask(text, countValue, maxAccumulation, isCycling, priority, folderId)
        refreshTasks(folderId)
    }

    fun editTask(
        taskId: Long,
        text: String,
        countValue: Int,
        maxAccumulation: Int,
        isCycling: Boolean,
        priority: Int,
    ) {
        val task = TasksRealmController.getTask(taskId) ?: return
        val folderId = task.taskFolderId
        TasksRealmController.editTask(task, text, countValue, maxAccumulation, isCycling, priority)
        refreshTasks(folderId)
    }

    fun setTaskDone(taskId: Long, done: Boolean) {
        val task = TasksRealmController.getTask(taskId) ?: return
        val folderId = task.taskFolderId
        TasksRealmController.setTaskDoneOrParticullaryDone(task, done)
        refreshTasks(folderId)
    }

    fun deleteTask(taskId: Long) {
        val task = TasksRealmController.getTask(taskId) ?: return
        val folderId = task.taskFolderId
        TasksRealmController.deleteTask(task)
        refreshTasks(folderId)
    }

    fun setTaskPriority(taskId: Long, priority: Int) {
        val task = TasksRealmController.getTask(taskId) ?: return
        TasksRealmController.setTaskPriority(task, priority)
        refreshTasks(task.taskFolderId)
    }

    fun reorderTask(folderId: Long, from: Int, to: Int) {
        val list = TasksRealmController.getFolderTasksRealmListFromFolder(folderId) ?: return
        if (from !in list.indices || to !in list.indices) return
        App.initRealm()
        App.realm.executeTransaction {
            list.add(to, list.removeAt(from))
        }
        refreshTasks(folderId)
    }

    private fun FolderTaskObject.toSnapshot() = FolderTaskSnapshot(
        id = id,
        name = name.orEmpty(),
        isDaily = isDaily,
    )

    private fun TaskObject.toSnapshot() = TaskSnapshot(
        id = id,
        folderId = taskFolderId,
        text = text.orEmpty(),
        priority = priority,
        countValue = countValue,
        maxAccumulation = maxAccumulation,
        countAccumulation = countAccumulation,
        isCycling = isCycling,
        isDone = isDone,
    )
}
