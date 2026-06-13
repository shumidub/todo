package com.shumidub.todoapprealm.realmcontrollers.reportcontroller;

import com.shumidub.todoapprealm.App;
import com.shumidub.todoapprealm.realmcontrollers.RealmDb;
import com.shumidub.todoapprealm.realmmodel.report.ReportObject;

import java.util.List;

public class ReportRealmController  {


    public static List<ReportObject> getReportList() {
        RealmDb.realm();
        return App.realmFoldersContainer.reportObjectList;
    }


    public static ReportObject getReport(long id) {
        return RealmDb.findById(ReportObject.class, id);
    }


    public static long addReport(String date, int dayCount, String textReport,
                                 int soulRating, int healthRating, int phinanceRating,
                                 int englishRating, int socialRating, int famillyRating,
                                 boolean isWeekReport, int weekNumber) {

        long id = RealmDb.newUniqueId(ReportObject.class);

        RealmDb.write(() -> {
            ReportObject reportObject = RealmDb.realm().createObject(ReportObject.class);
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

            App.realmFoldersContainer.reportObjectList.add(reportObject);
        });
        return id;
    }


    public static void editReport(long id, String date, int dayCount, String textReport,
                                  int soulRating, int healthRating, int phinanceRating,
                                  int englishRating,int socialRating, int famillyRating,
                                  int weekNumber) {
        ReportObject reportObject = getReport(id);
        RealmDb.write(() -> {
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
        ReportObject reportObject = getReport(id);
        RealmDb.write(() -> {
            App.realmFoldersContainer.reportObjectList.remove(reportObject);
            reportObject.deleteFromRealm();
            RealmDb.realm().where(ReportObject.class).equalTo("id", id).findAll().deleteAllFromRealm();
        });
    }
}
