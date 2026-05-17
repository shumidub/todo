package com.shumidub.todoapprealm.ui.dialog.task_bottomsheet;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.shumidub.todoapprealm.R;
import com.shumidub.todoapprealm.realmcontrollers.taskcontroller.FolderTaskRealmController;
import com.shumidub.todoapprealm.realmcontrollers.taskcontroller.TasksRealmController;
import com.shumidub.todoapprealm.realmmodel.task.FolderTaskObject;
import com.shumidub.todoapprealm.realmmodel.task.TaskObject;
import com.shumidub.todoapprealm.ui.theme.CanaryPalette;
import com.shumidub.todoapprealm.ui.theme.CornflowerPalette;

import java.util.ArrayList;
import java.util.List;

/**
 * BottomSheet editor for a single task. Replaces the read-only
 * {@code MaterialAlertDialogBuilder.setMessage(...)} that was shown on a single
 * tap from {@code SmallTasksFragment.onItemClicked}. Implements task-001.
 */
public class TaskEditorBottomSheet extends BottomSheetDialogFragment {

    public static final String TAG = "TaskEditorBottomSheet";

    private static final String ARG_TASK_ID = "arg_task_id";
    private static final String ARG_TASK_GROUP = "arg_task_group";
    private static final String KEY_DRAFT_TEXT = "draft_text";

    private long taskId;
    private int taskGroup;
    private TaskObject task;
    private Palette palette;

    private LinearLayout root;
    private TextView tvTitle;
    private TextView tvValue;
    private TextView tvMaxAcc;
    private TextView tvPriority;
    private TextView tvCycling;
    private CheckBox cbDone;
    private TextInputLayout tilText;
    private TextInputEditText etText;
    private TextView tvCategoriesLabel;
    private RecyclerView rvCategories;
    private View dragHandle;

    private CategoriesAdapter categoriesAdapter;
    private Runnable onDismissListener;

    /** Original text snapshot (Realm value at open time) — diff target for autosave. */
    private String originalText;

    /** Live mutable draft of the numeric/flag fields (we still write live to Realm). */
    private int draftPriority;
    private int draftValue;
    private int draftMaxAcc;
    private boolean draftCycling;

    public static TaskEditorBottomSheet newInstance(long taskId, int taskGroup) {
        TaskEditorBottomSheet f = new TaskEditorBottomSheet();
        Bundle b = new Bundle();
        b.putLong(ARG_TASK_ID, taskId);
        b.putInt(ARG_TASK_GROUP, taskGroup);
        f.setArguments(b);
        return f;
    }

