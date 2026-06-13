# Sync Subsystem — Jetpack Compose Migration Spec

**Scope:** JSON backup/restore, Firebase upload/download, share intent, email/password authentication.  
**Status:** Current behavior (Java/Fragment), mapping to Jetpack Compose.  
**Written:** 2026-06-13.

---

## 1. Current behavior (exhaustive, Java/Fragment)

### 1.1 Entry point & dialog

**File:** `ui/dialog/syncdialog/SyncDialog.java`  
**Trigger:** Menu → Sync option opens a `DialogFragment` with 6 action buttons.

**Button layout** (`res/layout/sync_dialog.xml`, lines 20–128):
1. **Account row** (line 21–32): TextView showing email or "Not signed in — tap to sign in"
   - Click → dismisses account, shows `FirebaseAuthDialog`, refreshes email on success
   - State: shown only if Firebase is configured (try/catch, line 118–124)
2. **Save as text** (line 41–51): Share intent with formatted text dump (not JSON)
3. **Backup to JSON** (line 60–70): Exports to Downloads folder via MediaStore/File API
4. **Export to Firebase** (line 79–89): Push current DB to `users/{uid}/backup` in RTDB
5. **Import from Firebase** (line 98–108): Pull `users/{uid}/backup` from RTDB, restore
6. **Restore from JSON** (line 117–128): SAF picker for user-selected JSON file

---

### 1.2 JSON export (backup)

**File:** `sync/JsonSyncUtil.java`, lines 36–54

```java
public void realmBdToJson() {
    GsonBuilder builder = new GsonBuilder().setPrettyPrinting();
    Gson gson = builder.create();
    String json = gson.toJson(
        App.realm.copyFromRealm(
            App.realm.where(RealmFoldersContainer.class).findFirst()
        )
    );
    FileWritter.saveFile(json);
    if (jsonIsExist()) {
        showToast("Saved to Download folder as REALM_BD_JSON.txt!");
    } else {
        showToast("Error!");
    }
}
```

**Flow:**
1. Get single `RealmFoldersContainer` (root model)
2. Detach via `copyFromRealm()` (removes live Realm tracking)
3. Serialize with **Gson (pretty-printed)**
4. Write via `FileWritter.saveFile()` (handles API 30+ MediaStore vs legacy File API)
5. Show toast

**JSON structure:**
```json
{
  "folderOfTasksList": [
    {
      "id": <long>,
      "name": <string>,
      "colorValue": <int>,
      "isDaily": <boolean>,
      "isVisible": <boolean>,
      "tasks": [
        {
          "id": <long>,
          "text": <string>,
          "done": <boolean>,
          "taskFolderId": <long>,
          "priority": <int>,
          "lastDoneDate": <int>,
          "isCycling": <boolean>,
          "countValue": <int>,
          "maxAccumulation": <int>,
          "countAccumulation": <int>,
          "dateCountAccumulation": [{"myInteger": <int>}, ...],
          "extraFolderIds": [<long>, ...],  /* nullable; absent in pre-multicategory backups */
          "sectionId": <long>,  /* 0 = "free" task */
          "position": <int>
        },
        ...
      ]
    },
    ...
  ],
  "folderOfTasksList2": [...],  /* Tasks tab 2 */
  "folderOfTasksList3": [...],  /* Tasks tab 3 */
  "folderOfTasksList4": [...],  /* Notes-as-tasks tab */
  "folderOfNotesList": [
    {
      "id": <long>,
      "name": <string>,
      "colorValue": <int>,
      "tasks": [  /* NoteObject, not TaskObject */
        {
          "id": <long>,
          "text": <string>,
          "done": <boolean>,
          /* no task-specific fields */
        },
        ...
      ]
    },
    ...
  ],
  "reportObjectList": [
    {
      "id": <long>,
      "name": <string>,
      /* fields vary; ReportObject is legacy */
    },
    ...
  ]
}
```

**File destination:**
- **API 30+:** MediaStore.Downloads via `FileWritter.saveViaMediaStore()` (lines 58–98)
  - Filename: `REALM_BD_JSON.txt` (constant line 28)
  - MIME type: `text/plain` (line 29)
  - Creates or updates existing entry; marks pending during write, clears pending on success
  - Handles OS write access (no runtime permission needed on API 30+)
- **API 29–<30:** WRITE_EXTERNAL_STORAGE permission + legacy File API (lines 136–145)
- **API <29:** READ + WRITE_EXTERNAL_STORAGE + File API (lines 147–165)

**Overwrite behavior:**
- Existing file found via `findDownloadUri()` (lines 119–134): if file already exists, update in-place
- First call to backup creates the file; subsequent calls replace

---

