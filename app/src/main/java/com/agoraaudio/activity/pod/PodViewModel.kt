package com.agoraaudio.activity.pod

import androidx.lifecycle.ViewModel

class PodViewModel : ViewModel() {
    var isForegroundServiceRunning: Boolean = false
    var isBackgroundMusicPlay: Boolean = false
    var isMute: Boolean = false
    var isReceiverRegistered: Boolean = false
}