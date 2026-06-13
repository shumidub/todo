package com.shumidub.todoapprealm.ui.dialog.report_dialog;

import androidx.appcompat.app.AlertDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import com.google.android.material.textfield.TextInputLayout;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.Switch;
import android.widget.TextView;

import com.shumidub.todoapprealm.R;
import com.shumidub.todoapprealm.ui.activity.main.MainActivity;
import com.shumidub.todoapprealm.ui.fragment.report_section.report_fragment.ReportFragment;

import java.util.Calendar;
import java.util.List;

import io.reactivex.annotations.NonNull;

/**
 * Created by A.shumidub on 05.02.18.
 *
 */

public class BaseReportDialog extends androidx.fragment.app.DialogFragment {

    public static final String ADD_REPORT_TITLE = "Add new report";
    public static final String EDIT_REPORT_TITLE = "Edit report";
    public static final String DELETE_REPORT_TITLE = "Delete report";

    public static final String ADD_BUTTON_TEXT = "ADD";
    public static final String EDIT_BUTTON_TEXT = "EDIT";

    protected MainActivity activity;
    protected EditText etDate;
    protected EditText etCountValue;
    protected EditText etTextReport;


    protected RatingBar rbHealth;
    protected RatingBar rbSoul;
    protected RatingBar ratingBarPhinance;
    protected RatingBar ratingBarEnglish;
    protected RatingBar ratingBarSocial;
    protected RatingBar ratingBarFamilly;

    protected Switch switchWeek;
    protected LinearLayout llSwitchWeekContainer;

    protected TextInputLayout tilDate;
    protected TextInputLayout tilCountValue;

    TextView cancelButton;
    TextView actionButton;

    String title;
    View view;
    String positiveButtonText;
    PositiveButtonInterface positiveButtonInterface;

    int currentWeekNumber;
    Calendar calendar;


    AlertDialog dialog;

    interface PositiveButtonInterface {
        void onClick(View view);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        currentWeekNumber = calendar.get(Calendar.WEEK_OF_YEAR);

        activity = (MainActivity) getActivity();
        setTitle();
        setView();
        setPositiveButtonText();
        setPositiveButtonInterface();
        setDialogViews();




        AlertDialog.Builder builder = new MaterialAlertDialogBuilder(getActivity());
        builder .setView(view);

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

    private void setTitle() {
        title = "title";
    }

    protected void setView() {
        view = getActivity().getLayoutInflater().inflate(R.layout.report_dialog_add_edit_show_fully_size, null);
    }

    protected void setPositiveButtonText() {
        positiveButtonText = "positiveButtonText";
    }

    protected void setPositiveButtonInterface() {
        positiveButtonInterface = (v) -> Log.d("DTAG", "setPositiveButtonInterface");
    }

    protected void setDialogViews() {
        etDate = view.findViewById(R.id.tv_date);
        etCountValue = view.findViewById(R.id.tv_count_value);
        etTextReport = view.findViewById(R.id.tv_report_text);

        rbHealth = view.findViewById(R.id.ratingbar_health);
        rbSoul = view.findViewById(R.id.ratingbar_soul);
        ratingBarPhinance = view.findViewById(R.id.ratingbar_phinance);
        ratingBarEnglish = view.findViewById(R.id.ratingbar_english);
        ratingBarSocial = view.findViewById(R.id.ratingbar_social);
        ratingBarFamilly = view.findViewById(R.id.ratingbar_familly);

        tilDate = view.findViewById(R.id.til_date);
        tilCountValue = view.findViewById(R.id.til_count_value);
        switchWeek = view.findViewById(R.id.switch_week);
        llSwitchWeekContainer = view.findViewById(R.id.ll_week_switc_container);

        cancelButton = view.findViewById(R.id.btn_cancel);
        actionButton = view.findViewById(R.id.btn_action);

        cancelButton.setOnClickListener((v) -> {
            InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(getDialog().getWindow().getDecorView().getWindowToken(), 0);
            dialog.cancel();
        });
        actionButton.setOnClickListener((v)->  positiveButtonInterface.onClick(v));
        actionButton.setText(positiveButtonText);

    }

