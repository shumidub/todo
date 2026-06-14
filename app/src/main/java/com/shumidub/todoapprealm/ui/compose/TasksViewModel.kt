package com.shumidub.todoapprealm.ui.compose

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shumidub.todoapprealm.data.FolderRef
import com.shumidub.todoapprealm.data.GroupUiState
import com.shumidub.todoapprealm.data.TasksRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * Holds the detached UI state for one task group and forwards user actions to
 * [TasksRepository]. Each pager page owns its own instance (keyed by group).
 *
 * No Dagger yet: the project has Dagger 2.50 on the classpath but no graph in source
 * (see docs/specs/data-interop.md). A multibinding ViewModel factory is deferred; this
 * uses a plain compose `viewModel(factory = …)` initializer.
 */
class TasksViewModel(private val group: Int) : ViewModel() {

    val state: StateFlow<GroupUiState> =
        TasksRepository.groupFlow(group)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GroupUiState(emptyList(), 0))

    // tasks
    fun addTask(folderId: Long, text: String, count: Int = 1, max: Int = 1, cycling: Boolean = false, priority: Int = 0) =
        TasksRepository.addTask(folderId, text, count, max, cycling, priority)

    fun editTask(taskId: Long, text: String, count: Int, max: Int, cycling: Boolean, priority: Int) =
        TasksRepository.editTask(taskId, text, count, max, cycling, priority)

    fun toggleDone(taskId: Long, done: Boolean) = TasksRepository.setDone(taskId, done)
    fun deleteTask(taskId: Long) = TasksRepository.deleteTask(taskId)
    fun setCategories(taskId: Long, folderIds: List<Long>) = TasksRepository.setCategories(taskId, folderIds)
    fun allFolderRefs(): List<FolderRef> = TasksRepository.allFolderRefs()

    // folders
    fun addFolder(name: String) = TasksRepository.addFolder(group, name)
    fun editFolder(folderId: Long, name: String, isDaily: Boolean) = TasksRepository.editFolder(folderId, name, isDaily)
    fun deleteFolder(folderId: Long) = TasksRepository.deleteFolder(folderId)
    fun moveFolderToGroup(folderId: Long, targetGroup: Int) = TasksRepository.moveFolderToGroup(folderId, targetGroup)
    fun reorderFolders(orderedIds: List<Long>) = TasksRepository.reorderFolders(group, orderedIds)

    // sections
    fun addSection(folderId: Long, name: String) = TasksRepository.addSection(folderId, name)
    fun editSection(sectionId: Long, name: String) = TasksRepository.editSection(sectionId, name)
    fun deleteSection(sectionId: Long) = TasksRepository.deleteSection(sectionId)
    fun setSectionCollapsed(sectionId: Long, collapsed: Boolean) = TasksRepository.setSectionCollapsed(sectionId, collapsed)
    fun setSectionCollapsedByDefault(sectionId: Long, collapsedByDefault: Boolean) =
        TasksRepository.setSectionCollapsedByDefault(sectionId, collapsedByDefault)

    fun applyReorder(
        folderId: Long,
        outer: List<com.shumidub.todoapprealm.data.ReorderEntry>,
        inner: Map<Long, List<Long>>,
    ) = TasksRepository.applyReorder(folderId, outer, inner)
}
