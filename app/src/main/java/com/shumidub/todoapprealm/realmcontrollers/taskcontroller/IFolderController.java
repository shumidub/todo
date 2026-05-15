package com.shumidub.todoapprealm.realmcontrollers.taskcontroller;

/**
 * Created by Артем on 28.01.2018.
 */

public interface IFolderController {

    static long getIdForNextValue() {
        return System.currentTimeMillis();
    }

}
