package com.shumidub.todoapprealm.realmmodel.task;

import io.realm.RealmObject;
import io.realm.annotations.RealmClass;

/**
 * Per-category placement of a {@link TaskObject} (task-004 / SCHEMA_VERSION 6).
 *
 * <p>A task may belong to several folders ({@link TaskObject#getTaskFolderId() primary} +
 * {@code extraFolderIds}). Before this type its single {@code position}/{@code sectionId} were
 * shared across every folder, so a task could not be ordered or sectioned independently per
 * category. A {@code TaskPlacement} records, <b>for one folder</b>, where the task sits:
 * its {@code position} in that folder and which of that folder's sections it lives in
 * ({@code sectionId == 0} = free zone).
 *
 * <p><b>Embedded</b>: owned by its parent {@link TaskObject#getPlacements()} list, so it is
 * deleted with the task and travels inside the task when the container graph is serialized for
 * backup/sync (Gson) — per-category order survives a restore.
 *
 * <p><b>Lazy invariant</b> (see {@code TasksRealmController}): single-category tasks carry no
 * placements and fall back to the legacy {@code position}/{@code sectionId}; placements are
 * materialized (one per assigned folder) only when a task becomes multi-category. So existing
 * data needs no migration backfill — behavior is unchanged until a task is multi-category.
 */
@RealmClass(embedded = true)
public class TaskPlacement extends RealmObject {

    private long folderId;
    private int position;
    private long sectionId;

    public long getFolderId() { return folderId; }
    public void setFolderId(long folderId) { this.folderId = folderId; }

    public int getPosition() { return position; }
    public void setPosition(int position) { this.position = position; }

    public long getSectionId() { return sectionId; }
    public void setSectionId(long sectionId) { this.sectionId = sectionId; }
}
