package com.shumidub.todoapprealm

import android.app.Application
import com.shumidub.todoapprealm.data.realm.REALM_SCHEMA
import com.shumidub.todoapprealm.data.realm.REALM_SCHEMA_VERSION
import com.shumidub.todoapprealm.data.realm.RealmBackup
import com.shumidub.todoapprealm.data.realm.todoRealmMigration
import com.shumidub.todoapprealm.realmcontrollers.taskcontroller.FolderTaskRealmController
import com.shumidub.todoapprealm.realmcontrollers.taskcontroller.TasksRealmController
import com.shumidub.todoapprealm.realmmodel.RealmFoldersContainer
import dagger.hilt.android.HiltAndroidApp
import io.realm.kotlin.Realm
import io.realm.kotlin.RealmConfiguration
import io.realm.kotlin.ext.query
import java.util.Calendar

@HiltAndroidApp
class App : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this

        RealmBackup.backupIfMigrationNeeded(this)

        val config = RealmConfiguration.Builder(REALM_SCHEMA)
            .schemaVersion(REALM_SCHEMA_VERSION)
            .migration(todoRealmMigration)
            .build()
        realm = Realm.open(config)
        ensureContainer()
    }

    private fun ensureContainer() {
        if (!FolderTaskRealmController.containerOfFolderIsExist()) {
            realm.writeBlocking {
                copyToRealm(RealmFoldersContainer())
            }
        }
    }

    companion object {
        @JvmStatic
        lateinit var instance: App
            private set

        @JvmStatic
        lateinit var realm: Realm
            private set

        @JvmStatic
        var dayScope: Int = 0

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
    }
}
