package com.shumidub.todoapprealm.ui.fragment.task_section.small_tasks_fragment;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.fragment.app.FragmentStatePagerAdapter;
import android.util.Log;

import com.shumidub.todoapprealm.App;
import com.shumidub.todoapprealm.realmcontrollers.taskcontroller.FolderTaskRealmController;
import com.shumidub.todoapprealm.realmmodel.task.FolderTaskObject;

import java.util.ArrayList;

import io.realm.RealmList;

/**
 * Created by Артем on 16.01.2018.
 */

public class SmallTaskFragmentPagerAdapter extends FragmentStatePagerAdapter {

    private final int taskGroup;

    public SmallTaskFragmentPagerAdapter(FragmentManager fm) {
        this(fm, 0);
    }

    public SmallTaskFragmentPagerAdapter(FragmentManager fm, int taskGroup) {
        super(fm);
        this.taskGroup = taskGroup;
    }


    @Override
    public Fragment getItem(int position) {
        long id = FolderTaskRealmController.getFoldersList(taskGroup).get(position).getId();
        return SmallTasksFragment.newInstance (id);
    }


    @Override
    public int getItemPosition(Object object) {
        return super.getItemPosition(object);
    }

    @Override
    public int getCount() {
        return FolderTaskRealmController.getFoldersList(taskGroup).size();
    }


    @Override
    public void notifyDataSetChanged() {
        super.notifyDataSetChanged();
    }
}
