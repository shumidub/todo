package com.shumidub.todoapprealm.ui.fragment.task_section.folder_panel_sliding_fragment.adapter;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Color;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.shumidub.todoapprealm.R;
import com.shumidub.todoapprealm.realmmodel.task.FolderTaskObject;
import com.shumidub.todoapprealm.realmmodel.RealmInteger;
import com.shumidub.todoapprealm.realmmodel.task.TaskObject;
import com.shumidub.todoapprealm.ui.theme.CornflowerPalette;
import androidx.cardview.widget.CardView;


import java.util.Calendar;

import io.realm.RealmList;

/**
 * Created by user on 22.01.18.
 */

public class FolderOfTaskRecyclerViewAdapter
        extends RecyclerView.Adapter<FolderOfTaskRecyclerViewAdapter.ViewHolder> {


    RealmList<FolderTaskObject> realmListFolder;
    OnHolderTextViewOnClickListener onHolderTextViewOnClickListener;
    OnHolderTextViewOnLongClickListener onHolderTextViewOnLongClickListener;
    Activity activity;
    private final int taskGroup;
    private CornflowerPalette palette;


    public interface OnHolderTextViewOnClickListener {
       void onClick(ViewHolder holder, int position);
    }
    public interface OnHolderTextViewOnLongClickListener {
       void onLongClick(ViewHolder holder, int position);
    }

    public FolderOfTaskRecyclerViewAdapter(RealmList<FolderTaskObject> realmListFolder, Activity activity){
        this(realmListFolder, activity, 0);
    }

    public FolderOfTaskRecyclerViewAdapter(RealmList<FolderTaskObject> realmListFolder, Activity activity, int taskGroup){
        this.realmListFolder = realmListFolder;
        this.activity = activity;
        this.taskGroup = taskGroup;
        if (taskGroup == 1) palette = new CornflowerPalette(activity);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view;
//        view = LayoutInflater.from(parent.getContext()).inflate(R.layout.folder_card_view, parent, false);
        view = LayoutInflater.from(parent.getContext()).inflate(R.layout.folder_tasks_item_card_view, parent, false);

        return new ItemViewHolder(view);
    }

    @SuppressLint({"All", "ClickableViewAccessibility"})
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Log.d("DTAG", "onBindViewHolder: position" + position + "array size = " + realmListFolder.size());

        ((ItemViewHolder) holder).textView.setText("" + realmListFolder.get(position).getName());
        ((ItemViewHolder) holder).textView.setTag(realmListFolder.get(position).getId());

        if (palette != null) applyPaletteToFolderCard((ItemViewHolder) holder);

        ((ItemViewHolder) holder).textView.setOnClickListener(
                (v)->onHolderTextViewOnClickListener.onClick(holder, position));
        ((ItemViewHolder) holder).textView.setOnLongClickListener(
                (v)->{
                    onHolderTextViewOnLongClickListener.onLongClick(holder, position);
                    return true;
                }
        );

        setFolderTaskCounts(holder, position);

    }


    @Override
    public int getItemCount() {
        return realmListFolder.size();
    }


    private void setFolderTaskCounts(ViewHolder holder, int position){
        int done = 0;
        int all = 0;

        RealmList<TaskObject> realmList = realmListFolder.get(position).getTasks();

        for (TaskObject task: realmList){

            all = all + (task.getCountValue() * task.getMaxAccumulation());
            done = done + (task.getCountAccumulation() * task.getCountValue());

        }

        String folderTaskCounts = String.format("%d / %d", done, all);
        ((ItemViewHolder) holder).tvFolderTaskCounts.setText(folderTaskCounts);
        if (palette != null) {
            ((ItemViewHolder) holder).tvFolderTaskCounts.setTextColor(palette.counter);
        } else if (realmListFolder.get(position).isDaily){
            ((ItemViewHolder) holder).tvFolderTaskCounts.setTextColor(activity.getApplicationContext().getResources().getColor(R.color.colorPrimaryDark));
        } else {
            ((ItemViewHolder) holder).tvFolderTaskCounts.setTextColor(Color.GRAY);
        }
    }

    private void applyPaletteToFolderCard(ItemViewHolder holder) {
        View root = holder.itemView;
        if (root instanceof CardView) {
            ((CardView) root).setCardBackgroundColor(palette.surface);
        } else {
            root.setBackgroundColor(palette.surface);
        }
        holder.textView.setTextColor(palette.inputText);
    }



    public class ViewHolder extends RecyclerView.ViewHolder {
        public ViewHolder(View itemView) {
            super(itemView);
        }
    }

    public class ItemViewHolder extends ViewHolder {
        TextView textView;
        TextView tvFolderTaskCounts;

        public ItemViewHolder(View itemView) {
            super(itemView);
//          textView = itemView.findViewById(R.id.item_text);
            textView = itemView.findViewById(R.id.tv_note_text);
            tvFolderTaskCounts = itemView.findViewById(R.id.tvFolderTaskCounts);
        }
    }


    public void setOnHolderTextViewOnClickListener(OnHolderTextViewOnClickListener onHolderTextViewOnClickListener){
        this.onHolderTextViewOnClickListener = onHolderTextViewOnClickListener;
    }

    public void setOnHolderTextViewOnLongClickListener(OnHolderTextViewOnLongClickListener onHolderTextViewOnLongClickListener){
        this.onHolderTextViewOnLongClickListener = onHolderTextViewOnLongClickListener;
    }



}



