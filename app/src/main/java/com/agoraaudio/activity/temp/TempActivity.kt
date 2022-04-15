package com.agoraaudio.activity.temp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.agoraaudio.R

import com.agoraaudio.activity.pod.Constant
import com.agoraaudio.activity.pod.PodsActivity


class TempActivity : AppCompatActivity() {

    private var role="host"
    private val PERMISSION_REQ_ID_RECORD_AUDIO = 22

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_temp)
        findViewById<TextView>(R.id.tvAgoraService).setOnClickListener {
            if (checkSelfPermission(Manifest.permission.RECORD_AUDIO, PERMISSION_REQ_ID_RECORD_AUDIO)) {
                joinWithAgoraService()
            }
        }
        setRole()
    }

    private fun setRole() {
        role="host"
        val radioGroup=findViewById<RadioGroup>(R.id.radioGroup)
        radioGroup.setOnCheckedChangeListener { radioGroup, i ->
            val radio: RadioButton = findViewById(i)
            val selectedType = radio.text
            if(selectedType.equals("Host")){
                role="host"
            }else if(selectedType.equals("Audience")){
                role="Audience"
            }
            Toast.makeText(applicationContext, " ${radio.text}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun joinWithAgoraService() {
        val mRoomName: String = findViewById<EditText>(R.id.etChannelName).text.trim().toString()
        startActivity(Intent(this,PodsActivity::class.java)
            .putExtra(Constant.TOKEN,"006198262ced2f14f3aabdf03bcd45657feIACbMD2Iwg1XgFtIzW0nbo0Hp6Psf5X7YW8Nw2CrHI5p2Q2+1RoAAAAAEAAjgBf2LEhaYgEAAQArSFpi")
            .putExtra(Constant.CHANNEL_NAME,mRoomName)
            .putExtra(Constant.ROLE,role))
    }

    private fun checkSelfPermission(permission: String, requestCode: Int): Boolean {
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) { ActivityCompat.requestPermissions(this, arrayOf(permission), requestCode)
            return false
        }
        return true
    }

}