    public void setOnDismissListener(Runnable r) {
        this.onDismissListener = r;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args != null) {
            taskId = args.getLong(ARG_TASK_ID, 0L);
            taskGroup = args.getInt(ARG_TASK_GROUP, 0);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottomsheet_task_editor, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        task = TasksRealmController.getTask(taskId);
        if (task == null || !task.isValid()) {
            dismissAllowingStateLoss();
            return;
        }

        palette = Palette.forGroup(requireContext(), taskGroup);

        root = view.findViewById(R.id.bs_root);
        dragHandle = view.findViewById(R.id.bs_drag_handle);
        tvTitle = view.findViewById(R.id.bs_title);
        tvValue = view.findViewById(R.id.bs_task_value);
        tvMaxAcc = view.findViewById(R.id.bs_task_max_acc);
        tvPriority = view.findViewById(R.id.bs_task_priority);
        tvCycling = view.findViewById(R.id.bs_task_cycling);
        cbDone = view.findViewById(R.id.bs_done_checkbox);
        tilText = view.findViewById(R.id.bs_text_layout);
        etText = view.findViewById(R.id.bs_et_text);
        tvCategoriesLabel = view.findViewById(R.id.bs_categories_label);
        rvCategories = view.findViewById(R.id.bs_rv_categories);

        originalText = task.getText() == null ? "" : task.getText();
        draftPriority = task.getPriority();
        draftValue = task.getCountValue();
        draftMaxAcc = task.getMaxAccumulation();
        draftCycling = task.isCycling();

        String initialText = originalText;
        if (savedInstanceState != null && savedInstanceState.containsKey(KEY_DRAFT_TEXT)) {
            initialText = savedInstanceState.getString(KEY_DRAFT_TEXT, originalText);
        }
        etText.setText(initialText);

        cbDone.setChecked(task.isDone());

        renderValue();
        renderMaxAcc();
        renderPriority();
        renderCycling();

        applyPalette();

        tvValue.setOnClickListener(v -> {
            draftValue = cycleOneToNine(draftValue);
            renderValue();
            saveNumericLive();
        });
        tvMaxAcc.setOnClickListener(v -> {
            draftMaxAcc = cycleOneToNine(draftMaxAcc);
            renderMaxAcc();
            saveNumericLive();
        });
        tvPriority.setOnClickListener(v -> {
            draftPriority = (draftPriority + 1) % 4;
            renderPriority();
            saveNumericLive();
        });
        tvCycling.setOnClickListener(v -> {
            draftCycling = !draftCycling;
            renderCycling();
            saveNumericLive();
        });
        cbDone.setOnCheckedChangeListener((CompoundButton btn, boolean checked) -> {
            if (task == null || !task.isValid()) return;
            TasksRealmController.setTaskDoneOrParticullaryDone(task, checked);
        });

        etText.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) expandSheet();
        });

        // Categories
        categoriesAdapter = new CategoriesAdapter();
        rvCategories.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvCategories.setAdapter(categoriesAdapter);
        rebuildCategoryRows();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (etText != null && etText.getText() != null) {
            outState.putString(KEY_DRAFT_TEXT, etText.getText().toString());
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dlg = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        dlg.setOnShowListener(d -> {
            BottomSheetBehavior<?> behavior = dlg.getBehavior();
            behavior.setSkipCollapsed(true);
            behavior.setHalfExpandedRatio(0.55f);
            behavior.setState(BottomSheetBehavior.STATE_HALF_EXPANDED);
        });
        if (dlg.getWindow() != null) {
            dlg.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
        return dlg;
    }

    private void expandSheet() {
        Dialog d = getDialog();
        if (d instanceof BottomSheetDialog) {
            BottomSheetBehavior<?> behavior = ((BottomSheetDialog) d).getBehavior();
            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        }
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        // Autosave text on dismiss (only if changed).
        try {
            if (task != null && task.isValid() && etText != null) {
                String newText = etText.getText() == null ? "" : etText.getText().toString();
                if (!newText.equals(originalText) && !newText.isEmpty()) {
                    TasksRealmController.editTask(task, newText, draftValue, draftMaxAcc,
                            draftCycling, draftPriority);
                }
            }
        } catch (Exception ignored) {
            // best-effort autosave; Realm may be in odd state during teardown
        }
        super.onDismiss(dialog);
        if (onDismissListener != null) onDismissListener.run();
    }

    // -------- live numeric save --------

    private void saveNumericLive() {
        if (task == null || !task.isValid()) return;
        String text = task.getText() == null ? "" : task.getText();
        TasksRealmController.editTask(task, text, draftValue, draftMaxAcc, draftCycling, draftPriority);
    }

    private int cycleOneToNine(int v) {
        if (v < 1) return 1;
        if (v >= 9) return 1;
        return v + 1;
    }

    // -------- rendering --------

    private void renderValue() {
        tvValue.setText(String.valueOf(draftValue));
        tvValue.setTextColor(draftValue >= 2 ? palette.accent : palette.counter);
    }

    private void renderMaxAcc() {
        tvMaxAcc.setText(String.valueOf(draftMaxAcc));
        tvMaxAcc.setTextColor(draftMaxAcc >= 2 ? palette.accent : palette.counter);
    }

    private void renderPriority() {
        StringBuilder s = new StringBuilder("!");
        for (int i = 1; i < Math.max(1, draftPriority); i++) s.append('!');
        tvPriority.setText(s.toString());
        tvPriority.setTextColor(draftPriority > 0 ? palette.accent : palette.textSoft);
    }

    private void renderCycling() {
        tvCycling.setTextColor(draftCycling ? palette.accent : palette.textSoft);
    }

    private void applyPalette() {
        root.setBackgroundColor(palette.bg);
        dragHandle.setBackgroundColor(palette.textSoft);
        tvTitle.setTextColor(palette.text);
        tvCategoriesLabel.setTextColor(palette.textSoft);
        if (etText != null) etText.setTextColor(palette.inputText);
        if (tilText != null) {
            try {
                tilText.setBoxStrokeColor(palette.accent);
                tilText.setHintTextColor(ColorStateList.valueOf(palette.textSoft));
            } catch (Exception ignored) {}
        }
        cbDone.setButtonTintList(ColorStateList.valueOf(palette.accent));
    }

    // -------- categories --------

    private void rebuildCategoryRows() {
        if (task == null || !task.isValid()) return;
        List<Long> activeIds = TasksRealmController.getCategoryIds(task);
        List<FolderTaskObject> allFolders = new ArrayList<>(FolderTaskRealmController.getFoldersList(taskGroup));

        List<FolderTaskObject> current = new ArrayList<>();
        List<FolderTaskObject> other = new ArrayList<>();

        // 'current' preserves activeIds order (primary first)
        for (Long id : activeIds) {
            for (FolderTaskObject f : allFolders) {
                if (f.getId() == id) { current.add(f); break; }
            }
        }
        for (FolderTaskObject f : allFolders) {
            if (!activeIds.contains(f.getId())) other.add(f);
        }

        List<Row> rows = new ArrayList<>();
        if (!current.isEmpty()) {
            rows.add(new Row(true, "Current", null, false));
            for (FolderTaskObject f : current) rows.add(new Row(false, null, f, true));
        }
        if (!other.isEmpty()) {
            rows.add(new Row(true, "Other", null, false));
            for (FolderTaskObject f : other) rows.add(new Row(false, null, f, false));
        }
        categoriesAdapter.setRows(rows);
        tvCategoriesLabel.setVisibility(rows.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void onCategoryTap(FolderTaskObject folder, boolean wasActive) {
        if (task == null || !task.isValid() || folder == null) return;
        List<Long> active = new ArrayList<>(TasksRealmController.getCategoryIds(task));
        if (wasActive) {
            if (active.size() <= 1) {
                // Last active — ignore (R11)
                return;
            }
            active.remove(Long.valueOf(folder.getId()));
        } else {
            if (!active.contains(folder.getId())) active.add(folder.getId());
        }
        TasksRealmController.setTaskCategories(task, active);
        rebuildCategoryRows();
    }

    // -------- Row model --------

    private static final class Row {
        final boolean isHeader;
        final String headerText;
        final FolderTaskObject folder;
        final boolean active;

        Row(boolean isHeader, String headerText, FolderTaskObject folder, boolean active) {
            this.isHeader = isHeader;
            this.headerText = headerText;
            this.folder = folder;
            this.active = active;
        }
    }

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_CATEGORY = 1;

    private final class CategoriesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private List<Row> rows = new ArrayList<>();

        void setRows(List<Row> r) {
            this.rows = r;
            notifyDataSetChanged();
        }

        @Override
        public int getItemViewType(int position) {
            return rows.get(position).isHeader ? TYPE_HEADER : TYPE_CATEGORY;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inf = LayoutInflater.from(parent.getContext());
            if (viewType == TYPE_HEADER) {
                View v = inf.inflate(R.layout.item_bs_category_header, parent, false);
                return new HeaderVH(v);
            }
            View v = inf.inflate(R.layout.item_bs_category_row, parent, false);
            return new CategoryVH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            Row row = rows.get(position);
            if (holder instanceof HeaderVH) {
                HeaderVH h = (HeaderVH) holder;
                h.tv.setText(row.headerText);
                h.tv.setTextColor(palette.textSoft);
            } else {
                CategoryVH c = (CategoryVH) holder;
                String name = row.folder.getName() == null ? "" : row.folder.getName();
                c.name.setText(name);
                if (row.active) {
                    c.mark.setText("✓");
                    c.mark.setTextColor(palette.accent);
                    c.name.setTextColor(palette.accent);
                } else {
                    c.mark.setText(" ");
                    c.name.setTextColor(palette.text);
                }
                c.itemView.setOnClickListener(v -> onCategoryTap(row.folder, row.active));
            }
        }

        @Override
        public int getItemCount() {
            return rows.size();
        }
    }

    private static final class HeaderVH extends RecyclerView.ViewHolder {
        final TextView tv;
        HeaderVH(View v) {
            super(v);
            tv = v.findViewById(R.id.bs_cat_header_text);
        }
    }

    private static final class CategoryVH extends RecyclerView.ViewHolder {
        final TextView mark;
        final TextView name;
        CategoryVH(View v) {
            super(v);
            mark = v.findViewById(R.id.bs_cat_row_mark);
            name = v.findViewById(R.id.bs_cat_row_name);
        }
    }

    // -------- Palette wrapper --------

    /** Unified colour set so the UI code has no per-tab branches. */
    private static final class Palette {
        final int bg;
        final int surface;
        final int text;
        final int textSoft;
        final int inputText;
        final int counter;
        final int accent;
        final int divider;

        private Palette(int bg, int surface, int text, int textSoft,
                        int inputText, int counter, int accent, int divider) {
            this.bg = bg;
            this.surface = surface;
            this.text = text;
            this.textSoft = textSoft;
            this.inputText = inputText;
            this.counter = counter;
            this.accent = accent;
            this.divider = divider;
        }

        static Palette forGroup(Context ctx, int group) {
            switch (group) {
                case 1: {
                    CornflowerPalette p = new CornflowerPalette(ctx);
                    return new Palette(p.bg, p.surface, p.text, p.textSoft,
                            p.inputText, p.counter, p.accent, p.divider);
                }
                case 2: {
                    CanaryPalette p = new CanaryPalette(ctx);
                    return new Palette(p.bg, p.surface, p.text, p.textSoft,
                            p.inputText, p.counter, p.accent, p.divider);
                }
                default: {
                    int accent = ContextCompat.getColor(ctx, R.color.colorAccent);
                    int onSurface = ContextCompat.getColor(ctx, R.color.colorDialogOnSurface);
                    int onSurfaceVariant = ContextCompat.getColor(ctx, R.color.colorDialogOnSurfaceVariant);
                    int white = ContextCompat.getColor(ctx, R.color.colorWhite);
                    int dialogSurface = ContextCompat.getColor(ctx, R.color.colorDialogSurface);
                    return new Palette(
                            /* bg */ dialogSurface,
                            /* surface */ dialogSurface,
                            /* text */ onSurface,
                            /* textSoft */ onSurfaceVariant,
                            /* inputText */ white,
                            /* counter */ white,
                            /* accent */ accent,
                            /* divider */ onSurfaceVariant);
                }
            }
        }
    }
}
