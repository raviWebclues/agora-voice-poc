package com.agoraaudio.service

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
enum class PodState: Parcelable {
    START_SERVICE,
    STOP_SERVICE,
    INITIALIZE_ENGINE,
    RELEASE_ENGINE,
    JOIN_CHANNEL,
    ROLE_AUDIENCE,
    ROLE_HOST,
    ROLE_MODERATOR,
    ROLE_SPEAKER,
    SHUFFLE_SONGS,
    PLAY_BACKGROUND_MUSIC,
    STOP_BACKGROUND_MUSIC,
    PUBLISH_VOLUME,
    PLAY_VOLUME,
    MUTE,
    UN_MUTE
}