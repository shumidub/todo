package com.shumidub.todoapprealm.data.notes

data class FolderNoteSnapshot(
    val id: Long,
    val name: String,
)

data class NoteSnapshot(
    val id: Long,
    val folderId: Long,
    val text: String,
)
