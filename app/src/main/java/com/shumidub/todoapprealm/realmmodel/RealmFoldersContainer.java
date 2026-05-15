package com.shumidub.todoapprealm.realmmodel;

import com.shumidub.todoapprealm.realmmodel.notes.FolderNotesObject;
import com.shumidub.todoapprealm.realmmodel.report.ReportObject;
import com.shumidub.todoapprealm.realmmodel.task.FolderTaskObject;

import java.io.Serializable;

import io.realm.RealmList;
import io.realm.RealmObject;

/**
 * Created by Артем on 25.01.2018.
 *
 */

public class RealmFoldersContainer extends RealmObject implements Serializable{
    public RealmList<FolderTaskObject> folderOfTasksList;
    public RealmList<FolderNotesObject> folderOfNotesList;
    public RealmList<ReportObject> reportObjectList;

}