### 1.3 JSON import (restore)

**File:** `sync/JsonSyncUtil.java`, lines 70–127

**Entry point 1: File picker (SAF)**
```java
public void realmBdFromJsonUri(Uri uri) {
    if (uri == null) {
        showToast("Backup not picked");
        return;
    }
    String json = readJsonFromUri(uri);
    if (TextUtils.isEmpty(json)) {
        showToast("Picked file is empty or unreadable");
        return;
    }
    realmBdFromJsonString(json);
}
```

**Entry point 2: Firebase (called after download)**
```java
// FirebaseSyncUtil.java line 144
new JsonSyncUtil(activity).realmBdFromJsonString(json);
```

**Restore logic** (lines 87–113):
```java
public void realmBdFromJsonString(String json) {
    if (TextUtils.isEmpty(json)) {
        showToast("Backup is empty");
        return;
    }
    GsonBuilder builder = new GsonBuilder().setPrettyPrinting();
    Gson gson = builder.create();
    App.initRealm();
    
    App.realm.executeTransaction((transaction) -> {
        ContainersRealmController.deleteFromRealmAllContainers();
        RealmFoldersContainer restored = gson.fromJson(json, RealmFoldersContainer.class);
        App.realm.insertOrUpdate(restored);
        normalizeExtraFolderIds();  /* ← ensure task.extraFolderIds is never null */
        Log.d("DTAG44444", "realm container count = " + ...);
    });
    
    App.rebindContainers();  /* ← re-point static container refs */
    MainActivity.refreshAfterRestore();  /* ← refresh live UI in place */
    showToast("Restored!");
}
```

**Backward compatibility:**
- `normalizeExtraFolderIds()` (lines 63–67): backups pre-dating multi-category support won't have `extraFolderIds` field; Gson leaves it null. Loop over all `TaskObject` in Realm and set to empty `RealmList<>()` if null.
- This ensures restore doesn't crash when old backups are loaded.

**File read** (lines 115–127):
- ContentResolver openInputStream on the Uri
- Read to ByteArrayOutputStream in 8KB chunks
- Return UTF-8 string or empty string on IOException

**UI refresh after restore** (`MainActivity.refreshAfterRestore()`, MainActivity.java:379–389):
```java
App.setDayScopeValue();  /* daily reset counter */
invalidateOptionsMenu();  /* redraw day counter */
for (Fragment f : getSupportFragmentManager().getFragments()) {
    if (f instanceof FolderSlidingPanelFragment) {
        f.reloadFromRealm();  /* refresh folder/task lists */
    } else if (f instanceof FolderNoteFragment) {
        f.setFolderNoteViews();  /* refresh notes */
    }
}
```

---

### 1.4 Firebase auth

**File:** `ui/dialog/firebase/FirebaseAuthDialog.java`

**Layout** (`res/layout/dialog_firebase_auth.xml`):
- Email TextInputEditText (line 17–22)
- Password TextInputEditText (line 34–39)
- Error message TextView (line 44–50, initially GONE)

**Flow:**
1. User enters email + password
2. Tap "Sign in" or "Register" button
3. `authenticate(register: boolean)` (lines 60–88):
   - Validate: email non-empty, password ≥6 chars (lines 63–70)
   - Disable buttons (line 71)
   - Call `FirebaseAuth.signInWithEmailAndPassword()` or `createUserWithEmailAndPassword()`
   - On success: callback `onAuth.onSignedIn()` + dismiss (line 79–80)
   - On failure: enable buttons, show error message (line 82–85)

**Error handling:**
- Button enabled/disabled state controlled via `setButtonsEnabled()` (lines 90–95)
- Messages shown in the error TextView, initially GONE (line 97–102)
- Dialog does NOT auto-dismiss on failure

---

### 1.5 Firebase sync

**File:** `sync/FirebaseSyncUtil.java`

**Auth checks:**
```java
public boolean isSignedIn() {
    return FirebaseAuth.getInstance().getCurrentUser() != null;
}

public String currentEmail() {
    FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
    return u == null ? null : u.getEmail();
}

public void signOut() {
    FirebaseAuth.getInstance().signOut();
}
```

**Upload (export)** (lines 55–101):
```java
public void uploadAll(Callback cb) {
    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
    if (user == null) {
        cb.onResult(false, "Not signed in");
        return;
    }
    
    App.initRealm();
    RealmFoldersContainer container = App.realm.where(...).findFirst();
    if (container == null) {
        cb.onResult(false, "Nothing to upload");
        return;
    }
    
    // Serialize: detach, JSON, re-parse as Object/Map/List tree
    Gson gson = new Gson();
    String json = gson.toJson(App.realm.copyFromRealm(container));
    Object tree = gson.fromJson(json, Object.class);  /* structured, not string */
    
    // Upload to RTDB
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
                cb.onResult(false, msg);
            }
        });
}
```

