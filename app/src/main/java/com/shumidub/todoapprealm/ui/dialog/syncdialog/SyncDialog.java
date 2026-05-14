package com.shumidub.todoapprealm.ui.dialog.syncdialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.view.KeyEvent;
import android.view.View;

import com.shumidub.todoapprealm.R;
import com.shumidub.todoapprealm.realmcontrollers.notescontroller.FolderNotesRealmController;
import com.shumidub.todoapprealm.sync.JsonSyncUtil;
import com.shumidub.todoapprealm.sync.LocalSyncUtil;
import com.shumidub.todoapprealm.ui.activity.main.MainActivity;
import com.shumidub.todoapprealm.ui.fragment.note_fragment.FolderNoteFragment;

import java.util.List;


/**
 * Created by A.shumidub on 05.02.18.
 *
 */

public class SyncDialog extends android.support.v4.app.DialogFragment {


    AlertDialog dialog;



    @Nullable
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        JsonSyncUtil jsonSyncUtil = new JsonSyncUtil(getActivity());

        View view = getActivity().getLayoutInflater()
                .inflate(R.layout.sync_dialog, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(view)
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

        view.findViewById(R.id.btnSaveText).setOnClickListener((v)->{
            new LocalSyncUtil(getActivity()).putAllRealmDbAsMessage();
        });


        view.findViewById(R.id.btnRestore).setOnClickListener((v)->{
            jsonSyncUtil.realmBdFromJson();
            ((MainActivity)getActivity()).invalidateOptionsMenu();


        });


        view.findViewById(R.id.btnBackup).setOnClickListener((v)->{
            jsonSyncUtil.realmBdToJson();
            dialog.cancel();
        });

//        if (!jsonSyncUtil.jsonIsExist()){
//            view.findViewById(R.id.btnRestore).setEnabled(false);
//        }

        dialog.getWindow().setBackgroundDrawableResource(R.color.colorPrimaryDark);
//
//        view.findViewById(R.id.btnBackup).setBackgroundColor(ContextCompat.getColor(getContext(), R.color.colorPrimaryDarker));
//        view.findViewById(R.id.btnRestore).setBackgroundColor(ContextCompat.getColor(getContext(), R.color.colorPrimaryDarker));
//        view.findViewById(R.id.btnSaveText).setBackgroundColor(ContextCompat.getColor(getContext(), R.color.colorPrimaryDarker));

        return dialog;
    }


    @Override
    public void onStart() {
        super.onStart();

        dialog.getButton(DialogInterface.BUTTON_NEGATIVE).setTextColor(Color.WHITE);

    }
}