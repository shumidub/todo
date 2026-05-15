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


    public SmallTaskFragmentPagerAdapter(FragmentManager fm) {
        super(fm);
    }


    @Override
    public Fragment getItem(int position) {

        ArrayList<Long> arrayList = new ArrayList<>();

//        for (int i = 0; i<App.folderOfTasksListFromContainer.size(); i++){
//            arrayList.add(App.folderOfTasksListFromContainer.get(i).getId());
//        }
//
//        Log.d("DTAG2425", "folderIdArray = : " + arrayList.toString());

        long id = App.folderOfTasksListFromContainer.get(position).getId();
//
//        Log.d("DTAG2425", "getItem: folderID = " + id);
//        Log.d("DTAG2425", " ");

        return SmallTasksFragment.newInstance (id);
    }


    @Override
    public int getItemPosition(Object object) {
        return super.getItemPosition(object);

    }

    @Override
    public int getCount() {
        int size = App.folderOfTasksListFromContainer.size();
        return size;
    }


    @Override
    public void notifyDataSetChanged() {
        super.notifyDataSetChanged();
    }
}
