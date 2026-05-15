package com.shumidub.todoapprealm.ui.fragment.task_section.small_tasks_fragment;

import android.annotation.SuppressLint;
import android.graphics.Canvas;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.ItemTouchHelper;
import android.util.Log;

import com.shumidub.todoapprealm.App;
import com.shumidub.todoapprealm.realmcontrollers.taskcontroller.TasksRealmController;
import com.shumidub.todoapprealm.realmmodel.task.TaskObject;
import com.shumidub.todoapprealm.ui.actionmode.EmptyActionModeCallback;
import com.shumidub.todoapprealm.ui.activity.main.MainActivity;

import java.util.List;

import static androidx.recyclerview.widget.ItemTouchHelper.ACTION_STATE_IDLE;

/**
 * Created by A.shumidub on 27.02.18.
 */

public class ItemTouchHelperAttacher {

    ItemTouchHelper itemTouchHelper;
    ItemTouchHelper.SimpleCallback itemTouchHelperSimpleCallback;
    SmallTasksFragment smallTasksFragment;
    MainActivity activity;
    TasksRecyclerViewAdapter tasksRecyclerViewAdapter;
    List<TaskObject> tasks;

    public ItemTouchHelperAttacher (SmallTasksFragment smallTasksFragment){
        this.smallTasksFragment = smallTasksFragment;
        activity = (MainActivity) smallTasksFragment.getActivity();
        setAdapter();
        setTasks();
    }

    public void setAdapter(){
        tasksRecyclerViewAdapter = (TasksRecyclerViewAdapter) smallTasksFragment.rvTasks.getAdapter();
    }

    public void setTasks(){
        tasks = tasksRecyclerViewAdapter.tasks;
    }


    public void attachTouchHelperToRecyclerView(RecyclerView recyclerView){
        itemTouchHelperSimpleCallback =  new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN ,0) {

            int dragFrom = -1;
            int dragTo = -1;


            @Override
            public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
                Log.d("DTAG733", "onChildDraw: ");

                if (smallTasksFragment.isAllTaskShowing) return ;


                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }

            @Override
            public void onChildDrawOver(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {

                if (smallTasksFragment.isAllTaskShowing) return ;


                Log.d("DTAG733", "onChildDrawOver: ");
                super.onChildDrawOver(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }

            @Override
            public void onMoved(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, int fromPos, RecyclerView.ViewHolder target, int toPos, int x, int y) {

                if (smallTasksFragment.isAllTaskShowing) return ;


                Log.d("DTAG733", "onMoved: ");
                super.onMoved(recyclerView, viewHolder, fromPos, target, toPos, x, y);
            }

            @Override
            public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
                Log.d("DTAG733", "getMovementFlags: ");
                if (smallTasksFragment.isAllTaskShowing) return ACTION_STATE_IDLE;
                return super.getMovementFlags(recyclerView, viewHolder);
            }


            @Override
            public boolean isLongPressDragEnabled() {

                if (smallTasksFragment.isAllTaskShowing) return false;

                Log.d("DTAG733", "isLongPressDragEnabled: ");
                return super.isLongPressDragEnabled();

            }


            @Override
            public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
                Log.d("DTAG733", "onSelectedChanged: ");
                super.onSelectedChanged(viewHolder, actionState);
            }





            @SuppressLint("RestrictedApi")
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder,
                                  RecyclerView.ViewHolder target) {

                if (smallTasksFragment.isAllTaskShowing){
                    Log.d("DTAG", "onMove: smallTasksFragment.isAllTaskShowing = " + smallTasksFragment.isAllTaskShowing);
                    return false;
                }

                activity.getSupportActionBar().startActionMode(new EmptyActionModeCallback());

                int fromPosition = viewHolder.getAdapterPosition();
                int toPosition = target.getAdapterPosition();

                if (fromPosition > tasks.size() -1 || tasks.get(fromPosition).isDone()
                        || toPosition > tasks.size() - 1 || tasks.get(toPosition).isDone()){
                    tasksRecyclerViewAdapter.touchOutsideUnDoneTaskArea = true;
//                    notifyItemMoved(fromPosition, tasks.size() -1);
                    return false;
                }

                if (dragFrom == -1) {
                    dragFrom = fromPosition;
                }
                dragTo = toPosition;

                //todo не сбрасывается from он становится равен предыдущему to?
                Log.d("DTAG47", String.format("onMove: from %d  to %d ", fromPosition, toPosition));

                if (! (viewHolder instanceof TasksRecyclerViewAdapter.FooterViewHolder)){
                    recyclerView.getAdapter().notifyItemMoved(fromPosition, toPosition);
                }


                return true;
            }

            private void reallyMoved(TaskObject taskTarget, TaskObject taskTargetPosition) {
                App.initRealm();
                App.realm.executeTransaction((realm) ->{
                    long folderId = ((TaskObject) taskTarget).getTaskFolderId();
                    TasksRealmController.changeOrder(folderId, taskTarget, taskTargetPosition );
                });
            }

            @Override
            public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {

                if (tasksRecyclerViewAdapter.touchOutsideUnDoneTaskArea) {
                    tasksRecyclerViewAdapter.touchOutsideUnDoneTaskArea = false;
                    dragFrom = dragTo = -1;
                    return;
                }

                super.clearView(recyclerView, viewHolder);

                if (dragFrom != -1 && dragTo != -1 && dragFrom != dragTo) {
                    if (! (viewHolder instanceof TasksRecyclerViewAdapter.FooterViewHolder)) {
                        if (dragFrom > tasks.size()-1) return;
                        TaskObject taskTarget = tasks.get(dragFrom);
                        dragTo = dragTo < tasks.size() ? dragTo:tasks.size()-1;
                        TaskObject taskTargetPosition = tasks.get(dragTo);
                        reallyMoved(taskTarget, taskTargetPosition);
                        //todo не сбрасывается from он становится равен предыдущему to?
                        Log.d("DTAG487", String.format("onMove: from %d  to %d ", dragFrom, dragTo));
                    }
                }
                dragFrom = dragTo = -1;
            }

            @Override
            public void onSwiped(final RecyclerView.ViewHolder viewHolder, int swipeDir) { }
        };
        itemTouchHelper = new ItemTouchHelper(itemTouchHelperSimpleCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);
    }

}
