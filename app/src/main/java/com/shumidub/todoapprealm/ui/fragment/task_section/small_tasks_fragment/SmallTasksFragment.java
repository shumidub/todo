package com.shumidub.todoapprealm.ui.fragment.task_section.small_tasks_fragment;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.shumidub.todoapprealm.R;
import com.shumidub.todoapprealm.realmcontrollers.taskcontroller.TasksRealmController;
import com.shumidub.todoapprealm.realmmodel.task.TaskObject;
import com.shumidub.todoapprealm.ui.actionmode.EmptyActionModeCallback;
import com.shumidub.todoapprealm.ui.activity.main.MainActivity;
import com.shumidub.todoapprealm.ui.actionmode.task.TaskActionModeCallback;

import java.util.List;


/**
 * Created by Артем on 16.01.2018.
 */

public class SmallTasksFragment extends Fragment {

    int position;


    //TASKS VIEW, ADAPTER
    RecyclerView rvTasks;
    LinearLayout emptyState;
    LinearLayoutManager llm;
    TasksRecyclerViewAdapter tasksRecyclerViewAdapter;
    static ActionMode actionMode;
    ItemTouchHelperAttacher itemTouchHelperAttacher;

    TasksRecyclerViewAdapter.OnItemLongClicked onItemLongClicked;
    TasksRecyclerViewAdapter.OnItemClicked onItemClicked;

    public List<TaskObject> tasks;
    public List<TaskObject> doneTasks;
    public boolean isAllTaskShowing;

    //TASKS
    long tasksFolderId;
    public static final String TASK_FOLDER_ID_KEY = "TASK_FOLDER_ID_KEY";

    public static final SmallTasksFragment newInstance(long tasksListId) {
        Bundle args = new Bundle();
        args.putLong(TASK_FOLDER_ID_KEY, tasksListId);
        Log.d("DTAG2425", "newInstance: folderId = " + tasksListId);
        Log.d("DTAG2425", " ");
        SmallTasksFragment fragment = new SmallTasksFragment();
        fragment.setArguments(args);
        return fragment;
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState!=null){
            tasksFolderId = getArguments().getLong(TASK_FOLDER_ID_KEY,0);
            Log.d("DTAG2425", "onCreate1: folderId =" + tasksFolderId);
        }

        if (getArguments()!= null){
            long tasksFolderId2 = getArguments().getLong(TASK_FOLDER_ID_KEY, 0);
            if (tasksFolderId2 != 0 && tasksFolderId == 0){
                tasksFolderId = tasksFolderId2;
                Log.d("DTAG2425", "onCreate2: folderId =" + tasksFolderId);
            }
        }

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        if (tasksFolderId==0){
            if (savedInstanceState!=null){
                tasksFolderId = getArguments().getLong(TASK_FOLDER_ID_KEY,0);
                Log.d("DTAG2425", "onCreateView1: folderId =" + tasksFolderId);
            }

            if (getArguments()!= null && tasksFolderId == 0){
                long tasksFolderId2 = getArguments().getLong(TASK_FOLDER_ID_KEY, 0);
                if (tasksFolderId2 != 0 ){
                    tasksFolderId = tasksFolderId2;
                    Log.d("DTAG2425", "onCreateView2: folderId =" + tasksFolderId);
                }
            }
        }


        View view = inflater.inflate(R.layout.rv_fragment_template_layout, container, false);
        emptyState = view.findViewById(R.id.empty_state);
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);


        if (tasksFolderId==0){
            if (savedInstanceState!=null){
                tasksFolderId = getArguments().getLong(TASK_FOLDER_ID_KEY,0);
                Log.d("DTAG2425", "onViewCreated1: folderId =" + tasksFolderId);
            }

            if (getArguments()!= null && tasksFolderId == 0){
                long tasksFolderId2 = getArguments().getLong(TASK_FOLDER_ID_KEY, 0);
                if (tasksFolderId2 != 0 ){
                    tasksFolderId = tasksFolderId2;
                    Log.d("DTAG2425", "onViewCreated2: folderId =" + tasksFolderId);
                }
            }
        }



//        TASK
        isAllTaskShowing = false;

//        tasksFolderId = getArguments().getLong(TASK_FOLDER_ID_KEY, 0);
//        if (FolderTaskRealmController.getFolder(tasksFolderId)==null) tasksFolderId =0;


        rvTasks = view.findViewById(R.id.rv);

