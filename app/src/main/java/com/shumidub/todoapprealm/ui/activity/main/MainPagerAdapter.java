package com.shumidub.todoapprealm.ui.activity.main;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import com.shumidub.todoapprealm.ui.fragment.note_fragment.FolderNoteFragment;
import com.shumidub.todoapprealm.ui.fragment.report_section.report_fragment.ReportFragment;
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
        return 3;
    }

    @Override
    public Fragment getItem(int position) {
        if (position == 1){
            return new FolderSlidingPanelFragment();
        }else if (position == 2){
            return new ReportFragment();
        } else if (position == 0) {
            return new FolderNoteFragment();
        }
        else {
            return null;
        }
    }

}
