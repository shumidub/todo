package com.shumidub.todoapprealm.ui.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shumidub.todoapprealm.data.tasks.FolderTaskSnapshot
import com.shumidub.todoapprealm.data.tasks.TaskSnapshot
import com.shumidub.todoapprealm.data.tasks.TasksRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class TasksViewModel @Inject constructor(
    private val repository: TasksRepository,
) : ViewModel() {

    private val addForm = MutableStateFlow(AddTaskForm())
    private val dialog = MutableStateFlow<TasksDialog>(TasksDialog.None)
    private val selectedFolderIndex = MutableStateFlow(0)
    private val showDoneTasks = MutableStateFlow(false)

    val state: StateFlow<TasksState> = combine(
        repository.folders,
        repository.tasksByFolder,
        addForm,
        dialog,
        combine(selectedFolderIndex, showDoneTasks) { idx, showDone -> idx to showDone },
    ) { folders, tasksByFolder, form, currentDialog, (idx, showDone) ->
        val safeIndex = idx.coerceIn(0, (folders.size - 1).coerceAtLeast(0))
        TasksState(
            folders = folders,
            tasksByFolder = tasksByFolder,
            addForm = form,
            dialog = currentDialog,
            selectedFolderIndex = safeIndex,
            showDoneTasks = showDone,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, TasksState())

    fun selectFolder(index: Int) {
        selectedFolderIndex.value = index
    }

    fun toggleShowDoneTasks() {
        showDoneTasks.value = !showDoneTasks.value
    }

    fun onAddFormTextChange(value: String) {
        addForm.value = addForm.value.copy(text = value)
    }

    fun cycleCount() {
        addForm.value = addForm.value.copy(countValue = cycle1to9(addForm.value.countValue))
    }

    fun cycleMaxAccumulation() {
        addForm.value = addForm.value.copy(maxAccumulation = cycle1to9(addForm.value.maxAccumulation))
    }

    fun cyclePriority() {
        addForm.value = addForm.value.copy(priority = (addForm.value.priority + 1) % 4)
    }

    fun toggleCycling() {
        addForm.value = addForm.value.copy(isCycling = !addForm.value.isCycling)
    }

    fun submitAddTask(folderId: Long) {
        val form = addForm.value
        if (form.text.isBlank()) return
        repository.addTask(
            folderId = folderId,
            text = form.text,
            countValue = form.countValue,
            maxAccumulation = form.maxAccumulation,
            isCycling = form.isCycling,
            priority = form.priority,
        )
        addForm.value = AddTaskForm()
    }

    fun openAddFolder() {
        dialog.value = TasksDialog.AddFolder
    }

    fun openFolderActions(folderId: Long) {
        dialog.value = TasksDialog.FolderActions(folderId)
    }

    fun openEditFolder(folderId: Long) {
        val folder = repository.getFolder(folderId) ?: return
        dialog.value = TasksDialog.EditFolder(folder)
    }

    fun openDeleteFolder(folderId: Long) {
        dialog.value = TasksDialog.DeleteFolder(folderId)
    }

    fun openTaskActions(taskId: Long) {
        dialog.value = TasksDialog.TaskActions(taskId)
    }

    fun openEditTask(taskId: Long) {
        val task = repository.getTask(taskId) ?: return
        dialog.value = TasksDialog.EditTask(task)
    }

    fun openDeleteTask(taskId: Long) {
        dialog.value = TasksDialog.DeleteTask(taskId)
    }

    fun dismissDialog() {
        dialog.value = TasksDialog.None
    }

    fun addFolder(name: String, isDaily: Boolean) {
        repository.addFolder(name, isDaily)
        dismissDialog()
    }

    fun saveFolder(id: Long, name: String, isDaily: Boolean) {
        repository.editFolder(id, name, isDaily)
        dismissDialog()
    }

    fun confirmDeleteFolder(id: Long) {
        repository.deleteFolder(id)
        dismissDialog()
    }

    fun saveTask(
        id: Long,
        text: String,
        countValue: Int,
        maxAccumulation: Int,
        isCycling: Boolean,
        priority: Int,
    ) {
        repository.editTask(id, text, countValue, maxAccumulation, isCycling, priority)
        dismissDialog()
    }

    fun confirmDeleteTask(id: Long) {
        repository.deleteTask(id)
        dismissDialog()
    }

    fun toggleTaskDone(taskId: Long, done: Boolean) {
        repository.setTaskDone(taskId, done)
    }

    fun cycleTaskPriority(taskId: Long, currentPriority: Int) {
        repository.setTaskPriority(taskId, (currentPriority + 1) % 4)
    }

    fun reorderFolder(from: Int, to: Int) {
        if (from == to) return
        repository.reorderFolder(from, to)
    }

    fun reorderTask(folderId: Long, from: Int, to: Int) {
        if (from == to) return
        repository.reorderTask(folderId, from, to)
    }

    private fun cycle1to9(current: Int): Int = if (current < 9) current + 1 else 1
}

data class TasksState(
    val folders: List<FolderTaskSnapshot> = emptyList(),
    val tasksByFolder: Map<Long, List<TaskSnapshot>> = emptyMap(),
    val addForm: AddTaskForm = AddTaskForm(),
    val dialog: TasksDialog = TasksDialog.None,
    val selectedFolderIndex: Int = 0,
    val showDoneTasks: Boolean = false,
)

data class AddTaskForm(
    val text: String = "",
    val countValue: Int = 1,
    val maxAccumulation: Int = 1,
    val priority: Int = 0,
    val isCycling: Boolean = false,
)

sealed interface TasksDialog {
    data object None : TasksDialog
    data object AddFolder : TasksDialog
    data class FolderActions(val folderId: Long) : TasksDialog
    data class EditFolder(val folder: FolderTaskSnapshot) : TasksDialog
    data class DeleteFolder(val id: Long) : TasksDialog
    data class TaskActions(val taskId: Long) : TasksDialog
    data class EditTask(val task: TaskSnapshot) : TasksDialog
    data class DeleteTask(val id: Long) : TasksDialog
}
