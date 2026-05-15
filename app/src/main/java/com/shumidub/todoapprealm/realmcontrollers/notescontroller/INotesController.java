package com.shumidub.todoapprealm.realmcontrollers.notescontroller;

import com.shumidub.todoapprealm.realmmodel.notes.FolderNotesObject;

import io.realm.RealmList;

/**
 * Created by Артем on 28.01.2018.
 *
 */

interface INotesController {

    //FolderNotesContainer
    static RealmList<FolderNotesObject> getFolderNotesContainerList(){return null;}

    //FolderNotes

    static FolderNotesObject getFolderNote() {
        return null;
    }

    static long addFolderNote(String name){return 0;}

    static void editFolderNote(long id, String name){}

    static void delFolderNote(long id){}

    /** container is one */
    static void reorderFolderNote(long idFolderNotesObject, int from, int to){}

    static long getNewValidFolderNotesId() {
        return System.currentTimeMillis();
    }

    //Notes

    static void getNotesList(long idFolderNotesObject){}

    static void getNote(long idNotesObject){}

    static long addNote(String text){return 0;}

    static void editNote(long idNotesObject, String text ){}

    static void delNote(long idNotesObject){}

    static void reorderNote(long idFolderNotesObject, long idNotesObject, int from, int to){}

    static long getNewValidNotesId() {
        return System.currentTimeMillis();
    }

}
