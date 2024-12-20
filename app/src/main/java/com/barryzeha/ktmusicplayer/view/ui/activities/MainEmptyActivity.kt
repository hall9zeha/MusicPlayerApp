package com.barryzeha.ktmusicplayer.view.ui.activities

import android.Manifest
import android.content.Intent
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.barryzeha.core.common.checkPermissions
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainEmptyActivity : AppCompatActivity() {
    private val permissionList:MutableList<String> =  if(VERSION.SDK_INT >= VERSION_CODES.TIRAMISU){
        mutableListOf(
            Manifest.permission.POST_NOTIFICATIONS,
            Manifest.permission.READ_MEDIA_AUDIO,
            // Se requiere para detectar los eventos de conexión y desconexión de dispositivos bluetooth
            // cuando el servicio bluetooth del móvil esté activo.
            Manifest.permission.BLUETOOTH_CONNECT)
    }else{
        mutableListOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        installSplashScreen().apply {
            CoroutineScope(Dispatchers.IO).launch {
                delay(500)
                setKeepOnScreenCondition{false}
            }
        }
        initCheckPermission()
    }
    private fun initCheckPermission(){

        checkPermissions(this,permissionList){isGranted,_->
            val activity:Intent
            if(isGranted){
                activity=Intent(this,MainActivity::class.java)
            }else{
                activity=Intent(this,MainPermissionsActivity::class.java)
            }
            startActivity(activity)
            finish()
        }
    }
}