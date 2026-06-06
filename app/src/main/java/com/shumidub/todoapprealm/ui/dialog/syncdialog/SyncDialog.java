package com.shumidub.todoapprealm.ui.dialog.syncdialog;

import androidx.appcompat.app.AlertDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;

import com.shumidub.todoapprealm.R;
import com.shumidub.todoapprealm.realmcontrollers.notescontroller.FolderNotesRealmController;
import com.shumidub.todoapprealm.sync.FirebaseSyncUtil;
import com.shumidub.todoapprealm.sync.JsonSyncUtil;
import com.shumidub.todoapprealm.sync.LocalSyncUtil;
import com.shumidub.todoapprealm.ui.activity.main.MainActivity;
import com.shumidub.todoapprealm.ui.dialog.firebase.FirebaseAuthDialog;
import com.shumidub.todoapprealm.ui.dialog.report_dialog.AddReportDialog;
import com.shumidub.todoapprealm.ui.fragment.note_fragment.FolderNoteFragment;

import java.util.List;


/**
 * Created by A.shumidub on 05.02.18.
 *
 */

public class SyncDialog extends androidx.fragment.app.DialogFragment {


    AlertDialog dialog;



    @Nullable
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        JsonSyncUtil jsonSyncUtil = new JsonSyncUtil(getActivity());

        View view = getActivity().getLayoutInflater()
                .inflate(R.layout.sync_dialog, null);

        AlertDialog.Builder builder = ((MainActivity) getActivity()).dialogBuilder();
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
            // Use SAF picker — scoped storage on API 30+ won't let us silently read backup
            // files we didn't create (e.g. a backup made by a previous app install).
            ((MainActivity)getActivity()).pickBackupForRestore();
            dialog.cancel();
        });


        view.findViewById(R.id.btnBackup).setOnClickListener((v)->{
            jsonSyncUtil.realmBdToJson();
            dialog.cancel();
        });


        setupAccountRow(view);

        view.findViewById(R.id.btnFirebase).setOnClickListener((v)->{
            runFirebase(true);
            dialog.cancel();
        });

        view.findViewById(R.id.btnFirebaseImport).setOnClickListener((v)->{
            runFirebase(false);
            dialog.cancel();
        });

//        if (!jsonSyncUtil.jsonIsExist()){
//            view.findViewById(R.id.btnRestore).setEnabled(false);
//        }

//
//        view.findViewById(R.id.btnBackup).setBackgroundColor(ContextCompat.getColor(getContext(), R.color.colorPrimaryDarker));
//        view.findViewById(R.id.btnRestore).setBackgroundColor(ContextCompat.getColor(getContext(), R.color.colorPrimaryDarker));
//        view.findViewById(R.id.btnSaveText).setBackgroundColor(ContextCompat.getColor(getContext(), R.color.colorPrimaryDarker));

        return dialog;
    }

    /**
     * Account row at the top of the dialog: shows the signed-in email with a "(change)"
     * affordance. Tapping it signs the current user out (if any) and opens the
     * email/password dialog so a different account can be used.
     */
    private void setupAccountRow(View view) {
        final TextView row = view.findViewById(R.id.btnFirebaseAccount);
        if (row == null) return;
        final MainActivity act = (MainActivity) getActivity();

        final FirebaseSyncUtil firebase;
        try {
            firebase = new FirebaseSyncUtil(act);
        } catch (Throwable t) {
            // Firebase not configured — hide the row entirely.
            row.setVisibility(View.GONE);
            return;
        }

        refreshAccountRow(row, firebase);

        row.setOnClickListener(v -> {
            final androidx.fragment.app.FragmentManager fm = act.getSupportFragmentManager();
            if (firebase.isSignedIn()) {
                firebase.signOut();
            }
            FirebaseAuthDialog auth = new FirebaseAuthDialog();
            auth.setOnAuth(() -> {
                if (act != null && !act.isFinishing()) {
                    refreshAccountRow(row, firebase);
                    act.showToast("Signed in as " + firebase.currentEmail());
                }
            });
            auth.show(fm, "fbauth");
        });
    }

    private void refreshAccountRow(TextView row, FirebaseSyncUtil firebase) {
        if (firebase.isSignedIn()) {
            row.setText(firebase.currentEmail() + "  (change)");
        } else {
            row.setText("Not signed in — tap to sign in");
        }
    }

    /**
     * Export (upload) or import (download) the whole DB via Firebase. Requires an
     * email/password sign-in first; if not signed in, shows the auth dialog and runs
     * the action on success. The Activity is captured up-front because this dialog is
     * dismissed before the async Firebase callback returns.
     */
    private void runFirebase(boolean isExport) {
        final MainActivity act = (MainActivity) getActivity();
        final androidx.fragment.app.FragmentManager fm = act.getSupportFragmentManager();
        try {
            final FirebaseSyncUtil firebase = new FirebaseSyncUtil(act);
            final FirebaseSyncUtil.Callback done = (ok, msg) -> {
                if (act != null && !act.isFinishing()) act.showToast(msg);
            };
            final Runnable action = () -> {
                if (isExport) firebase.uploadAll(done);
                else firebase.downloadAll(done);
            };
            if (firebase.isSignedIn()) {
                action.run();
            } else {
                FirebaseAuthDialog auth = new FirebaseAuthDialog();
                auth.setOnAuth(action::run);
                auth.show(fm, "fbauth");
            }
        } catch (Throwable t) {
            // FirebaseApp not initialized yet (google-services.json + plugin not set up).
            if (act != null && !act.isFinishing())
                act.showToast("Firebase not configured — see FIREBASE_SETUP.md");
        }
    }


    @Override
    public void onStart() {
        super.onStart();

        dialog.getButton(DialogInterface.BUTTON_NEGATIVE).setTextColor(Color.WHITE);

    }
}