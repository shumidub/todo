package com.shumidub.todoapprealm.ui.fragment.task_section.small_tasks_fragment;

import android.graphics.Color;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.ItemTouchHelper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import com.shumidub.todoapprealm.App;
import com.shumidub.todoapprealm.R;
import com.shumidub.todoapprealm.realmcontrollers.taskcontroller.SectionsRealmController;
import com.shumidub.todoapprealm.realmcontrollers.taskcontroller.TasksRealmController;
import com.shumidub.todoapprealm.realmmodel.task.SectionObject;
import com.shumidub.todoapprealm.realmmodel.task.TaskObject;
import com.shumidub.todoapprealm.ui.activity.main.MainActivity;
import com.shumidub.todoapprealm.ui.dialog.section_dialog.SectionEditDialog;
import com.shumidub.todoapprealm.ui.theme.CornflowerPalette;
import com.shumidub.todoapprealm.ui.theme.CanaryPalette;
import androidx.cardview.widget.CardView;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Multi-view-type RecyclerView adapter (task-002).
 * <p>Renders an {@link #items} list flattened from sections + tasks:
 * <pre>
 *   [free_task, free_task, section_header, task_in_section, ..., section_header, ..., done_footer]
 * </pre>
 * The legacy {@link #tasks} field is preserved for backwards compatibility with the
 * old footer-count / drag-helper code paths; for drag use {@link #getItem(int)}.
 */
public class TasksRecyclerViewAdapter extends RecyclerView.Adapter<TasksRecyclerViewAdapter.ViewHolder> {

    /** Legacy list of all undone tasks in the folder (no longer drives view binding). */
    public List<TaskObject> tasks;
    private List<TaskObject> doneTasks;
    /** Flattened display list. Drives {@link #getItemCount()} / {@link #getItemViewType(int)}. */
    public List<AdapterItem> items = new ArrayList<>();

    private static final int VIEW_TYPE_TASK = 1;
    private static final int VIEW_TYPE_SECTION_HEADER = 2;
    private static final int FOOTER_VIEW = 123;
    private static final int VIEW_TYPE_EMPTY = 99;

    public boolean touchOutsideUnDoneTaskArea = false;
    private SmallTasksFragment smallTasksFragment;
    private OnItemLongClicked onItemLongClicked;
    private OnItemClicked onItemClicked;
    MainActivity activity;
    private CornflowerPalette cornflowerPalette;
    private CanaryPalette canaryPalette;

    public void useCornflowerPalette(boolean enabled) {
        cornflowerPalette = enabled ? new CornflowerPalette(activity) : null;
        if (enabled) canaryPalette = null;
        notifyDataSetChanged();
    }

    public void useCanaryPalette(boolean enabled) {
        canaryPalette = enabled ? new CanaryPalette(activity) : null;
        if (enabled) cornflowerPalette = null;
        notifyDataSetChanged();
    }

    private boolean hasActivePalette() {
        return cornflowerPalette != null || canaryPalette != null;
    }

    private int activeAccent() {
        if (cornflowerPalette != null) return cornflowerPalette.accent;
        if (canaryPalette != null) return canaryPalette.accent;
        return activity.getResources().getColor(R.color.colorAccent);
    }

    private int activeSurface() {
        if (cornflowerPalette != null) return cornflowerPalette.surface;
        if (canaryPalette != null) return canaryPalette.surface;
        return 0;
    }

    private int activeInputText() {
        if (cornflowerPalette != null) return cornflowerPalette.inputText;
        if (canaryPalette != null) return canaryPalette.inputText;
        return 0;
    }

    private int activeCounter() {
        if (cornflowerPalette != null) return cornflowerPalette.counter;
        if (canaryPalette != null) return canaryPalette.counter;
        return 0;
    }

    public interface OnItemLongClicked { void onLongClick(View view, int position); }
    public interface OnItemClicked { void onClick(View view, int position); }

    public void setOnLongClicked(OnItemLongClicked onItemLongClicked) {
        this.onItemLongClicked = onItemLongClicked;
    }

    public void setOnClicked(OnItemClicked onItemClicked) {
        this.onItemClicked = onItemClicked;
    }

    public TasksRecyclerViewAdapter(MainActivity activity, List<TaskObject> tasks,
                                    List<TaskObject> doneTasks, SmallTasksFragment smallTasksFragment) {
        this.activity = activity;
        this.tasks = tasks;
        this.doneTasks = doneTasks;
        this.smallTasksFragment = smallTasksFragment;
        rebuildItems();
    }

    /** Rebuild {@link #items} from current {@link #tasks} + sections in the folder. */
    public void rebuildItems() {
        items = flatten();
    }

    private List<AdapterItem> flatten() {
        List<AdapterItem> out = new ArrayList<>();
        long folderId = smallTasksFragment == null ? 0 : smallTasksFragment.getTasksFolderId();
        if (folderId == 0 || tasks == null) {
            // No sections concept outside of a folder; fall back to plain task list.
            if (tasks != null) for (TaskObject t : tasks) out.add(AdapterItem.ofTask(t));
            if ((tasks != null && !tasks.isEmpty()) || (doneTasks != null && !doneTasks.isEmpty())) {
                out.add(AdapterItem.doneFooter());
            }
            return out;
        }

        List<SectionObject> sections = new ArrayList<>(SectionsRealmController.getSections(folderId));
        // Bucket tasks by sectionId. Only "tasks" (= not-done) drives this flow; done tasks live in footer.
        Map<Long, List<TaskObject>> bySection = new HashMap<>();
        for (TaskObject t : tasks) {
            Long key = t.getSectionId();
            List<TaskObject> bucket = bySection.get(key);
            if (bucket == null) { bucket = new ArrayList<>(); bySection.put(key, bucket); }
            bucket.add(t);
        }
        // tasks already arrive sorted by position from the controller; ensure consistency.

        List<TaskObject> freeTasks = bySection.get(0L);
        if (freeTasks == null) freeTasks = new ArrayList<>();

        // Merge outer space by position.
        int si = 0, ti = 0;
        while (si < sections.size() && ti < freeTasks.size()) {
            SectionObject s = sections.get(si);
            TaskObject t = freeTasks.get(ti);
            if (s.getPosition() <= t.getPosition()) {
                emitSection(out, s, bySection);
                si++;
            } else {
                out.add(AdapterItem.ofTask(t));
                ti++;
            }
        }
        while (si < sections.size()) emitSection(out, sections.get(si++), bySection);
        while (ti < freeTasks.size()) out.add(AdapterItem.ofTask(freeTasks.get(ti++)));

        if (!tasks.isEmpty() || (doneTasks != null && !doneTasks.isEmpty())) {
            out.add(AdapterItem.doneFooter());
        }
        return out;
    }

    private void emitSection(List<AdapterItem> out, SectionObject s, Map<Long, List<TaskObject>> bySection) {
        out.add(AdapterItem.ofSection(s));
        if (!s.isCurrentlyCollapsed()) {
            List<TaskObject> inner = bySection.get(s.getId());
            if (inner != null) for (TaskObject t : inner) out.add(AdapterItem.ofTask(t));
        }
    }

    /** Safe accessor used by drag-helper. */
    public AdapterItem getItem(int adapterPosition) {
        if (adapterPosition < 0 || adapterPosition >= items.size()) return null;
        return items.get(adapterPosition);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view;
        if (viewType == FOOTER_VIEW) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.task_card_view_done_tasks, parent, false);
            return new FooterViewHolder(view);
        }
        if (viewType == VIEW_TYPE_SECTION_HEADER) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.section_header_card_view, parent, false);
            return new SectionHeaderViewHolder(view);
        }
        if (viewType == VIEW_TYPE_TASK) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.task_card_view, parent, false);
            return new NormalViewHolder(view);
        }
        // Empty state
        view = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_view_empty_state, parent, false);
        return new ViewHolder(view);
    }

    private void setTasksTextColor(ViewHolder holder, boolean isDone) {
        if (isDone) holder.textView.setTextColor(Color.GRAY);
        else holder.textView.setTextColor(Color.BLACK);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        AdapterItem it = position < items.size() ? items.get(position) : null;
        if (it == null) return;

        if (it.kind == AdapterItem.Kind.SECTION_HEADER && holder instanceof SectionHeaderViewHolder) {
            bindSectionHeader((SectionHeaderViewHolder) holder, it.section);
            return;
        }
        if (it.kind == AdapterItem.Kind.DONE_FOOTER && holder instanceof FooterViewHolder) {
            holder.textViewDoneTask.setText("Done " + (smallTasksFragment.doneTasks == null ? 0 : smallTasksFragment.doneTasks.size()) + " tasks");
            holder.textViewDoneTask.setTag("footer");
            holder.textViewDoneTask.setOnClickListener(v -> smallTasksFragment.showAllTasks());
            return;
        }
        if (it.kind == AdapterItem.Kind.TASK && holder instanceof NormalViewHolder) {
            bindTask((NormalViewHolder) holder, it.task, position);
        }
    }

    private void bindSectionHeader(SectionHeaderViewHolder holder, SectionObject section) {
        if (section == null || !section.isValid()) return;
        final long sectionId = section.getId();
        holder.tvName.setText(section.getName());
        holder.tvChevron.setText(section.isCurrentlyCollapsed() ? "▶" : "▼");

        int accent = activeAccent();
        holder.tvName.setTextColor(accent);
        holder.tvChevron.setTextColor(accent);
        if (holder.divider != null) holder.divider.setBackgroundColor(accent);

        holder.itemView.setOnClickListener(v -> {
            SectionObject s = SectionsRealmController.getSection(sectionId);
            if (s != null && s.isValid()) {
                SectionsRealmController.setCurrentlyCollapsed(s, !s.isCurrentlyCollapsed());
                smallTasksFragment.setTasksAndNotifyDataSetChanged();
            }
        });
        holder.itemView.setOnLongClickListener(v -> {
            SectionObject s = SectionsRealmController.getSection(sectionId);
            if (s != null && s.isValid()) {
                SectionEditDialog.forEdit(s).show(activity.getSupportFragmentManager(), "editsection");
            }
            return true;
        });
    }

    private void bindTask(NormalViewHolder holder, TaskObject taskObject, int position) {
        long taskId = taskObject.getId();
        String text = taskObject.getText();

        holder.textView.setText(text);
        holder.textView.setTag(taskId);
        holder.tvCount.setText("" + taskObject.getCountValue());
        holder.tvAccumulation.setText(taskObject.getCountAccumulation() + "/" + taskObject.getMaxAccumulation());

        int priorityFromTaskObject = taskObject.getPriority();
        int priorityCount = taskObject.getPriority();
        String textPriority = "";
        while (priorityCount > 0) { textPriority += "!"; priorityCount -= 1; }
        holder.tvPriority.setText(textPriority);
        int accentColor = activeAccent();
        if (priorityFromTaskObject > 0) holder.tvPriority.setTextColor(accentColor);
        else holder.tvPriority.setTextColor(activity.getResources().getColor(R.color.colorWhite));

        holder.tvPriority.setOnClickListener(listener -> {
            View view = holder.tvPriority;
            int priority = taskObject.getPriority();
            if (priority > 2) priority = 0;
            else priority++;
            TasksRealmController.setTaskPriority(taskObject, priority);
            if (priority > 1) {
                String priorityText = "!";
                int i = priority;
                while (i > 1) { priorityText += "!"; i--; }
                ((TextView) view).setText(priorityText);
            } else ((TextView) view).setText("!");
            if (priority > 0) ((TextView) view).setTextColor(accentColor);
            else ((TextView) view).setTextColor(activity.getResources().getColor(R.color.colorWhite));
        });

        bindCategoryStripes(holder, taskObject);
        applyPaletteIfNeeded(holder);

        holder.checkBox.setChecked(taskObject.isDone());

        if (taskObject.isCycling() && !taskObject.isDone()) holder.checkBox.setButtonDrawable(R.drawable.unchecked_accent_color_checkbox);
        else if (!taskObject.isCycling() && !taskObject.isDone()) holder.checkBox.setButtonDrawable(R.drawable.unchecked_gray_checkbox);
        if (taskObject.isCycling() && taskObject.isDone()) holder.checkBox.setButtonDrawable(R.drawable.checked_accent_color_checkbox);
        else if (!taskObject.isCycling() && taskObject.isDone()) holder.checkBox.setButtonDrawable(R.drawable.checked_gray_checkbox);

        if (hasActivePalette() && taskObject.isCycling()) {
            holder.checkBox.setButtonTintList(android.content.res.ColorStateList.valueOf(activeAccent()));
        } else {
            holder.checkBox.setButtonTintList(null);
        }

        setTasksTextColor(holder, taskObject.isDone());

        holder.checkBox.setOnClickListener(cb -> {
            TasksRealmController.setTaskDoneOrParticullaryDone(taskObject, holder.checkBox.isChecked());
            holder.checkBox.setChecked(taskObject.isDone() ? false : true);
            holder.itemView
                    .animate()
                    .scaleX(0.002f).scaleY(0.002f).alpha(0.2f)
                    .setDuration(175L)
                    .withEndAction(() -> smallTasksFragment.notifyDataChanged());
            smallTasksFragment.getActivity().invalidateOptionsMenu();
            setTasksTextColor(holder, taskObject.isDone());
            for (com.shumidub.todoapprealm.ui.fragment.task_section.folder_panel_sliding_fragment.fragment.FolderSlidingPanelFragment p : App.folderSlidingPanelFragments) {
                p.notifyFolderOfTasksRVAdapterDataSetChanged();
            }
        });

        holder.textView.setOnLongClickListener(view -> {
            if (onItemLongClicked != null) onItemLongClicked.onLongClick(view, position);
            return true;
        });
        holder.textView.setOnClickListener(view -> {
            if (onItemClicked != null) onItemClicked.onClick(view, position);
        });
    }

    @Override
    public int getItemViewType(int position) {
        if (items.isEmpty()) return VIEW_TYPE_EMPTY;
        AdapterItem it = items.get(position);
        switch (it.kind) {
            case SECTION_HEADER: return VIEW_TYPE_SECTION_HEADER;
            case DONE_FOOTER: return FOOTER_VIEW;
            case TASK:
            default: return VIEW_TYPE_TASK;
        }
    }

    @Override
    public int getItemCount() {
        if (items.isEmpty()) return 1; // empty state row
        return items.size();
    }

    private void applyPaletteIfNeeded(ViewHolder holder) {
        if (!hasActivePalette()) return;
        int surface = activeSurface();
        int inputText = activeInputText();
        int counter = activeCounter();
        int accent = activeAccent();
        View root = holder.itemView;
        if (root instanceof CardView) {
            ((CardView) root).setCardBackgroundColor(surface);
        } else {
            root.setBackgroundColor(surface);
        }
        if (holder.textView != null) holder.textView.setTextColor(inputText);
        if (holder.tvCount != null) holder.tvCount.setTextColor(counter);
        if (holder.tvAccumulation != null) holder.tvAccumulation.setTextColor(counter);
        if (holder.categoryStripes != null) holder.categoryStripes.setBackgroundColor(accent);
    }

    private void bindCategoryStripes(ViewHolder holder, TaskObject taskObject) {
        if (holder.categoryStripes == null) return;
        int extraCount = taskObject.getExtraFolderIds() == null ? 0 : taskObject.getExtraFolderIds().size();
        holder.categoryStripes.setVisibility(extraCount > 0 ? View.VISIBLE : View.GONE);
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        TextView textView;
        TextView tvCount;
        TextView tvPriority;
        TextView tvCycling;
        CheckBox checkBox;
        TextView textViewDoneTask;
        TextView tvAccumulation;
        View categoryStripes;

        public ViewHolder(View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.tv);
            checkBox = itemView.findViewById(R.id.checkbox);
            tvCount = itemView.findViewById(R.id.task_value);
            tvPriority = itemView.findViewById(R.id.task_priority);
            tvCycling = itemView.findViewById(R.id.task_cycling);
            textViewDoneTask = itemView.findViewById(R.id.tv_done_tasks);
            tvAccumulation = itemView.findViewById(R.id.task_accumulation);
            categoryStripes = itemView.findViewById(R.id.category_stripes);
        }
    }

    public class FooterViewHolder extends ViewHolder {
        public FooterViewHolder(View itemView) { super(itemView); }
    }

    public class NormalViewHolder extends ViewHolder {
        public NormalViewHolder(View itemView) { super(itemView); }
    }

    public class SectionHeaderViewHolder extends ViewHolder {
        public final TextView tvName;
        public final TextView tvChevron;
        public final View divider;
        public SectionHeaderViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.section_name);
            tvChevron = itemView.findViewById(R.id.section_chevron);
            divider = itemView.findViewById(R.id.section_divider);
        }
    }
}
