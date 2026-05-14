package com.shumidub.todoapprealm.realmcontrollers.taskcontroller

import com.shumidub.todoapprealm.App
import com.shumidub.todoapprealm.realmmodel.RealmFoldersContainer
import com.shumidub.todoapprealm.realmmodel.task.FolderTaskObject
import io.realm.kotlin.ext.query

object FolderTaskRealmController {

    fun getFoldersList(): List<FolderTaskObject> =
        App.realm.query<RealmFoldersContainer>().first().find()
            ?.folderOfTasksList
            ?.toList()
            .orEmpty()

    fun getFolder(id: Long): FolderTaskObject? =
        App.realm.query<FolderTaskObject>("id == $0", id).first().find()

    fun addFolder(name: String, isDaily: Boolean): Long {
        val newId = getIdForNextValue()
        App.realm.writeBlocking {
            val container = query<RealmFoldersContainer>().first().find() ?: return@writeBlocking
            val folder = copyToRealm(FolderTaskObject().apply {
                this.id = newId
                this.name = name
                this.isDaily = isDaily
            })
            container.folderOfTasksList.add(folder)
        }
        return newId
    }

    fun editFolder(id: Long, name: String, isDaily: Boolean) {
        App.realm.writeBlocking {
            val folder = query<FolderTaskObject>("id == $0", id).first().find() ?: return@writeBlocking
            folder.name = name
            folder.isDaily = isDaily
        }
    }

    fun deleteFolder(id: Long) {
        App.realm.writeBlocking {
            val folder = query<FolderTaskObject>("id == $0", id).first().find() ?: return@writeBlocking
            folder.folderTasks.toList().forEach { delete(it) }
            delete(folder)
        }
    }

    fun reorderFolder(from: Int, to: Int) {
        App.realm.writeBlocking {
            val list = query<RealmFoldersContainer>().first().find()?.folderOfTasksList ?: return@writeBlocking
            if (from !in list.indices || to !in list.indices) return@writeBlocking
            val item = list.removeAt(from)
            list.add(to, item)
        }
    }

    fun listOfFolderIsEmpty(): Boolean =
        App.realm.query<FolderTaskObject>().count().find() == 0L

    fun containerOfFolderIsExist(): Boolean =
        App.realm.query<RealmFoldersContainer>().first().find() != null

    private fun getIdForNextValue(): Long {
        var id = System.currentTimeMillis()
        while (App.realm.query<FolderTaskObject>("id == $0", id).first().find() != null) {
            id++
        }
        return id
    }
}
