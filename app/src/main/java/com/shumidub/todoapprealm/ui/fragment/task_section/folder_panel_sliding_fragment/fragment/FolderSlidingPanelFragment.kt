package com.shumidub.todoapprealm.ui.fragment.task_section.folder_panel_sliding_fragment.fragment

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
import com.shumidub.todoapprealm.R
import com.shumidub.todoapprealm.ui.tasks.SheetController
import com.shumidub.todoapprealm.ui.tasks.TasksScreen
import com.shumidub.todoapprealm.ui.tasks.TasksViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FolderSlidingPanelFragment : Fragment() {

    private val viewModel: TasksViewModel by viewModels()
    private val sheetController = SheetController()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        setHasOptionsMenu(true)
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                TasksScreen(viewModel = viewModel, sheetController = sheetController)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        (activity as? AppCompatActivity)?.supportActionBar?.title = getValidTitle()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        menu.add(2, 2, 2, "add")
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        menu.findItem(2).setIcon(R.drawable.ic_add)
        menu.findItem(2).setOnMenuItemClickListener {
            if (!sheetController.isExpanded()) {
                viewModel.openAddFolder()
            }
            true
        }
    }

    fun getValidTitle(): String {
        if (!sheetController.isExpanded()) return DEFAULT_TITLE
        val state = viewModel.state.value
        return state.folders.getOrNull(state.selectedFolderIndex)?.name ?: DEFAULT_TITLE
    }

    fun isSheetExpanded(): Boolean = sheetController.isExpanded()

    fun collapseSheet(): Boolean = sheetController.collapseIfExpanded()

    companion object {
        private const val DEFAULT_TITLE = "Tasks"

        @JvmStatic
        fun getTitle(): String = DEFAULT_TITLE
    }
}
