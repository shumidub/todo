package com.shumidub.todoapprealm.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shumidub.todoapprealm.App
import com.shumidub.todoapprealm.data.tasks.TasksRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    tasksRepository: TasksRepository,
) : ViewModel() {

    val dayScope: StateFlow<Int> = tasksRepository.tasksByFolder
        .map {
            runCatching { App.setDayScopeValue() }
            App.dayScope
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, App.dayScope)
}
