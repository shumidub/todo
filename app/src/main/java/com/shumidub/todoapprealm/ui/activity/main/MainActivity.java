package com.shumidub.todoapprealm.ui.activity.main;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import java.util.ArrayList;
import java.util.List;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import com.shumidub.todoapprealm.sync.JsonSyncUtil;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
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
import com.shumidub.todoapprealm.ui.fragment.task_section.folder_panel_sliding_fragment.fragment.FolderSlidingPanelFragment;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

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

    private final ActivityResultLauncher<String[]> permissionsLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                // No-op: the app keeps working without these — they only gate export/backup features
                // (legacy external storage) and notification posting on API 33+.
            });

    private final ActivityResultLauncher<String[]> pickBackupLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri == null) return;
                new JsonSyncUtil(this).realmBdFromJsonUri(uri);
            });

    public void pickBackupForRestore() {
        // SAF picker — works on any API level, no permission needed, user-mediated access
        // even across scoped storage and "ownerless" MediaStore entries.
        pickBackupLauncher.launch(new String[]{"application/json", "text/plain", "text/*", "*/*"});
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        onCreateActions();
        requestRuntimePermissions();
    }

    private void requestRuntimePermissions() {
        // Backup/restore JSON to Downloads:
        //   - API >= 30 (R+): MediaStore.Downloads — no runtime permission, system handles access.
        //   - API 29:         WRITE_EXTERNAL_STORAGE still applies for legacy paths.
        //   - API < 29:       READ + WRITE storage required to touch Downloads via File API.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return;
        }
        List<String> needed = new ArrayList<>();
        addIfMissing(needed, Manifest.permission.READ_EXTERNAL_STORAGE);
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
            addIfMissing(needed, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if (!needed.isEmpty()) {
            permissionsLauncher.launch(needed.toArray(new String[0]));
        }
    }

    private void addIfMissing(List<String> bucket, String permission) {
        if (ContextCompat.checkSelfPermission(this, permission)
                != PackageManager.PERMISSION_GRANTED) {
            bucket.add(permission);
        }
    }


    public void onCreateActions(){
        setContentView(R.layout.activity_main);

        // ActionBar + AppCompat already inset for the status bar (and camera cutout) with the
        // default decor fitting; we only need to pad the bottom-most content for gesture nav and IME.
        rootLayout = findViewById(R.id.root_layout);
        ViewCompat.setOnApplyWindowInsetsListener(rootLayout, (v, windowInsets) -> {
            Insets nav = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars());
            Insets ime = windowInsets.getInsets(WindowInsetsCompat.Type.ime());
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(),
                    Math.max(nav.bottom, ime.bottom));
            return windowInsets;
        });

        actionBar = getSupportActionBar();


        viewPager = findViewById(R.id.viewpager);
        mainPagerAdapter = new MainPagerAdapter(getSupportFragmentManager());
        viewPager.setAdapter(mainPagerAdapter);
        viewPager.setOffscreenPageLimit(1);
        viewPager.setCurrentItem(1);

        rootLayout.post(() -> {
            App.setDayScopeValue();
            if (dayScopeMenu != null) dayScopeMenu.setTitle("" + App.dayScope);
        });
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

                else if (position == 1 || position == 2){

                    for (Fragment fragment: getSupportFragmentManager ().getFragments()){
                        if (fragment instanceof FolderSlidingPanelFragment
                                && ((FolderSlidingPanelFragment) fragment).getTaskGroup() == position - 1){
                            actionBar.setTitle( ((FolderSlidingPanelFragment) fragment).getValidTitle() );
                        }
                    }
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

        if (currentFragmentItem == 1 || currentFragmentItem == 2){
            for (Fragment fragment: getSupportFragmentManager ().getFragments()){
                if (fragment instanceof FolderSlidingPanelFragment
                        && ((FolderSlidingPanelFragment) fragment).getTaskGroup() == currentFragmentItem - 1){
                    SlidingUpPanelLayout slidingUpPanelLayout = ((FolderSlidingPanelFragment) fragment).slidingUpPanelLayout;
                    if ( slidingUpPanelLayout.getPanelState()== SlidingUpPanelLayout.PanelState.EXPANDED){
                        slidingUpPanelLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
                        return;
                    } else{
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