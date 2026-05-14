package com.shumidub.todoapprealm.realmcontrollers.taskcontroller

import com.shumidub.todoapprealm.App
import com.shumidub.todoapprealm.realmmodel.task.FolderTaskObject
import com.shumidub.todoapprealm.realmmodel.task.TaskObject
import io.realm.RealmList
import io.realm.RealmResults
import io.realm.Sort
import java.util.Calendar

object TasksRealmController {

    fun getTasks(folderId: Long): RealmResults<TaskObject> {
        App.initRealm()
        return getFolderTasksRealmListFromFolder(folderId)?.sort("done", Sort.ASCENDING)
            ?: App.realm.where(TaskObject::class.java)
                .alwaysFalse()
                .findAll()
    }

    fun getDoneAndPartiallyDoneTasks(): RealmResults<TaskObject> {
        App.initRealm()
        return App.realm.where(TaskObject::class.java)
            .notEqualTo("countAccumulation", 0)
            .findAll()
            .sort("done", Sort.ASCENDING, "id", Sort.ASCENDING)
    }

    fun getTask(idTask: Long): TaskObject? {
        App.initRealm()
        return App.realm.where(TaskObject::class.java).equalTo("id", idTask).findFirst()
    }

    fun addTask(
        text: String,
        count: Int,
        maxAccumulation: Int,
        cycling: Boolean,
        priority: Int,
        taskFolderId: Long,
    ) {
        App.initRealm()
        App.realm.executeTransaction { realm ->
            val task = realm.createObject(TaskObject::class.java).apply {
                this.id = getIdForNextValue()
                this.text = text
                this.lastDoneDate = 0
                this.priority = priority
                this.taskFolderId = taskFolderId
                this.countValue = count
                this.maxAccumulation = maxAccumulation
                this.countAccumulation = 0
                this.isCycling = cycling
            }
            FolderTaskRealmController.getFolder(taskFolderId)?.folderTasks?.add(task)
        }
    }

    fun editTask(
        task: TaskObject,
        text: String,
        count: Int,
        maxAccumulation: Int,
        cycling: Boolean,
        priority: Int,
    ) {
        App.initRealm()
        App.realm.executeTransaction {
            if (text.isNotEmpty()) task.text = text
            task.priority = priority
            task.countValue = count
            task.maxAccumulation = maxAccumulation
            task.isCycling = cycling
        }
    }

    fun setTaskDoneOrParticullaryDone(task: TaskObject, done: Boolean) {
        App.initRealm()
        App.realm.executeTransaction {
            if (!done) {
                task.done = false
                task.clearDateCountAccumulation()
                task.lastDoneDate = 0
            } else {
                val cal = Calendar.getInstance()
                val date = "${cal.get(Calendar.DAY_OF_YEAR)}${cal.get(Calendar.YEAR)}".toInt()
                task.addDateCountAccumulation(date)
                task.lastDoneDate = date
                if (task.countAccumulation >= task.maxAccumulation) {
                    task.done = true
                }
            }
        }
    }

    fun deleteTask(task: TaskObject) {
        App.initRealm()
        val taskId = task.id
        val deletion: () -> Unit = {
            task.dateCountAccumulation.clear()
            val folder = FolderTaskRealmController.getFolder(task.taskFolderId)
            if (task.isValid && folder?.folderTasks?.contains(task) == true) {
                folder.folderTasks.remove(task)
            }
            task.deleteFromRealm()
            App.realm.where(TaskObject::class.java)
                .equalTo("id", taskId)
                .findAll()
                .deleteAllFromRealm()
        }
        if (App.realm.isInTransaction) deletion() else App.realm.executeTransaction { deletion() }
    }

    fun setTaskPriority(task: TaskObject, priority: Int) {
        if (priority !in 0..3) return
        App.initRealm()
        App.realm.executeTransaction { task.priority = priority }
    }

    fun getFolderTasksRealmListFromFolder(folderId: Long): RealmList<TaskObject>? {
        App.initRealm()
        return App.realm.where(FolderTaskObject::class.java)
            .equalTo("id", folderId)
            .findFirst()
            ?.folderTasks
    }

    private fun getIdForNextValue(): Long {
        App.initRealm()
        var id = System.currentTimeMillis()
        while (App.realm.where(TaskObject::class.java).equalTo("id", id).findFirst() != null) {
            id++
        }
        return id
    }
}
