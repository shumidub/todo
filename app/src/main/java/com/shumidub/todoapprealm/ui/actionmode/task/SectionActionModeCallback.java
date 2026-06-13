package com.shumidub.todoapprealm.ui.actionmode.task;

import androidx.appcompat.view.ActionMode;

import com.shumidub.todoapprealm.R;
import com.shumidub.todoapprealm.realmcontrollers.taskcontroller.SectionsRealmController;
import com.shumidub.todoapprealm.realmmodel.task.SectionObject;
import com.shumidub.todoapprealm.ui.actionmode.EditDeleteActionModeCallback;
import com.shumidub.todoapprealm.ui.activity.main.MainActivity;
import com.shumidub.todoapprealm.ui.dialog.section_dialog.SectionEditDialog;
import com.shumidub.todoapprealm.ui.fragment.task_section.small_tasks_fragment.SmallTasksFragment;

/**
 * sprint-002 polish: ActionMode shown when a section header is long-pressed.
 *
 * <p>Replaces the previous direct-open of {@link SectionEditDialog} on long-press, which
 * conflicted with the long-press gesture used to start drag-and-drop. The user now picks
 * Edit (opens the dialog) or Delete (confirmation handled inline) from the contextual bar.
 */
public class SectionActionModeCallback extends EditDeleteActionModeCallback {

    private final SmallTasksFragment smallTasksFragment;
    private final long sectionId;

    public SectionActionModeCallback(MainActivity activity, SmallTasksFragment smallTasksFragment,
                                     long sectionId) {
        super(activity, titleOf(sectionId));
        this.smallTasksFragment = smallTasksFragment;
        this.sectionId = sectionId;
    }

    private static CharSequence titleOf(long sectionId) {
        SectionObject s = SectionsRealmController.getSection(sectionId);
        return (s != null && s.isValid()) ? s.getName() : null;
    }

    @Override
    protected CharSequence editLabel() {
        return activity.getString(R.string.action_edit_section);
    }

    @Override
    protected CharSequence deleteLabel() {
        return activity.getString(R.string.action_delete_section);
    }

    @Override
    protected void onEditClicked(ActionMode actionMode) {
        SectionObject section = SectionsRealmController.getSection(sectionId);
        if (section != null && section.isValid()) {
            SectionEditDialog.forEdit(section)
                    .show(activity.getSupportFragmentManager(), "editsection");
        }
        actionMode.finish();
    }

    @Override
    protected void onDeleteClicked(ActionMode actionMode) {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(activity.dialogContext())
                .setMessage(R.string.delete_section_confirm)
                .setNegativeButton("Cancel", (di, w) -> di.cancel())
                .setPositiveButton("Delete", (di, w) -> {
                    SectionObject section = SectionsRealmController.getSection(sectionId);
                    if (section != null && section.isValid()) {
                        SectionsRealmController.deleteSection(section);
                    }
                    smallTasksFragment.setTasksAndNotifyDataSetChanged();
                    activity.invalidateOptionsMenu();
                    activity.showToast("Done");
                    actionMode.finish();
                })
                .show();
    }
}
