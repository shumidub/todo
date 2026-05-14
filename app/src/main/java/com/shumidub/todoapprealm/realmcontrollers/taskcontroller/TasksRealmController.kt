package com.shumidub.todoapprealm.realmcontrollers.taskcontroller

import com.shumidub.todoapprealm.App
import com.shumidub.todoapprealm.realmmodel.RealmInteger
import com.shumidub.todoapprealm.realmmodel.task.FolderTaskObject
import com.shumidub.todoapprealm.realmmodel.task.TaskObject
import io.realm.kotlin.ext.query
import java.util.Calendar

object TasksRealmController {

    fun getTasks(folderId: Long): List<TaskObject> =
        App.realm.query<TaskObject>("taskFolderId == $0 SORT(done ASC, id ASC)", folderId)
            .find()
            .toList()

    fun getDoneAndPartiallyDoneTasks(): List<TaskObject> =
        App.realm.query<TaskObject>("countAccumulation != 0 SORT(done ASC, id ASC)")
            .find()
            .toList()

    fun getTask(idTask: Long): TaskObject? =
        App.realm.query<TaskObject>("id == $0", idTask).first().find()

    fun addTask(
        text: String,
        count: Int,
        maxAccumulation: Int,
        cycling: Boolean,
        priority: Int,
        taskFolderId: Long,
    ) {
        val newId = getIdForNextValue()
        App.realm.writeBlocking {
            val folder = query<FolderTaskObject>("id == $0", taskFolderId).first().find()
                ?: return@writeBlocking
            val task = copyToRealm(TaskObject().apply {
                this.id = newId
                this.text = text
                this.priority = priority
                this.taskFolderId = taskFolderId
                this.countValue = count
                this.maxAccumulation = maxAccumulation
                this.isCycling = cycling
            })
            folder.folderTasks.add(task)
        }
    }

    fun editTask(
        taskId: Long,
        text: String,
        count: Int,
        maxAccumulation: Int,
        cycling: Boolean,
        priority: Int,
    ) {
        App.realm.writeBlocking {
            val task = query<TaskObject>("id == $0", taskId).first().find() ?: return@writeBlocking
            if (text.isNotEmpty()) task.text = text
            task.priority = priority
            task.countValue = count
            task.maxAccumulation = maxAccumulation
            task.isCycling = cycling
        }
    }

    fun setTaskDoneOrParticullaryDone(taskId: Long, done: Boolean) {
        App.realm.writeBlocking {
            val task = query<TaskObject>("id == $0", taskId).first().find() ?: return@writeBlocking
            if (!done) {
                task.done = false
                task.dateCountAccumulation.clear()
                task.countAccumulation = 0
                task.lastDoneDate = 0
            } else {
                val cal = Calendar.getInstance()
                val today = "${cal.get(Calendar.DAY_OF_YEAR)}${cal.get(Calendar.YEAR)}".toInt()
                if (task.dateCountAccumulation.size < task.maxAccumulation) {
                    task.dateCountAccumulation.add(
                        copyToRealm(RealmInteger().apply { myInteger = today })
                    )
                }
                task.countAccumulation = task.dateCountAccumulation.size
                task.lastDoneDate = today
                if (task.countAccumulation >= task.maxAccumulation) {
                    task.done = true
                }
            }
        }
    }

    fun deleteTask(taskId: Long) {
        App.realm.writeBlocking {
            val task = query<TaskObject>("id == $0", taskId).first().find() ?: return@writeBlocking
            delete(task)
        }
    }

    fun setTaskPriority(taskId: Long, priority: Int) {
        if (priority !in 0..3) return
        App.realm.writeBlocking {
            val task = query<TaskObject>("id == $0", taskId).first().find() ?: return@writeBlocking
            task.priority = priority
        }
    }

    fun reorderTask(folderId: Long, from: Int, to: Int) {
        App.realm.writeBlocking {
            val list = query<FolderTaskObject>("id == $0", folderId).first().find()
                ?.folderTasks ?: return@writeBlocking
            if (from !in list.indices || to !in list.indices) return@writeBlocking
            val item = list.removeAt(from)
            list.add(to, item)
        }
    }

    private fun getIdForNextValue(): Long {
        var id = System.currentTimeMillis()
        while (App.realm.query<TaskObject>("id == $0", id).first().find() != null) {
            id++
        }
        return id
    }
}