    protected void notifyDataChanged() {
        List<androidx.fragment.app.Fragment> fragments
                = (getActivity()).getSupportFragmentManager().getFragments();

        for (androidx.fragment.app.Fragment fragment : fragments) {
            if (fragment instanceof ReportFragment) {
                ((ReportFragment) fragment).notifyDataChanged();
            }
        }
    }

    protected void setDateError(String errorText, boolean errorEnable){
        tilDate.setErrorEnabled(errorEnable);
        tilDate.setError(errorText);
    }

    protected void setCountValueError(String errorText, boolean errorEnable){
        tilCountValue.setErrorEnabled(errorEnable);
        tilCountValue.setError(errorText);
    }

    // ---------- shared form validation / collection (Add + Edit) ----------

    /** Whether the form is in week-report mode. Add: the switch; Edit: the stored flag. */
    protected boolean isWeekMode() {
        return switchWeek != null && switchWeek.isChecked();
    }

    private static Integer parseIntOrNull(String s) {
        try {
            return Integer.valueOf(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Validates the form, showing/clearing the field errors.
     * Week mode accepts only the current or previous week number.
     * @return true when the report can be saved.
     */
    protected boolean validateFormAndShowErrors() {
        String dateText = etDate.getText().toString();
        String countText = etCountValue.getText().toString();
        boolean isWeek = isWeekMode();

        Integer count = parseIntOrNull(countText);
        Integer weekNumber = parseIntOrNull(dateText);

        boolean countOk = count != null && count < 500;
        boolean weekOk = weekNumber != null
                && (weekNumber == currentWeekNumber || weekNumber == currentWeekNumber - 1);
        boolean dateOk = isWeek ? weekOk : dateText.length() == 10;

        if (dateText.isEmpty()) {
            setDateError("Should be filled", true);
        } else if (!dateOk) {
            setDateError(isWeek ? "Not valid week number" : "Not valid date", true);
        } else {
            setDateError("", false);
        }

        if (countText.isEmpty()) {
            setCountValueError("Should be filled", true);
        } else if (!countOk) {
            setCountValueError("Count value too match", true);
        } else {
            setCountValueError("", false);
        }

        return !dateText.isEmpty() && dateOk && countOk;
    }

    /** Snapshot of the validated form values, ready for the controller call. */
    protected static final class ReportForm {
        public final String date;
        public final int dayCount;
        public final String textReport;
        public final int soulRating, healthRating, phinanceRating;
        public final int englishRating, socialRating, famillyRating;
        public final int weekNumber;

        ReportForm(String date, int dayCount, String textReport,
                   int soulRating, int healthRating, int phinanceRating,
                   int englishRating, int socialRating, int famillyRating, int weekNumber) {
            this.date = date;
            this.dayCount = dayCount;
            this.textReport = textReport;
            this.soulRating = soulRating;
            this.healthRating = healthRating;
            this.phinanceRating = phinanceRating;
            this.englishRating = englishRating;
            this.socialRating = socialRating;
            this.famillyRating = famillyRating;
            this.weekNumber = weekNumber;
        }
    }

    /** Collect form values. Call only after {@link #validateFormAndShowErrors()} passed. */
    protected ReportForm collectForm() {
        String date;
        int weekNumber;
        if (isWeekMode()) {
            weekNumber = Integer.parseInt(etDate.getText().toString().trim());
            date = calendar.get(Calendar.DATE) + "." + calendar.get(Calendar.MONTH) + "."
                    + calendar.get(Calendar.YEAR);
        } else {
            date = etDate.getText().toString();
            weekNumber = currentWeekNumber;
        }
        return new ReportForm(date,
                Integer.parseInt(etCountValue.getText().toString().trim()),
                etTextReport.getText().toString(),
                rbSoul.getProgress(), rbHealth.getProgress(), ratingBarPhinance.getProgress(),
                ratingBarEnglish.getProgress(), ratingBarSocial.getProgress(),
                ratingBarFamilly.getProgress(), weekNumber);
    }

    /** Refresh the report list, hide the keyboard and close the dialog. */
    protected void finishAfterSave() {
        notifyDataChanged();
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(getDialog().getWindow().getDecorView().getWindowToken(), 0);
        dismiss();
    }
}