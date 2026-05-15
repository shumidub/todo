package com.shumidub.todoapprealm.ui.dialog.report_dialog;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AlertDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import android.view.KeyEvent;
import android.view.View;

import com.shumidub.todoapprealm.realmcontrollers.reportcontroller.ReportRealmController;
import com.shumidub.todoapprealm.ui.actionmode.report.ReportActionModeCallback;
import com.shumidub.todoapprealm.ui.fragment.report_section.report_fragment.ReportFragment;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import io.reactivex.annotations.NonNull;

/**
 * Created by A.shumidub on 05.02.18.
 */

public class DellReportDialog extends androidx.fragment.app.DialogFragment {

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new MaterialAlertDialogBuilder(getActivity());
        builder
                .setTitle("Delete")
                .setMessage("Are you sure?")
                .setPositiveButton("Delete", (dialogInterface, i) -> {

                    long id = ReportFragment.id;
                    //todo set id 0 actionmode finish
                    if (id != 0) ReportRealmController.delReport(id);

                    for (Fragment fragment
                            : (getActivity()).getSupportFragmentManager().getFragments()) {
                        if (fragment instanceof ReportFragment) {
                            ((ReportFragment) fragment).notifyDataChanged();
                        }
                    }

                    }
                )
                .setNegativeButton("Cancel", ((dialogInterface, i) -> dialogInterface.dismiss()))
                .setOnKeyListener((DialogInterface dialogInterface, int keyCode, KeyEvent event) -> {
                    if (keyCode == KeyEvent.KEYCODE_BACK) {
                        // do nothing
                        return true;
                    }
                    return false;
                });

        return builder.create();
    }



}
