package com.shumidub.todoapprealm.data

/**
 * Immutable, **detached** snapshots of the Realm task model for the Compose UI.
 *
 * Per migration invariant G1 (docs/specs/_GAPS.md): no live Realm object ever crosses
 * into composition. Repositories read managed objects on the main thread and immediately
 * map them to these plain data classes; the UI only ever sees DTOs.
 */
data class TaskDto(
    val id: Long,
    val text: String,
    val done: Boolean,
    val priority: Int,
    val countValue: Int,
    val maxAccumulation: Int,
    val countAccumulation: Int,
    val isCycling: Boolean,
    val sectionId: Long,
    val position: Int,
    val taskFolderId: Long,
    val extraFolderIds: List<Long> = emptyList(),
)

/** A task section (collapsible group) inside a folder. */
data class SectionDto(
    val id: Long,
    val name: String,
    val currentlyCollapsed: Boolean,
    val collapsedByDefault: Boolean,
    val position: Int,
)

data class FolderDto(
    val id: Long,
    val name: String,
    val isDaily: Boolean,
    val tasks: List<TaskDto>,
    val sections: List<SectionDto> = emptyList(),
)

/** Lightweight folder reference for the task-editor category picker (across all groups). */
data class FolderRef(
    val id: Long,
    val name: String,
    val group: Int,
)

/** One emission of a task group's UI state: its folders + the global day-score counter. */
data class GroupUiState(
    val folders: List<FolderDto>,
    val dayScope: Int,
)
