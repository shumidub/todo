package com.shumidub.todoapprealm.realmmodel.notes;

import com.shumidub.todoapprealm.realmmodel.task.TaskObject;

import io.realm.RealmList;
import io.realm.RealmObject;

/**
 * Created by Артем on 24.12.2017.
 *
 */

public class FolderNotesObject extends RealmObject {

    private String name;
    private long id;
    public RealmList<NoteObject> notesObjectRealmList;

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public long getId() {
        return id;
    }
    public void setId(long id) {
        this.id = id;
    }

    //todo need refactor getNotes
    public RealmList<NoteObject> getTasks() {
        return notesObjectRealmList;
    }
    public void setTasks(RealmList<TaskObject> tasks) {
        this.notesObjectRealmList = notesObjectRealmList;
    }
}
