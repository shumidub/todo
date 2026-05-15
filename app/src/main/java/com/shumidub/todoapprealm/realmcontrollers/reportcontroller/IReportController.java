package com.shumidub.todoapprealm.realmcontrollers.reportcontroller;

import com.shumidub.todoapprealm.realmmodel.report.ReportObject;

import java.util.List;

public interface IReportController {

    List<ReportObject> getReportList();

    void getReport(long id);

    /** return id creating report, private method*/
    long addReport(long id, String date, int dayCount, String textReport);

    /** public method*/
    long addReport(int dayCount, String textReport);

    void editReport(long id, String date, int dayCount, String textReport);

    void delReport(long id);

    /** return unique id*/
    long getValidId();

}
