package com.shumidub.todoapprealm.realmcontrollers;

import com.shumidub.todoapprealm.App;

import io.realm.Realm;
import io.realm.RealmModel;

/**
 * Single entry point for the controllers' Realm access. Hides two invariants
 * every controller used to re-implement (inconsistently):
 *
 * 1. The Realm instance must be initialised before use — callers no longer
 *    sprinkle App.initRealm() in front of every query.
 * 2. Writes must work both standalone and when the caller is already inside
 *    a transaction (Realm throws on nested executeTransaction). Only two of
 *    the old controller methods guarded against this; write() guards always.
 */
public final class RealmDb {

    private RealmDb() {}

    /** The app's UI-thread Realm, initialised on first use. */
    public static Realm realm() {
        App.initRealm();
        return App.realm;
    }

    /** Runs {@code body} in a transaction; reuses the surrounding one if already inside. */
    public static void write(Runnable body) {
        Realm r = realm();
        if (r.isInTransaction()) {
            body.run();
        } else {
            r.executeTransaction(tx -> body.run());
        }
    }

    /** Same as {@link #write(Runnable)} for bodies that want the Realm handle. */
    public static void write(Realm.Transaction body) {
        Realm r = realm();
        if (r.isInTransaction()) {
            body.execute(r);
        } else {
            r.executeTransaction(body);
        }
    }

    /** First object of {@code type} whose "id" field equals {@code id}, or null. */
    public static <T extends RealmModel> T findById(Class<T> type, long id) {
        return realm().where(type).equalTo("id", id).findFirst();
    }

    /** Unique id for a new object of {@code type}: current time, bumped past collisions. */
    public static <T extends RealmModel> long newUniqueId(Class<T> type) {
        long id = System.currentTimeMillis();
        while (findById(type, id) != null) id++;
        return id;
    }
}
