package com.shumidub.todoapprealm.ui.dialog.report_dialog;

import android.widget.CompoundButton;

import com.shumidub.todoapprealm.realmcontrollers.reportcontroller.ReportRealmController;
import com.shumidub.todoapprealm.ui.fragment.task_section.folder_panel_sliding_fragment.fragment.FolderSlidingPanelFragment;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by A.shumidub on 05.02.18.
 *
 */

public class AddReportDialog extends BaseReportDialog {

    @Override
    protected void setView() {
        super.setView();
    }

    @Override
    protected void setPositiveButtonText() {
        positiveButtonText = ADD_BUTTON_TEXT;
    }

    @Override
    protected void setDialogViews() {
        super.setDialogViews();
        String defaultDate = new SimpleDateFormat("dd.MM.yyyy").format(new Date(System.currentTimeMillis()));
        int defaultCount = FolderSlidingPanelFragment.getDayScopeValue();

        etDate.setText(defaultDate);
        etCountValue.setText("" + defaultCount);


        switchWeek.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b){
                    etDate.setText(String.valueOf(currentWeekNumber));
                    tilCountValue.setHint("Week count");
                    tilDate.setHint("Week number");
                }
                else{
                    etDate.setText(defaultDate);
                    tilCountValue.setHint("Day count");
                    tilDate.setHint("Date");
                }
            }
        });
            }

    @Override
    protected void setPositiveButtonInterface() {
        positiveButtonInterface = (v)-> {
            if (!validateFormAndShowErrors()) return;

            ReportForm f = collectForm();
            ReportRealmController.addReport(f.date, f.dayCount, f.textReport,
                    f.soulRating, f.healthRating, f.phinanceRating,
                    f.englishRating, f.socialRating, f.famillyRating,
                    isWeekMode(), f.weekNumber);
            finishAfterSave();
        };
    }

    @Override
    public void onStart() {
        super.onStart();
        etTextReport.requestFocus();
    }

}
