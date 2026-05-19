package com.shumidub.todoapprealm.ui.actionmode.task;

import android.view.Menu;
import android.view.MenuItem;

import androidx.appcompat.view.ActionMode;

import com.shumidub.todoapprealm.R;
import com.shumidub.todoapprealm.realmcontrollers.taskcontroller.SectionsRealmController;
import com.shumidub.todoapprealm.realmmodel.task.SectionObject;
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
public class SectionActionModeCallback {

    private MainActivity activity;

    public ActionMode.Callback getCallback(MainActivity activity,
                                           SmallTasksFragment smallTasksFragment,
                                           long sectionId) {
        this.activity = activity;

        return new ActionMode.Callback() {
            @Override
            public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
                SectionObject s = SectionsRealmController.getSection(sectionId);
                if (s != null && s.isValid()) {
                    actionMode.setTitle(s.getName());
                }

                MenuItem editItem = menu.add(R.string.action_edit_section);
                editItem.setIcon(R.drawable.ic_edit);
                editItem.setOnMenuItemClickListener(item -> {
                    SectionObject section = SectionsRealmController.getSection(sectionId);
                    if (section != null && section.isValid()) {
                        SectionEditDialog.forEdit(section)
                                .show(activity.getSupportFragmentManager(), "editsection");
                    }
                    actionMode.finish();
                    return true;
                });

                MenuItem deleteItem = menu.add(R.string.action_delete_section);
                deleteItem.setIcon(R.drawable.ic_del);
                deleteItem.setOnMenuItemClickListener(item -> {
                    new com.google.android.material.dialog.MaterialAlertDialogBuilder(
                            activity.dialogContext())
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
                    return true;
                });

                activity.tintActionModeBarForCurrentTab();
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
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
}
