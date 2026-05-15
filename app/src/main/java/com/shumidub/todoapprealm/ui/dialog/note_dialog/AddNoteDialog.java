package com.shumidub.todoapprealm.ui.dialog.note_dialog;

import androidx.appcompat.app.AlertDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import androidx.annotation.Nullable;
import com.google.android.material.textfield.TextInputLayout;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
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

public class AddNoteDialog extends androidx.fragment.app.DialogFragment {

    protected MainActivity activity;
    protected EditText etText;
    protected TextInputLayout tilText;

    int type;
    long id;


    AlertDialog dialog;

    public static final int TYPE_FOLDER = 7;
    public static final int TYPE_NOTE = 5;

    public static final String TYPE_KEY = "Type";
    public static final String ID_KEY = "Id";


    String positiveButtonText = "Add";
    PositiveButtonInterface positiveButtonInterface = new PositiveButtonInterface() {
        @Override
        public void onClick() {
            String text = etText.getText().toString();
            Log.d("DTAG", "onClick: id = " + id);

            if(type == TYPE_FOLDER){
                FolderNotesRealmController.addFolderNote(text);
            }else if(type == TYPE_NOTE){
                FolderNotesRealmController.addNote(id, text);
            }
        }
    };

    interface PositiveButtonInterface {
        void onClick();
    }

    public static AddNoteDialog newInstance(int type, long id) {

        Bundle args = new Bundle();
        args.putLong(ID_KEY,id);
        args.putInt(TYPE_KEY,type);
        AddNoteDialog fragment = new AddNoteDialog();
        fragment.setArguments(args);
        return fragment;
    }


    @Nullable
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        setParametres();

        if (getArguments() != null){
            type = getArguments().getInt(TYPE_KEY, -1);
            id = getArguments().getLong(ID_KEY, -1);

            if ((type != TYPE_FOLDER && type != TYPE_NOTE) ){
                return null;
            }
        }

        activity = (MainActivity) getActivity();

        View view = getActivity().getLayoutInflater()
                .inflate(R.layout.note_and_folder_add_edit_dialog, null);

        etText = view.findViewById(R.id.note_text);
        tilText = view.findViewById(R.id.til_note_text);

        setEtText();

        AlertDialog.Builder builder = new MaterialAlertDialogBuilder(getActivity());
        builder .setView(view)
                .setPositiveButton(positiveButtonText, (di,i)-> {})
                .setNegativeButton("Cancel", (dialog, i) -> {
                    InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(getDialog().getWindow().getDecorView().getWindowToken(), 0);
                    dialog.cancel();
                });

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

    protected void setEtText(){}

    @Override
    public void onStart() {
        super.onStart();
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

    protected void setParametres(){

    }

}