package com.shumidub.todoapprealm.ui.dialog.report_dialog;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import com.shumidub.todoapprealm.realmcontrollers.reportcontroller.ReportRealmController;
import com.shumidub.todoapprealm.realmmodel.report.ReportObject;
import com.shumidub.todoapprealm.ui.fragment.report_section.report_fragment.ReportFragment;

import java.util.Calendar;

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
    protected void setPositiveButtonInterface() {
        positiveButtonInterface = (v)-> {


            //todo need check
            if (!etDate.getText().toString().isEmpty() && !etCountValue.getText().toString().isEmpty()
                    && (Integer.valueOf(etCountValue.getText().toString()) < 500)
                    && ((( !reportObject.isWeekReport() && etDate.getText().toString().length()==10 )
                    || (reportObject.isWeekReport()
                        && ((Integer.valueOf(etDate.getText().toString())== currentWeekNumber
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

                int weekNumber;

                if (reportObject.isWeekReport()){
                    weekNumber = Integer.valueOf(etDate.getText().toString());
                    date = calendar.get(Calendar.DATE) + "." + calendar.get(Calendar.MONTH) + "."
                            + calendar.get(Calendar.YEAR);
                    Log.d("DTAG", "setPositiveButtonInterface: date = " + date);
                }
                else {
                    date = etDate.getText().toString();
                    weekNumber = currentWeekNumber;
                }

                ReportRealmController.editReport(id, date, dayCount, textReport,
                        soulRating, healthRating, phinanceRating,
                        englishRating, socialRating, famillyRating,
                        weekNumber);
                notifyDataChanged();
                InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(getDialog().getWindow().getDecorView().getWindowToken(), 0);
                dismiss();

            } else {
                if (etDate.getText().toString().isEmpty()) {
                    setDateError("Should be filled", true);
                } else if (!reportObject.isWeekReport() && etDate.getText().toString().length()!=10){
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
                if (reportObject.isWeekReport() && !etDate.getText().toString().isEmpty() ){
                    weekNumberValid
                            =  ((Integer.valueOf(etDate.getText().toString())== currentWeekNumber
                            || (Integer.valueOf(etDate.getText().toString())== currentWeekNumber -1)));
                }

                if ( reportObject.isWeekReport() && !etDate.getText().toString().isEmpty()
                        && !weekNumberValid ) {
                    setDateError("Not valid week number", true);
                } else if (reportObject.isWeekReport() && !etDate.getText().toString().isEmpty()
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
        actionButton.setOnClickListener((v)->
                positiveButtonInterface.onClick(v));
        etTextReport.requestFocus();
        if (id == 0) dismiss();
    }
}
