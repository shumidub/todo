package com.shumidub.todoapprealm.ui.fragment.note_fragment;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import androidx.appcompat.app.ActionBar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.ItemTouchHelper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;

import com.shumidub.todoapprealm.App;
import com.shumidub.todoapprealm.R;
import com.shumidub.todoapprealm.realmcontrollers.notescontroller.FolderNotesRealmController;
import com.shumidub.todoapprealm.realmmodel.notes.FolderNotesObject;
import com.shumidub.todoapprealm.ui.actionmode.EmptyActionModeCallback;
import com.shumidub.todoapprealm.ui.actionmode.note.FolderNoteActionModeCallback;
import com.shumidub.todoapprealm.ui.activity.main.MainActivity;
import com.shumidub.todoapprealm.ui.dialog.note_dialog.AddNoteDialog;
import com.shumidub.todoapprealm.ui.dialog.note_dialog.EditNoteDialog;

import io.realm.RealmList;

import static com.shumidub.todoapprealm.ui.dialog.note_dialog.AddNoteDialog.TYPE_NOTE;


/**
 * Created by Артем on 19.12.2017.
 *
 */


public class FolderNoteFragment extends Fragment{

    ActionBar actionBar;
    public boolean actionModeIsEnabled = false;

    RecyclerView rv;
    LinearLayout emptyState;

    FolderNotesRecyclerViewAdapter folderAdapter;
    NotesRecyclerViewAdapter noteAdapter;

    int type = AddNoteDialog.TYPE_FOLDER;

    public boolean isNoteFragment = false;

    String title = "Notes";

    long idFolderNoteObject = 0;
    long idNoteObject = 0;
    long id;


