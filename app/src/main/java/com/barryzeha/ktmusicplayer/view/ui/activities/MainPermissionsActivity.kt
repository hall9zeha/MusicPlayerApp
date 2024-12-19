package com.barryzeha.ktmusicplayer.view.ui.activities

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Build.*
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.barryzeha.core.common.checkPermissions
import com.barryzeha.core.R as coreRes
import com.barryzeha.ktmusicplayer.databinding.ActivityMainPermissionsBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainPermissionsActivity : AppCompatActivity() {
    private lateinit var launcherPermission: ActivityResultLauncher<String>
    private lateinit var bind:ActivityMainPermissionsBinding
    val permissionList:MutableList<String> =  if(VERSION.SDK_INT >= VERSION_CODES.TIRAMISU){
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
    private val permissionStatusMap = mutableMapOf<String, Boolean>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen().apply {
            CoroutineScope(Dispatchers.IO).launch {
                delay(500)
                setKeepOnScreenCondition{false}
            }
        }
        bind = ActivityMainPermissionsBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(bind.root)
        ViewCompat.setOnApplyWindowInsetsListener(bind.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        showViews()
        setupListeners()
        initCheckPermission()
        activityResultForPermission()
    }
    private fun activityResultForPermission(){
        launcherPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()){
            val requestedPermission = permissionStatusMap.keys.last()
            if(it){

                    val button =bind.root.findViewWithTag<Button>(requestedPermission)
                    button.text="Allowed"
                    button.isClickable=false
                    checkPermissions()

            }
            permissionStatusMap.remove(requestedPermission)
        }
    }
    private fun showViews()=with(bind){
        if(VERSION.SDK_INT >= VERSION_CODES.TIRAMISU){
           ctlOldPermissions.visibility=View.GONE
           ctlLatestPermissions.visibility=View.VISIBLE
        }else{
           ctlOldPermissions.visibility=View.VISIBLE
           ctlLatestPermissions.visibility=View.GONE
        }
    }
    private fun setupListeners()=with(bind){
        btnFinish.setOnClickListener {
            startActivity(Intent(this@MainPermissionsActivity,MainActivity::class.java))
        }
    }
    private fun checkPermissions(){
        checkPermissions(this,permissionList){isGranted,permissions->
            if(isGranted){
                bind.btnFinish.visibility = View.VISIBLE
            }else{
                bind.btnFinish.visibility = View.GONE
            }
        }
    }

    private fun initCheckPermission(){

        checkPermissions(this,permissionList){isGranted,permissions->
            if(isGranted){
                startActivity(Intent(this,MainActivity::class.java))
                bind.btnBtPermission.text="Allowed";bind.btnBtPermission.isClickable=false
                bind.btnNotifyPermission.text="Allowed"; bind.btnNotifyPermission.isClickable=false
                bind.btnReadMediaPermission.text="Allowed";bind.btnReadMediaPermission.isClickable=false
                bind.btnWriteStoragePermission.text="Allowed";bind.btnWriteStoragePermission.isClickable=false
                bind.btnReadStoragePermission.text="Allowed";bind.btnReadStoragePermission.isClickable=false
            }else{
                permissions.forEach {(permission, granted)->
                    if(!granted){
                        val button=bind.root.findViewWithTag<Button>(permission)
                        button.text="Allow"
                        button.setOnClickListener {
                            permissionStatusMap[permission]=false
                            launcherPermission.launch(permission)
                        }
                    }
                }
            }
        }
    }
}