**Download (import)** (lines 108–150):
```java
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
                cb.onResult(false, msg);
                return;
            }
            Object tree = task.getResult() == null ? null : task.getResult().getValue();
            if (tree == null) {
                cb.onResult(false, "No backup in Firebase");
                return;
            }
            
            // Re-parse into JSON string
            final String json = new Gson().toJson(tree);
            
            // Restore via JsonSyncUtil (handles Realm write + UI refresh + toast)
            new JsonSyncUtil(activity).realmBdFromJsonString(json);
        });
}
```

**Flow in dialog** (`SyncDialog.runFirebase()`, lines 158–182):
```java
private void runFirebase(boolean isExport) {
    final MainActivity act = (MainActivity) getActivity();
    final androidx.fragment.app.FragmentManager fm = act.getSupportFragmentManager();
    
    final FirebaseSyncUtil firebase = new FirebaseSyncUtil(act);
    final Runnable action = () -> {
        if (isExport) firebase.uploadAll(done);
        else firebase.downloadAll(done);
    };
    
    if (firebase.isSignedIn()) {
        action.run();  /* signed in, go directly */
    } else {
        FirebaseAuthDialog auth = new FirebaseAuthDialog();
        auth.setOnAuth(action::run);  /* not signed in, auth first, then action */
        auth.show(fm, "fbauth");
    }
}
```

**Callback timing:** callbacks are on the main thread (Firebase default).

---

### 1.6 Share intent

**File:** `sync/LocalSyncUtil.java`

```java
public void putMessage(String msg) {
    Intent sendIntent = new Intent();
    sendIntent.setAction(Intent.ACTION_SEND);
    sendIntent.putExtra(Intent.EXTRA_TEXT, msg);
    sendIntent.setType("text/plain");
    activity.startActivity(sendIntent);
}

public void putAllRealmDbAsMessage() {
    putMessage(getRealmDbAsString());
}

private String getRealmDbAsString() {
    String message = "";
    String indent = "    ";
    String nextLine = "\n";
    
    // Format: sections, then all tasks/notes in plain text (not JSON)
    message = ">>>>> NOTES >>>>>" + nextLine + nextLine;
    for (FolderNotesObject folder : App.realm.where(RealmFoldersContainer.class).findFirst().folderOfNotesList) {
        message += indent + folder.getName() + " :" + nextLine + nextLine;
        for (NoteObject note : folder.getTasks()) {
            message += indent + indent + " --> " + note.getText() + nextLine + nextLine;
        }
        message += nextLine;
    }
    
    message += ">>>>> TASKS >>>>>" + nextLine + nextLine;
    for (FolderTaskObject folder : App.realm.where(...).folderOfTasksList) {
        message += indent + folder.getName() + (folder.isDaily() ? " " : " NOT_DAILY") + " :" + nextLine + nextLine;
        for (TaskObject task : folder.getTasks()) {
            if (!(task.isDone() && !task.isCycling())) {
                message += indent + " --> " + task.getText()
                    + nextLine + indent + indent + " count = " + task.getCountValue()
                    + nextLine + indent + indent + " maxAccum = " + task.getMaxAccumulation()
                    + nextLine + indent + indent + " priority = " + task.getPriority()
                    + nextLine + indent + indent + " cycling = " + task.isCycling()
                    + nextLine + nextLine;
            }
        }
        message += nextLine;
    }
    
    message += ">>>>> REPORTS >>>>>" + nextLine + nextLine;
    for (ReportObject report : App.realm.where(ReportObject.class).findAll()) {
        message += report.toString() + nextLine + nextLine;
    }
    
    return message;
}
```

**Note:** This is **NOT** JSON; it's a human-readable formatted dump. Opened via share chooser (email, messaging, etc.).

---

### 1.7 File access (SAF + ActivityResult)

**File:** `ui/activity/main/MainActivity.java`, lines 68–78

```java
private final ActivityResultLauncher<String[]> pickBackupLauncher =
        registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
            if (uri == null) return;
            new JsonSyncUtil(this).realmBdFromJsonUri(uri);
        });

public void pickBackupForRestore() {
    // MIME types: application/json, text/plain, text/*, */*
    // Works on any API level, no permission needed
    pickBackupLauncher.launch(new String[]{"application/json", "text/plain", "text/*", "*/*"});
}
```

**Flow:**
1. User clicks "Restore from JSON" in SyncDialog
2. Dialog cancels (line 73 in SyncDialog.java)
3. Activity calls `pickBackupForRestore()`
4. SAF picker opens, user selects a file
5. ActivityResult lambda calls `realmBdFromJsonUri(uri)` to read and restore

