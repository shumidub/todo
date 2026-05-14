package com.shumidub.todoapprealm.data.notes

import com.shumidub.todoapprealm.realmcontrollers.notescontroller.FolderNotesRealmController
import com.shumidub.todoapprealm.realmmodel.notes.FolderNotesObject
import com.shumidub.todoapprealm.realmmodel.notes.NoteObject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotesRepository @Inject constructor() {

    private val _folders = MutableStateFlow<List<FolderNoteSnapshot>>(emptyList())
    val folders: StateFlow<List<FolderNoteSnapshot>> = _folders.asStateFlow()

    private val _notesByFolder = MutableStateFlow<Map<Long, List<NoteSnapshot>>>(emptyMap())
    val notesByFolder: StateFlow<Map<Long, List<NoteSnapshot>>> = _notesByFolder.asStateFlow()

    init {
        refreshFolders()
    }

    fun refreshFolders() {
        _folders.value = FolderNotesRealmController.getFoldersList().map { it.toSnapshot() }
    }

    fun refreshNotes(folderId: Long) {
        val notes = FolderNotesRealmController.getNotesList(folderId).map { it.toSnapshot() }
        _notesByFolder.value = _notesByFolder.value.toMutableMap().apply {
            put(folderId, notes)
        }
    }

    fun notesFor(folderId: Long): List<NoteSnapshot> = _notesByFolder.value[folderId].orEmpty()

    fun getFolder(id: Long): FolderNoteSnapshot? =
        FolderNotesRealmController.getFolderNote(id)?.toSnapshot()

    fun getNote(id: Long): NoteSnapshot? =
        FolderNotesRealmController.getNote(id)?.toSnapshot()

    fun addFolder(name: String): Long {
        val id = FolderNotesRealmController.addFolderNote(name)
        refreshFolders()
        return id
    }

    fun editFolder(id: Long, name: String) {
        FolderNotesRealmController.editFolderNote(id, name)
        refreshFolders()
    }

    fun deleteFolder(id: Long) {
        FolderNotesRealmController.delFolderNote(id)
        _notesByFolder.value = _notesByFolder.value - id
        refreshFolders()
    }

    fun reorderFolder(from: Int, to: Int) {
        FolderNotesRealmController.reorderFolderNote(from, to)
        refreshFolders()
    }

    fun addNote(folderId: Long, text: String): Long {
        val id = FolderNotesRealmController.addNote(folderId, text)
        refreshNotes(folderId)
        return id
    }

    fun editNote(id: Long, text: String) {
        FolderNotesRealmController.editNote(id, text)
        val note = getNote(id) ?: return
        refreshNotes(note.folderId)
    }

    fun deleteNote(id: Long) {
        val folderId = getNote(id)?.folderId
        FolderNotesRealmController.delNote(id)
        if (folderId != null) refreshNotes(folderId)
    }

    fun reorderNote(folderId: Long, noteId: Long, from: Int, to: Int) {
        FolderNotesRealmController.reorderNote(folderId, noteId, from, to)
        refreshNotes(folderId)
    }

    private fun FolderNotesObject.toSnapshot() = FolderNoteSnapshot(
        id = id,
        name = name.orEmpty(),
    )

    private fun NoteObject.toSnapshot() = NoteSnapshot(
        id = id,
        folderId = idFolder,
        text = text.orEmpty(),
    )
}
