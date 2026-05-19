package com.shumidub.todoapprealm.ui.fragment.task_section.small_tasks_fragment;

import android.annotation.SuppressLint;
import android.graphics.Canvas;
import android.os.Handler;
import android.os.Looper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.ItemTouchHelper;
import android.util.Log;

import com.shumidub.todoapprealm.App;
import com.shumidub.todoapprealm.realmcontrollers.taskcontroller.SectionsRealmController;
import com.shumidub.todoapprealm.realmcontrollers.taskcontroller.TasksRealmController;
import com.shumidub.todoapprealm.realmmodel.task.SectionObject;
import com.shumidub.todoapprealm.realmmodel.task.TaskObject;
import com.shumidub.todoapprealm.ui.actionmode.EmptyActionModeCallback;
import com.shumidub.todoapprealm.ui.activity.main.MainActivity;

import java.util.ArrayList;
import java.util.List;

import static androidx.recyclerview.widget.ItemTouchHelper.ACTION_STATE_IDLE;

/**
 * Drag-n-drop helper for tasks + section headers (task-002).
 *
 * <p>Supports three move modes:
 * <ul>
 *   <li>Task ↔ Task across same/different sections — task.sectionId is rewritten to match
 *       the surrounding container at drop time, then positions compacted.</li>
 *   <li>Section header (treated as block — its expanded children follow in the adapter list,
 *       but in Realm only the header's outer position changes; children keep their inner
 *       positions and sectionId).</li>
 *   <li>Auto-expand on hover over collapsed section header (~400ms) so user can drop inside.</li>
 * </ul>
 */
public class ItemTouchHelperAttacher {

    ItemTouchHelper itemTouchHelper;
    ItemTouchHelper.SimpleCallback itemTouchHelperSimpleCallback;
    SmallTasksFragment smallTasksFragment;
    MainActivity activity;
    TasksRecyclerViewAdapter tasksRecyclerViewAdapter;
    List<TaskObject> tasks;

    private static final long AUTO_EXPAND_MS = 400;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable pendingAutoExpand;
    private long pendingAutoExpandSectionId = -1;

    public ItemTouchHelperAttacher(SmallTasksFragment smallTasksFragment) {
        this.smallTasksFragment = smallTasksFragment;
        activity = (MainActivity) smallTasksFragment.getActivity();
        setAdapter();
        setTasks();
    }

    public void setAdapter() {
        tasksRecyclerViewAdapter = (TasksRecyclerViewAdapter) smallTasksFragment.rvTasks.getAdapter();
    }

    public void setTasks() {
        if (tasksRecyclerViewAdapter != null) tasks = tasksRecyclerViewAdapter.tasks;
    }

    public void attachTouchHelperToRecyclerView(RecyclerView recyclerView) {
        itemTouchHelperSimpleCallback = new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {

            int dragFrom = -1;
            int dragTo = -1;

            @Override
            public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder,
                                    float dX, float dY, int actionState, boolean isCurrentlyActive) {
                if (smallTasksFragment.isAllTaskShowing) return;
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }

            @Override
            public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
                if (smallTasksFragment.isAllTaskShowing) return ACTION_STATE_IDLE;
                if (viewHolder instanceof TasksRecyclerViewAdapter.FooterViewHolder) return 0;
                // sprint-002 task-002: rails and empty placeholder are not draggable.
                if (viewHolder instanceof TasksRecyclerViewAdapter.RailViewHolder) return 0;
                if (viewHolder instanceof TasksRecyclerViewAdapter.SectionEmptyViewHolder) return 0;
                return super.getMovementFlags(recyclerView, viewHolder);
            }

            @Override
            public boolean isLongPressDragEnabled() {
                if (smallTasksFragment.isAllTaskShowing) return false;
                return super.isLongPressDragEnabled();
            }