**Scoped storage (API 30+):**
- SAF doesn't require runtime permissions (system handles access)
- Even if a file wasn't created by this app (e.g., backup from a previous install), SAF picker lets the user grant access

---

## 2. State & data flow

### 2.1 State holders

| Component | State | Thread | Lifetime |
|-----------|-------|--------|----------|
| `RealmFoldersContainer` | Root model (folders, tasks, notes, reports) | Main thread (allowQueriesOnUiThread) | App lifetime (Realm instance open) |
| `FirebaseAuth` | Current user (email, uid) | Async callback → main thread | User action (sign-in/out) |
| `SyncDialog` | Dialog open/closed; doesn't hold Realm objects | UI thread | User dismissal |
| `FirebaseAuthDialog` | Email, password input; auth in progress | UI thread | User submits or cancels |
| `FileWritter` static | Backup file path/metadata (cached via query) | IO thread (ContentResolver) | Write only; read cached |

### 2.2 Data mutations

**Export (JSON write):**
1. Read-only: query `RealmFoldersContainer`, detach (no mutations)
2. Serialize Gson → UTF-8 JSON string
3. Write to MediaStore/File (blocking on IO thread, but called from UI)

**Import (JSON read + Realm write):**
1. Read from Uri/File → UTF-8 string (blocking)
2. Parse Gson → RealmFoldersContainer object (CPU-bound)
3. Realm transaction: delete all + insert restored container (Realm thread = main)
4. Re-bind static container refs
5. Refresh fragments (UI thread)

**Firebase upload:**
1. Read + detach (no mutations)
2. Serialize Gson → JSON → Object tree (CPU)
3. Firebase async: push to RTDB (network)
4. Callback on main thread with result

**Firebase download:**
1. Firebase async: fetch from RTDB (network)
2. Callback on main thread
3. Parse Gson → JSON string
4. Delegate to `JsonSyncUtil.realmBdFromJsonString()` (same as file restore)

### 2.3 Threading

| Operation | Thread | Blocking? |
|-----------|--------|-----------|
| `realmBdToJson()` | Main (UI) | Yes (~100ms for large DB) |
| `FileWritter.saveFile()` | Main (UI) | Yes (ContentResolver IO) |
| `readJsonFromUri()` | Main (UI) | Yes (ContentResolver IO) |
| `Gson.fromJson()` | Main (UI) | Yes (CPU-bound, ~100ms) |
| `App.realm.executeTransaction()` | Main (UI) | Yes (Realm only on main thread) |
| Firebase `uploadAll()` callback | Network → Main | No (async), callback on main |
| Firebase `downloadAll()` callback | Network → Main | No (async), callback on main |

**Known issue:** Export + import are blocking on the main thread. For large DBs (>10K items), this could lock the UI. Not fixed in current Java version; acceptable at current data scale.

---

## 3. Edge cases & gotchas

### 3.1 Empty states

**Empty RealmFoldersContainer:**
- `App.realm.where(RealmFoldersContainer.class).findFirst()` returns null if the container was never created
- Backup: `JsonSyncUtil.realmBdToJson()` will crash with NPE on `copyFromRealm(null)`
- **Mitigation:** Container is always created in `App.initContainers()` (lines 85–96)

**Empty backup file:**
- `realmBdFromJsonUri()` / `realmBdFromJsonString()` check `TextUtils.isEmpty(json)` and bail with toast

**No backup in Firebase:**
- `downloadAll()` checks `tree == null` (line 130) and returns "No backup in Firebase" (line 131)

### 3.2 Backward compatibility (extraFolderIds)

**Problem:** Backups created before multi-category support (when `TaskObject.extraFolderIds` field was added) won't have the field in JSON.

**Solution:** `normalizeExtraFolderIds()` (lines 63–67):
```java
private static void normalizeExtraFolderIds() {
    for (TaskObject t : App.realm.where(TaskObject.class).findAll()) {
        if (t.getExtraFolderIds() == null) {
            t.setExtraFolderIds(new RealmList<>());
        }
    }
}
```

**Called:** Inside the Realm transaction during restore (line 102), AFTER `insertOrUpdate()`.

**Guarantee:** All restored tasks will have a non-null `extraFolderIds` list, even if the backup is old.

### 3.3 Configuration changes & back-press

**SyncDialog:**
- `DialogFragment` survives configuration changes (system restores state)
- Back button: handled by `setOnKeyListener()` (lines 56–62) → do nothing (return true)
- Buttons programmatically dismiss the dialog after action (lines 73, 79, 87, 92)

