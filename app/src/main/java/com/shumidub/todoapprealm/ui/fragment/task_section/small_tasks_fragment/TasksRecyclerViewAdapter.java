package com.shumidub.todoapprealm.ui.fragment.task_section.small_tasks_fragment;

import android.graphics.Color;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.ItemTouchHelper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import com.shumidub.todoapprealm.App;
import com.shumidub.todoapprealm.R;
import com.shumidub.todoapprealm.realmcontrollers.taskcontroller.TasksRealmController;
import com.shumidub.todoapprealm.realmmodel.task.TaskObject;
import com.shumidub.todoapprealm.ui.activity.main.MainActivity;
import com.shumidub.todoapprealm.ui.theme.CornflowerPalette;
import androidx.cardview.widget.CardView;


import java.util.List;

/**
 * Created by Артем on 19.12.2017.
 */

public class TasksRecyclerViewAdapter extends RecyclerView.Adapter<TasksRecyclerViewAdapter.ViewHolder> {

    public List<TaskObject> tasks;
    private List<TaskObject> doneTasks;
    private boolean isNotEmpty;
    private static final int FOOTER_VIEW = 123;
    public boolean touchOutsideUnDoneTaskArea = false;
    private SmallTasksFragment smallTasksFragment;
    private OnItemLongClicked onItemLongClicked;
    private OnItemClicked onItemClicked;
    private ItemTouchHelper itemTouchHelper;
    private ItemTouchHelper.SimpleCallback itemTouchHelperSimpleCallback;
    MainActivity activity;
    private CornflowerPalette palette;

    public void useCornflowerPalette(boolean enabled) {
        palette = enabled ? new CornflowerPalette(activity) : null;
        notifyDataSetChanged();
    }


    public interface OnItemLongClicked{
        void onLongClick (View view, int position);
    }

    public interface OnItemClicked{
        void onClick (View view, int position);
    }

    public void setOnLongClicked(OnItemLongClicked onItemLongClicked){
        this.onItemLongClicked = onItemLongClicked;
    }

    public void setOnClicked(OnItemClicked onItemClicked){
        this.onItemClicked = onItemClicked;
    }


    public TasksRecyclerViewAdapter(MainActivity activity, List<TaskObject> tasks, List<TaskObject> doneTasks, SmallTasksFragment smallTasksFragment){
        this.activity = activity;
        this.tasks = tasks;
        this.doneTasks = doneTasks;
        this.smallTasksFragment = smallTasksFragment;
    }

//    @Override
//    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
//        super.onAttachedToRecyclerView(recyclerView);
//        attachTouchHelperToRecyclerView(recyclerView);
//    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        // todo НЕ ВЫЗВАЛСЯ
        Log.d("DTAG4568", "onCreateViewHolder: 4");


