package com.shumidub.todoapprealm.realmcontrollers.notescontroller

import com.shumidub.todoapprealm.App
import com.shumidub.todoapprealm.realmmodel.RealmFoldersContainer
import com.shumidub.todoapprealm.realmmodel.notes.FolderNotesObject
import com.shumidub.todoapprealm.realmmodel.notes.NoteObject
import io.realm.kotlin.ext.query

object FolderNotesRealmController {

    // Folders

    fun getFoldersList(): List<FolderNotesObject> =
        App.realm.query<RealmFoldersContainer>().first().find()
            ?.folderOfNotesList
            ?.toList()
            .orEmpty()

    fun getFolderNote(id: Long): FolderNotesObject? =
        App.realm.query<FolderNotesObject>("id == $0", id).first().find()

    fun addFolderNote(name: String): Long {
        val newId = getNewValidFolderNotesId()
        App.realm.writeBlocking {
            val container = query<RealmFoldersContainer>().first().find() ?: return@writeBlocking
            val folder = copyToRealm(FolderNotesObject().apply {
                this.id = newId
                this.name = name
            })
            container.folderOfNotesList.add(folder)
        }
        return newId
    }

    fun editFolderNote(id: Long, name: String) {
        App.realm.writeBlocking {
            val folder = query<FolderNotesObject>("id == $0", id).first().find()
                ?: return@writeBlocking
            folder.name = name
        }
    }

    fun delFolderNote(id: Long) {
        App.realm.writeBlocking {
            val folder = query<FolderNotesObject>("id == $0", id).first().find()
                ?: return@writeBlocking
            folder.notesObjectRealmList.toList().forEach { delete(it) }
            delete(folder)
        }
    }

    fun reorderFolderNote(from: Int, to: Int) {
        App.realm.writeBlocking {
            val list = query<RealmFoldersContainer>().first().find()?.folderOfNotesList
                ?: return@writeBlocking
            if (from !in list.indices || to !in list.indices) return@writeBlocking
            val item = list.removeAt(from)
            list.add(to, item)
        }
    }

    // Notes

    fun getNotesList(folderId: Long): List<NoteObject> =
        App.realm.query<FolderNotesObject>("id == $0", folderId).first().find()
            ?.notesObjectRealmList
            ?.toList()
            .orEmpty()

    fun getNote(id: Long): NoteObject? =
        App.realm.query<NoteObject>("id == $0", id).first().find()

    fun addNote(folderId: Long, text: String): Long {
        val newId = getNewValidNotesId()
        App.realm.writeBlocking {
            val folder = query<FolderNotesObject>("id == $0", folderId).first().find()
                ?: return@writeBlocking
            val note = copyToRealm(NoteObject().apply {
                this.id = newId
                this.text = text
                this.idFolder = folderId
            })
            folder.notesObjectRealmList.add(note)
        }
        return newId
    }

    fun editNote(id: Long, text: String) {
        App.realm.writeBlocking {
            val note = query<NoteObject>("id == $0", id).first().find() ?: return@writeBlocking
            note.text = text
        }
    }

    fun delNote(id: Long) {
        App.realm.writeBlocking {
            val note = query<NoteObject>("id == $0", id).first().find() ?: return@writeBlocking
            delete(note)
        }
    }

    fun reorderNote(folderId: Long, noteId: Long, from: Int, to: Int) {
        App.realm.writeBlocking {
            val list = query<FolderNotesObject>("id == $0", folderId).first().find()
                ?.notesObjectRealmList ?: return@writeBlocking
            if (from !in list.indices || to !in list.indices) return@writeBlocking
            val item = list.removeAt(from)
            list.add(to, item)
        }
    }

    private fun getNewValidFolderNotesId(): Long {
        var id = System.currentTimeMillis()
        while (App.realm.query<FolderNotesObject>("id == $0", id).first().find() != null) {
            id++
        }
        return id
    }

    private fun getNewValidNotesId(): Long {
        var id = System.currentTimeMillis()
        while (App.realm.query<NoteObject>("id == $0", id).first().find() != null) {
            id++
        }
        return id
    }
}
