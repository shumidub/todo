package com.shumidub.todoapprealm.ui.actionmode.task;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import androidx.appcompat.app.AlertDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import android.view.ActionMode;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import com.shumidub.todoapprealm.App;
import com.shumidub.todoapprealm.R;
import com.shumidub.todoapprealm.realmcontrollers.taskcontroller.FolderTaskRealmController;
import com.shumidub.todoapprealm.realmcontrollers.taskcontroller.TasksRealmController;
import com.shumidub.todoapprealm.realmmodel.task.FolderTaskObject;
import com.shumidub.todoapprealm.realmmodel.task.TaskObject;
import com.shumidub.todoapprealm.ui.activity.main.MainActivity;
import com.shumidub.todoapprealm.ui.fragment.task_section.folder_panel_sliding_fragment.fragment.FolderSlidingPanelFragment;
import com.shumidub.todoapprealm.ui.fragment.task_section.small_tasks_fragment.SmallTasksFragment;

import java.util.ArrayList;
import java.util.List;

import io.realm.RealmList;


/**
 * Created by Артем on 08.01.2018.
 */

public class TaskActionModeCallback  {

    MainActivity activity;

    ActionMode.Callback callback;
    AlertDialog dialog;
    private String taskText;
    int taskCount;
    int taskPriority;
    boolean taskCycling;

    int defaultColor;
    int accentColor;

    public ActionMode.Callback getCallback(Activity activity, SmallTasksFragment smallTasksFragment, TaskObject task){

        this.activity = (MainActivity) activity;

        callback = new ActionMode.Callback() {
            @Override
            public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {



                actionMode.setTitle(task.getText());

                LayoutInflater inflater = activity.getLayoutInflater();
                View view = inflater.inflate(R.layout.dialog_edit_task, null);

                EditText etEditTask = view.findViewById(R.id.et);
                TextView tvEditTaskCycling = view.findViewById(R.id.task_cycling);
                TextView tvEditTaskPriority = view.findViewById(R.id.task_priority);
                TextView tvEditTaskCountValue = view.findViewById(R.id.task_value);
                TextView tvEditTaskMaxAccumulate = view.findViewById(R.id.task_max_accumulate);
                TextView tvEditTaskEdit = view.findViewById(R.id.tvEdit);
                TextView tvEditTaskCancel = view.findViewById(R.id.tvCancel);

                taskText = task.getText();
                taskCount = task.getCountValue();
                taskPriority = task.getPriority();
                taskCycling = task.isCycling();

                defaultColor = activity.getResources().getColor(R.color.colorWhite);
                accentColor =  activity.getResources().getColor(R.color.colorAccent);

                int tvTaskCyclingColor  = taskCycling ? accentColor : defaultColor;

                etEditTask.setText(taskText);
                tvEditTaskCycling.setTextColor(tvTaskCyclingColor);
                tvEditTaskCountValue.setText("" + taskCount);
                tvEditTaskMaxAccumulate.setText("" + task.getMaxAccumulation());
                setEditTaskValueColor(tvEditTaskCountValue);
                setPriorityTextAndColor(tvEditTaskPriority);

                tvEditTaskCountValue.setOnClickListener((v) -> onEditTaskValueClick(tvEditTaskCountValue));
                tvEditTaskPriority.setOnClickListener((v) -> onEditTaskPriorityClick(tvEditTaskPriority));
                tvEditTaskCycling.setOnClickListener((v) -> onEditTaskCyclingClick(tvEditTaskCycling));
                tvEditTaskMaxAccumulate.setOnClickListener((v) -> onEditTaskValueClick(tvEditTaskMaxAccumulate));
                tvEditTaskCancel.setOnClickListener((x)-> {
                    InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(dialog.getWindow().getDecorView().getWindowToken(), 0);
                    dialog.dismiss();actionMode.finish();});
                tvEditTaskEdit.setOnClickListener((v) -> {
                    String text = etEditTask.getText().toString();
                    int value = Integer.valueOf(tvEditTaskCountValue.getText().toString());
                    int maxAccumulation = Integer.valueOf(tvEditTaskMaxAccumulate.getText().toString());
                    onEditTaskEditClick(activity, smallTasksFragment, actionMode,
                            task, text, value, maxAccumulation , taskCycling, taskPriority );
                });



                MenuItem editList = menu.add("edit ");
                editList.setIcon(R.drawable.ic_edit);
                editList.setOnMenuItemClickListener((MenuItem a) -> {
                    AlertDialog.Builder dialogBuilder = new MaterialAlertDialogBuilder(activity);
                    dialogBuilder.setView(view);
                    dialog = dialogBuilder.create();
                    dialog.setCanceledOnTouchOutside(false);
                    dialog.setOnKeyListener(new Dialog.OnKeyListener() {
                        @Override
                        public boolean onKey(DialogInterface dialog, int keyCode,
                                             KeyEvent event) {
                            if (keyCode == KeyEvent.KEYCODE_BACK) {
                                // do nothing
                                return true;
                            }
                            return false;
                        }
                    });
                    dialog.show();
                    InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.toggleSoftInputFromWindow(
                            activity.getWindow().getDecorView().getApplicationWindowToken(),
                            InputMethodManager.SHOW_FORCED, 0);
                    return true;
                });


                MenuItem categories = menu.add("categories ");
                categories.setIcon(R.drawable.ic_add);
                categories.setOnMenuItemClickListener((MenuItem a) -> {
                    showCategoriesDialog((MainActivity) activity, task, smallTasksFragment);
                    return true;
                });


                MenuItem deleteList = menu.add("delete ");
                deleteList.setIcon(R.drawable.ic_del);
                deleteList.setOnMenuItemClickListener((MenuItem a) -> {
                    TasksRealmController.deleteTask(task);
                    smallTasksFragment.notifyDataChanged();
                    actionMode.finish();
                    ((MainActivity)smallTasksFragment.getActivity()).invalidateOptionsMenu();
                    smallTasksFragment.getActivity().invalidateOptionsMenu();
//                    Toast.makeText(activity, "done", Toast.LENGTH_SHORT).show();
                    ((MainActivity)smallTasksFragment.getActivity()).showToast("Done");
                    return true;
                });


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
                if (App.getFolderSlidingPanelFragment() != null){
                    App.getFolderSlidingPanelFragment()
                            .notifyFolderOfTasksRVAdapterDataSetChanged();
                }
            }
        };
        return callback;
    }


