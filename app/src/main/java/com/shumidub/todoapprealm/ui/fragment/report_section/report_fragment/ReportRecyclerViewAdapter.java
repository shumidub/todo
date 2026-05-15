package com.shumidub.todoapprealm.ui.fragment.report_section.report_fragment;

import android.content.Context;
import android.graphics.Color;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;

import com.shumidub.todoapprealm.App;
import com.shumidub.todoapprealm.R;
import com.shumidub.todoapprealm.realmcontrollers.reportcontroller.ReportRealmController;
import com.shumidub.todoapprealm.realmmodel.report.ReportObject;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Created by Артем on 19.12.2017.
 */

public class ReportRecyclerViewAdapter extends RecyclerView.Adapter<ReportRecyclerViewAdapter.ViewHolder> {

    View view;
    Context context;


    private OnItemLongClicked onItemLongClicked;
    private OnItemClicked onItemClicked;
    private ItemTouchHelper itemTouchHelper;
    private ItemTouchHelper.SimpleCallback itemTouchHelperSimpleCallback;

    public interface OnItemLongClicked{
        boolean onLongClick(View view, int position, long idReportObject);
    }

    public interface OnItemClicked{
        void onClick(View view, int position, long idReportObject);
    }

    public void setOnLongClicked(OnItemLongClicked onItemLongClicked){
        this.onItemLongClicked = onItemLongClicked;
    }

    public void setOnClicked(OnItemClicked onItemClicked){
        this.onItemClicked = onItemClicked;
    }


    public ReportRecyclerViewAdapter(Context context){
       this.context = context;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view;
        view = LayoutInflater.from(parent.getContext()).inflate(R.layout.report_card_view, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        if (!ReportRealmController.getReportList().isEmpty()) {
            ReportObject reportObject = ReportRealmController.getReportList().get(position);

            int countOfDay = reportObject.getCountOfDay();
            int color = Color.BLACK;


            holder.tvDayCount.setText(String.valueOf(countOfDay));

            if (reportObject.isWeekReport()){
                if (countOfDay >= 50) color = context.getResources().getColor(R.color.colorCountValue100per);
                else if (countOfDay >= 40) color = context.getResources().getColor(R.color.colorCountValue80per);
                else if (countOfDay >= 30) color = context.getResources().getColor(R.color.colorCountValue60per);
                else if (countOfDay >= 20) color = context.getResources().getColor(R.color.colorCountValue0per);
                else if (countOfDay < 20) color = context.getResources().getColor(R.color.colorCountValue0per);
            } else {
                if (countOfDay >= 100) color = context.getResources().getColor(R.color.colorCountValue100per);
                else if (countOfDay >= 80) color = context.getResources().getColor(R.color.colorCountValue80per);
                else if (countOfDay >= 60) color = context.getResources().getColor(R.color.colorCountValue60per);
                else if (countOfDay >= 40) color = context.getResources().getColor(R.color.colorCountValue0per);
                else if (countOfDay < 40) color = context.getResources().getColor(R.color.colorCountValue0per);
            }

            holder.tvDayCount.setTextColor(color);

            holder.tvRetortText.setText(reportObject.getReportText());

            holder.ratingBarSoul.setRating(reportObject.getSoulRating());
            holder.ratingBarSoul.setIsIndicator(true);

            holder.ratingBarPhinance.setRating(reportObject.getPhinanceRating());
            holder.ratingBarPhinance.setIsIndicator(true);

            holder.ratingBarEnglish.setRating(reportObject.getEnglishRating());
            holder.ratingBarEnglish.setIsIndicator(true);

            holder.ratingBarSocial.setRating(reportObject.getSocialRating());
            holder.ratingBarSocial.setIsIndicator(true);

            holder.ratingBarFamilly.setRating(reportObject.getFamillyRating());
            holder.ratingBarFamilly.setIsIndicator(true);

            holder.ratingBarHealth.setRating(reportObject.getHealthRating());
            holder.ratingBarHealth.setIsIndicator(true);

            holder.itemView.setTag(reportObject.getId());
            holder.itemView.setOnClickListener((view)-> onItemClicked.onClick(view, position, reportObject.getId() ));
            holder.itemView.setOnLongClickListener((View view) -> {
                return onItemLongClicked.onLongClick(view, position, reportObject.getId());
            });


            if (reportObject.isWeekReport()){
                holder.cardView.setBackgroundColor(context.getResources().getColor(R.color.colorWhiteTransparent));
                holder.tvDate.setText("Week " + reportObject.getWeekNumber());
                holder.tvDayCountFieldName.setText("Week count");

            }
            else{
                String strDate = reportObject.getDate();



                DateFormat formatter ;
                Date date ;
                formatter = new SimpleDateFormat("dd.MM.yyyy");
                try {
                    date = (Date)formatter.parse(strDate);
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTime(date);
                    int dayOfYear = calendar.get(Calendar.DAY_OF_YEAR);
                    strDate = "Day " + dayOfYear + " ("+ strDate + ")";
                } catch (ParseException e) {
                    e.printStackTrace();
                }


                holder.tvDate.setText(strDate);

            }
        }
    }


    @Override
    public int getItemCount() {
        return ReportRealmController.getReportList().size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        CardView cardView;

        TextView tvDate;
        TextView tvDayCount;
        TextView tvRetortText;
        TextView tvDayCountFieldName;

        RatingBar ratingBarSoul;
        RatingBar ratingBarHealth;
        RatingBar ratingBarPhinance;
        RatingBar ratingBarEnglish;
        RatingBar ratingBarSocial;
        RatingBar ratingBarFamilly;

        LinearLayout llDivider;


        public ViewHolder(View itemView) {
            super(itemView);

            if(!ReportRealmController.getReportList().isEmpty()) {
                cardView = itemView.findViewById(R.id.item_card_view);
                tvDate = itemView.findViewById(R.id.tv_date);
                tvDayCount = itemView.findViewById(R.id.tv_count_value);
                tvRetortText = itemView.findViewById(R.id.tv_report_text);

                ratingBarSoul = itemView.findViewById(R.id.ratingbar_soul);
                ratingBarHealth = itemView.findViewById(R.id.ratingbar_health);
                ratingBarPhinance = itemView.findViewById(R.id.ratingbar_phinance);
                ratingBarEnglish = itemView.findViewById(R.id.ratingbar_english);
                ratingBarSocial = itemView.findViewById(R.id.ratingbar_social);
                ratingBarFamilly = itemView.findViewById(R.id.ratingbar_familly);

                tvDayCountFieldName = itemView.findViewById(R.id.tv_count_field_name);
                llDivider = itemView.findViewById(R.id.ll_divider);

            }
        }
    }
}