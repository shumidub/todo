package com.shumidub.todoapprealm.ui.activity.main;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.shumidub.todoapprealm.App;
import com.shumidub.todoapprealm.R;
import com.shumidub.todoapprealm.realmmodel.notes.NoteObject;
import com.shumidub.todoapprealm.realmmodel.report.ReportObject;
import com.shumidub.todoapprealm.realmmodel.task.TaskObject;
import com.shumidub.todoapprealm.ui.actionmode.EmptyActionModeCallback;
import com.shumidub.todoapprealm.ui.fragment.note_fragment.FolderNoteFragment;
import com.shumidub.todoapprealm.ui.fragment.report_section.report_fragment.ReportFragment;
import com.shumidub.todoapprealm.ui.fragment.task_section.folder_panel_sliding_fragment.fragment.FolderSlidingPanelFragment;

import io.realm.RealmObject;


public class MainActivity extends AppCompatActivity {

    public LinearLayout rootLayout;
    CustomViewPager viewPager;
    long time = 0;
    ActionBar actionBar;
    MainPagerAdapter mainPagerAdapter;
    ActionMode actionMode;

    int pagerAdapterPosition = 1;

    MenuItem dayScopeMenu;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        onCreateActions();
    }


    public void onCreateActions(){
        App.setDayScopeValue();

        //todo need fix up view with open keyboard
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);


        setContentView(R.layout.activity_main);

        rootLayout = findViewById(R.id.root_layout);
        actionBar = getSupportActionBar();


        viewPager = findViewById(R.id.viewpager);
        mainPagerAdapter = new MainPagerAdapter(getSupportFragmentManager());
        viewPager.setAdapter(mainPagerAdapter);
        viewPager.setOffscreenPageLimit(5);
        viewPager.setCurrentItem(1);
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {

                pagerAdapterPosition = position;


                if (position==1){
                    actionBar.setDisplayHomeAsUpEnabled(false);
                    actionBar.setTitle(FolderSlidingPanelFragment.getTitle());
                }if (position ==0){

                    /*
                    try use
                    private static String makeFragmentName(int viewId, long id) {
                        return "android:switcher:" + viewId + ":" + id;
                    }
                     */


                    for (Fragment fragment: getSupportFragmentManager ().getFragments()){
                        if (fragment instanceof FolderNoteFragment){
                            if (((FolderNoteFragment) fragment).isNoteFragment){
                                actionBar.setDisplayHomeAsUpEnabled(true);
                            }
                        }
                    }
                }else {
                    actionBar.setDisplayHomeAsUpEnabled(false);
                }
                actionMode = startSupportActionMode(new EmptyActionModeCallback());


                if (position == 0){

                    for (Fragment fragment: getSupportFragmentManager ().getFragments()){
                        if (fragment instanceof FolderNoteFragment){
                            actionBar.setTitle( ((FolderNoteFragment) fragment).getValidTitle() );
                        }
                    }
                }

                else if (position == 1){

                    for (Fragment fragment: getSupportFragmentManager ().getFragments()){
                        if (fragment instanceof FolderSlidingPanelFragment){
                            actionBar.setTitle( ((FolderSlidingPanelFragment) fragment).getValidTitle() );
                        }
                    }
                }

                else if (position == 2){
                    actionBar.setTitle("Reports");
                }


            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });


        actionBar.setTitle("Tasks");
    }


    public void resetAllView(){

        viewPager.removeAllViews();
        mainPagerAdapter = new MainPagerAdapter(getSupportFragmentManager());
        viewPager.setAdapter(mainPagerAdapter);

        mainPagerAdapter = new MainPagerAdapter(getSupportFragmentManager());
        viewPager = null;

    }

    @Override
    protected void onPause() {
        App.closeRealm();
        super.onPause();
    }

    @Override
    protected void onRestart() {
//        /**Проверка на возможность загрузить список при возврате на экран.
//        * Например, после ухода с экрана, категория со списком могла быть удалена и при возврате на
//        * экран и тапе на список - была ошибка.
//        */
//        if (FolderRealmController.getFolder(listId)==null){
//
//            long defaultListId = new SharedPrefHelper(this).getDefaultListId();
//            if (FolderRealmController.getFolder(defaultListId)!=null)  listId = defaultListId;
//            else listId = 0;
//            fragmentManager.beginTransaction().replace(R.id.container,
//                    FolderSlidingPanelFragment.newInstance(listId)).commitAllowingStateLoss();
////          try fix run method before savedInstance outStatte
////          FolderSlidingPanelFragment.newInstance(listId)).commit();
//        }
        super.onRestart();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        dayScopeMenu = menu.add(2,2,2,"" + App.dayScope);
        dayScopeMenu.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        dayScopeMenu.setOnMenuItemClickListener((v)->{

            App.initRealm();

            Log.d("DTAG", "onCreateOptionsMenu: reports =  " + App.realm.where(ReportObject.class).findAll().toString());
            Log.d("DTAG", "onCreateOptionsMenu: notes ="  + App.realm.where(NoteObject.class).findAll().toString());
            Log.d("DTAG", "onCreateOptionsMenu: tasks" + App.realm.where(TaskObject.class).findAll().toString());

            return true;});
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void invalidateOptionsMenu() {
        App.setDayScopeValue();
        if (dayScopeMenu !=null) dayScopeMenu.setTitle("" + App.dayScope);
        super.invalidateOptionsMenu();
    }

    @Override
    public void onBackPressed() {
        int currentFragmentItem = viewPager.getCurrentItem();

        if (currentFragmentItem == 1){
            for (Fragment fragment: getSupportFragmentManager ().getFragments()){
                if (fragment instanceof FolderSlidingPanelFragment){
                    FolderSlidingPanelFragment tasksFragment = (FolderSlidingPanelFragment) fragment;
                    if (tasksFragment.collapseSheet()) {
                        return;
                    } else {
                        onBackPressedWithTimer();
                        return;
                    }
                }
            }
        } else if (currentFragmentItem ==2){
            for (Fragment fragment: getSupportFragmentManager ().getFragments()){
                if (fragment instanceof ReportFragment) {
                    if (((ReportFragment) fragment).actionModeIsEnabled) {
                        ((ReportFragment) fragment).finishActionMode();
                    } else {
                        onBackPressedWithTimer();
                        return;
                    }
                }
            }
        } else if (currentFragmentItem ==0){
            for (Fragment fragment: getSupportFragmentManager ().getFragments()){
                if (fragment instanceof FolderNoteFragment) {
                    if (((FolderNoteFragment) fragment).actionModeIsEnabled) {
                        ((FolderNoteFragment) fragment).finishActionMode();
                    } else if (((FolderNoteFragment) fragment).isNoteFragment) {
                        ((FolderNoteFragment) fragment).setFolderNoteViews();
                    } else {
                        onBackPressedWithTimer();
                        return;
                    }
                }
            }
        } else {
            onBackPressedWithTimer();
        }
    }

    private void onBackPressedWithTimer(){
        if (time!=0 && System.currentTimeMillis() - time<2000) super.onBackPressed();
        else{
            time = System.currentTimeMillis();
            showToast("For exit press again");
// Toast.makeText(this, "For exit press again", Toast.LENGTH_SHORT).show();
            Log.d("DTAG", "onBackPressedWithTimer: ");
        }
    }


    public void setPageCanChangedScrolled(boolean canScrolled){
      viewPager.setPageCanChangedScrolled(canScrolled);
    }


    public int getPixelsFromDPs(int dps){
        Resources r = ((MainActivity)this).getResources();
        int  px = (int) (TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dps, r.getDisplayMetrics()));
        return px;
    }



    public void showToast(String text){
        if (!MainActivity.this.isFinishing()) {
            Toast toast = Toast.makeText(this, text, Toast.LENGTH_SHORT);
//        View view = toast.getView();
//        view.setBackgroundColor(getResources().getColor(R.color.colorAccent));
//        TextView textView = (TextView) view.findViewById(android.R.id.message);
//        textView.setTextColor(getResources().getColor(R.color.colorPrimary));
            toast.show();
        }
    }

    public int getPagerAdapterPosition() {
        return pagerAdapterPosition;
    }
}