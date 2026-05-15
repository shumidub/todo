package com.shumidub.todoapprealm.ui.fragment.task_section.folder_panel_sliding_fragment.fragment;

import androidx.appcompat.app.ActionBar;
import android.view.ActionMode;

/**
 * Created by A.shumidub on 15.02.18.
 */

public interface IViewFolderSlidingPanelFragment {


    ActionBar actionBar = null;
    static ActionMode actionMode = null;
    ActionMode.Callback folderCallback = null;
    static final int FOLDER_ACTIONMODE = 2;





}
