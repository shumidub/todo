package com.shumidub.todoapprealm.ui.dialog.task_folder_dialog;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.shumidub.todoapprealm.R;

/**
 * Shared helpers for the green/blue/yellow tab-color picker used by
 * {@link AddFolderDialog} and {@link EditDelFolderDialog}.
 *
 * Maps between the toggle-group's checked button id and the integer
 * folder group (0 = green, 1 = blue, 2 = yellow). Green is the
 * defensive fallback when nothing is selected or the group is unknown.
 */
final class TabColorPickerHelper {

    private TabColorPickerHelper() {}

    /** Returns the folder group (0/1/2) for the currently checked button; green (0) as fallback. */
    static int resolveSelectedGroup(MaterialButtonToggleGroup g) {
        if (g == null) return 0;
        int checkedId = g.getCheckedButtonId();
        if (checkedId == R.id.tabColorBlue) return 1;
        if (checkedId == R.id.tabColorYellow) return 2;
        return 0; // green / fallback
    }

    /** Checks the toggle button matching the supplied folder group; green for unknown/-1. */
    static void setCheckedByGroup(MaterialButtonToggleGroup g, int group) {
        if (g == null) return;
        int id = R.id.tabColorGreen;
        if (group == 1) id = R.id.tabColorBlue;
        else if (group == 2) id = R.id.tabColorYellow;
        g.check(id);
    }
}