    private void onEditTaskValueClick(TextView view) {
        int i = Integer.valueOf(view.getText().toString());
        if (i<10){
            i++;
        }else if (i>9){
            i=1;
        }
        view.setText("" + i);
        setEditTaskValueColor(view);
        if (i<2) view.setTextColor(defaultColor);
        else view.setTextColor(accentColor);
    }

    private void setEditTaskValueColor(TextView view){
        if ( Integer.valueOf(view.getText().toString())<2) view.setTextColor(defaultColor);
        else view.setTextColor(accentColor);
    }

    public void onEditTaskPriorityClick(TextView view) {
        if (taskPriority>2) taskPriority =0;
        else taskPriority ++;
        setPriorityTextAndColor(view);
    }

    private void setPriorityTextAndColor(TextView tv){
        if (taskPriority>1){
            String text = "!";
            int i = taskPriority;
            while (i>1){
                text +="!";
                i--;
            }
            tv.setText(text);
        } else tv.setText("!");

        if (taskPriority>0) tv.setTextColor(accentColor);
        else tv.setTextColor(defaultColor);
    }

    public void onEditTaskCyclingClick(TextView view) {
        taskCycling = !taskCycling;
        if (taskCycling) view.setTextColor(accentColor);
        else view.setTextColor(defaultColor);
    }

    private void showCategoriesDialog(MainActivity activity, TaskObject task, SmallTasksFragment smallTasksFragment) {
        List<FolderTaskObject> folders = FolderTaskRealmController.getAllFolders();
        if (folders.isEmpty()) {
            activity.showToast("No folders");
            return;
        }

        final List<Long> folderIds = new ArrayList<>();
        final CharSequence[] folderNames = new CharSequence[folders.size()];
        final boolean[] checked = new boolean[folders.size()];

        List<Long> current = TasksRealmController.getCategoryIds(task);

        for (int i = 0; i < folders.size(); i++) {
            FolderTaskObject f = folders.get(i);
            folderIds.add(f.getId());
            int group = FolderTaskRealmController.getFolderGroup(f);
            String tag = group == 1 ? " [Tasks2]" : (group == 0 ? " [Tasks1]" : "");
            folderNames[i] = f.getName() + tag;
            checked[i] = current.contains(f.getId());
        }

        new MaterialAlertDialogBuilder(activity)
                .setTitle("Categories")
                .setMultiChoiceItems(folderNames, checked, (d, which, isChecked) -> checked[which] = isChecked)
                .setPositiveButton("OK", (d, w) -> {
                    // primary = first checked; preserve current primary's position if still checked
                    List<Long> selected = new ArrayList<>();
                    long currentPrimary = task.getTaskFolderId();
                    int primaryIdx = folderIds.indexOf(currentPrimary);
                    if (primaryIdx >= 0 && checked[primaryIdx]) selected.add(currentPrimary);
                    for (int i = 0; i < checked.length; i++) {
                        if (checked[i] && folderIds.get(i) != currentPrimary) selected.add(folderIds.get(i));
                    }
                    if (selected.isEmpty()) {
                        activity.showToast("Pick at least one category");
                        return;
                    }
                    TasksRealmController.setTaskCategories(task, selected);
                    smallTasksFragment.notifyDataChanged();
                    for (FolderSlidingPanelFragment p : App.folderSlidingPanelFragments) {
                        p.notifySmallTasksViewPagerListsChanged();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    public void onEditTaskEditClick(Context context, SmallTasksFragment smallTasksFragment, ActionMode actionMode,
                                    TaskObject task, String taskText, int count, int maxAccumulation, boolean taskCycling, int priority) {
        TasksRealmController.editTask(task, taskText, count, maxAccumulation, taskCycling, priority);
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(dialog.getWindow().getDecorView().getWindowToken(), 0);

        dialog.dismiss();
        actionMode.finish();
        smallTasksFragment.notifyDataChanged();
        smallTasksFragment.getActivity().invalidateOptionsMenu();
//        Toast.makeText(context, "done", Toast.LENGTH_SHORT).show();
        activity.showToast("Done");
    }


}
