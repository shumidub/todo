package com.shumidub.todoapprealm.ui.fragment.task_section.small_tasks_fragment;

import com.shumidub.todoapprealm.realmmodel.task.SectionObject;
import com.shumidub.todoapprealm.realmmodel.task.TaskObject;

/**
 * Tagged union for the multi-view-type RecyclerView in {@link TasksRecyclerViewAdapter}
 * (task-002). One of {@link #task} / {@link #section} is non-null based on {@link #kind};
 * {@code DONE_FOOTER} carries neither. {@code RAIL_TOP}, {@code RAIL_BOTTOM} and
 * {@code SECTION_EMPTY} (sprint-002 task-002) carry the owning {@link #section} so drag-n-drop
 * can resolve the destination container when a task is dropped on a rail/empty item.
 */
public final class AdapterItem {

    public enum Kind { TASK, SECTION_HEADER, DONE_FOOTER, RAIL_TOP, RAIL_BOTTOM, SECTION_EMPTY }

    public final Kind kind;
    public final TaskObject task;
    public final SectionObject section;

    private AdapterItem(Kind k, TaskObject t, SectionObject s) {
        this.kind = k;
        this.task = t;
        this.section = s;
    }

    public static AdapterItem ofTask(TaskObject t) {
        return new AdapterItem(Kind.TASK, t, null);
    }

    public static AdapterItem ofSection(SectionObject s) {
        return new AdapterItem(Kind.SECTION_HEADER, null, s);
    }

    public static AdapterItem doneFooter() {
        return new AdapterItem(Kind.DONE_FOOTER, null, null);
    }

    public static AdapterItem ofRailTop(SectionObject s) {
        return new AdapterItem(Kind.RAIL_TOP, null, s);
    }

    public static AdapterItem ofRailBottom(SectionObject s) {
        return new AdapterItem(Kind.RAIL_BOTTOM, null, s);
    }

    public static AdapterItem ofSectionEmpty(SectionObject s) {
        return new AdapterItem(Kind.SECTION_EMPTY, null, s);
    }
}
