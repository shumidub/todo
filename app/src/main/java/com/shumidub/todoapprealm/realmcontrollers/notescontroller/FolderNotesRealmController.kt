package com.shumidub.todoapprealm.realmcontrollers.notescontroller

import com.shumidub.todoapprealm.App
import com.shumidub.todoapprealm.realmmodel.notes.FolderNotesObject
import com.shumidub.todoapprealm.realmmodel.notes.NoteObject
import io.realm.RealmList

object FolderNotesRealmController {

    // Folders

    fun getFolderNote(id: Long): FolderNotesObject? {
        App.initRealm()
        return App.realm.where(FolderNotesObject::class.java).equalTo("id", id).findFirst()
    }

    fun addFolderNote(name: String): Long {
        val id = getNewValidFolderNotesId()
        App.initRealm()
        App.realm.executeTransaction { realm ->
            val folder = realm.createObject(FolderNotesObject::class.java).apply {
                this.id = id
                this.name = name
            }
            App.folderOfNotesContainerList?.add(folder)
        }
        return id
    }

    fun editFolderNote(id: Long, name: String) {
        App.initRealm()
        App.realm.executeTransaction {
            App.realm.where(FolderNotesObject::class.java)
                .equalTo("id", id)
                .findFirst()
                ?.name = name
        }
    }

    fun delFolderNote(id: Long) {
        App.initRealm()
        val folder = getFolderNote(id) ?: return
        val notes = folder.notesObjectRealmList
        val deletion: () -> Unit = {
            notes.deleteAllFromRealm()
            App.realm.where(FolderNotesObject::class.java)
                .equalTo("id", id)
                .findAll()
                .deleteAllFromRealm()
        }
        if (App.realm.isInTransaction) deletion() else App.realm.executeTransaction { deletion() }
    }

    fun reorderFolderNote(from: Int, to: Int) {
        App.initRealm()
        val list = App.folderOfNotesContainerList ?: return
        if (from !in list.indices || to !in list.indices) return
        App.realm.executeTransaction { list.add(to, list.removeAt(from)) }
    }

    // Notes

    fun getNotesList(idFolderNotesObject: Long): RealmList<NoteObject>? =
        getFolderNote(idFolderNotesObject)?.notesObjectRealmList

    fun getNote(idNotesObject: Long): NoteObject? {
        App.initRealm()
        return App.realm.where(NoteObject::class.java).equalTo("id", idNotesObject).findFirst()
    }

    fun addNote(idFolderNotesObject: Long, text: String): Long {
        val id = getNewValidNotesId()
        App.initRealm()
        App.realm.executeTransaction { realm ->
            val folder = realm.where(FolderNotesObject::class.java)
                .equalTo("id", idFolderNotesObject)
                .findFirst() ?: return@executeTransaction
            val note = realm.createObject(NoteObject::class.java).apply {
                this.id = id
                this.text = text
                this.idFolder = idFolderNotesObject
            }
            folder.notesObjectRealmList.add(note)
        }
        return id
    }

    fun editNote(idNotesObject: Long, text: String) {
        App.initRealm()
        App.realm.executeTransaction {
            App.realm.where(NoteObject::class.java)
                .equalTo("id", idNotesObject)
                .findFirst()
                ?.text = text
        }
    }

    fun delNote(idNotesObject: Long) {
        App.initRealm()
        App.realm.executeTransaction {
            val note = App.realm.where(NoteObject::class.java)
                .equalTo("id", idNotesObject)
                .findFirst() ?: return@executeTransaction
            App.realm.where(FolderNotesObject::class.java)
                .equalTo("id", note.idFolder)
                .findFirst()
                ?.notesObjectRealmList
                ?.remove(note)
            note.deleteFromRealm()
        }
    }

    fun reorderNote(idFolderNotesObject: Long, idNotesObject: Long, from: Int, to: Int) {
        App.initRealm()
        val notes = App.realm.where(FolderNotesObject::class.java)
            .equalTo("id", idFolderNotesObject)
            .findFirst()
            ?.notesObjectRealmList ?: return
        if (from !in notes.indices || to !in notes.indices) return
        App.realm.executeTransaction { notes.add(to, notes.removeAt(from)) }
    }

    private fun getNewValidFolderNotesId(): Long {
        App.initRealm()
        var id = System.currentTimeMillis()
        while (App.realm.where(FolderNotesObject::class.java).equalTo("id", id).findFirst() != null) {
            id++
        }
        return id
    }

    private fun getNewValidNotesId(): Long {
        App.initRealm()
        var id = System.currentTimeMillis()
        while (App.realm.where(NoteObject::class.java).equalTo("id", id).findFirst() != null) {
            id++
        }
        return id
    }
}
