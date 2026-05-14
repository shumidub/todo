package com.shumidub.todoapprealm.data.tasks

data class FolderTaskSnapshot(
    val id: Long,
    val name: String,
    val isDaily: Boolean,
)

data class TaskSnapshot(
    val id: Long,
    val folderId: Long,
    val text: String,
    val priority: Int,
    val countValue: Int,
    val maxAccumulation: Int,
    val countAccumulation: Int,
    val isCycling: Boolean,
    val isDone: Boolean,
)
