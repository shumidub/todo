package com.shumidub.todoapprealm.ui.actionmode;

import android.content.Context;
import android.view.Menu;
import android.view.MenuItem;
import android.view.inputmethod.InputMethodManager;

import androidx.appcompat.view.ActionMode;

import com.shumidub.todoapprealm.R;
import com.shumidub.todoapprealm.ui.activity.main.MainActivity;

/**
 * Contextual action mode with the app's standard Edit + Delete pair.
 * Subclasses supply the two click handlers; lifecycle hooks are optional.
 * The bar is tinted with the current tab palette (no-op on unthemed tabs).
 */
public abstract class EditDeleteActionModeCallback implements ActionMode.Callback {

    protected final MainActivity activity;
    private final CharSequence title;

    protected EditDeleteActionModeCallback(MainActivity activity, CharSequence title) {
        this.activity = activity;
        this.title = title;
    }

    protected abstract void onEditClicked(ActionMode actionMode);

    protected abstract void onDeleteClicked(ActionMode actionMode);

    /** Called when the action mode appears. */
    protected void onShown() {}

    /** Called when the action mode is dismissed. */
    protected void onDismissed() {}

    protected CharSequence editLabel() { return "edit "; }

    protected CharSequence deleteLabel() { return "delete "; }

    @Override
    public final boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
        onShown();

        MenuItem edit = menu.add(editLabel());
        edit.setIcon(R.drawable.ic_edit);
        edit.setOnMenuItemClickListener(item -> {
            onEditClicked(actionMode);
            return true;
        });

        MenuItem delete = menu.add(deleteLabel());
        delete.setIcon(R.drawable.ic_del);
        delete.setOnMenuItemClickListener(item -> {
            onDeleteClicked(actionMode);
            return true;
        });

        activity.tintActionModeBarForCurrentTab();
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
        if (title != null) actionMode.setTitle(title);
        return false;
    }

    @Override
    public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
        return false;
    }

    @Override
    public void onDestroyActionMode(ActionMode actionMode) {
        onDismissed();
    }

    /** Pop the soft keyboard for edit dialogs that open with a focused text field. */
    protected void showKeyboard() {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInputFromWindow(
                activity.getWindow().getDecorView().getApplicationWindowToken(),
                InputMethodManager.SHOW_FORCED, 0);
    }
}
