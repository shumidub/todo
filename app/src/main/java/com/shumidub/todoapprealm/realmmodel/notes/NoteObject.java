package com.shumidub.todoapprealm.realmmodel.notes;

import io.realm.RealmObject;

/**
 * Created by Артем on 19.12.2017.
 *
 */

public class NoteObject extends RealmObject {

    private long id;
    private long idFolder;
    private String text;

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

    public long getIdFolder() {
        return idFolder;
    }

    public void setIdFolder(long idFolder) {
        this.idFolder = idFolder;
    }
}
