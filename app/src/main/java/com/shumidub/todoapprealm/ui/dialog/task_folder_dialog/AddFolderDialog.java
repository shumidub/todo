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

import com.shumidub.todoapprealm.R;
import com.shumidub.todoapprealm.realmcontrollers.taskcontroller.FolderTaskRealmController;
import com.shumidub.todoapprealm.ui.activity.main.MainActivity;
import com.shumidub.todoapprealm.ui.fragment.task_section.folder_panel_sliding_fragment.fragment.FolderSlidingPanelFragment;

import io.reactivex.annotations.NonNull;

/**
 * Created by Артем on 24.12.2017.
 */

public class AddFolderDialog extends androidx.fragment.app.DialogFragment {

    EditText etName;
    CheckBox cbIsDaily;
    MainActivity activity;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        View view = getActivity().getLayoutInflater().inflate(R.layout.dialog_add_folder_layout, null);
        etName = view.findViewById(R.id.name);
        cbIsDaily = view.findViewById(R.id.checkboxIsDaily);

        AlertDialog.Builder builder = new MaterialAlertDialogBuilder(getActivity());
        builder.setTitle("Add new folder ")
                .setView(view)
//              .setIcon(R.drawable.ic_launcher_cat)
                .setPositiveButton("Add", (dialogInterface, i) -> {
                        String text = ((EditText) getDialog().findViewById(R.id.name)).getText().toString();
                        if (!text.isEmpty()){
                            long idFolder = FolderTaskRealmController.addFolder(text, cbIsDaily.isChecked());
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
