package com.shumidub.todoapprealm.ui.fragment.note_fragment;

import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.shumidub.todoapprealm.App;
import com.shumidub.todoapprealm.R;
import com.shumidub.todoapprealm.realmmodel.notes.FolderNotesObject;
import com.shumidub.todoapprealm.realmmodel.notes.NoteObject;
import com.shumidub.todoapprealm.ui.activity.main.MainActivity;

import io.realm.RealmList;

/**
 * Created by A.shumidub on 07.02.18.
 *
 */

public class NotesRecyclerViewAdapter extends RecyclerView.Adapter<NotesRecyclerViewAdapter.ViewHolder> {

    RealmList<NoteObject> notesList;
    OnClickListener onClickListener;
    OnLongClickListener onLongClickListener;
    MainActivity activity;
    RealmList<NoteObject> noteList;



    long id = 0;

    public interface OnClickListener {
        void onClick(ViewHolder holder, int position, long id);
    }
    public interface OnLongClickListener {
        boolean onLongClick(ViewHolder holder, int position, long id);
    }

    public NotesRecyclerViewAdapter(long folderNotesId, MainActivity activity) {
        this.activity = activity;
        App.initRealm();
        notesList = App.realm.where(FolderNotesObject.class)
                .equalTo("id", folderNotesId).findFirst().getTasks();
    }

    @Override
    public NotesRecyclerViewAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.notes_item_card_view, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(NotesRecyclerViewAdapter.ViewHolder holder, int position) {
        long id = notesList.get(position).getId();

        holder.text.setText(notesList.get(position).getText());
        holder.text.setTag(id);

//        holder.text.setOnClickListener((v)-> onClickListener.onClick(holder,position,id));
        holder.text.setOnLongClickListener((v)-> onLongClickListener.onLongClick(holder,position,id));

    }

    @Override
    public int getItemCount() {
        return notesList.size();
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

        if (from < noteList.size() ){
            App.realm.executeTransaction((realm) -> {
                int to2 = to<noteList.size() ? to : noteList.size()-1;
                noteList.add(to2, noteList.remove(from));
            });
        }

    }

    public void setId(long id){
        this.id = id;
        noteList = App.realm.where(FolderNotesObject.class)
                .equalTo("id", id).findFirst().getTasks();
    }
}
