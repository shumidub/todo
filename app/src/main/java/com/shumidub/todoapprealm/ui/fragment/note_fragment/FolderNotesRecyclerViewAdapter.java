package com.shumidub.todoapprealm.ui.fragment.note_fragment;

import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.shumidub.todoapprealm.App;
import com.shumidub.todoapprealm.R;
import com.shumidub.todoapprealm.realmmodel.notes.FolderNotesObject;
import com.shumidub.todoapprealm.ui.activity.main.MainActivity;


import io.realm.RealmList;

/**
 * Created by A.shumidub on 07.02.18.
 *
 */

public class FolderNotesRecyclerViewAdapter extends RecyclerView.Adapter<FolderNotesRecyclerViewAdapter.ViewHolder> {

    RealmList<FolderNotesObject> folderNotesList;
    OnClickListener onClickListener;
    OnLongClickListener onLongClickListener;
    MainActivity activity;




    public interface OnClickListener {
        void onClick(ViewHolder holder, int position, long id);
    }
    public interface OnLongClickListener {
        boolean onLongClick(ViewHolder holder, int position, long id);
    }

    public FolderNotesRecyclerViewAdapter(MainActivity activity) {
        this.activity = activity;
        App.initRealm();
        folderNotesList = App.folderOfNotesContainerList;
    }

    @Override
    public FolderNotesRecyclerViewAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.notes_item_card_view, parent, false);
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


    public void itemMove(int from, int to){
        App.initRealm();
        if (from < App.folderOfNotesContainerList.size() ){
            App.realm.executeTransaction((realm) -> {
                int to2 = to<App.folderOfNotesContainerList.size() ? to : App.folderOfNotesContainerList.size()-1;
                App.folderOfNotesContainerList.add(to2, App.folderOfNotesContainerList.remove(from));

            });
        }
    }
}
