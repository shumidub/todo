package com.shumidub.todoapprealm.realmcontrollers.ContainersControllers;

import com.shumidub.todoapprealm.App;
import com.shumidub.todoapprealm.realmcontrollers.notescontroller.FolderNotesRealmController;
import com.shumidub.todoapprealm.realmcontrollers.reportcontroller.ReportRealmController;
import com.shumidub.todoapprealm.realmcontrollers.taskcontroller.FolderTaskRealmController;
import com.shumidub.todoapprealm.realmcontrollers.taskcontroller.TasksRealmController;
import com.shumidub.todoapprealm.realmmodel.RealmFoldersContainer;
import com.shumidub.todoapprealm.realmmodel.notes.FolderNotesObject;
import com.shumidub.todoapprealm.realmmodel.report.ReportObject;
import com.shumidub.todoapprealm.realmmodel.task.FolderTaskObject;

/**
 * Created by A.shumidub on 29.03.18.
 */

public class ContainersRealmController {


    public static void deleteFromRealmAllContainers() {
        App.initRealm();

        for (RealmFoldersContainer realmFoldersContainer: App.realm.where(RealmFoldersContainer.class).findAll()){

            for (FolderNotesObject folderNotesObject : App.realm.where(FolderNotesObject.class).findAll()){
                long id = folderNotesObject.getId();
                FolderNotesRealmController.delFolderNote(id);
            }

            for (FolderTaskObject folderTaskObject: App.realm.where(FolderTaskObject.class).findAll()){
                FolderTaskRealmController.deleteFolder(folderTaskObject);
            }

            for (ReportObject reportObject:App.realm.where(ReportObject.class).findAll()){
                ReportRealmController.delReport(reportObject.getId());
            }

        }

        App.realm.where(RealmFoldersContainer.class).findAll().deleteAllFromRealm();
    }
}
