package com.shumidub.todoapprealm.ui.dialog.section_dialog;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;
import com.shumidub.todoapprealm.R;
import com.shumidub.todoapprealm.realmcontrollers.taskcontroller.SectionsRealmController;
import com.shumidub.todoapprealm.realmmodel.task.SectionObject;
import com.shumidub.todoapprealm.ui.activity.main.MainActivity;

/**
 * Section create/edit dialog (task-002, R10/R11/R14).
 *
 * <p>Modes:
 * <ul>
 *   <li>{@code create} — fields blank; positive button = "Add"; no Delete.</li>
 *   <li>{@code edit} — fields populated from existing {@link SectionObject};
 *       positive button = "Save"; "Delete" is a neutral button.</li>
 * </ul>
 *
 * <p>Notifies host via {@link androidx.fragment.app.FragmentResultListener} on result key
 * {@link #RESULT_KEY}.
 */
public class SectionEditDialog extends DialogFragment {

    private static final String ARG_MODE = "mode";
    private static final String ARG_FOLDER_ID = "folderId";
    private static final String ARG_SECTION_ID = "sectionId";

    private static final String MODE_CREATE = "create";
    private static final String MODE_EDIT = "edit";

    public static final String RESULT_KEY = "section_changed";

    public static SectionEditDialog forCreate(long folderId) {
        SectionEditDialog d = new SectionEditDialog();
        Bundle b = new Bundle();
        b.putString(ARG_MODE, MODE_CREATE);
        b.putLong(ARG_FOLDER_ID, folderId);
        d.setArguments(b);
        return d;
    }

    public static SectionEditDialog forEdit(SectionObject s) {
        SectionEditDialog d = new SectionEditDialog();
        Bundle b = new Bundle();
        b.putString(ARG_MODE, MODE_EDIT);
        b.putLong(ARG_FOLDER_ID, s.getParentFolderId());
        b.putLong(ARG_SECTION_ID, s.getId());
        d.setArguments(b);
        return d;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Bundle args = getArguments() != null ? getArguments() : new Bundle();
        final String mode = args.getString(ARG_MODE, MODE_CREATE);
        final long folderId = args.getLong(ARG_FOLDER_ID, 0);
        final long sectionId = args.getLong(ARG_SECTION_ID, 0);

        MainActivity activity = (MainActivity) getActivity();
        View view = LayoutInflater.from(activity.dialogContext())
                .inflate(R.layout.dialog_section_edit, null);
        final TextInputLayout nameLayout = view.findViewById(R.id.section_name_layout);
        final EditText nameEt = view.findViewById(R.id.section_name);
        final SwitchCompat collapsedSw = view.findViewById(R.id.section_collapsed_default);

        SectionObject existing = null;
        if (MODE_EDIT.equals(mode)) {
            existing = SectionsRealmController.getSection(sectionId);
            if (existing != null && existing.isValid()) {
                nameEt.setText(existing.getName());
                nameEt.setSelection(nameEt.getText().length());
                collapsedSw.setChecked(existing.isCollapsedByDefault());
            }
        }

        final SectionObject sectionForEdit = existing;
        AlertDialog.Builder builder = activity.dialogBuilder()
                .setTitle(MODE_CREATE.equals(mode) ? R.string.add_section : R.string.section_name_hint)
                .setView(view)
                .setPositiveButton(MODE_CREATE.equals(mode) ? "Add" : "Save", null) // override later
                .setNegativeButton("Cancel", (d, w) -> d.cancel());

        if (MODE_EDIT.equals(mode)) {
            builder.setNeutralButton("Delete", null); // override later for confirm
        }

        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);

        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String n = nameEt.getText() == null ? "" : nameEt.getText().toString().trim();
                if (n.isEmpty() || n.length() > 40) {
                    nameLayout.setError("1..40 chars required");
                    return;
                }
                nameLayout.setError(null);
                try {
                    if (MODE_CREATE.equals(mode)) {
                        int pos = SectionsRealmController.nextOuterPosition(folderId);
                        SectionsRealmController.addSection(folderId, n, collapsedSw.isChecked(), pos);
                    } else if (sectionForEdit != null && sectionForEdit.isValid()) {
                        SectionsRealmController.editSection(sectionForEdit, n, collapsedSw.isChecked());
                    }
                } catch (IllegalArgumentException ex) {
                    nameLayout.setError(ex.getMessage());
                    return;
                }
                notifyHost();
                dialog.dismiss();
            });

            if (MODE_EDIT.equals(mode)) {
                dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
                    new MaterialAlertDialogBuilder(activity.dialogContext())
                            .setMessage(R.string.delete_section_confirm)
                            .setNegativeButton("Cancel", (di, w) -> di.cancel())
                            .setPositiveButton("Delete", (di, w) -> {
                                if (sectionForEdit != null && sectionForEdit.isValid()) {
                                    SectionsRealmController.deleteSection(sectionForEdit);
                                }
                                notifyHost();
                                dialog.dismiss();
                            })
                            .show();
                });
            }
        });

        return dialog;
    }

    private void notifyHost() {
        Bundle r = new Bundle();
        r.putBoolean("changed", true);
        if (getActivity() != null) {
            getActivity().getSupportFragmentManager()
                    .setFragmentResult(RESULT_KEY, r);
        }
    }
}
