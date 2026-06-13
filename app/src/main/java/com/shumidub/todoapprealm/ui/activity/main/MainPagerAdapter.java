package com.shumidub.todoapprealm.ui.activity.main;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import com.shumidub.todoapprealm.Tabs;
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
        return Tabs.PAGE_COUNT;
    }

    @Override
    public Fragment getItem(int position) {
        int group = Tabs.groupForPosition(position);
        if (group == -1) return new FolderNoteFragment();
        return FolderSlidingPanelFragment.newInstance(group);
    }

}
