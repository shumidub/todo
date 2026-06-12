package com.shumidub.todoapprealm.ui.actionmode.report;

import androidx.appcompat.view.ActionMode;

import com.shumidub.todoapprealm.ui.actionmode.EditDeleteActionModeCallback;
import com.shumidub.todoapprealm.ui.activity.main.MainActivity;
import com.shumidub.todoapprealm.ui.dialog.report_dialog.DellReportDialog;
import com.shumidub.todoapprealm.ui.dialog.report_dialog.EditReportDialog;

public class ReportActionModeCallback extends EditDeleteActionModeCallback {

    public ReportActionModeCallback(MainActivity activity) {
        super(activity, "Report");
    }

    @Override
    protected void onEditClicked(ActionMode actionMode) {
        new EditReportDialog().show(activity.getSupportFragmentManager(), EditReportDialog.EDIT_REPORT_TITLE);
        showKeyboard();
        actionMode.finish();
    }

    @Override
    protected void onDeleteClicked(ActionMode actionMode) {
        new DellReportDialog().show(activity.getSupportFragmentManager(), "DELL");
        actionMode.finish();
    }
}
