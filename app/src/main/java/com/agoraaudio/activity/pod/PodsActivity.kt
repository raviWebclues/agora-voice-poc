package com.agoraaudio.activity.pod

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import android.view.View
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.agoraaudio.R
import com.agoraaudio.service.NOTIFICATION_TEXT
import com.agoraaudio.service.PodServiceMain
import com.agoraaudio.service.PodState
import com.agoraaudio.service.SERVICE_COMMAND
import org.koin.android.ext.android.inject

const val POD_ACTION = "POD_ACTION"
class PodsActivity : AppCompatActivity() {

    private val viewModel: PodViewModel by inject()
    private val podReceiver: PodsReceiver by lazy { PodsReceiver() }
    private val PERMISSION_REQ_ID_RECORD_AUDIO = 22
    private var role:String?=""
    private var volume:Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pods)
        role = intent.getStringExtra(Constant.ROLE)
        initUI()
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO, PERMISSION_REQ_ID_RECORD_AUDIO)) {
            sendCommandToForegroundService(PodState.START_SERVICE)
            joinChannel()
        }
    }

    private fun initUI() {
        findViewById<TextView>(R.id.tvEndPod).setOnClickListener { endPod() }
        findViewById<TextView>(R.id.tvLeaveQuietly).setOnClickListener { endPod() }
        findViewById<ImageView>(R.id.ivMic).setOnClickListener { muteUnmute() }
        findViewById<ImageView>(R.id.ivBackgroundMusic).setOnClickListener { backgroundMusic() }
        setVolumeBar()
    }

    private fun muteUnmute() {
        if(viewModel.isMute){
            sendCommandToForegroundService(PodState.UN_MUTE)
            findViewById<ImageView>(R.id.ivMic).setImageResource(R.drawable.ic_baseline_mic_24)
        }else{
            sendCommandToForegroundService(PodState.MUTE)
            findViewById<ImageView>(R.id.ivMic).setImageResource(R.drawable.ic_baseline_mic_off_24)
        }
        viewModel.isMute=!viewModel.isMute
    }

    private fun setVolumeBar() {
        val seek = findViewById<SeekBar>(R.id.seekBar)
        seek?.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seek: SeekBar, progress: Int, fromUser: Boolean) {
                volume=seek.progress
                Toast.makeText(this@PodsActivity, "Volume is: " + seek.progress + "%", Toast.LENGTH_SHORT).show()
                sendCommandToForegroundService(PodState.PUBLISH_VOLUME)
                sendCommandToForegroundService(PodState.PLAY_VOLUME)
            }

            override fun onStartTrackingTouch(seek: SeekBar) {
            }

            override fun onStopTrackingTouch(seek: SeekBar) {

            }
        })
    }

    private fun backgroundMusic() {
        if(viewModel.isBackgroundMusicPlay){
            findViewById<SeekBar>(R.id.seekBar).visibility= View.GONE
            sendCommandToForegroundService(PodState.STOP_BACKGROUND_MUSIC)
        }else{
            sendCommandToForegroundService(PodState.PLAY_BACKGROUND_MUSIC)
            findViewById<SeekBar>(R.id.seekBar).visibility= View.VISIBLE
        }
        viewModel.isBackgroundMusicPlay=!viewModel.isBackgroundMusicPlay
    }

    private fun endPod() {
        sendCommandToForegroundService(PodState.STOP_SERVICE)
        finish()
    }

    private fun setUiAsPrRole() {
        if(role.equals("host",true)){
            findViewById<TextView>(R.id.tvEndPod).visibility= View.VISIBLE
            findViewById<TextView>(R.id.tvLeaveQuietly).visibility= View.GONE
            findViewById<ImageView>(R.id.ivMic).visibility= View.VISIBLE
            findViewById<ImageView>(R.id.ivBackgroundMusic).visibility= View.VISIBLE
        }else{
            findViewById<TextView>(R.id.tvEndPod).visibility= View.GONE
            findViewById<TextView>(R.id.tvLeaveQuietly).visibility= View.VISIBLE
            findViewById<ImageView>(R.id.ivMic).visibility= View.GONE
            findViewById<ImageView>(R.id.ivBackgroundMusic).visibility= View.GONE
        }
    }

    private fun checkSelfPermission(permission: String, requestCode: Int): Boolean {
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) { ActivityCompat.requestPermissions(this, arrayOf(permission), requestCode)
            return false
        }
        return true
    }

    private fun sendCommandToForegroundService(podState: PodState) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(this,  getServiceIntent(podState))
        } else {
            this.startService( getServiceIntent(podState))
        }
        viewModel.isForegroundServiceRunning = podState != PodState.STOP_SERVICE
    }

    private fun getServiceIntent(command: PodState) =
        Intent(this, PodServiceMain::class.java).apply {
            putExtra(SERVICE_COMMAND, command as Parcelable)
                .putExtra(Constant.TOKEN,intent.getStringExtra(Constant.TOKEN))
                .putExtra(Constant.CHANNEL_NAME,intent.getStringExtra(Constant.CHANNEL_NAME))
                .putExtra(Constant.ROLE,intent.getStringExtra(Constant.ROLE))
                .putExtra(Constant.MUSIC_URL,"https://www.soundhelix.com/examples/mp3/SoundHelix-Song-8.mp3")
                .putExtra(Constant.PLAY_VOLUME,volume)
                .putExtra(Constant.PUBLISH_VOLUME,volume)
        }

    private fun joinChannel() {
        sendCommandToForegroundService(PodState.JOIN_CHANNEL)
    }

    override fun onDestroy() {
        super.onDestroy()
        sendCommandToForegroundService(PodState.STOP_SERVICE)
    }

    inner class PodsReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == POD_ACTION) updateUi(intent.getIntExtra(NOTIFICATION_TEXT, 0))
        }
    }

    private fun updateUi(uid: Int) {
        Log.e("agora", "*********** uid: $uid")
        setUiAsPrRole()
        findViewById<ProgressBar>(R.id.pbBar).visibility= View.GONE
        findViewById<LinearLayout>(R.id.llMain).visibility= View.VISIBLE
        findViewById<TextView>(R.id.tvUser).text= uid.toString()
    }

    override fun onResume() {
        super.onResume()
        // register foreground service receiver if needed
        if (!viewModel.isReceiverRegistered) {
            registerReceiver(podReceiver, IntentFilter(POD_ACTION))
            viewModel.isReceiverRegistered = true
        }
    }

    override fun onPause() {
        super.onPause()
        // reset foreground service receiver if it's registered
        if (viewModel.isReceiverRegistered) {
            unregisterReceiver(podReceiver)
            viewModel.isReceiverRegistered = false
        }
    }
}