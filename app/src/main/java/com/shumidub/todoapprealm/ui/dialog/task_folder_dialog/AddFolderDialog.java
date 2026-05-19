package com.shumidub.todoapprealm.ui.dialog.task_folder_dialog;

import androidx.appcompat.app.AlertDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.EditText;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.shumidub.todoapprealm.R;
import com.shumidub.todoapprealm.realmcontrollers.taskcontroller.FolderTaskRealmController;
import com.shumidub.todoapprealm.ui.activity.main.MainActivity;
import com.shumidub.todoapprealm.ui.fragment.task_section.folder_panel_sliding_fragment.fragment.FolderSlidingPanelFragment;

import io.reactivex.annotations.NonNull;

/**
 * Created by Артем on 24.12.2017.
 */

public class AddFolderDialog extends androidx.fragment.app.DialogFragment {

    private static final String ARG_TASK_GROUP = "task_group";

    EditText etName;
    CheckBox cbIsDaily;
    MaterialButtonToggleGroup tabColorToggleGroup;
    MainActivity activity;

    public static AddFolderDialog newInstance(int taskGroup) {
        AddFolderDialog d = new AddFolderDialog();
        Bundle args = new Bundle();
        args.putInt(ARG_TASK_GROUP, taskGroup);
        d.setArguments(args);
        return d;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        View view = android.view.LayoutInflater.from(((MainActivity) getActivity()).dialogContext())
                .inflate(R.layout.dialog_add_folder_layout, null);
        etName = view.findViewById(R.id.name);
        cbIsDaily = view.findViewById(R.id.checkboxIsDaily);
        tabColorToggleGroup = view.findViewById(R.id.tabColorToggleGroup);
        int initialGroup = getArguments() == null ? 0 : getArguments().getInt(ARG_TASK_GROUP, 0);
        TabColorPickerHelper.setCheckedByGroup(tabColorToggleGroup, initialGroup);

        AlertDialog.Builder builder = ((MainActivity) getActivity()).dialogBuilder();
        builder.setTitle("Add new folder ")
                .setView(view)
//              .setIcon(R.drawable.ic_launcher_cat)
                .setPositiveButton("Add", (dialogInterface, i) -> {
                        String text = ((EditText) getDialog().findViewById(R.id.name)).getText().toString();
                        if (!text.isEmpty()){
                            int group = TabColorPickerHelper.resolveSelectedGroup(tabColorToggleGroup);
                            long idFolder = FolderTaskRealmController.addFolder(text, cbIsDaily.isChecked(), group);
//                            Toast.makeText(getContext(),"Done", Toast.LENGTH_SHORT).show();
                            activity = (MainActivity) getActivity();
                            activity.showToast("Done");
                            for (Fragment fragment : activity.getSupportFragmentManager().getFragments()){
                                if (fragment instanceof FolderSlidingPanelFragment){
                                    ((FolderSlidingPanelFragment) fragment).notifySmallTasksViewPagerListsChanged();
                                }
                            }
                        }

                    InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(getDialog().getWindow().getDecorView().getWindowToken(), 0);
//                    imm.hideSoftInputFromWindow(getActivity().getWindow().getDecorView().getWindowToken(), 0);
//                    dialogInterface.dismiss();
//                    dialogInterface.cancel();



                })
                .setNegativeButton("Cancel", (dialog, i) ->  {
                    InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(getDialog().getWindow().getDecorView().getWindowToken(), 0);
                    dialog.cancel();
                });

        AlertDialog dialog = builder.create();


        dialog.setCanceledOnTouchOutside(false);
        dialog.setOnKeyListener((DialogInterface dialogInterface, int keyCode,KeyEvent event) -> {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                // do nothing
                return true;
            }
            return false;
        });


        return dialog;
    }

}
