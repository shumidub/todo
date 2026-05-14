package com.shumidub.todoapprealm.realmcontrollers.taskcontroller

import com.shumidub.todoapprealm.App
import com.shumidub.todoapprealm.realmmodel.RealmFoldersContainer
import com.shumidub.todoapprealm.realmmodel.task.FolderTaskObject
import com.shumidub.todoapprealm.realmmodel.task.TaskObject
import io.realm.RealmList

object FolderTaskRealmController {

    fun getFoldersList(): RealmList<FolderTaskObject>? {
        App.initRealm()
        return App.folderOfTasksListFromContainer
    }

    fun getFolder(id: Long): FolderTaskObject? {
        App.initRealm()
        return App.realm.where(FolderTaskObject::class.java).equalTo("id", id).findFirst()
    }

    fun addFolder(name: String, isDaily: Boolean): Long {
        val id = getIdForNextValue()
        App.initRealm()
        App.realm.executeTransaction { realm ->
            val folder = realm.createObject(FolderTaskObject::class.java).apply {
                this.id = id
                this.name = name
                this.isDaily = isDaily
            }
            App.folderOfTasksListFromContainer?.add(folder)
        }
        return id
    }

    fun editFolder(id: Long, name: String, isDaily: Boolean): Long {
        App.initRealm()
        val folder = getFolder(id) ?: return id
        App.realm.executeTransaction {
            folder.name = name
            folder.isDaily = isDaily
        }
        return id
    }

    fun deleteFolder(folder: FolderTaskObject) {
        App.initRealm()
        val folderId = folder.id
        val tasks = folder.folderTasks
        val deletion: () -> Unit = {
            tasks.deleteAllFromRealm()
            App.realm.where(TaskObject::class.java)
                .equalTo("taskFolderId", folderId)
                .findAll()
                .deleteAllFromRealm()
            App.folderOfTasksListFromContainer?.remove(folder)
            folder.deleteFromRealm()
            App.realm.where(FolderTaskObject::class.java)
                .equalTo("id", folderId)
                .findAll()
                .deleteAllFromRealm()
        }
        if (App.realm.isInTransaction) deletion() else App.realm.executeTransaction { deletion() }
    }

    fun deleteFolder(id: Long) {
        getFolder(id)?.let { deleteFolder(it) }
    }

    fun listOfFolderIsEmpty(): Boolean {
        App.initRealm()
        val all = App.realm.where(FolderTaskObject::class.java).findAll()
        return all.isEmpty()
    }

    fun containerOfFolderIsExist(): Boolean {
        App.initRealm()
        return App.realm.where(RealmFoldersContainer::class.java).findFirst() != null
    }

    private fun getIdForNextValue(): Long {
        App.initRealm()
        var id = System.currentTimeMillis()
        while (App.realm.where(FolderTaskObject::class.java).equalTo("id", id).findFirst() != null) {
            id++
        }
        return id
    }
}
