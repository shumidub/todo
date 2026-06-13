package com.shumidub.todoapprealm;

/**
 * Single source of truth for the main pager layout.
 *
 * Pages: 0 = old Notes (FolderNoteFragment), 1..4 = task tabs.
 * Task groups: 0 = Tasks1 (default chrome), 1 = Tasks2 (Cornflower),
 * 2 = Tasks3 (Canary), 3 = Notes (Indigo). Group N lives on page N+1.
 *
 * Adding a tab: bump GROUP_COUNT/PAGE_COUNT, add a list field +
 * migration in RealmFoldersContainer.tasksListForGroup, add a palette
 * case in ui/theme/Palette.forGroup.
 */
public final class Tabs {

    public static final int PAGE_COUNT = 5;
    public static final int GROUP_COUNT = 4;

    /** Page the app opens on (Tasks1). */
    public static final int START_PAGE = 1;

    private Tabs() {}

    /** Task group hosted on a pager page, or -1 for non-task pages (old Notes). */
    public static int groupForPosition(int position) {
        return (position >= 1 && position < PAGE_COUNT) ? position - 1 : -1;
    }

    /** Pager page hosting a task group. */
    public static int positionForGroup(int group) {
        return group + 1;
    }
}