**FirebaseAuthDialog:**
- Back button: not explicitly handled, uses default dismiss
- Buttons: "Sign in" and "Register" are disabled during auth attempt (line 71)
- If activity is destroyed mid-auth, the callback checks `!isAdded()` and bails (line 77)

**MainActivity:**
- SAF picker survives configuration changes (ActivityResultLauncher state is saved)
- If activity destroyed during picker, the result will fire on the new instance

### 3.4 Firebase auth state persistence

**Problem:** `FirebaseAuth.getInstance().getCurrentUser()` is only valid while the Firebase SDK is initialized and the user session is valid.

**Behavior:**
- User signs in, email is displayed in account row
- User closes the app; Firebase SDK persists the session locally
- App reopens: `currentEmail()` returns the persisted email
- User taps account row → `signOut()` clears it; next sign-in is fresh

**No logout on navigation:** Closing the SyncDialog does NOT sign out. Sign-out only happens if the user taps the account row.

### 3.5 Race: Firebase download + activity destruction

**Scenario:**
1. User clicks "Import from Firebase"
2. Dialog dismisses
3. Firebase download starts async
4. User closes the app
5. Callback fires after app is destroyed

**Mitigation in code:**
```java
// FirebaseSyncUtil line 76–80
task.addOnCompleteListener(t -> {
    if (!isAdded()) return;  /* DialogFragment check */
    if (t.isSuccessful()) {
        ...
    }
});
```

**Also:** `MainActivity.refreshAfterRestore()` checks if fragments exist (line 382–388) before calling methods on them.

### 3.6 Realm transaction during restore

**Problem:** `App.realm.executeTransaction()` is synchronous and blocks the main thread.

**In restore:**
```java
App.realm.executeTransaction((transaction) -> {
    ContainersRealmController.deleteFromRealmAllContainers();
    RealmFoldersContainer restored = gson.fromJson(json, RealmFoldersContainer.class);
    App.realm.insertOrUpdate(restored);
    normalizeExtraFolderIds();
});
```

