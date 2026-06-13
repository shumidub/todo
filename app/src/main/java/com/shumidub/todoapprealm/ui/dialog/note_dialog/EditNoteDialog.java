package com.shumidub.todoapprealm.ui.dialog.note_dialog;

import android.os.Bundle;

import com.shumidub.todoapprealm.App;
import com.shumidub.todoapprealm.realmcontrollers.notescontroller.FolderNotesRealmController;
import com.shumidub.todoapprealm.realmmodel.notes.FolderNotesObject;
import com.shumidub.todoapprealm.realmmodel.notes.NoteObject;

/**
 * Created by Артем on 08.02.2018.
 *
 */

public class EditNoteDialog extends AddNoteDialog {

    public static EditNoteDialog newInstance(int type, long id) {

        Bundle args = new Bundle();
        args.putLong(ID_KEY,id);
        args.putInt(TYPE_KEY,type);
        EditNoteDialog fragment = new EditNoteDialog();
        fragment.setArguments(args);
        return fragment;
    }


    @Override
    protected void setParametres() {
        positiveButtonText = "Edit";
        positiveButtonInterface = new PositiveButtonInterface() {
            @Override
            public void onClick() {
                String text = etText.getText().toString();

                if(type == TYPE_FOLDER){
                    FolderNotesRealmController.editFolderNote(id, text);
                }else if(type == TYPE_NOTE){
                    FolderNotesRealmController.editNote(id, text);
                }

            }
        };
    }

    @Override
    protected void setEtText() {
        String name = "";
        if(type == TYPE_FOLDER){
            name = App.realm.where(FolderNotesObject.class)
                    .equalTo("id", id).findFirst().getName();
        }else if(type == TYPE_NOTE){
            name = App.realm.where(NoteObject.class)
                    .equalTo("id", id).findFirst().getText();
        }
        etText.setText(name);
    }
}
