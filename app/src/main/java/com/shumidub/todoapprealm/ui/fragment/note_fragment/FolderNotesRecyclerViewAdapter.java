package com.shumidub.todoapprealm.ui.fragment.note_fragment;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.shumidub.todoapprealm.App;
import com.shumidub.todoapprealm.R;
import com.shumidub.todoapprealm.realmmodel.notes.FolderNotesObject;



import io.realm.RealmList;

/**
 * Created by A.shumidub on 07.02.18.
 *
 */

public class FolderNotesRecyclerViewAdapter extends RecyclerView.Adapter<FolderNotesRecyclerViewAdapter.ViewHolder> {

    RealmList<FolderNotesObject> folderNotesList;
    OnClickListener onClickListener;
    OnLongClickListener onLongClickListener;

    public interface OnClickListener {
        void onClick(ViewHolder holder, int position, long id);
    }
    public interface OnLongClickListener {
        boolean onLongClick(ViewHolder holder, int position, long id);
    }

    public FolderNotesRecyclerViewAdapter() {
        App.initRealm();
        folderNotesList = App.folderOfNotesContainerList;
    }

    @Override
    public FolderNotesRecyclerViewAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.note_fragment_layout, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(FolderNotesRecyclerViewAdapter.ViewHolder holder, int position) {
        long id = folderNotesList.get(position).getId();

        holder.text.setText(folderNotesList.get(position).getName());
        holder.text.setTag(id);

        holder.text.setOnClickListener((v)-> onClickListener.onClick(holder,position,id));
        holder.text.setOnLongClickListener((v)-> onLongClickListener.onLongClick(holder,position,id));
    }

    @Override
    public int getItemCount() {
        return folderNotesList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder{

        TextView text;

        public ViewHolder(View itemView) {
            super(itemView);
            text = itemView.findViewById(R.id.tv_note_text);
        }
    }

    public void setOnClickListener(OnClickListener onClickListener) {
        this.onClickListener = onClickListener;
    }

    public void setOnLongClickListener(OnLongClickListener onLongClickListener) {
        this.onLongClickListener = onLongClickListener;
    }
}