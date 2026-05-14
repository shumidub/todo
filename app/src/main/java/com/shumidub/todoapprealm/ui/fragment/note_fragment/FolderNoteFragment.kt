package com.shumidub.todoapprealm.ui.fragment.note_fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.shumidub.todoapprealm.R
import com.shumidub.todoapprealm.ui.notes.NotesDialog
import com.shumidub.todoapprealm.ui.notes.NotesMode
import com.shumidub.todoapprealm.ui.notes.NotesScreen
import com.shumidub.todoapprealm.ui.notes.NotesViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class FolderNoteFragment : Fragment() {

    private val viewModel: NotesViewModel by viewModels()

    // Mirrored from ViewModel state for MainActivity field access via Java.
    @JvmField var actionModeIsEnabled: Boolean = false
    @JvmField var isNoteFragment: Boolean = false

    private var currentTitle: String = DEFAULT_TITLE

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        setHasOptionsMenu(true)
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent { NotesScreen(viewModel) }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    syncWithLegacyActionBar(state.mode, state.dialog)
                }
            }
        }
    }

    private fun syncWithLegacyActionBar(mode: NotesMode, dialog: NotesDialog) {
        isNoteFragment = mode is NotesMode.Notes
        actionModeIsEnabled = dialog is NotesDialog.FolderActions ||
            dialog is NotesDialog.NoteActions

        currentTitle = when (mode) {
            NotesMode.Folders -> DEFAULT_TITLE
            is NotesMode.Notes -> mode.folderName.ifBlank { DEFAULT_TITLE }
        }

        val actionBar = (activity as? AppCompatActivity)?.supportActionBar ?: return
        actionBar.title = currentTitle
        actionBar.setDisplayHomeAsUpEnabled(isNoteFragment)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        menu.add(2, 2, 2, "add")
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        menu.findItem(2).setIcon(R.drawable.ic_add)
        menu.findItem(2).setOnMenuItemClickListener {
            viewModel.openAdd()
            true
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            viewModel.backToFolders()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    // Compatibility shims for MainActivity.onBackPressed
    fun finishActionMode() {
        viewModel.dismissDialog()
        actionModeIsEnabled = false
    }

    fun setFolderNoteViews() {
        viewModel.backToFolders()
    }

    fun getValidTitle(): String = currentTitle

    private companion object {
        const val DEFAULT_TITLE = "Notes"
    }
}
