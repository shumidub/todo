package com.shumidub.todoapprealm.realmmodel.task;

import io.realm.RealmObject;
import io.realm.annotations.Index;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.Required;

/**
 * Realm model for category sections (task-002).
 *
 * <p>NOTE (Wave 1 / task-003): This pure model class is created here so the Realm
 * schema generator sees a Java class matching the {@code SectionObject} schema
 * entry added in {@link com.shumidub.todoapprealm.RealmMigrations}'s
 * {@code oldVersion < 4} block. All controller logic, UI integration, and
 * sectioning behaviour live in task-002 (Wave 2). Field names and types are
 * frozen by the migration: do not rename or change types without bumping
 * SCHEMA_VERSION.
 */
public class SectionObject extends RealmObject {

    @PrimaryKey
    private long id;

    @Required
    private String name;

    private boolean collapsedByDefault;

    private boolean currentlyCollapsed;

    @Index
    private long parentFolderId;

    private int position;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public boolean isCollapsedByDefault() { return collapsedByDefault; }
    public void setCollapsedByDefault(boolean collapsedByDefault) { this.collapsedByDefault = collapsedByDefault; }

    public boolean isCurrentlyCollapsed() { return currentlyCollapsed; }
    public void setCurrentlyCollapsed(boolean currentlyCollapsed) { this.currentlyCollapsed = currentlyCollapsed; }

    public long getParentFolderId() { return parentFolderId; }
    public void setParentFolderId(long parentFolderId) { this.parentFolderId = parentFolderId; }

    public int getPosition() { return position; }
    public void setPosition(int position) { this.position = position; }
}
