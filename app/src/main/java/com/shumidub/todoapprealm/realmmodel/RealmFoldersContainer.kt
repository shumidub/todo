package com.shumidub.todoapprealm.realmmodel

import com.shumidub.todoapprealm.realmmodel.notes.FolderNotesObject
import com.shumidub.todoapprealm.realmmodel.report.ReportObject
import com.shumidub.todoapprealm.realmmodel.task.FolderTaskObject
import io.realm.RealmList
import io.realm.RealmObject
import java.io.Serializable

open class RealmFoldersContainer : RealmObject(), Serializable {
    var folderOfTasksList: RealmList<FolderTaskObject> = RealmList()
    var folderOfNotesList: RealmList<FolderNotesObject> = RealmList()
    var reportObjectList: RealmList<ReportObject> = RealmList()
}
