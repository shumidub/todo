package com.shumidub.todoapprealm.realmcontrollers.notescontroller;


import com.shumidub.todoapprealm.App;
import com.shumidub.todoapprealm.realmcontrollers.RealmDb;
import com.shumidub.todoapprealm.realmmodel.notes.FolderNotesObject;
import com.shumidub.todoapprealm.realmmodel.notes.NoteObject;

import io.realm.RealmList;

public class FolderNotesRealmController {

    //FolderNotes

    public static FolderNotesObject getFolderNote(long id) {
        return RealmDb.findById(FolderNotesObject.class, id);
    }

    public static long addFolderNote(String name){
        long id = RealmDb.newUniqueId(FolderNotesObject.class);
        RealmDb.write(() -> {
            FolderNotesObject folder = RealmDb.realm().createObject(FolderNotesObject.class);
            folder.setId(id);
            folder.setName(name);
            App.folderOfNotesContainerList.add(folder);
        });
        return id;
    }

    public static void editFolderNote(long id, String name){
        RealmDb.write(() -> getFolderNote(id).setName(name));
    }

    public static void delFolderNote(long id){
        RealmList<NoteObject> realmList = getFolderNote(id).getTasks();
        RealmDb.write(() -> {
            realmList.deleteAllFromRealm();
            RealmDb.realm().where(FolderNotesObject.class).equalTo("id", id).findAll().deleteAllFromRealm();
        });
    }


    public static void reorderFolderNote(int from, int to){
        RealmDb.write(() ->
                App.folderOfNotesContainerList.add(to, App.folderOfNotesContainerList.remove(from)));
    }

    public static long getNewValidFolderNotesId() {
        return RealmDb.newUniqueId(FolderNotesObject.class);
    }

    //Notes

    public static RealmList<NoteObject> getNotesList(long idFolderNotesObject){
        return getFolderNote(idFolderNotesObject).getTasks();
    }

    public static NoteObject getNote(long idNotesObject){
        return RealmDb.findById(NoteObject.class, idNotesObject);
    }

    public static long addNote(long idFolderNotesObject, String text){
        long id = RealmDb.newUniqueId(NoteObject.class);
        FolderNotesObject folderNotesObject = getFolderNote(idFolderNotesObject);
        RealmDb.write(() -> {
            NoteObject noteObject = RealmDb.realm().createObject(NoteObject.class);
            noteObject.setId(id);
            noteObject.setText(text);
            noteObject.setIdFolder(idFolderNotesObject);
            folderNotesObject.getTasks().add(noteObject);
        });
        return id;
    }

    public static void editNote(long idNotesObject, String text ){
        RealmDb.write(() -> getNote(idNotesObject).setText(text));
    }

    public static void delNote(long idNotesObject){
        RealmDb.write(() -> {
            NoteObject noteObject = getNote(idNotesObject);
            FolderNotesObject folderNotesObject = getFolderNote(noteObject.getIdFolder());
            folderNotesObject.getTasks().remove(noteObject);
            noteObject.deleteFromRealm();
        });
    }

    public static void reorderNote(long idFolderNotesObject, long idNotesObject, int from, int to){
        RealmList<NoteObject> notesList = getFolderNote(idFolderNotesObject).getTasks();
        RealmDb.write(() -> notesList.add(to, notesList.remove(from)));
    }

    public static long getNewValidNotesId() {
        return RealmDb.newUniqueId(NoteObject.class);
    }
}
