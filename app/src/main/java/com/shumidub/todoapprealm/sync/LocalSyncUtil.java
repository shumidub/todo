package com.shumidub.todoapprealm.sync;

import android.app.Activity;
import android.content.Intent;

import com.shumidub.todoapprealm.App;
import com.shumidub.todoapprealm.realmmodel.task.FolderTaskObject;
import com.shumidub.todoapprealm.realmmodel.RealmFoldersContainer;
import com.shumidub.todoapprealm.realmmodel.task.TaskObject;
import com.shumidub.todoapprealm.realmmodel.notes.FolderNotesObject;
import com.shumidub.todoapprealm.realmmodel.notes.NoteObject;
import com.shumidub.todoapprealm.realmmodel.report.ReportObject;

import io.realm.RealmList;


public class LocalSyncUtil {

    Activity activity;

    public LocalSyncUtil(Activity activity){
        this.activity = activity;
    }

    public void putMessage(String msg){
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, msg);
        sendIntent.setType("text/plain");
        activity.startActivity(sendIntent);
    }

    private String getRealmDbAsString(){
        String message = "";
        String indent = "    ";
        String smallIndent = " ";
        String nextLine = "\n";

        App.initRealm();

        message = ">>>>> NOTES >>>>>" + nextLine + nextLine;
        RealmList<FolderNotesObject> folderOfNotesList = App.realm.where(RealmFoldersContainer.class).findFirst().folderOfNotesList;
        for (FolderNotesObject folderNotesObject : folderOfNotesList){
            message = message + indent + folderNotesObject.getName() + " :"+ nextLine + nextLine;
            RealmList<NoteObject> notesList = folderNotesObject.getTasks();
            for (NoteObject noteObject : notesList){
                message = message + indent + indent + " --> " + noteObject.getText() + nextLine + nextLine;
            }
            message = message + nextLine;
        }
        message = message + nextLine + nextLine;

        message = message + ">>>>> TASKS >>>>>" + nextLine + nextLine;

        RealmList<FolderTaskObject> folderOfTasksList = App.realm.where(RealmFoldersContainer.class).findFirst().folderOfTasksList;

        for (FolderTaskObject folderTaskObject : folderOfTasksList){


            message = message + indent + folderTaskObject.getName() + (folderTaskObject.isDaily() ? " " : " NOT_DAILLY" ) + " :" + nextLine + nextLine;

            RealmList<TaskObject> taskList = folderTaskObject.getTasks();
            for (TaskObject taskObject : taskList){
                if (! (taskObject.isDone() && !taskObject.isCycling())){
                    message = message + indent  +
                            " --> " + taskObject.getText()
                            + nextLine + indent + indent + " count = " + taskObject.getCountValue()
                            + nextLine + indent + indent + " maxAccum = " + taskObject.getMaxAccumulation()
                            + nextLine + indent + indent + " priority = " + taskObject.getPriority()
                            + nextLine + indent + indent + " cycling = " + taskObject.isCycling()
                            + nextLine + nextLine;
                }
            }
            message = message + nextLine;
        }
        message = message + nextLine + nextLine;


        message = message + ">>>>> REPORTS >>>>>" + nextLine + nextLine;
        for (ReportObject reportObject : App.realm.where(ReportObject.class).findAll()){
            message = message + reportObject.toString() + nextLine + nextLine;
        }

        return message;

    }


    public void putAllRealmDbAsMessage(){
        putMessage(getRealmDbAsString());
    }


}
