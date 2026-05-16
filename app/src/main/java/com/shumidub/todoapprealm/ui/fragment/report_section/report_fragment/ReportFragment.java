package com.shumidub.todoapprealm.ui.fragment.report_section.report_fragment;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.view.ActionMode;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;

import com.shumidub.todoapprealm.R;
import com.shumidub.todoapprealm.realmcontrollers.reportcontroller.ReportRealmController;
import com.shumidub.todoapprealm.realmmodel.report.ReportObject;
import com.shumidub.todoapprealm.ui.actionmode.EmptyActionModeCallback;
import com.shumidub.todoapprealm.ui.actionmode.report.ReportActionModeCallback;
import com.shumidub.todoapprealm.ui.activity.main.MainActivity;
import com.shumidub.todoapprealm.ui.dialog.report_dialog.AddReportDialog;
import com.shumidub.todoapprealm.ui.dialog.report_dialog.FullSizeReportDialog;

import java.util.List;

/**
 * Created by Артем on 19.12.2017.
 *
 */

public class ReportFragment extends Fragment{

    RecyclerView recyclerView;
    LinearLayout emptyState;
    ActionBar actionBar;
    ReportRecyclerViewAdapter reportRecyclerViewAdapter;
//    List<ReportObject> reportObjectList;
    public boolean actionModeIsEnabled = false;

    public static long id = 0l;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.rv_fragment_template_layout, container, false);
        return view;
    }

    @SuppressLint("RestrictedApi")
    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        actionBar = ((MainActivity) getActivity()).getSupportActionBar();
        setHasOptionsMenu(true);



        recyclerView = view.findViewById(R.id.rv);
        emptyState = view.findViewById(R.id.empty_state);

//        reportObjectList = ReportRealmController.getReportList();
        reportRecyclerViewAdapter = new ReportRecyclerViewAdapter(getContext());
        recyclerView.setAdapter(reportRecyclerViewAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        reportRecyclerViewAdapter.setOnClicked((v, position, reportId)->{
            FullSizeReportDialog dialogFullSize = FullSizeReportDialog.newInstance(reportId);
            if (dialogFullSize!=null) dialogFullSize.show(getFragmentManager(), "Full_size");
        });
        reportRecyclerViewAdapter.setOnLongClicked((v, position, reportId)->{
            setId(reportId);
            ActionMode.Callback actionModeCallback
                    = new ReportActionModeCallback().getReportActionMode(getActivity(), reportId);
            actionBar.startActionMode(actionModeCallback);
            actionModeIsEnabled = true;
            return true;});

        setEmptyStateIfNeed();
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        MenuItem add = menu.add(3,3,3,"add ");
        add.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        add.setIcon(R.drawable.ic_add);
        add.setOnMenuItemClickListener((MenuItem a) -> {
            new AddReportDialog().show(getActivity().getSupportFragmentManager(), AddReportDialog.ADD_REPORT_TITLE);

            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.toggleSoftInputFromWindow(
                    getActivity().getWindow().getDecorView().getApplicationWindowToken(),
                    InputMethodManager.SHOW_FORCED, 0);

            return true;
        });
    }

    public void notifyDataChanged(){
//        reportObjectList = ReportRealmController.getReportList();
        reportRecyclerViewAdapter.notifyDataSetChanged();

        setEmptyStateIfNeed();

//        reportRecyclerViewAdapter = new ReportRecyclerViewAdapter(reportObjectList);
//        recyclerView.setAdapter(reportRecyclerViewAdapter);

    }


    public static void setId(long idReport){
        id = idReport;
    }

    @SuppressLint("RestrictedApi")
    public void finishActionMode(){
        actionBar.startActionMode(new EmptyActionModeCallback());
        actionModeIsEnabled = false;
    }


    private void setEmptyStateIfNeed(){

        RecyclerView.Adapter adapter = recyclerView.getAdapter();

        if (adapter.getItemCount() == 0){
            emptyState.setVisibility(View.VISIBLE);
        } else {
            emptyState.setVisibility(View.GONE);
        }
    }



}
