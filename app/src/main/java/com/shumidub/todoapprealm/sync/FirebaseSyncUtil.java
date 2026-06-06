package com.shumidub.todoapprealm.sync;

import android.app.Activity;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.gson.Gson;
import com.shumidub.todoapprealm.App;
import com.shumidub.todoapprealm.realmmodel.RealmFoldersContainer;

import java.util.HashMap;
import java.util.Map;

/**
 * Uploads the whole notes/tasks/folders container to Firebase Realtime Database
 * under {@code users/{uid}/backup}. Requires an authenticated (email/password) user.
 *
 * <p>Reuses the same Gson serialization as {@link JsonSyncUtil#realmBdToJson()} — the
 * detached Realm copy is turned into JSON, then re-parsed into a plain Map/List tree so
 * the data lands as a readable structure in the Realtime Database (not a single string).
 */
public class FirebaseSyncUtil {

    private static final String TAG = "FirebaseSyncUtil";

    /** Result callback, always delivered on the main thread (Firebase default). */
    public interface Callback {
        void onResult(boolean ok, String message);
    }

    private final Activity activity;

    public FirebaseSyncUtil(Activity activity) {
        this.activity = activity;
    }

    public boolean isSignedIn() {
        return FirebaseAuth.getInstance().getCurrentUser() != null;
    }

    public String currentEmail() {
        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
        return u == null ? null : u.getEmail();
    }

    /** Sign the current user out (used by the "change account" action). */
    public void signOut() {
        FirebaseAuth.getInstance().signOut();
    }

    /** Serialize the local DB and write it under the signed-in user's node. */
    public void uploadAll(Callback cb) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            cb.onResult(false, "Not signed in");
            return;
        }

        App.initRealm();
        RealmFoldersContainer container =
                App.realm.where(RealmFoldersContainer.class).findFirst();
        if (container == null) {
            cb.onResult(false, "Nothing to upload");
            return;
        }

        final Object tree;
        try {
            Gson gson = new Gson();
            String json = gson.toJson(App.realm.copyFromRealm(container));
            // Re-parse into Map/List/primitives so RTDB stores a structured, readable tree.
            tree = gson.fromJson(json, Object.class);
        } catch (Exception e) {
            Log.e(TAG, "serialize failed", e);
            cb.onResult(false, "Serialize error: " + e.getMessage());
            return;
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("backup", tree);
        payload.put("updatedAt", ServerValue.TIMESTAMP);

        FirebaseDatabase.getInstance()
                .getReference("users")
                .child(user.getUid())
                .updateChildren(payload)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        cb.onResult(true, "Uploaded to Firebase");
                    } else {
                        String msg = task.getException() == null
                                ? "Upload failed"
                                : task.getException().getMessage();
                        Log.e(TAG, "upload failed: " + msg);
                        cb.onResult(false, msg);
                    }
                });
    }

    /**
     * Read {@code users/{uid}/backup} from Firebase and restore it into the local DB
     * (replacing current data — same semantics as the JSON restore). On success the
     * activity is restarted by {@link JsonSyncUtil#realmBdFromJsonString(String)}.
     */
    public void downloadAll(Callback cb) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            cb.onResult(false, "Not signed in");
            return;
        }

        FirebaseDatabase.getInstance()
                .getReference("users")
                .child(user.getUid())
                .child("backup")
                .get()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        String msg = task.getException() == null
                                ? "Download failed"
                                : task.getException().getMessage();
                        Log.e(TAG, "download failed: " + msg);
                        cb.onResult(false, msg);
                        return;
                    }
                    Object tree = task.getResult() == null ? null : task.getResult().getValue();
                    if (tree == null) {
                        cb.onResult(false, "No backup in Firebase");
                        return;
                    }
                    final String json;
                    try {
                        json = new Gson().toJson(tree);
                    } catch (Exception e) {
                        Log.e(TAG, "deserialize failed", e);
                        cb.onResult(false, "Parse error: " + e.getMessage());
                        return;
                    }
                    try {
                        // Performs the Realm write + activity restart and shows its own toast.
                        new JsonSyncUtil(activity).realmBdFromJsonString(json);
                    } catch (Exception e) {
                        Log.e(TAG, "restore failed", e);
                        cb.onResult(false, "Restore error: " + e.getMessage());
                    }
                });
    }
}