//        rvTasks.setItemAnimator(new SlideInDownItemAnimator());
//        rvTasks.getItemAnimator().setRemoveDuration(20000);

        rvTasks.setHasFixedSize(true);
        setTasksListClickListeners();
        setTasksAndRV();
        setEmptyStateIfNeed();
    }

    private void setItemTouchHelperAttacher(){
        if (itemTouchHelperAttacher == null){
            itemTouchHelperAttacher = new ItemTouchHelperAttacher(this);
        }
    }

    private void setTasksListClickListeners(){
        onItemLongClicked = (View view, int position) -> {
            long idTask = (Long) view.getTag();
            TaskObject task = TasksRealmController.getTask(idTask);
            ActionMode.Callback callback = new TaskActionModeCallback().getCallback(getActivity(), SmallTasksFragment.this, task);
            actionMode = getActivity().startActionMode(callback);
        };

        onItemClicked = (View view, int position) -> {
            if (view!=null){
                long idTask = (Long) view.getTag();
                TaskObject task = TasksRealmController.getTask(idTask);
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setMessage(task.getText()).create().show();
            }
        };
    }


    public void setTasksAndNotifyDataSetChanged(){
        tasks = TasksRealmController.getNotDoneTasks(tasksFolderId);
        doneTasks = TasksRealmController.getDoneTasks(tasksFolderId);
        notifyDataChanged();
    }


    public void setTasksAndRV(){


//        if (tasksFolderId == 0) tasks = TasksRealmController.getNotDoneTasks();
//        else
        tasks = TasksRealmController.getNotDoneTasks(tasksFolderId);



//        if (tasksFolderId == 0) doneTasks = TasksRealmController.getDoneTasks();
//        else
        doneTasks = TasksRealmController.getDoneTasks(tasksFolderId);


        llm = new LinearLayoutManager(getContext());

        rvTasks.setLayoutManager(llm);
        tasksRecyclerViewAdapter = new TasksRecyclerViewAdapter((MainActivity) getActivity(), tasks, doneTasks, this);
        rvTasks.setAdapter(tasksRecyclerViewAdapter);

        tasksRecyclerViewAdapter.setOnLongClicked(onItemLongClicked);
        tasksRecyclerViewAdapter.setOnClicked(onItemClicked);

        Log.d("DTAG458", "setTasksAndRV: ATTACHEDDD" + rvTasks.getAdapter().getItemCount());

        setItemTouchHelperAttacher();
        itemTouchHelperAttacher.attachTouchHelperToRecyclerView(rvTasks);
    }

    public void notifyDataChanged(){

        position = llm.findFirstVisibleItemPosition();

        Log.d("DTAG458", "notifyDataChanged: position = " + position);

        //tasksRecyclerViewAdapter, reset task and done task
        if (tasksRecyclerViewAdapter ==null){
            tasksRecyclerViewAdapter =  new TasksRecyclerViewAdapter((MainActivity) getActivity(), tasks, doneTasks, this);
            rvTasks.setAdapter(tasksRecyclerViewAdapter);
        }
        else{
            tasksRecyclerViewAdapter.notifyDataSetChanged();
        }

        rvTasks.setAdapter(tasksRecyclerViewAdapter);
        llm.scrollToPosition(position);



//        tasksRecyclerViewAdapter =  new TasksRecyclerViewAdapter((MainActivity) getActivity(), tasks, doneTasks, this);
//        rvTasks.setAdapter(tasksRecyclerViewAdapter);

    }

    //Show Done and Not done Tasks
    public void showAllTasks(){
        if(!isAllTaskShowing) {
            int position = llm.findFirstVisibleItemPosition();
            if (tasksFolderId == 0) tasks = TasksRealmController.getTasks();
            else tasks = TasksRealmController.getTasks(tasksFolderId);
            tasksRecyclerViewAdapter = new TasksRecyclerViewAdapter((MainActivity) getActivity(),tasks,doneTasks, this);
            tasksRecyclerViewAdapter.setOnLongClicked(onItemLongClicked);
            tasksRecyclerViewAdapter.setOnClicked(onItemClicked);
            rvTasks.setAdapter(tasksRecyclerViewAdapter);
            rvTasks.scrollToPosition(position);
            isAllTaskShowing = true;
        } else if (isAllTaskShowing){
            if (tasksFolderId == 0) tasks = TasksRealmController.getNotDoneTasks();
            else tasks = TasksRealmController.getNotDoneTasks(tasksFolderId);
            tasksRecyclerViewAdapter = new TasksRecyclerViewAdapter((MainActivity) getActivity(),tasks,doneTasks, this);
            tasksRecyclerViewAdapter.setOnLongClicked(onItemLongClicked);
            tasksRecyclerViewAdapter.setOnClicked(onItemClicked);
            rvTasks.setAdapter(tasksRecyclerViewAdapter);
            isAllTaskShowing = false;
        }

        setItemTouchHelperAttacher();
        itemTouchHelperAttacher.setAdapter();
        itemTouchHelperAttacher.setTasks();
    }

//    public void tasksDataChanged(){
//        // if you need use it, use it without attach touch helper inside setTasksAndRV();
//        setTasksAndRV();
//    }

//    public void finishActionMode(){
//        ((MainActivity) getActivity()).startSupportActionMode(new EmptyActionModeCallback());
//    }

    private void setEmptyStateIfNeed(){


        if (tasksRecyclerViewAdapter.getItemCount() == 0){
            emptyState.setVisibility(View.VISIBLE);
        } else {
            emptyState.setVisibility(View.GONE);
        }
    }




}



