package com.shumidub.todoapprealm.realmmodel;

import io.realm.RealmObject;

/**
 * Created by Артем on 14.01.2018.
 */

public class RealmInteger extends RealmObject {

    private int myInteger;

    public int getMyInteger() {
        return myInteger;
    }

    public void setMyInteger(int myInteger) {
        this.myInteger = myInteger;
    }
}