            @SuppressLint("RestrictedApi")
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder,
                                  RecyclerView.ViewHolder target) {
                if (smallTasksFragment.isAllTaskShowing) return false;
                activity.getSupportActionBar().startActionMode(new EmptyActionModeCallback());

                int fromPosition = viewHolder.getAdapterPosition();
                int toPosition = target.getAdapterPosition();

                List<AdapterItem> items = tasksRecyclerViewAdapter.items;
                if (fromPosition < 0 || toPosition < 0
                        || fromPosition >= items.size() || toPosition >= items.size()) {
                    tasksRecyclerViewAdapter.touchOutsideUnDoneTaskArea = true;
                    return false;
                }
                AdapterItem fromItem = items.get(fromPosition);
                AdapterItem toItem = items.get(toPosition);
                if (toItem.kind == AdapterItem.Kind.DONE_FOOTER) {
                    tasksRecyclerViewAdapter.touchOutsideUnDoneTaskArea = true;
                    return false;
                }
                // Tasks and sections both draggable; sections may move over tasks (drop inside)
                // or over other sections.
                if (fromItem.kind == AdapterItem.Kind.DONE_FOOTER) return false;
                if (fromItem.kind == AdapterItem.Kind.TASK && fromItem.task != null && fromItem.task.isDone()) {
                    tasksRecyclerViewAdapter.touchOutsideUnDoneTaskArea = true;
                    return false;
                }

                // Auto-expand on hover over collapsed section — only when dragging a TASK.
                // sprint-002 follow-up: when the dragged item is itself a section header,
                // auto-expand the target on hover would be confusing (you can't drop a
                // section inside another section) — design docs §5c / task-002 risk 3.
                maybeScheduleAutoExpand(fromItem, toItem);

                if (dragFrom == -1) dragFrom = fromPosition;
                dragTo = toPosition;

                recyclerView.getAdapter().notifyItemMoved(fromPosition, toPosition);
                // Also move in our items list to keep indices consistent during drag.
                AdapterItem moved = items.remove(fromPosition);
                items.add(toPosition, moved);
                return true;
            }

            private void maybeScheduleAutoExpand(AdapterItem fromItem, AdapterItem toItem) {
                // Sections never auto-expand another section while being dragged.
                if (fromItem != null && fromItem.kind == AdapterItem.Kind.SECTION_HEADER) {
                    cancelAutoExpand();
                    return;
                }
                if (toItem.kind != AdapterItem.Kind.SECTION_HEADER || toItem.section == null) {
                    cancelAutoExpand();
                    return;
                }
                if (!toItem.section.isCurrentlyCollapsed()) {
                    cancelAutoExpand();
                    return;
                }
                long sectionId = toItem.section.getId();
                if (pendingAutoExpandSectionId == sectionId) return;
                cancelAutoExpand();
                pendingAutoExpandSectionId = sectionId;
                pendingAutoExpand = () -> {
                    SectionObject s = SectionsRealmController.getSection(sectionId);
                    if (s != null && s.isValid() && s.isCurrentlyCollapsed()) {
                        SectionsRealmController.setCurrentlyCollapsed(s, false);
                        smallTasksFragment.setTasksAndNotifyDataSetChanged();
                    }
                    pendingAutoExpandSectionId = -1;
                };
                handler.postDelayed(pendingAutoExpand, AUTO_EXPAND_MS);
            }

            private void cancelAutoExpand() {
                if (pendingAutoExpand != null) handler.removeCallbacks(pendingAutoExpand);
                pendingAutoExpand = null;
                pendingAutoExpandSectionId = -1;
            }

            @Override
            public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
                cancelAutoExpand();
                if (tasksRecyclerViewAdapter.touchOutsideUnDoneTaskArea) {
                    tasksRecyclerViewAdapter.touchOutsideUnDoneTaskArea = false;
                    dragFrom = dragTo = -1;
                    return;
                }
                super.clearView(recyclerView, viewHolder);

                if (dragFrom != -1 && dragTo != -1 && dragFrom != dragTo) {
                    commitMove(viewHolder);
                }
                dragFrom = dragTo = -1;
                // Re-flatten authoritatively from Realm.
                smallTasksFragment.setTasksAndNotifyDataSetChanged();
            }

            private void commitMove(RecyclerView.ViewHolder viewHolder) {
                List<AdapterItem> items = tasksRecyclerViewAdapter.items;
                if (dragFrom >= items.size() || dragTo >= items.size()) return;

                AdapterItem moved = items.get(dragTo); // already moved during onMove
                long folderId = smallTasksFragment.getTasksFolderId();
                List<SectionsRealmController.ItemMove> moves = new ArrayList<>();

                if (moved.kind == AdapterItem.Kind.SECTION_HEADER) {
                    // Section-as-block — only the header's outer position changes.
                    int outerPos = computeOuterPositionAt(items, dragTo);
                    moves.add(new SectionsRealmController.ItemMove(
                            SectionsRealmController.ItemMove.Kind.SECTION,
                            moved.section.getId(), outerPos, -1L));
                } else if (moved.kind == AdapterItem.Kind.TASK) {
                    // Determine container at the new position: walk upward for the nearest section header.
                    // sprint-002 task-002: rail/empty items above the drop position resolve directly to
                    // their owning section (skip walk for them — they encode the same answer).
                    long containerSectionId = 0L;
                    for (int i = dragTo - 1; i >= 0; i--) {
                        AdapterItem above = items.get(i);
                        if (above.kind == AdapterItem.Kind.RAIL_TOP
                                || above.kind == AdapterItem.Kind.RAIL_BOTTOM
                                || above.kind == AdapterItem.Kind.SECTION_EMPTY) {
                            if (above.section != null && !above.section.isCurrentlyCollapsed()) {
                                containerSectionId = above.section.getId();
                            }
                            break;
                        }
                        if (above.kind == AdapterItem.Kind.SECTION_HEADER) {
                            // Only count it as container if it's not collapsed (otherwise tasks below it
                            // belong to outer space). When auto-expanded, isCurrentlyCollapsed is false.
                            if (above.section != null && !above.section.isCurrentlyCollapsed()) {
                                containerSectionId = above.section.getId();
                            }
                            break;
                        }
                    }
                    int newPos = computePositionInContainer(items, dragTo, containerSectionId);
                    moves.add(new SectionsRealmController.ItemMove(
                            SectionsRealmController.ItemMove.Kind.TASK,
                            moved.task.getId(), newPos, containerSectionId));
                }

                if (!moves.isEmpty()) SectionsRealmController.reorderItems(folderId, moves);
            }

            /** Outer-space index for the item currently at adapter position {@code idx}. */
            private int computeOuterPositionAt(List<AdapterItem> items, int idx) {
                int count = 0;
                for (int i = 0; i < idx; i++) {
                    AdapterItem it = items.get(i);
                    if (it.kind == AdapterItem.Kind.SECTION_HEADER) count++;
                    else if (it.kind == AdapterItem.Kind.TASK && it.task != null && it.task.getSectionId() == 0) count++;
                }
                return count;
            }

            /** New position field for a task placed at adapter idx, given its container sectionId. */
            private int computePositionInContainer(List<AdapterItem> items, int idx, long containerSectionId) {
                int count = 0;
                if (containerSectionId == 0L) {
                    // outer space — count section headers + free tasks before idx
                    for (int i = 0; i < idx; i++) {
                        AdapterItem it = items.get(i);
                        if (it.kind == AdapterItem.Kind.SECTION_HEADER) count++;
                        else if (it.kind == AdapterItem.Kind.TASK && it.task != null && it.task.getSectionId() == 0) count++;
                    }
                } else {
                    // inner space — count tasks belonging to this section before idx
                    for (int i = 0; i < idx; i++) {
                        AdapterItem it = items.get(i);
                        if (it.kind == AdapterItem.Kind.TASK && it.task != null && it.task.getSectionId() == containerSectionId) count++;
                    }
                }
                return count;
            }

            @Override
            public void onSwiped(final RecyclerView.ViewHolder viewHolder, int swipeDir) { }
        };
        itemTouchHelper = new ItemTouchHelper(itemTouchHelperSimpleCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);
    }
}
