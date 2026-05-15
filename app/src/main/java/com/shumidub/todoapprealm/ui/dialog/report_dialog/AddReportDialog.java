package com.shumidub.todoapprealm.ui.dialog.report_dialog;

import androidx.appcompat.app.AlertDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import android.content.Context;
import android.os.Build;
import android.util.Log;
import android.view.inputmethod.InputMethodManager;
import android.widget.CompoundButton;

import com.shumidub.todoapprealm.realmcontrollers.reportcontroller.ReportRealmController;
import com.shumidub.todoapprealm.ui.fragment.task_section.folder_panel_sliding_fragment.fragment.FolderSlidingPanelFragment;

import java.text.SimpleDateFormat;
import java.util.Calendar;
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


            //todo need check
            if (!etDate.getText().toString().isEmpty() && !etCountValue.getText().toString().isEmpty()
                    && Integer.valueOf(etCountValue.getText().toString()) < 500
                    && ((( !switchWeek.isChecked() && etDate.getText().toString().length()==10 )
                        || (switchWeek.isChecked()
                            && ( (Integer.valueOf(etDate.getText().toString())== currentWeekNumber
                            || (Integer.valueOf(etDate.getText().toString())== currentWeekNumber -1 ))))))){

                String date;
                int dayCount = Integer.valueOf(etCountValue.getText().toString());
                String textReport = etTextReport.getText().toString();

                int soulRating = rbSoul.getProgress();
                int healthRating = rbHealth.getProgress();
                int phinanceRating = ratingBarPhinance.getProgress();
                int englishRating = ratingBarEnglish.getProgress();
                int socialRating = ratingBarSocial.getProgress();
                int famillyRating = ratingBarFamilly.getProgress();


                boolean isWeekReport = switchWeek.isChecked();
                int weekNumber;

                if (isWeekReport){
                    weekNumber = Integer.valueOf(etDate.getText().toString());
                    date = calendar.get(Calendar.DATE) + "." + calendar.get(Calendar.MONTH) + "."
                            + calendar.get(Calendar.YEAR);
                    Log.d("DTAG", "setPositiveButtonInterface: date = " + date);
                }
                else {
                    date = etDate.getText().toString();
                    weekNumber = currentWeekNumber;
                }

                ReportRealmController.addReport(date, dayCount, textReport,
                        soulRating, healthRating, phinanceRating,
                        englishRating, socialRating, famillyRating,
                        isWeekReport, weekNumber);


                notifyDataChanged();
                InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(getDialog().getWindow().getDecorView().getWindowToken(), 0);
                dismiss();

            } else {
                if (etDate.getText().toString().isEmpty()) {
                    setDateError("Should be filled", true);
                } else if (!switchWeek.isChecked() && etDate.getText().toString().length()!=10){
                    setDateError("Not valid date", true);
                }else {
                    setDateError("", false);
                }
                if (etCountValue.getText().toString().isEmpty()) {
                    setCountValueError("Should be filled", true);
                } else {
                    setCountValueError("", false);
                }

                boolean weekNumberValid = false;
                if (switchWeek.isChecked() && !etDate.getText().toString().isEmpty() ){
                    weekNumberValid
                            =  ((Integer.valueOf(etDate.getText().toString())== currentWeekNumber
                            || (Integer.valueOf(etDate.getText().toString())== currentWeekNumber -1)));
                }


                if ( switchWeek.isChecked() && !etDate.getText().toString().isEmpty()
                        && !weekNumberValid ) {
                    setDateError("Not valid week number", true);
                } else if (switchWeek.isChecked() && !etDate.getText().toString().isEmpty()
                        && weekNumberValid){
                    setDateError("", false);
                }


                if (!(Integer.valueOf(etCountValue.getText().toString()) < 500)){
                    setCountValueError("Count value too match", true);
                } else {
                    setCountValueError("", false);
                }

            }



        };
    }

    @Override
    public void onStart() {
        super.onStart();
        actionButton.setOnClickListener(
                (v)-> positiveButtonInterface.onClick(v));
        etTextReport.requestFocus();
    }

}
