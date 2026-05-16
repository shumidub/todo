package com.shumidub.todoapprealm.ui.dialog.task_folder_dialog;

import androidx.appcompat.app.AlertDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.EditText;

import com.shumidub.todoapprealm.R;
import com.shumidub.todoapprealm.realmcontrollers.taskcontroller.FolderTaskRealmController;
import com.shumidub.todoapprealm.realmmodel.task.FolderTaskObject;

import com.shumidub.todoapprealm.ui.activity.main.MainActivity;
import com.shumidub.todoapprealm.ui.fragment.task_section.folder_panel_sliding_fragment.fragment.FolderSlidingPanelFragment;

import io.reactivex.annotations.NonNull;

/**
 * Created by Артем on 24.12.2017.
 */

public class EditDelFolderDialog extends androidx.fragment.app.DialogFragment{

    public static String ID_FOLDER = "idFolder";
    public static String MODE_LIST = "ModeList";
    public static String EDIT_LIST = "Edit ";
    public static String DELETE_LIST = "Delete ";
    long idFolder;
    String title;
    FolderTaskObject folderObject;
    String currentTextList;
    EditText etName;
    CheckBox cbIsDaily;
    CheckBox cbTasks2;
    long defaultFolderId;
    MainActivity activity;
    static FolderSlidingPanelFragment folderSlidingPanelFragment;

    public static EditDelFolderDialog newInstance(long idList, String mode, FolderSlidingPanelFragment fragment){
        folderSlidingPanelFragment = fragment;
        EditDelFolderDialog dialog = new EditDelFolderDialog();
        Bundle arg = new Bundle();
        arg.putLong(ID_FOLDER, idList);
        arg.putString(MODE_LIST, mode);
        dialog.setArguments(arg);
        return dialog;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        String textButton = "Edit";

        if(getArguments()!=null){
            idFolder = getArguments().getLong(ID_FOLDER);
            title = getArguments().getString(MODE_LIST);
            if (title == DELETE_LIST) textButton = "DELETE";
            if (title == EDIT_LIST) textButton = "Done";

            folderObject = FolderTaskRealmController.getFolder(idFolder);
            currentTextList = folderObject.getName();
        }

        AlertDialog.Builder builder = ((MainActivity) getActivity()).dialogBuilder();
        if (title == EDIT_LIST ){
            View view = android.view.LayoutInflater.from(((MainActivity) getActivity()).dialogContext())
                    .inflate(R.layout.dialog_add_folder_layout, null);
            etName = view.findViewById(R.id.name);
            cbIsDaily = view.findViewById(R.id.checkboxIsDaily);
            cbTasks2 = view.findViewById(R.id.checkbox_tasks2);
            etName.setText(folderObject.getName());
            cbIsDaily.setChecked(folderObject.isDaily());
            cbTasks2.setVisibility(View.VISIBLE);
            cbTasks2.setChecked(FolderTaskRealmController.getFolderGroup(folderObject) == 1);
            builder.setView(view);
        } else if (title == DELETE_LIST ){
            builder.setMessage("Are you sure?");
        }
        builder.setTitle(title)
//                .setIcon(R.drawable.ic_launcher_cat)
                .setPositiveButton(textButton, (dialog, i) -> {
                    activity = (MainActivity) getActivity();
                    if (title == EDIT_LIST ) {
                        String text = etName.getText().toString();
                        FolderTaskRealmController.editFolder(folderObject, text, cbIsDaily.isChecked());
                        int targetGroup = cbTasks2 != null && cbTasks2.isChecked() ? 1 : 0;
                        FolderTaskRealmController.moveFolderToGroup(folderObject, targetGroup);
                        folderSlidingPanelFragment.finishActionMode();
                        for (com.shumidub.todoapprealm.ui.fragment.task_section.folder_panel_sliding_fragment.fragment.FolderSlidingPanelFragment p
                                : com.shumidub.todoapprealm.App.folderSlidingPanelFragments) {
                            p.notifySmallTasksViewPagerListsChanged();
                        }

                        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(getDialog().getWindow().getDecorView().getWindowToken(), 0);

                        if (activity == null) activity = (MainActivity) getActivity();
                        if (activity.isFinishing()) return;
                        else {
                            activity.showToast("Done");
//                        Toast.makeText(getContext(), "Done", Toast.LENGTH_SHORT).show();
                        }


                    } else if (title == DELETE_LIST){
                        if (folderObject.getId() != defaultFolderId) {
                            FolderTaskRealmController.deleteFolder(folderObject);

//                            Toast.makeText(getContext(), "Deleted", Toast.LENGTH_SHORT).show();
                            folderSlidingPanelFragment.finishActionMode();
                            activity.invalidateOptionsMenu();
                            folderSlidingPanelFragment.notifySmallTasksViewPagerListsChanged();
                            if (activity == null) activity = (MainActivity) getActivity();
                            if (activity.isFinishing()) return;
                            activity.showToast("Deleted");
                        }else{
                            if (activity == null) activity = (MainActivity) getActivity();
                            if (activity.isFinishing()) return;
                            activity.showToast("Can't delete default folderObject");
//                            Toast.makeText(getContext(),
//                                    "Can't delete default folderObject", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(getDialog().getWindow().getDecorView().getWindowToken(), 0);
                        dialog.cancel();
                    }
                });

        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.setOnKeyListener( (dialogInterface, keyCode, event) -> {
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    // do nothing
                    return true;
                }
                return false;
        });

        return dialog;
    }
}