    boolean folderViewShowing= true;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.note_fragment_layout, container, false);
        actionBar = ((MainActivity) getActivity()).getSupportActionBar();
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        emptyState =  view.findViewById(R.id.empty_state);
        rv = view.findViewById(R.id.recycle_view);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        setFolderNoteViews();
        actionBar = ((MainActivity) getActivity()).getSupportActionBar();
        setHasOptionsMenu(true);
        setTouchHelper(rv);
        id = idFolderNoteObject;


    }



    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        MenuItem add = menu.add(2,2,2,"add ");
        add.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        add.setIcon(R.drawable.ic_add);
        add.setOnMenuItemClickListener((MenuItem a) -> {
            AddNoteDialog addNoteDialog = AddNoteDialog.newInstance(type, id);
            try {
                addNoteDialog.show(getActivity().getSupportFragmentManager(), "Add_note");
                InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.toggleSoftInputFromWindow(
                        getActivity().getWindow().getDecorView().getApplicationWindowToken(),
                        InputMethodManager.SHOW_FORCED, 0);
            }catch (NullPointerException e){e.printStackTrace();}


//            new AddReportDialog().show(getActivity().getSupportFragmentManager(), AddReportDialog.ADD_REPORT_TITLE);
            return true;
        });

    }

    public void notifyDataChanged(){

        RecyclerView.Adapter adapter = rv.getAdapter();

        if (adapter instanceof FolderNotesRecyclerViewAdapter){
            folderAdapter.notifyDataSetChanged();
        } else{
            noteAdapter.notifyDataSetChanged();
        }

        if (adapter.getItemCount() == 0){
            emptyState.setVisibility(View.VISIBLE);
        } else {
            emptyState.setVisibility(View.GONE);
        }
    }



    @SuppressLint("RestrictedApi")
    public void setFolderNoteViews(){


        actionBar.setDisplayHomeAsUpEnabled(false);

        title = "Notes";
        if ( ((MainActivity)getActivity()).getPagerAdapterPosition() == 0 )
            actionBar.setTitle(title);


        type = AddNoteDialog.TYPE_FOLDER;
        isNoteFragment = false;

        folderAdapter = new FolderNotesRecyclerViewAdapter((MainActivity) getActivity());
        folderAdapter.setOnClickListener((h,p,idFolderFromAdapter)->{
            setNoteViews(idFolderFromAdapter);
        });
        folderAdapter.setOnLongClickListener((h,p,id1)->{
            actionBar.startActionMode(new FolderNoteActionModeCallback()
                            .getFolderNoteActionMode((MainActivity) getActivity(), this,
                                    EditNoteDialog.TYPE_FOLDER, id1));

            return true;
        });
        rv.setAdapter(folderAdapter);

        if (folderAdapter.getItemCount() == 0){
            emptyState.setVisibility(View.VISIBLE);
        } else {
            emptyState.setVisibility(View.GONE);
        }

    }

    @SuppressLint("RestrictedApi")
    public void setNoteViews(long idFolderFromAdapter){


        
        actionBar.setDisplayHomeAsUpEnabled(true);
        
        title = FolderNotesRealmController.getFolderNote(idFolderFromAdapter).getName();
        actionBar.setTitle(title);

        isNoteFragment = true;

        type = TYPE_NOTE;
        id = idFolderFromAdapter;

        noteAdapter = new NotesRecyclerViewAdapter(idFolderFromAdapter, (MainActivity) getActivity());
        noteAdapter.setId(idFolderFromAdapter);
        rv.setAdapter(noteAdapter);
        noteAdapter.setOnClickListener((h,p,id)-> setFolderNoteViews());
        noteAdapter.setOnLongClickListener((h,p,id)->{
            actionBar.startActionMode(new FolderNoteActionModeCallback()
                    .getFolderNoteActionMode((MainActivity) getActivity(), this,
                            EditNoteDialog.TYPE_NOTE, id));
            return true;
        });
        rv.setAdapter(noteAdapter);

        if (noteAdapter.getItemCount() == 0){
            emptyState.setVisibility(View.VISIBLE);
        } else {
            emptyState.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == android.R.id.home ){
            setFolderNoteViews();
            actionBar.setTitle(title);
        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressLint("RestrictedApi")
    public void finishActionMode(){
        actionBar.startActionMode(new EmptyActionModeCallback());
    }


    private void setTouchHelper(RecyclerView rv){


        // set ITEM TOUCH HELPER for folder rv
        App.initRealm();
        RealmList<FolderNotesObject> folderOfNotesContainerList = App.folderOfNotesContainerList;
        //todo try is it working, or need not use linked variable

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN ,0) {

            int dragFrom = -1;
            int dragTo = -1;

            @SuppressLint("RestrictedApi")
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder,
                                  RecyclerView.ViewHolder target) {

                    actionBar.startActionMode(new EmptyActionModeCallback());

                int fromPosition = viewHolder.getAdapterPosition();
                int toPosition = target.getAdapterPosition();

                if (dragFrom == -1) {
                    dragFrom = fromPosition;
                }
                dragTo = toPosition;

                rv.getAdapter().notifyItemMoved(fromPosition, toPosition);
                return true;
            }

            // todo need fix if move bellow "add list"
            private void reallyMoved(int from, int to) {

                RecyclerView.Adapter adapter = rv.getAdapter();

                if (adapter instanceof FolderNotesRecyclerViewAdapter){
                    ((FolderNotesRecyclerViewAdapter) adapter).itemMove(from, to);
                }else if (adapter instanceof NotesRecyclerViewAdapter){
                    ((NotesRecyclerViewAdapter) adapter).itemMove(from, to);
                }

            }

            @Override
            public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
                super.clearView(recyclerView, viewHolder);

                if (dragFrom != -1 && dragTo != -1 && dragFrom != dragTo) {
                    reallyMoved(dragFrom, dragTo);
                }
                dragFrom = dragTo = -1;
            }

            @Override
            public void onSwiped(final RecyclerView.ViewHolder viewHolder, int swipeDir) { }

        });

        itemTouchHelper.attachToRecyclerView(rv);


    }


    public String getValidTitle(){
       return title;
    }



}





