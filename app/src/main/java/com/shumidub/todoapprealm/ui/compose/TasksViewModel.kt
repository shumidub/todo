package com.shumidub.todoapprealm.ui.compose

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shumidub.todoapprealm.data.FolderDto
import com.shumidub.todoapprealm.data.TasksRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * Holds the detached folder/task state for one task group and forwards user actions to
 * [TasksRepository]. Each pager page owns its own instance (keyed by group).
 *
 * No Dagger yet: the project has Dagger 2.50 on the classpath but no graph in source
 * (see docs/specs/data-interop.md). A multibinding ViewModel factory is deferred to the
 * proper Phase 1; this slice uses a plain compose `viewModel(factory = …)` initializer.
 */
class TasksViewModel(private val group: Int) : ViewModel() {

    val folders: StateFlow<List<FolderDto>> =
        TasksRepository.foldersFlow(group)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addTask(
        folderId: Long,
        text: String,
        count: Int = 1,
        max: Int = 1,
        cycling: Boolean = false,
        priority: Int = 0,
    ) = TasksRepository.addTask(folderId, text, count, max, cycling, priority)

    fun toggleDone(taskId: Long, done: Boolean) = TasksRepository.setDone(taskId, done)
    fun deleteTask(taskId: Long) = TasksRepository.deleteTask(taskId)
    fun addFolder(name: String) = TasksRepository.addFolder(group, name)
}