        View view;
        if ((tasks != null && !tasks.isEmpty() && tasks.size() > 0)
                || (doneTasks!=null && !doneTasks.isEmpty() && doneTasks.size()>0)) {

            //todo НЕ ВЫЗВАЛСЯ
            Log.d("DTAG4568", "onCreateViewHolder: 4 true");
            isNotEmpty = true;

            if(viewType!=FOOTER_VIEW) {
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.task_card_view, parent, false);
                return new NormalViewHolder(view);
            }else{
                //todo empty state need fix maybe
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.task_card_view_done_tasks, parent, false);
                return new FooterViewHolder(view);
            }

        }else{
            isNotEmpty = false;
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_view_empty_state, parent, false);
            return new ViewHolder(view);
        }
    }

    private void setTasksTextColor(ViewHolder holder, boolean isDone){
        if (isDone){
            holder.textView.setTextColor(Color.GRAY);



        }else if(!isDone){
            holder.textView.setTextColor(Color.BLACK);
        }
    }


    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {


        if ((tasks != null && !tasks.isEmpty() && tasks.size() > 0)
                || (doneTasks!=null && !doneTasks.isEmpty() && doneTasks.size()>0)) {

            Log.d("DTAG4568", "onCreateViewHolder: 4 true");
            isNotEmpty = true;

        }

        Log.d("DTAG4568", "onBindViewHolder: " + "position = " +position + " isNotEmpty = " + isNotEmpty);


        if (isNotEmpty ) {

            if (holder instanceof NormalViewHolder) {

                final TaskObject taskObject = tasks.get(position);

                long taskId = taskObject.getId();
                String text = taskObject.getText();

                holder.textView.setText(text);
                holder.textView.setTag(taskId);
                holder.tvCount.setText("" + taskObject.getCountValue());
                holder.tvAccumulation.setText(taskObject.getCountAccumulation() + "/" + taskObject.getMaxAccumulation());


                int priorityFromTaskObject = taskObject.getPriority();
                int priorityCount = taskObject.getPriority();
                String textPriority = "";

                while (priorityCount>0){
                    textPriority += "!";
                    priorityCount-=1;
                }

                holder.tvPriority.setText(textPriority);
                if (priorityFromTaskObject>0) holder.tvPriority.setTextColor(activity.getResources().getColor(R.color.colorAccent));
                else holder.tvPriority.setTextColor(activity.getResources().getColor(R.color.colorWhite));


                holder.tvPriority.setOnClickListener((listener)-> {

                    View view = holder.tvPriority;

                    int priority = taskObject.getPriority();

                        if (priority>2) priority =0;
                        else priority ++;

                        TasksRealmController.setTaskPriority(taskObject, priority);

                        if (priority>1){
                            String priorityText = "!";
                            int i = priority;
                            while (i>1){
                                priorityText +="!";
                                i--;
                            }
                            ((TextView) view).setText(priorityText);
                        } else ((TextView) view).setText("!");

                        if (priority>0) ((TextView) view).setTextColor(activity.getResources().getColor(R.color.colorAccent));
                        else ((TextView) view).setTextColor(activity.getResources().getColor(R.color.colorWhite));


                });

//                int color = taskObject.isCycling() ? Color.RED : Color.WHITE;
//                holder.tvCycling.setTextColor(color);

                bindCategoryStripes(holder, taskObject);
                applyPaletteIfNeeded(holder);

                holder.checkBox.setChecked(taskObject.isDone());

                if (taskObject.isCycling() && !taskObject.isDone()) holder.checkBox.setButtonDrawable(R.drawable.unchecked_accent_color_checkbox);
                else if (!taskObject.isCycling() && !taskObject.isDone()) holder.checkBox.setButtonDrawable(R.drawable.unchecked_gray_checkbox);

                if (taskObject.isCycling() && taskObject.isDone()) holder.checkBox.setButtonDrawable(R.drawable.checked_accent_color_checkbox);
                else if (!taskObject.isCycling() && taskObject.isDone()) holder.checkBox.setButtonDrawable(R.drawable.checked_gray_checkbox);

                if (palette != null && taskObject.isCycling()) {
                    holder.checkBox.setButtonTintList(android.content.res.ColorStateList.valueOf(palette.accent));
                } else {
                    holder.checkBox.setButtonTintList(null);
                }

                setTasksTextColor(holder, taskObject.isDone());

                holder.checkBox.setOnClickListener(
                        (cb) -> {
                            TasksRealmController.setTaskDoneOrParticullaryDone(taskObject, holder.checkBox.isChecked());

                            //todo need explore EXPLORE

                            holder.checkBox.setChecked(taskObject.isDone() ? false : true);

                            ((NormalViewHolder) holder).itemView
                                    .animate()
                                    .scaleX(0.002f)
                                    .scaleY(0.002f)
                                    .alpha(0.2f)
//                                  .translationX(10000f)
                                    .setDuration(175l)
                                    .withEndAction(()->smallTasksFragment.notifyDataChanged());

//                          smallTasksFragment.notifyDataChanged();
                            smallTasksFragment.getActivity().invalidateOptionsMenu();
                            setTasksTextColor(holder, taskObject.isDone());

                            Log.d("DTAG22222", "onBindViewHolder: sdfdsf1111");
                            if (App.getFolderSlidingPanelFragment() != null){
                                Log.d("DTAG22222", "onBindViewHolder: sdfdsf2222");
                                App.getFolderSlidingPanelFragment()
                                        .notifyFolderOfTasksRVAdapterDataSetChanged();
                            }
                        });

                holder.textView.setOnLongClickListener((View view) -> {
                    Log.d("DTAG", "onLongClick: " + view.toString() + " " + position);
                    onItemLongClicked.onLongClick(view, position);
                    return true;
                });

                holder.textView.setOnClickListener((View view) -> {
                    if (onItemClicked!=null) onItemClicked.onClick(view, position);
                });
            }
            else if (holder instanceof FooterViewHolder){
                holder.textViewDoneTask.setText("Done " + smallTasksFragment.doneTasks.size() + " tasks");
                holder.textViewDoneTask.setTag("footer");
                holder.textViewDoneTask.setOnClickListener((v) -> smallTasksFragment.showAllTasks());
            }

            isNotEmpty = false;
        } else {

            Log.d("DTAG", "onBindViewHolder: ");
        }
    }

    @Override
    public int getItemViewType(int position) {

        if (tasks.size() > 0 && position == tasks.size() ) {
            return FOOTER_VIEW;
        }

        else if (tasks.size() == 0 && position == 0 && doneTasks.size()>0 ) {
            return FOOTER_VIEW;
        }

        else if (tasks.size()>0 && position<tasks.size()){

        }

        else if (tasks.size() == 0 && doneTasks.size() == 0){
            isNotEmpty = false;
        }



        return super.getItemViewType(position);
    }

    @Override
    public int getItemCount() {
        return
                ((tasks != null && !tasks.isEmpty() && tasks.size() > 0))
                || (doneTasks!=null && !doneTasks.isEmpty() && doneTasks.size()>0)
                ? tasks.size()+1 : 1;
    }

    private void applyPaletteIfNeeded(ViewHolder holder) {
        if (palette == null) return;
        View root = holder.itemView;
        if (root instanceof CardView) {
            ((CardView) root).setCardBackgroundColor(palette.surface);
        } else {
            root.setBackgroundColor(palette.surface);
        }
        if (holder.textView != null) holder.textView.setTextColor(palette.inputText);
        if (holder.tvCount != null) holder.tvCount.setTextColor(palette.counter);
        if (holder.tvAccumulation != null) holder.tvAccumulation.setTextColor(palette.counter);
        if (holder.categoryStripes != null) holder.categoryStripes.setBackgroundColor(palette.accent);
    }

    private void bindCategoryStripes(ViewHolder holder, TaskObject taskObject) {
        if (holder.categoryStripes == null) return;
        int extraCount = taskObject.getExtraFolderIds() == null ? 0 : taskObject.getExtraFolderIds().size();
        holder.categoryStripes.setVisibility(extraCount > 0 ? View.VISIBLE : View.GONE);
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        TextView textView;
        TextView tvCount;
        TextView tvPriority;
        TextView tvCycling;
        CheckBox checkBox;
        TextView textViewDoneTask;
        TextView tvAccumulation;
        View categoryStripes;

        public ViewHolder(View itemView) {
            super(itemView);

            if(isNotEmpty) {
                textView = itemView.findViewById(R.id.tv);
                checkBox = itemView.findViewById(R.id.checkbox);
                tvCount = itemView.findViewById(R.id.task_value);
                tvPriority = itemView.findViewById(R.id.task_priority);
                tvCycling = itemView.findViewById(R.id.task_cycling);
                textViewDoneTask = itemView.findViewById(R.id.tv_done_tasks);
                tvAccumulation = itemView.findViewById(R.id.task_accumulation);
                categoryStripes = itemView.findViewById(R.id.category_stripes);
            }
        }
    }

    public class FooterViewHolder extends ViewHolder {
        public FooterViewHolder(View itemView) {
            super(itemView);
        }
    }

    public class NormalViewHolder extends ViewHolder {
        public NormalViewHolder(View itemView) {
            super(itemView);
        }
    }





}