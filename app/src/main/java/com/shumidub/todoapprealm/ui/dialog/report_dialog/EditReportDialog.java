package com.shumidub.todoapprealm.ui.dialog.report_dialog;

import android.graphics.Color;
import android.util.Log;
import android.view.View;

import com.shumidub.todoapprealm.realmcontrollers.reportcontroller.ReportRealmController;
import com.shumidub.todoapprealm.realmmodel.report.ReportObject;
import com.shumidub.todoapprealm.ui.fragment.report_section.report_fragment.ReportFragment;

/**
 * Created by A.shumidub on 05.02.18.
 *
 */

public class EditReportDialog extends BaseReportDialog {

    long id;
    ReportObject reportObject;


    @Override
    protected void setView() {
        super.setView();
    }

    @Override
    protected void setPositiveButtonText() {
        positiveButtonText = EDIT_BUTTON_TEXT;
    }

    @Override
    protected void setDialogViews() {
        super.setDialogViews();

        id = ReportFragment.id;

        reportObject = ReportRealmController.getReport(id);

        if (reportObject.isWeekReport()){
            etDate.setText(String.valueOf(reportObject.getWeekNumber()));
            tilCountValue.setHint("Week count");
            tilDate.setHint("Week number");
        } else{
            etDate.setText(reportObject.getDate());
            tilCountValue.setHint("Day count");
            tilDate.setHint("Date");
        }

        etCountValue.setText(String.valueOf(reportObject.getCountOfDay()));
        etTextReport.setText(reportObject.getReportText());
        rbHealth.setRating(reportObject.getHealthRating());
        rbSoul.setRating(reportObject.getSoulRating());

        ratingBarPhinance.setRating(reportObject.getPhinanceRating());
        ratingBarEnglish.setRating(reportObject.getEnglishRating());
        ratingBarSocial.setRating(reportObject.getSocialRating());
        ratingBarFamilly.setRating(reportObject.getFamillyRating());




        llSwitchWeekContainer.setVisibility(View.GONE);

        if (reportObject.isWeekReport()){
            if (currentWeekNumber != reportObject.getWeekNumber()){
                Log.d("DTAG", "setDialogViews: " + currentWeekNumber + " " + reportObject.getWeekNumber());
                etDate.setEnabled(false);
                etDate.setCursorVisible(false);
                etDate.setTextColor(Color.BLACK);
                etDate.setKeyListener(null);
            }
        }


    }

    @Override
    protected boolean isWeekMode() {
        return reportObject != null && reportObject.isWeekReport();
    }

    @Override
    protected void setPositiveButtonInterface() {
        positiveButtonInterface = (v)-> {
            if (!validateFormAndShowErrors()) return;

            ReportForm f = collectForm();
            ReportRealmController.editReport(id, f.date, f.dayCount, f.textReport,
                    f.soulRating, f.healthRating, f.phinanceRating,
                    f.englishRating, f.socialRating, f.famillyRating,
                    f.weekNumber);
            finishAfterSave();
        };
    }

    @Override
    public void onStart() {
        super.onStart();
        etTextReport.requestFocus();
        if (id == 0) dismiss();
    }
}