**What happens if activity destroyed during transaction?** The transaction completes (Realm doesn't care about the activity state), but then `refreshAfterRestore()` checks if fragments are still attached.

### 3.7 SAF picker & old backups

**Problem:** A user may have backed up with an old app version, then restored that backup years later.

**Example timeline:**
1. 2018: User exports backup (no `extraFolderIds` field in tasks)
2. 2026: User updates app, tries to restore old backup
3. JSON is parsed, but `extraFolderIds` is null on each task
4. `normalizeExtraFolderIds()` loop runs, sets all to empty RealmList

**Guarantee:** Old backups stay restorable indefinitely.

### 3.8 Overwrite on backup

**Behavior:** If the user clicks "Backup to JSON" twice, the second backup overwrites the first in Downloads.

**File check:**
```java
// FileWritter line 60
Uri existing = findDownloadUri(ctx);
if (existing != null) {
    target = existing;  /* reuse */
} else {
    target = resolver.insert(...);  /* create new */
}
```

**Expected:** Two consecutive backups result in one file (the latest).

### 3.9 Firebase upload without network

**Behavior:**
```java
FirebaseDatabase.getInstance()
    .getReference("users")
    .child(user.getUid())
    .updateChildren(payload)
    .addOnCompleteListener(task -> {
        if (task.isSuccessful()) {
            cb.onResult(true, "Uploaded to Firebase");
        } else {
            cb.onResult(false, task.getException().getMessage());
        }
    });
```

**No timeout check:** If the device is offline, the callback will eventually fire with a network error (timeout is Firebase SDK default, ~30s). The callback shows the error message in a toast.

---

## 4. Compose mapping

### 4.1 Architecture overview

**From:** `MainActivity` + `SyncDialog` (DialogFragment) + `FirebaseAuthDialog` (DialogFragment)  
**To:** `MainActivity` (ComponentActivity) + `SyncScreen` (Composable) + `FirebaseAuthScreen` (Composable)

**State flow:**
```
MainScreen (Compose)
├─ SyncViewModel (Dagger-injected)
│  ├─ Firebase auth state (email, isSignedIn)
│  ├─ Sync operation state (loading, error, success)
│  └─ Dialog visibility
├─ SyncScreen Composable
│  ├─ Account row (shows email, on click → Firebase auth dialog)
│  ├─ 6 action buttons (local backup/restore, Firebase export/import, share text)
│  └─ Loading/error overlays
└─ FirebaseAuthScreen (nested state in ViewModel or local composable state)
   ├─ Email input
   ├─ Password input
   ├─ Error message
   └─ Sign in / Register buttons (with loading state)
```

### 4.2 ViewModel & state management

**New:** `SyncViewModel` (Kotlin, Dagger-injected)

```kotlin
@HiltViewModel
class SyncViewModel @Inject constructor(
    private val firebaseSyncUtil: FirebaseSyncUtil,  /* existing Java class, injected */
    private val jsonSyncUtil: JsonSyncUtil,
    private val localSyncUtil: LocalSyncUtil
) : ViewModel() {
    
    // Firebase auth state
    private val _currentEmail = MutableStateFlow<String?>(null)
    val currentEmail = _currentEmail.asStateFlow()
    
    private val _isSignedIn = MutableStateFlow(false)
    val isSignedIn = _isSignedIn.asStateFlow()
    
    // Sync operation state
    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState = _syncState.asStateFlow()
    
    // Dialog visibility
    private val _showAuthDialog = MutableStateFlow(false)
    val showAuthDialog = _showAuthDialog.asStateFlow()
    
    init {
        refreshAuthState()
    }
    
    private fun refreshAuthState() {
        _currentEmail.value = firebaseSyncUtil.currentEmail()
        _isSignedIn.value = firebaseSyncUtil.isSignedIn()
    }
    
    fun exportToJson() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                jsonSyncUtil.realmBdToJson()  /* blocking, on IO thread */
                _syncState.value = SyncState.Success("Saved to Download folder")
            } catch (e: Exception) {
                _syncState.value = SyncState.Error(e.message ?: "Export failed")
            }
        }
    }
    
    fun importFromJson(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                jsonSyncUtil.realmBdFromJsonUri(uri)
                _syncState.value = SyncState.Success("Restored!")
            } catch (e: Exception) {
                _syncState.value = SyncState.Error(e.message ?: "Import failed")
            }
        }
    }
    
    fun shareAsText() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                localSyncUtil.putAllRealmDbAsMessage()
            } catch (e: Exception) {
                _syncState.value = SyncState.Error(e.message ?: "Share failed")
            }
        }
    }
    
    fun uploadToFirebase() {
        viewModelScope.launch(Dispatchers.IO) {
            _syncState.value = SyncState.Loading
            firebaseSyncUtil.uploadAll { ok, message ->
                _syncState.value = if (ok) SyncState.Success(message) else SyncState.Error(message)
                refreshAuthState()
            }
        }
    }
    
    fun downloadFromFirebase() {
        viewModelScope.launch(Dispatchers.IO) {
            _syncState.value = SyncState.Loading
            firebaseSyncUtil.downloadAll { ok, message ->
                _syncState.value = if (ok) SyncState.Success(message) else SyncState.Error(message)
                refreshAuthState()
            }
        }
    }
    
    fun showAuthDialog() {
        _showAuthDialog.value = true
    }
    
    fun hideAuthDialog() {
        _showAuthDialog.value = false
        refreshAuthState()
    }
    
    fun signOut() {
        firebaseSyncUtil.signOut()
        refreshAuthState()
    }
}

sealed class SyncState {
    object Idle : SyncState()
    object Loading : SyncState()
    data class Success(val message: String) : SyncState()
    data class Error(val message: String) : SyncState()
}
```

### 4.3 Composables

**SyncScreen:**
```kotlin
@Composable
fun SyncScreen(
    viewModel: SyncViewModel = viewModel(factory = ...),
    onDismiss: () -> Unit,
    pickBackupForRestore: (MimeTypes: Array<String>) -> Unit  /* activity callback */
) {
    val currentEmail by viewModel.currentEmail.collectAsState()
    val isSignedIn by viewModel.isSignedIn.collectAsState()
    val syncState by viewModel.syncState.collectAsState()
    val showAuthDialog by viewModel.showAuthDialog.collectAsState()
    
    val dialogDismissRequest = { onDismiss() }
    
    AlertDialog(
        onDismissRequest = dialogDismissRequest,
        confirmButton = {
            Button(onClick = dialogDismissRequest) { Text("Cancel") }
        },
        title = { Text("Synchronization") },
        text = {
            LazyColumn(Modifier.fillMaxWidth()) {
                /* Account row */
                item {
                    AccountRow(
                        email = currentEmail,
                        isSignedIn = isSignedIn,
                        onClick = {
                            viewModel.signOut()
                            viewModel.showAuthDialog()
                        }
                    )
                }
                
                /* Save as text */
                item {
                    SyncButton(
                        text = "Save as text",
                        onClick = { viewModel.shareAsText() }
                    )
                }
                
                /* Backup to JSON */
                item {
                    SyncButton(
                        text = "Backup to JSON",
                        onClick = { viewModel.exportToJson() }
                    )
                }
                
                /* Export to Firebase */
                item {
                    SyncButton(
                        text = "Export to Firebase",
                        onClick = {
                            if (isSignedIn) {
                                viewModel.uploadToFirebase()
                            } else {
                                viewModel.showAuthDialog()
                            }
                        }
                    )
                }
                
                /* Import from Firebase */
                item {
                    SyncButton(
                        text = "Import from Firebase",
                        onClick = {
                            if (isSignedIn) {
                                viewModel.downloadFromFirebase()
                            } else {
                                viewModel.showAuthDialog()
                            }
                        }
                    )
                }
                
                /* Restore from JSON */
                item {
                    SyncButton(
                        text = "Restore from JSON",
                        onClick = {
                            pickBackupForRestore(
                                arrayOf("application/json", "text/plain", "text/*", "*/*")
                            )
                            dialogDismissRequest()
                        }
                    )
                }
            }
        }
    )
    
    if (showAuthDialog) {
        FirebaseAuthScreen(
            onDismiss = { viewModel.hideAuthDialog() },
            onSignedIn = { viewModel.hideAuthDialog() }
        )
    }
    
    /* Show toasts for syncState */
    when (syncState) {
        is SyncState.Success -> {
            LaunchedEffect(syncState) {
                /* show toast via context or callback */
            }
        }
        is SyncState.Error -> {
            LaunchedEffect(syncState) {
                /* show toast */
            }
        }
        else -> {}
    }
}

@Composable
fun AccountRow(
    email: String?,
    isSignedIn: Boolean,
    onClick: () -> Unit
) {
    Text(
        text = if (isSignedIn) "$email  (change)" else "Not signed in — tap to sign in",
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        textAlign = TextAlign.Center
    )
}

@Composable
fun SyncButton(
    text: String,
    onClick: () -> Unit
) {
    Text(
        text = text,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        textAlign = TextAlign.Center
    )
}
```

**FirebaseAuthScreen:**
```kotlin
@Composable
fun FirebaseAuthScreen(
    viewModel: FirebaseAuthViewModel = viewModel(factory = ...),
    onDismiss: () -> Unit,
    onSignedIn: () -> Unit
) {
    val email by viewModel.email.collectAsState()
    val password by viewModel.password.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = { viewModel.signIn() },
                enabled = !isLoading
            ) { Text("Sign in") }
        },
        neutralButton = {
            Button(
                onClick = { viewModel.register() },
                enabled = !isLoading
            ) { Text("Register") }
        },
        dismissButton = {
            Button(onClick = onDismiss) { Text("Cancel") }
        },
        title = { Text("Sign in to Firebase") },
        text = {
            Column(Modifier.fillMaxWidth()) {
                TextField(
                    value = email,
                    onValueChange = { viewModel.setEmail(it) },
                    label = { Text("Email") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )
                Spacer(Modifier.height(12.dp))
                TextField(
                    value = password,
                    onValueChange = { viewModel.setPassword(it) },
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation()
                )
                if (errorMessage.isNotEmpty()) {
                    Text(
                        text = errorMessage,
                        color = Color.Red,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    )
    
    if (isLoading) {
        /* Show loading overlay */
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }
}

@HiltViewModel
class FirebaseAuthViewModel @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {
    
    private val _email = MutableStateFlow("")
    val email = _email.asStateFlow()
    
    private val _password = MutableStateFlow("")
    val password = _password.asStateFlow()
    
    private val _errorMessage = MutableStateFlow("")
    val errorMessage = _errorMessage.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()
    
    fun setEmail(value: String) { _email.value = value }
    fun setPassword(value: String) { _password.value = value }
    
    fun signIn() {
        authenticate(register = false)
    }
    
    fun register() {
        authenticate(register = true)
    }
    
    private fun authenticate(register: Boolean) {
        val e = _email.value.trim()
        val p = _password.value
        
        if (e.isEmpty() || p.isEmpty()) {
            _errorMessage.value = "Enter email and password"
            return
        }
        if (p.length < 6) {
            _errorMessage.value = "Password must be at least 6 characters"
            return
        }
        
        _isLoading.value = true
        val task = if (register) {
            firebaseAuth.createUserWithEmailAndPassword(e, p)
        } else {
            firebaseAuth.signInWithEmailAndPassword(e, p)
        }
        
        task.addOnCompleteListener { t ->
            _isLoading.value = false
            if (t.isSuccessful) {
                /* callback to parent */
            } else {
                val msg = t.exception?.message ?: "Authentication failed"
                _errorMessage.value = msg
            }
        }
    }
}
```

### 4.4 Activity integration

**MainActivity (Kotlin, ComponentActivity):**
```kotlin
class MainActivity : ComponentActivity() {
    
    private val pickBackupLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val jsonSyncUtil = JsonSyncUtil(this)
            jsonSyncUtil.realmBdFromJsonUri(uri)
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                MainScreen(
                    pickBackupForRestore = { mimeTypes ->
                        pickBackupLauncher.launch(mimeTypes)
                    }
                )
            }
        }
    }
}

@Composable
fun MainScreen(
    pickBackupForRestore: (Array<String>) -> Unit
) {
    var showSyncDialog by remember { mutableStateOf(false) }
    
    HorizontalPager(/* ... */) {
        /* ... other pages ... */
    }
    
    if (showSyncDialog) {
        SyncScreen(
            onDismiss = { showSyncDialog = false },
            pickBackupForRestore = pickBackupForRestore
        )
    }
}
```

### 4.5 Threading & coroutines

**Blocking operations moved to `Dispatchers.IO`:**

```kotlin
fun exportToJson() {
    viewModelScope.launch(Dispatchers.IO) {
        jsonSyncUtil.realmBdToJson()  /* blocking JSON write */
        _syncState.value = SyncState.Success("...")
    }
}

fun importFromJson(uri: Uri) {
    viewModelScope.launch(Dispatchers.IO) {
        jsonSyncUtil.realmBdFromJsonUri(uri)  /* blocking file read + Gson parse */
        _syncState.value = SyncState.Success("Restored!")
    }
}
```

**Firebase callbacks:**
- Firebase delivers callbacks on the main thread (no change)
- No need to wrap in `withContext(Dispatchers.Main)`

### 4.6 Changes to Java utilities

**No changes required:**
- `JsonSyncUtil` stays as-is (Java class)
- `FirebaseSyncUtil` stays as-is (Java class)
- `LocalSyncUtil` stays as-is (Java class)
- `FileWritter` stays as-is (Java class)

**Small change (optional, for Kotlin idiom):**
- Wrap `FirebaseSyncUtil.Callback` in a Kotlin lambda extension for cleaner API

---

## 5. Files to delete once migrated

**When the Compose version is complete and all fragments are removed:**

### 5.1 Sync dialogs
- `app/src/main/java/com/shumidub/todoapprealm/ui/dialog/syncdialog/SyncDialog.java`
- `app/src/main/java/com/shumidub/todoapprealm/ui/dialog/firebase/FirebaseAuthDialog.java`
- `app/src/main/res/layout/sync_dialog.xml`
- `app/src/main/res/layout/dialog_firebase_auth.xml`

### 5.2 Activities (if fully converted to Compose)
- `app/src/main/java/com/shumidub/todoapprealm/ui/activity/main/MainActivity.java` (replaced by Kotlin ComponentActivity)

### 5.3 Other (not sync-specific, but removable with this phase)
- Nothing else in the sync subsystem scope

**Do NOT delete:**
- `sync/JsonSyncUtil.java` (Java, reused by Compose ViewModel)
- `sync/FirebaseSyncUtil.java` (Java, reused by Compose ViewModel)
- `sync/LocalSyncUtil.java` (Java, reused by Compose ViewModel)
- `sync/FileWritter.java` (Java, reused by Compose ViewModel)
- `realmmodel/RealmFoldersContainer.java` and task/note models (Realm schema stays)

---

## 6. Summary & risks

### 6.1 Edge cases found
**Total: 9**
1. NPE on null RealmFoldersContainer (mitigated: always created)
2. Empty JSON file (handled: TextUtils.isEmpty check)
3. No backup in Firebase (handled: null check)
4. `extraFolderIds` null in old backups (handled: normalizeExtraFolderIds loop)
5. Configuration changes (handled: DialogFragment state saved, ActivityResultLauncher persists)
6. Firebase auth state loss after app close (expected: Firebase SDK persists session)
7. Firebase download callback after activity destroyed (handled: `!isAdded()` check + fragment lifecycle awareness)
8. Realm transaction blocking main thread (trade-off: acceptable at current scale)
9. SAF picker with "ownerless" MediaStore files (handled: SAF abstracts access, no permissions needed)

### 6.2 Biggest migration risk
**Threading & state isolation:**
- Moving from blocking Java calls → async coroutines
- Care needed to ensure UI state (loading, error) updates correctly during async operations
- Firebase callbacks are still async, but need to coordinate with Compose state updates
- **Mitigation:** Use `viewModelScope` for lifecycle safety; test Firebase upload/download under network flakiness

### 6.3 Backward compatibility guarantee
**Old JSON backups will restore correctly** because:
- `normalizeExtraFolderIds()` runs post-restore, fixing null fields
- Gson is forgiving: fields absent in JSON are left null/default in the object
- The Realm schema version is NOT bumped
- Existing `.realm` DBs open without migration

