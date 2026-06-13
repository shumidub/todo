package com.shumidub.todoapprealm.ui.actionmode.note;

import androidx.appcompat.view.ActionMode;

import com.shumidub.todoapprealm.ui.actionmode.EditDeleteActionModeCallback;
import com.shumidub.todoapprealm.ui.activity.main.MainActivity;
import com.shumidub.todoapprealm.ui.dialog.note_dialog.DellNoteDialog;
import com.shumidub.todoapprealm.ui.dialog.note_dialog.EditNoteDialog;
import com.shumidub.todoapprealm.ui.fragment.note_fragment.FolderNoteFragment;

public class FolderNoteActionModeCallback extends EditDeleteActionModeCallback {

    private final FolderNoteFragment fragment;
    private final int type;
    private final long id;

    public FolderNoteActionModeCallback(MainActivity activity, FolderNoteFragment fragment,
                                        int type, long id) {
        super(activity, "Note");
        this.fragment = fragment;
        this.type = type;
        this.id = id;
    }

    @Override
    protected void onShown() {
        fragment.actionModeIsEnabled = true;
    }

    @Override
    protected void onDismissed() {
        fragment.actionModeIsEnabled = false;
    }

    @Override
    protected void onEditClicked(ActionMode actionMode) {
        EditNoteDialog.newInstance(type, id)
                .show(activity.getSupportFragmentManager(), "edit_note_folder");
        showKeyboard();
        actionMode.finish();
    }

    @Override
    protected void onDeleteClicked(ActionMode actionMode) {
        DellNoteDialog.newInstance(type, id)
                .show(activity.getSupportFragmentManager(), "dell_note_folder");
        actionMode.finish();
    }
}
