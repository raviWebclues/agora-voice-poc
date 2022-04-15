package com.agoraaudio

import com.agoraaudio.activity.pod.PodViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

private val viewModelModule = module {
    viewModel { PodViewModel() }
}


val globalModule = viewModelModule
