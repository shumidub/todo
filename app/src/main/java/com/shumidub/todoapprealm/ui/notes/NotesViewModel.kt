package com.shumidub.todoapprealm.ui.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shumidub.todoapprealm.data.notes.FolderNoteSnapshot
import com.shumidub.todoapprealm.data.notes.NoteSnapshot
import com.shumidub.todoapprealm.data.notes.NotesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class NotesViewModel @Inject constructor(
    private val repository: NotesRepository,
) : ViewModel() {

    private val mode = MutableStateFlow<NotesMode>(NotesMode.Folders)
    private val dialog = MutableStateFlow<NotesDialog>(NotesDialog.None)

    val state: StateFlow<NotesState> = combine(
        repository.folders,
        repository.notesByFolder,
        mode,
        dialog,
    ) { folders, notesByFolder, currentMode, currentDialog ->
        val notes = when (currentMode) {
            is NotesMode.Notes -> notesByFolder[currentMode.folderId].orEmpty()
            NotesMode.Folders -> emptyList()
        }
        NotesState(
            mode = currentMode,
            folders = folders,
            notes = notes,
            dialog = currentDialog,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, NotesState())

    val currentMode: NotesMode get() = mode.value
    val currentDialog: NotesDialog get() = dialog.value

    fun openFolder(folder: FolderNoteSnapshot) {
        repository.refreshNotes(folder.id)
        mode.value = NotesMode.Notes(folderId = folder.id, folderName = folder.name)
    }

    fun backToFolders() {
        mode.value = NotesMode.Folders
        repository.refreshFolders()
    }

    fun openAdd() {
        dialog.value = when (val m = mode.value) {
            NotesMode.Folders -> NotesDialog.AddFolder
            is NotesMode.Notes -> NotesDialog.AddNote(m.folderId)
        }
    }

    fun openFolderActions(folderId: Long) {
        dialog.value = NotesDialog.FolderActions(folderId)
    }

    fun openNoteActions(noteId: Long) {
        dialog.value = NotesDialog.NoteActions(noteId)
    }

    fun openEditFolder(folderId: Long) {
        val folder = repository.getFolder(folderId) ?: return
        dialog.value = NotesDialog.EditFolder(folder)
    }

    fun openEditNote(noteId: Long) {
        val note = repository.getNote(noteId) ?: return
        dialog.value = NotesDialog.EditNote(note)
    }

    fun openDeleteFolder(folderId: Long) {
        dialog.value = NotesDialog.DeleteFolder(folderId)
    }

    fun openDeleteNote(noteId: Long) {
        dialog.value = NotesDialog.DeleteNote(noteId)
    }

    fun dismissDialog() {
        dialog.value = NotesDialog.None
    }

    fun addFolder(name: String) {
        repository.addFolder(name)
        dismissDialog()
    }

    fun addNote(folderId: Long, text: String) {
        repository.addNote(folderId, text)
        dismissDialog()
    }

    fun saveFolder(id: Long, name: String) {
        repository.editFolder(id, name)
        dismissDialog()
    }

    fun saveNote(id: Long, text: String) {
        repository.editNote(id, text)
        dismissDialog()
    }

    fun confirmDeleteFolder(id: Long) {
        repository.deleteFolder(id)
        if ((mode.value as? NotesMode.Notes)?.folderId == id) {
            mode.value = NotesMode.Folders
        }
        dismissDialog()
    }

    fun confirmDeleteNote(id: Long) {
        repository.deleteNote(id)
        dismissDialog()
    }

    fun reorderFolder(from: Int, to: Int) {
        if (from == to) return
        repository.reorderFolder(from, to)
    }

    fun reorderNote(from: Int, to: Int) {
        val m = mode.value as? NotesMode.Notes ?: return
        if (from == to) return
        val noteId = repository.notesFor(m.folderId).getOrNull(from)?.id ?: return
        repository.reorderNote(m.folderId, noteId, from, to)
    }
}

data class NotesState(
    val mode: NotesMode = NotesMode.Folders,
    val folders: List<FolderNoteSnapshot> = emptyList(),
    val notes: List<NoteSnapshot> = emptyList(),
    val dialog: NotesDialog = NotesDialog.None,
)

sealed interface NotesMode {
    data object Folders : NotesMode
    data class Notes(val folderId: Long, val folderName: String) : NotesMode
}

sealed interface NotesDialog {
    data object None : NotesDialog
    data object AddFolder : NotesDialog
    data class AddNote(val folderId: Long) : NotesDialog
    data class EditFolder(val folder: FolderNoteSnapshot) : NotesDialog
    data class EditNote(val note: NoteSnapshot) : NotesDialog
    data class FolderActions(val folderId: Long) : NotesDialog
    data class NoteActions(val noteId: Long) : NotesDialog
    data class DeleteFolder(val id: Long) : NotesDialog
    data class DeleteNote(val id: Long) : NotesDialog
}
