package com.shumidub.todoapprealm.realmmodel.task;



import com.shumidub.todoapprealm.realmmodel.RealmInteger;

import io.realm.RealmList;
import io.realm.RealmObject;

/**
 * Created by Артем on 19.12.2017.
 */

public class TaskObject extends RealmObject {

    private long id;
    private String text;
    private boolean done; // done or note completelly
    private long taskFolderId;
    private int priority;
    private int lastDoneDate; //
    private boolean isCycling;
    private int countValue; //
    private int maxAccumulation; //
    private int countAccumulation; //
    private RealmList<RealmInteger> dateCountAccumulation;


    public int getCountAccumulation() {
        return countAccumulation;
    }

    public void setCountAccumulation(int countAccumulation) {
        this.countAccumulation = countAccumulation;
    }

    public int getMaxAccumulation() {
        return maxAccumulation;
    }

    public void setMaxAccumulation(int maxAccumulation) {
        this.maxAccumulation = maxAccumulation;
    }

    public  RealmList<RealmInteger> getDateCountAccumulation() {
        return dateCountAccumulation;
    }

    public void clearDateCountAccumulation() {
        setCountAccumulation(0);
        dateCountAccumulation.clear();
    }

    public void addDateCountAccumulation(int lastDateCount) {
        if (dateCountAccumulation.size()<maxAccumulation){
            RealmInteger realmInteger = new RealmInteger();
            realmInteger.setMyInteger(lastDateCount);
            dateCountAccumulation.add(realmInteger);
        }
        setCountAccumulation(dateCountAccumulation.size());
    }

    public long getId() {return id;}
    public void setId(long id) {
        this.id = id;
    }

    public String getText() {
        return text;
    }
    public void setText(String text) {
        this.text = text;
    }

    public boolean isDone() {
        return done;
    }
    public void setDone(boolean done) {
        this.done = done;
    }

    public long getTaskFolderId() {return taskFolderId; }
    public void setTaskFolderId(long taskFolderId) {this.taskFolderId = taskFolderId;}

    public int getLastDoneDate() { return lastDoneDate;}
    public void setLastDoneDate(int lastDoneDate) {this.lastDoneDate = lastDoneDate;}

    public boolean isCycling() {
        return isCycling;
    }

    public void setCycling(boolean cycling) {
        isCycling = cycling;}

    public int getCountValue() {
        return countValue;
    }

    public void setCountValue(int countValue) {
        this.countValue = countValue;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

}
