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
    /** Folders shown on the second Tasks tab. Added after the Reports tab was removed. */
    public RealmList<FolderTaskObject> folderOfTasksList2;
    /** Folders shown on the third Tasks tab (Canary palette / Tasks3). Added in SCHEMA_VERSION 4. */
    public RealmList<FolderTaskObject> folderOfTasksList3;
    public RealmList<FolderNotesObject> folderOfNotesList;
    public RealmList<ReportObject> reportObjectList;

}




