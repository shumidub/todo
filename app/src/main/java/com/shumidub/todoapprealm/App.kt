package com.shumidub.todoapprealm

import android.app.Application
import android.content.Context
import androidx.multidex.MultiDex
import com.shumidub.todoapprealm.realmcontrollers.taskcontroller.FolderTaskRealmController
import com.shumidub.todoapprealm.realmcontrollers.taskcontroller.TasksRealmController
import com.shumidub.todoapprealm.realmmodel.RealmFoldersContainer
import com.shumidub.todoapprealm.realmmodel.notes.FolderNotesObject
import com.shumidub.todoapprealm.realmmodel.task.FolderTaskObject
import dagger.hilt.android.HiltAndroidApp
import io.realm.Realm
import io.realm.RealmConfiguration
import io.realm.RealmList
import java.util.Calendar

@HiltAndroidApp
class App : Application() {

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        MultiDex.install(this)
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        Realm.init(this)
        Realm.setDefaultConfiguration(
            RealmConfiguration.Builder()
                .schemaVersion(1)
                .deleteRealmIfMigrationNeeded()
                .build()
        )
        initRealm()
        initContainers()
    }

    companion object {
        private var _realm: Realm? = null

        @JvmStatic
        lateinit var instance: App
            private set

        @JvmStatic
        val realm: Realm
            get() = _realm ?: Realm.getDefaultInstance().also { _realm = it }

        @JvmStatic
        var realmFoldersContainer: RealmFoldersContainer? = null

        @JvmStatic
        var folderOfTasksListFromContainer: RealmList<FolderTaskObject>? = null

        @JvmStatic
        var folderOfNotesContainerList: RealmList<FolderNotesObject>? = null

        @JvmStatic
        var dayScope: Int = 0

        @JvmStatic
        fun initRealm() {
            if (_realm == null) _realm = Realm.getDefaultInstance()
        }

        @JvmStatic
        fun closeRealm() {
            _realm = null
        }

        @JvmStatic
        fun setDayScopeValue() {
            val cal = Calendar.getInstance()
            val today = "${cal.get(Calendar.DAY_OF_YEAR)}${cal.get(Calendar.YEAR)}".toInt()
            var total = 0
            for (task in TasksRealmController.getDoneAndPartiallyDoneTasks()) {
                if (task.lastDoneDate != today) continue
                val equalDateCount = task.dateCountAccumulation.count { it.myInteger == today }
                total += task.countValue * equalDateCount
            }
            dayScope = total
        }

        private fun initContainers() {
            initRealm()
            realm.executeTransaction { r ->
                realmFoldersContainer = if (!FolderTaskRealmController.containerOfFolderIsExist()) {
                    r.createObject(RealmFoldersContainer::class.java)
                } else {
                    r.where(RealmFoldersContainer::class.java).findFirst()
                }
            }
            realmFoldersContainer?.let { container ->
                folderOfTasksListFromContainer = container.folderOfTasksList
                folderOfNotesContainerList = container.folderOfNotesList
            }
        }
    }
}
