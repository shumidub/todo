package com.shumidub.todoapprealm.ui.actionmode.report;



import android.app.Activity;
import android.content.Context;
import androidx.appcompat.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.inputmethod.InputMethodManager;

import com.shumidub.todoapprealm.R;
import com.shumidub.todoapprealm.ui.activity.main.MainActivity;
import com.shumidub.todoapprealm.ui.dialog.report_dialog.DellReportDialog;
import com.shumidub.todoapprealm.ui.dialog.report_dialog.EditReportDialog;
import com.shumidub.todoapprealm.ui.fragment.task_section.folder_panel_sliding_fragment.fragment.FolderSlidingPanelFragment;


/**
 * Created by Артем on 03.01.2018.
 *
 */

public class ReportActionModeCallback {

    ActionMode.Callback mCallback;
    FolderSlidingPanelFragment folderSlidingPanelFragment;
    Activity activity;


    public ActionMode.Callback getReportActionMode(Activity activity, long id){

        return new ActionMode.Callback() {
            @Override
            public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
                MenuItem editList = menu.add("edit ");
                editList.setIcon(R.drawable.ic_edit);
                editList.setOnMenuItemClickListener((MenuItem a) -> {
                    EditReportDialog dialog = new EditReportDialog();
                    dialog.show(((MainActivity) activity).getSupportFragmentManager(), EditReportDialog.EDIT_REPORT_TITLE);
                    InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.toggleSoftInputFromWindow(
                            activity.getWindow().getDecorView().getApplicationWindowToken(),
                            InputMethodManager.SHOW_FORCED, 0);
                    actionMode.finish();
                    return true;
                });


                MenuItem deleteList = menu.add("delete ");
                deleteList.setIcon(R.drawable.ic_del);
                deleteList.setOnMenuItemClickListener((MenuItem a) -> {
                    new DellReportDialog().show(((MainActivity)activity).getSupportFragmentManager(), "DELL");
                    actionMode.finish();
                    return true;
                });
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
                actionMode.setTitle("Report");
                return false;
            }

            @Override
            public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
                return false;
            }

            @Override
            public void onDestroyActionMode(ActionMode actionMode) {

            }
        };
    }




























    /*

    public ActionMode.Callback getListActionModeCallback(MainActivity activity, FolderSlidingPanelFragment folderSlidingPanelFragment, long idOnTag) {

        this.folderSlidingPanelFragment = folderSlidingPanelFragment;

        mCallback = new ActionMode.Callback() {
            @Override
            public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {


                MenuItem editList = menu.add("edit ");
                editList.setIcon(R.drawable.ic_edit);
                editList.setOnMenuItemClickListener((MenuItem a) -> {

//                    EditDelFolderDialog dialog = EditDelFolderDialog.newInstance(idOnTag, EDIT_LIST, folderSlidingPanelFragment);
//                    dialog.show(activity.getSupportFragmentManager(), "editlist");



                    return true;
                });


                MenuItem deleteList = menu.add("delete ");
                deleteList.setIcon(R.drawable.ic_del);
                deleteList.setOnMenuItemClickListener((MenuItem a) -> {

//
//                    EditDelFolderDialog dialog = EditDelFolderDialog.newInstance(idOnTag, DELETE_LIST, folderSlidingPanelFragment);
//                    dialog.show(activity.getSupportFragmentManager(), "deletelist");
//


                    return true;
                });
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
                actionMode.setTitle("Report");
                return false;
            }

            @Override
            public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
                return false;
            }

            @Override
            public void onDestroyActionMode(ActionMode actionMode) {

            }
        };
        return mCallback;
    }

    */






}
