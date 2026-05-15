package com.shumidub.todoapprealm.ui.dialog.note_dialog;
import androidx.appcompat.app.AlertDialog;


import android.content.Context;
import android.os.Bundle;
import android.view.inputmethod.InputMethodManager;

import com.shumidub.todoapprealm.App;
import com.shumidub.todoapprealm.realmcontrollers.notescontroller.FolderNotesRealmController;
import com.shumidub.todoapprealm.realmmodel.notes.FolderNotesObject;
import com.shumidub.todoapprealm.realmmodel.notes.NoteObject;
import com.shumidub.todoapprealm.ui.fragment.note_fragment.FolderNoteFragment;

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

    @Override
    public void onStart() {
        super.onStart();
        positiveButtonInterface = new PositiveButtonInterface() {
            @Override
            public void onClick() {
                String text = etText.getText().toString();
                App.initRealm();
                if(type == TYPE_FOLDER){
                    FolderNotesRealmController.editFolderNote(id, text);
                }else if(type == TYPE_NOTE){
                    FolderNotesRealmController.editNote(id, text);
                }
            }
        };
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener((v)->{
            if (etText.getText().toString().isEmpty()) {
                tilText.setErrorEnabled(true);
                tilText.setError("Should be filled");
            } else{
                positiveButtonInterface.onClick();
                notifyDataChanged();
                InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(getDialog().getWindow().getDecorView().getWindowToken(), 0);
                dialog.dismiss();
            }
        });
    }
}
