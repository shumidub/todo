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
)

data class FolderDto(
    val id: Long,
    val name: String,
    val isDaily: Boolean,
    val tasks: List<TaskDto>,
)
