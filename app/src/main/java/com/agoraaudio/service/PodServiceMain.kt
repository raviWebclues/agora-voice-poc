package com.agoraaudio.service

import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.agoraaudio.R
import com.agoraaudio.activity.pod.Constant
import com.agoraaudio.activity.pod.NotificationHelper
import com.agoraaudio.activity.pod.POD_ACTION
import com.agoraaudio.activity.pod.PodsActivity
import io.agora.rtc.Constants
import io.agora.rtc.IAudioEffectManager
import io.agora.rtc.IRtcEngineEventHandler
import io.agora.rtc.RtcEngine
import io.agora.rtc.models.ClientRoleOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlin.coroutines.CoroutineContext

const val SERVICE_COMMAND = "Command"
const val NOTIFICATION_TEXT = "NotificationText"

class PodServiceMain: Service(), CoroutineScope {

    private var isLowLatency=false
    private var mRtcEngine: RtcEngine? = null
    private lateinit var audioEffectManager: IAudioEffectManager
    private val helper by lazy { NotificationHelper(this) }

    override fun onBind(p0: Intent?): IBinder?= null
    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        intent?.extras?.run {
            when (getSerializable(SERVICE_COMMAND) as PodState) {
                PodState.START_SERVICE -> serviceStarted(intent.getStringExtra(Constant.CHANNEL_NAME), intent.getStringExtra(Constant.TOKEN), intent.getStringExtra(Constant.UID))
                PodState.STOP_SERVICE -> stopService()
                PodState.JOIN_CHANNEL -> joinChannel(intent.getStringExtra(Constant.CHANNEL_NAME),
                    intent.getStringExtra(Constant.TOKEN),
                    intent.getStringExtra(Constant.UID),
                    intent.getStringExtra(Constant.ROLE))
                PodState.PLAY_BACKGROUND_MUSIC -> setBackgroundAudio(intent.getStringExtra(Constant.MUSIC_URL))
                PodState.STOP_BACKGROUND_MUSIC -> pauseMusic()
                PodState.PUBLISH_VOLUME -> publishVolume(intent.getIntExtra(Constant.PUBLISH_VOLUME,0))
                PodState.PLAY_VOLUME -> playoutVolume(intent.getIntExtra(Constant.PLAY_VOLUME,0))
                PodState.MUTE -> setUserMute()
                PodState.UN_MUTE -> setUserUnmute()
                else -> return START_NOT_STICKY
            }
        }
        return START_NOT_STICKY
    }

    private fun setUserUnmute() {
        mRtcEngine?.adjustRecordingSignalVolume(400)
    }

    private fun setUserMute() {
        mRtcEngine?.adjustRecordingSignalVolume(0)
    }

    private fun playoutVolume(intExtra: Int) {
        mRtcEngine?.adjustAudioMixingPlayoutVolume(intExtra)
    }

    private fun publishVolume(intExtra: Int) {
        mRtcEngine?.adjustAudioMixingPublishVolume(intExtra)
    }

    private fun serviceStarted(mRoomName: String?, token: String?, uid: String?) {
        initializeEngine(mRoomName,token,uid)
    }

    private fun initializeEngine(mRoomName: String?, token: String?, uid: String?) {
        startForeground(NotificationHelper.NOTIFICATION_ID, helper.getNotification())
        Log.e("PodsService","initializeEngine")
        mRtcEngine = try {
            RtcEngine.create(baseContext, getString(R.string.agora_app_id), mRtcEventHandler)
        } catch (e: Exception) {
            throw RuntimeException(
                "NEED TO check rtc sdk init fatal error${
                    Log.getStackTraceString(
                        e
                    )
                }".trimIndent()
            )
        }
        if (mRtcEngine != null) {
            setChannelProfile()
            audioEffectManager = mRtcEngine!!.audioEffectManager
        }
    }

    private fun setUserRoleAsBroadCaster() {
        Log.e("PodsService","Host")
        mRtcEngine?.setClientRole(IRtcEngineEventHandler.ClientRole.CLIENT_ROLE_BROADCASTER)
    }

    private fun setUserRoleAudience() {
        Log.e("PodsService","Audience")
        val clientRoleOptions = ClientRoleOptions()
        clientRoleOptions.audienceLatencyLevel = if (isLowLatency) Constants.AUDIENCE_LATENCY_LEVEL_ULTRA_LOW_LATENCY else Constants.AUDIENCE_LATENCY_LEVEL_LOW_LATENCY
        mRtcEngine?.setClientRole(IRtcEngineEventHandler.ClientRole.CLIENT_ROLE_AUDIENCE, clientRoleOptions)
    }
    private fun setChannelProfile() {
        Log.e("PodsService","setChannelProfile")
        mRtcEngine?.setChannelProfile(Constants.CHANNEL_PROFILE_LIVE_BROADCASTING)
    }

    private fun joinChannel(mRoomName: String?, token: String?, uid: String?,role:String?) {
        if(role.equals("host",true)){
            setUserRoleAsBroadCaster()
        }else{
            setUserRoleAudience()
        }
        Log.e("PodsService", "joinChannel--$mRoomName--$token")
        mRtcEngine?.setAudioProfile(Constants.AUDIO_PROFILE_SPEECH_STANDARD,Constants.AUDIO_SCENARIO_CHATROOM_ENTERTAINMENT)
        mRtcEngine?.joinChannel(token, mRoomName, "", 0)
    }

    private fun stopService() {
        RtcEngine.destroy()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            stopForeground(true)
        } else {
            stopSelf()
        }
    }

    private fun setBackgroundAudio(url:String?){
        Log.e("agora", "setBackgroundAudio")
        //https://www.soundhelix.com/examples/mp3/SoundHelix-Song-8.mp3
        mRtcEngine?.startAudioMixing(
            url,  // Specifies the absolute path of the local or online music file that you want to play.
            false,  // Sets whether to only play a music file on the local client. true represents that only the local user can hear the music; false represents that both the local user and remote users can hear the music.
            false,  // Sets whether to replace the audio captured by the microphone with a music file. true represents that the user can only hear music; false represents that the user can hear both the music and the audio captured by the microphone.
            -1,  // Sets the number of times to play the music file. 1 represents play the file once.
            0  // Sets the playback position (ms) of the music file. 0 represents that the playback starts at the 0 ms mark of the music file.
        )
    }

    private fun pauseMusic() {
        mRtcEngine?.pauseAudioMixing()
    }

    private val mRtcEventHandler: IRtcEngineEventHandler = object : IRtcEngineEventHandler() {

        override fun onJoinChannelSuccess(channel: String, uid: Int, elapsed: Int) {
            Log.e("agora", "Join channel success, uid: $uid")
            sendBroadcast(
                Intent(POD_ACTION)
                    .putExtra(NOTIFICATION_TEXT, uid)
            )
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            Log.e("agora", "onUserOffline, uid: $uid")
        }

        override fun onConnectionLost() {
            super.onConnectionLost()
            Log.e("agora", "onConnectionLost")
        }

        override fun onError(err: Int) {
            super.onError(err)
            Log.e("agora", err.toString())
        }
    }
}


