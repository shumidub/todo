package com.shumidub.todoapprealm.realmmodel

import com.shumidub.todoapprealm.realmmodel.notes.FolderNotesObject
import com.shumidub.todoapprealm.realmmodel.report.ReportObject
import com.shumidub.todoapprealm.realmmodel.task.FolderTaskObject
import io.realm.kotlin.ext.realmListOf
import io.realm.kotlin.types.RealmList
import io.realm.kotlin.types.RealmObject

class RealmFoldersContainer : RealmObject {
    var folderOfTasksList: RealmList<FolderTaskObject> = realmListOf()
    var folderOfNotesList: RealmList<FolderNotesObject> = realmListOf()
    var reportObjectList: RealmList<ReportObject> = realmListOf()
}
