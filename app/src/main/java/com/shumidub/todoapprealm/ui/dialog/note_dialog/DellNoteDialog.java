package com.shumidub.todoapprealm.ui.dialog.note_dialog;

import androidx.appcompat.app.AlertDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import androidx.annotation.Nullable;
import com.google.android.material.textfield.TextInputLayout;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;

import com.shumidub.todoapprealm.R;
import com.shumidub.todoapprealm.realmcontrollers.notescontroller.FolderNotesRealmController;
import com.shumidub.todoapprealm.ui.activity.main.MainActivity;
import com.shumidub.todoapprealm.ui.fragment.note_fragment.FolderNoteFragment;

import java.util.List;


/**
 * Created by A.shumidub on 05.02.18.
 *
 */

public class DellNoteDialog extends androidx.fragment.app.DialogFragment {

    int type;
    long id;


    AlertDialog dialog;

    public static final int TYPE_FOLDER = 7;
    public static final int TYPE_NOTE = 5;

    public static final String TYPE_KEY = "Type";
    public static final String ID_KEY = "Id";



    public static DellNoteDialog newInstance(int type, long id) {

        Bundle args = new Bundle();
        args.putLong(ID_KEY,id);
        args.putInt(TYPE_KEY,type);
        DellNoteDialog fragment = new DellNoteDialog();
        fragment.setArguments(args);
        return fragment;
    }


    @Nullable
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {


        if (getArguments() != null){
            type = getArguments().getInt(TYPE_KEY, -1);
            id = getArguments().getLong(ID_KEY, -1);

            if ((type != TYPE_FOLDER && type != TYPE_NOTE) ){
                return null;
            }
        }


        View view = getActivity().getLayoutInflater()
                .inflate(R.layout.note_and_folder_add_edit_dialog, null);

        AlertDialog.Builder builder = new MaterialAlertDialogBuilder(getActivity());
        builder .setMessage("Are you sure?")
                .setPositiveButton("Dell", (di,i)-> {
                    if(type == TYPE_FOLDER){
                        FolderNotesRealmController.delFolderNote(id);
                    }else if(type == TYPE_NOTE){
                        FolderNotesRealmController.delNote(id);
                    }
                    notifyDataChanged();
                    dialog.dismiss();

                })
                .setNegativeButton("Cancel", (dialog, i) -> dialog.cancel());

        dialog = builder.create();

        dialog.setCanceledOnTouchOutside(false);
        dialog.setOnKeyListener((DialogInterface dialogInterface, int keyCode, KeyEvent event) -> {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                // do nothing
                return true;
            }
            return false;
        });

        return dialog;
    }


    protected void notifyDataChanged() {
        List<androidx.fragment.app.Fragment> fragments
                = (getActivity()).getSupportFragmentManager().getFragments();

        for (androidx.fragment.app.Fragment fragment : fragments) {
            if (fragment instanceof FolderNoteFragment) {
                ((FolderNoteFragment) fragment).notifyDataChanged();
            }
        }
    }



}