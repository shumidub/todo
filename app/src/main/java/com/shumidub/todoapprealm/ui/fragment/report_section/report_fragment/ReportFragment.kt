package com.shumidub.todoapprealm.ui.fragment.report_section.report_fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import com.shumidub.todoapprealm.ui.reports.ReportsScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ReportFragment : Fragment() {

    // Read by MainActivity.onBackPressed while MainActivity remains XML-based.
    @JvmField var actionModeIsEnabled: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent { ReportsScreen() }
    }

    fun finishActionMode() {
        actionModeIsEnabled = false
    }

    companion object {
        @JvmField var id: Long = 0L
    }
}
