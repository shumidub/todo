package com.shumidub.todoapprealm.ui.actionmode.note;



import android.content.Context;
import androidx.appcompat.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.inputmethod.InputMethodManager;

import com.shumidub.todoapprealm.R;
import com.shumidub.todoapprealm.ui.activity.main.MainActivity;
import com.shumidub.todoapprealm.ui.dialog.note_dialog.DellNoteDialog;
import com.shumidub.todoapprealm.ui.dialog.note_dialog.EditNoteDialog;
import com.shumidub.todoapprealm.ui.fragment.note_fragment.FolderNoteFragment;
import com.shumidub.todoapprealm.ui.fragment.task_section.folder_panel_sliding_fragment.fragment.FolderSlidingPanelFragment;


/**
 * Created by Артем on 03.01.2018.
 *
 */

public class FolderNoteActionModeCallback {

    ActionMode.Callback mCallback;
    FolderSlidingPanelFragment folderSlidingPanelFragment;
    MainActivity activity;

    interface onMenuItemClick{
        void onEditMenuItemClick();
        void onDeleteMenuItemClick();
    }

    onMenuItemClick onMenuItemClick;


    public ActionMode.Callback getFolderNoteActionMode(MainActivity activity, FolderNoteFragment folderNoteFragment, int type, long id){

        return new ActionMode.Callback() {
            @Override
            public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {

                folderNoteFragment.actionModeIsEnabled = true;


                MenuItem editList = menu.add("edit ");
                editList.setIcon(R.drawable.ic_edit);
                editList.setOnMenuItemClickListener((MenuItem a) -> {
                    EditNoteDialog editNoteDialog = EditNoteDialog.newInstance(type, id);
                    editNoteDialog.show(((MainActivity) activity).getSupportFragmentManager(), "edit_note_folder");
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
                    DellNoteDialog dellNoteDialog = DellNoteDialog.newInstance(type, id);
                    dellNoteDialog.show(((MainActivity) activity).getSupportFragmentManager(), "dell_note_folder");
                    actionMode.finish();
                    return true;

                });
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
                actionMode.setTitle("Note");
                return false;
            }

            @Override
            public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
                return false;
            }

            @Override
            public void onDestroyActionMode(ActionMode actionMode) {

                folderNoteFragment.actionModeIsEnabled = false;

            }
        };
    }



    public void setFolderNoteActionMode(){
        onMenuItemClick = new onMenuItemClick() {
            @Override
            public void onEditMenuItemClick() {

            }

            @Override
            public void onDeleteMenuItemClick() {

            }
        };
    }






}
