package com.shumidub.todoapprealm.realmcontrollers.notescontroller;


import android.util.Log;

import com.shumidub.todoapprealm.App;
import com.shumidub.todoapprealm.realmmodel.notes.FolderNotesObject;
import com.shumidub.todoapprealm.realmmodel.notes.NoteObject;

import io.realm.RealmList;

public class FolderNotesRealmController implements INotesController {

    //FolderNotesContainer
    public static RealmList<FolderNotesObject> getFolderNotesContainerList(){return null;}

    //FolderNotes

    public static FolderNotesObject getFolderNote(long id) {
        App.initRealm();
        return App.realm.where(FolderNotesObject.class).equalTo("id", id).findFirst();
    }

    public static long addFolderNote(String name){
        long id = getNewValidFolderNotesId();
        App.initRealm();
        App.realm.executeTransaction((realm -> {
            FolderNotesObject folder = App.realm.createObject(FolderNotesObject.class);
            folder.setId(id);
            folder.setName(name);
            App.folderOfNotesContainerList.add(folder);
        }));
        return id;
    }

    public static void editFolderNote(long id, String name){
        App.initRealm();
        App.realm.executeTransaction((r) -> {
            App.realm.where(FolderNotesObject.class).equalTo("id", id).findFirst().setName(name);
        });

    }

    public static void delFolderNote(long id){
        App.initRealm();

        RealmList<NoteObject> realmList = getFolderNote(id).getTasks();



        Log.d("DTAG77777", "delFolderNote: size = " + realmList.size());

        App.initRealm();
        if (App.realm.isInTransaction()){
            realmList.deleteAllFromRealm();
            App.realm.where(FolderNotesObject.class).equalTo("id", id).findAll().deleteAllFromRealm();
        } else {
            App.realm.executeTransaction((r) -> {
                realmList.deleteAllFromRealm();
                App.realm.where(FolderNotesObject.class).equalTo("id", id).findAll().deleteAllFromRealm();
            });
        }
    }


    public static void reorderFolderNote(int from, int to){
        App.initRealm();
        App.realm.executeTransaction((r) -> {
            App.folderOfNotesContainerList.add(to, App.folderOfNotesContainerList.remove(from));
        });
    }

    public static long getNewValidFolderNotesId() {
        long id =  System.currentTimeMillis();
        App.initRealm();
        while ((App.realm.where(FolderNotesObject.class).equalTo("id", id)).findFirst()!=null){
            id ++;
        }
        return id;
    }

    //Notes

    public static RealmList<NoteObject> getNotesList(long idFolderNotesObject){
        return getFolderNote(idFolderNotesObject).getTasks();
    }

    public static NoteObject getNote(long idNotesObject){
        App.initRealm();
        return App.realm.where(NoteObject.class).equalTo("id", idNotesObject).findFirst();
    }

    public static long addNote(long idFolderNotesObject, String text){
        long id = getNewValidNotesId();
        App.initRealm();
        FolderNotesObject folderNotesObject
                = App.realm.where(FolderNotesObject.class)
                .equalTo("id", idFolderNotesObject).findFirst();
        App.realm.executeTransaction((realm -> {
            NoteObject noteObject = App.realm.createObject(NoteObject.class);
//            NoteObject noteObject = new NoteObject();
            noteObject.setId(id);
            noteObject.setText(text);
            noteObject.setIdFolder(idFolderNotesObject);
            folderNotesObject.getTasks().add(noteObject);



            Log.d("DTAG77777", "addNote:= " + App.realm.where(NoteObject.class).equalTo("id",noteObject.getId()).findAll().size());


        }));
        return id;
    }

    public static void editNote(long idNotesObject, String text ){
        App.initRealm();
        App.realm.executeTransaction((r) -> {
            App.realm.where(NoteObject.class).equalTo("id", idNotesObject)
                    .findFirst().setText(text);
        });
    }

    public static void delNote(long idNotesObject){
        App.initRealm();
        App.realm.executeTransaction((r) -> {

            NoteObject noteObject = App.realm.where(NoteObject.class).equalTo("id", idNotesObject).findFirst();

            long idFolderObject = noteObject.getIdFolder();
            FolderNotesObject folderNotesObject = App.realm.where(FolderNotesObject.class).equalTo("id", idFolderObject).findFirst();

            folderNotesObject.getTasks().remove(noteObject);

            App.realm.where(NoteObject.class).equalTo("id", idNotesObject)
                    .findFirst().deleteFromRealm();
        });
    }

    public static void reorderNote(long idFolderNotesObject, long idNotesObject, int from, int to){
        App.initRealm();
        RealmList <NoteObject> notesList
                = App.realm.where(FolderNotesObject.class)
                .equalTo("id", idFolderNotesObject).findFirst().getTasks();
        App.realm.executeTransaction((r) -> {
            notesList.add(to, notesList.remove(from));
        });
    }

    public static long getNewValidNotesId() {
        long id =  System.currentTimeMillis();
        App.initRealm();
        while ((App.realm.where(NoteObject.class).equalTo("id", id)).findFirst()!=null){
            id ++;
        }
        return id;
    }
}
