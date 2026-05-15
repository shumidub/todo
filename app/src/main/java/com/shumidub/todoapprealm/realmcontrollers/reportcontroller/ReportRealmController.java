package com.shumidub.todoapprealm.realmcontrollers.reportcontroller;

import android.util.Log;

import com.shumidub.todoapprealm.App;
import com.shumidub.todoapprealm.realmmodel.report.ReportObject;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static com.shumidub.todoapprealm.App.realm;

public class ReportRealmController  {


    public static List<ReportObject> getReportList() {
        App.initRealm();
        return App.realmFoldersContainer.reportObjectList;
//        return App.realm.where(ReportObject.class).findAllSorted("id");
    }


    public static ReportObject getReport(long id) {
        App.initRealm();
        return App.realm.where(ReportObject.class).equalTo("id", id).findFirst();
    }


    public static long addReport(String date, int dayCount, String textReport,
                                 int soulRating, int healthRating, int phinanceRating,
                                 int englishRating, int socialRating, int famillyRating,
                                 boolean isWeekReport, int weekNumber) {

        long id;

        if (isWeekReport){
            id = getValidIdForWeek();
        }
        else{
            id = getValidId();
        }

        App.initRealm();
        App.realm.executeTransaction((realm -> {
            ReportObject reportObject = App.realm.createObject(ReportObject.class);
            reportObject.setId(id);
            reportObject.setDate(date);
            reportObject.setCountOfDay(dayCount);
            reportObject.setReportText(textReport);

            reportObject.setSoulRating(soulRating);
            reportObject.setHealthRating(healthRating);
            reportObject.setPhinanceRating(phinanceRating);
            reportObject.setEnglishRating(englishRating);
            reportObject.setSocialRating(socialRating);
            reportObject.setFamillyRating(famillyRating);

            reportObject.setWeekReport(isWeekReport);
            reportObject.setWeekNumber(weekNumber);


            Log.d("DTAG", "addReport: ");
            
//            ??????
//            App.realm.insert(reportObject);

            App.realmFoldersContainer.reportObjectList.add(reportObject);


        }));
        return id;
    }


    public static void editReport(long id, String date, int dayCount, String textReport,
                                  int soulRating, int healthRating, int phinanceRating,
                                  int englishRating,int socialRating, int famillyRating,
                                  int weekNumber) {
        App.initRealm();
        ReportObject reportObject = App.realm.where(ReportObject.class).equalTo("id", id).findFirst();
        App.realm.executeTransaction((realm)->{
            reportObject.setDate(date);
            reportObject.setCountOfDay(dayCount);
            reportObject.setReportText(textReport);

            reportObject.setSoulRating(soulRating);
            reportObject.setHealthRating(healthRating);
            reportObject.setPhinanceRating(phinanceRating);
            reportObject.setEnglishRating(englishRating);
            reportObject.setSocialRating(socialRating);
            reportObject.setFamillyRating(famillyRating);

            reportObject.setWeekNumber(weekNumber);
        });
    }

    public static void delReport(long id) {
        Log.d("DTAG", "delReport: ");
        App.initRealm();
        ReportObject reportObject = App.realm.where(ReportObject.class).equalTo("id", id).findFirst();

        if (App.realm.isInTransaction()){
            App.realmFoldersContainer.reportObjectList.remove(reportObject);
            reportObject.deleteFromRealm();
            App.realm.where(ReportObject.class).equalTo("id", id).findAll().deleteAllFromRealm();

        } else {

            realm.executeTransaction((transaction) -> {
                App.realmFoldersContainer.reportObjectList.remove(reportObject);
                reportObject.deleteFromRealm();
                App.realm.where(ReportObject.class).equalTo("id", id).findAll().deleteAllFromRealm();
            });
        }

    }

    private static long getValidId() {
        long validId = System.currentTimeMillis();
        App.initRealm();
        while ( App.realm.where(ReportObject.class)
                .equalTo("id", validId).findFirst() != null){
            validId ++;
        }
        return validId;
    }


    private static long getValidIdForWeek() {
        int week = Calendar.getInstance().get(Calendar.WEEK_OF_YEAR);
        long validId = System.currentTimeMillis();
        App.initRealm();
        while ( App.realm.where(ReportObject.class)
                .equalTo("id", validId).findFirst() != null){
            validId ++;
        }
        return validId;
    }
}
