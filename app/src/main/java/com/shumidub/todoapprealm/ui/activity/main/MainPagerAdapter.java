package com.shumidub.todoapprealm.ui.activity.main;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import com.shumidub.todoapprealm.ui.fragment.note_fragment.FolderNoteFragment;
import com.shumidub.todoapprealm.ui.fragment.task_section.folder_panel_sliding_fragment.fragment.FolderSlidingPanelFragment;

/**
 * Created by user on 12.01.18.
 *
 */

public class MainPagerAdapter extends FragmentPagerAdapter {

    public MainPagerAdapter(FragmentManager fm) {
        super(fm);
    }



    @Override
    public int getCount() {
        return 4;
    }

    @Override
    public Fragment getItem(int position) {
        if (position == 0) return new FolderNoteFragment();
        if (position == 1) return FolderSlidingPanelFragment.newInstance(0);
        if (position == 2) return FolderSlidingPanelFragment.newInstance(1);
        if (position == 3) return FolderSlidingPanelFragment.newInstance(2);
        return null;
    }

}
