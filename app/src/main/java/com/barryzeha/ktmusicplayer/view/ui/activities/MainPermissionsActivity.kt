package com.barryzeha.ktmusicplayer.view.ui.activities

import android.Manifest
import android.content.Intent
import android.os.Build.*
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.barryzeha.core.common.checkPermissions
import com.barryzeha.core.common.getThemeResValue
import com.barryzeha.core.R as coreRes
import com.barryzeha.ktmusicplayer.databinding.ActivityMainPermissionsBinding
import com.barryzeha.ktmusicplayer.service.MusicPlayerService
import com.barryzeha.ktmusicplayer.view.viewmodel.MainViewModel
import com.google.android.material.button.MaterialButton
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainPermissionsActivity : AppCompatActivity() {
    private lateinit var launcherPermission: ActivityResultLauncher<String>
    private lateinit var bind:ActivityMainPermissionsBinding
    private val mainViewModel:MainViewModel by viewModels()
    private  var musicPlayerService:MusicPlayerService?=null

    private val permissionList:MutableList<String> =  if(VERSION.SDK_INT >= VERSION_CODES.TIRAMISU){
        mutableListOf(
            Manifest.permission.POST_NOTIFICATIONS,
            Manifest.permission.READ_MEDIA_AUDIO,
            Manifest.permission.READ_PHONE_STATE,
            // It is required to detect connection and disconnection events of Bluetooth devices when the mobile Bluetooth service is active.
            Manifest.permission.BLUETOOTH_CONNECT
        )
    }else{
        mutableListOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
        )
    }
    private val permissionStatusMap = mutableMapOf<String, Boolean>()
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(getThemeResValue())
        super.onCreate(savedInstanceState)

        bind = ActivityMainPermissionsBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(bind.root)
        ViewCompat.setOnApplyWindowInsetsListener(bind.scrollViewContent) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        showViews()
        setupListeners()
        initCheckPermission()
        activityResultForPermission()
        mainViewModel.serviceInstance.observe(this){(_, mServiceInstance)->
            musicPlayerService = mServiceInstance
        }
    }
    private fun activityResultForPermission(){
        launcherPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()){
            val requestedPermission = permissionStatusMap.keys.last()
            if(it){

                    val button =bind.root.findViewWithTag<MaterialButton>(requestedPermission)
                    button.text=getString(coreRes.string.granted)
                    button.setIconResource(coreRes.drawable.ic_check_rounded)
                    button.iconGravity= MaterialButton.ICON_GRAVITY_END
                    button.isClickable=false
                    checkPermissions()

            }
            permissionStatusMap.remove(requestedPermission)
        }
    }
    private fun showViews()=with(bind){
        bind.tvWelcomeToApp.text = String.format("%s %s" ,getString(coreRes.string.welcomeToApp),applicationInfo.loadLabel(packageManager).toString())
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
            finish()
            startActivity(Intent(this@MainPermissionsActivity, MainActivity::class.java))
        }
    }
    private fun checkPermissions(){
        checkPermissions(this,permissionList){isGranted,permission->
            if(isGranted){
                bind.btnFinish.visibility = View.VISIBLE
                bind.btnFinish.post {
                    bind.scrollViewContent.scrollTo(0,bind.btnFinish.bottom)
                }
            }else{
                bind.btnFinish.visibility = View.GONE
            }
        }
    }

    private fun initCheckPermission(){

        checkPermissions(this,permissionList){isGranted,permissions->
            if(isGranted){
                bind.btnBtPermission.text=getString(coreRes.string.granted);bind.btnBtPermission.isClickable=false
                bind.btnBtPermission.setIconResource(coreRes.drawable.ic_check_rounded)
                bind.btnBtPermission.iconGravity= MaterialButton.ICON_GRAVITY_END

                bind.btnNotifyPermission.text=getString(coreRes.string.granted); bind.btnNotifyPermission.isClickable=false
                bind.btnNotifyPermission.setIconResource(coreRes.drawable.ic_check_rounded)
                bind.btnNotifyPermission.iconGravity= MaterialButton.ICON_GRAVITY_END

                bind.btnReadMediaPermission.text=getString(coreRes.string.granted);bind.btnReadMediaPermission.isClickable=false
                bind.btnReadMediaPermission.setIconResource(coreRes.drawable.ic_check_rounded)
                bind.btnReadMediaPermission.iconGravity= MaterialButton.ICON_GRAVITY_END

                bind.btnWriteStoragePermission.text=getString(coreRes.string.granted);bind.btnWriteStoragePermission.isClickable=false
                bind.btnWriteStoragePermission.setIconResource(coreRes.drawable.ic_check_rounded)
                bind.btnWriteStoragePermission.iconGravity= MaterialButton.ICON_GRAVITY_END

                bind.btnReadStoragePermission.text=getString(coreRes.string.granted);bind.btnReadStoragePermission.isClickable=false
                bind.btnReadStoragePermission.setIconResource(coreRes.drawable.ic_check_rounded)
                bind.btnReadStoragePermission.iconGravity= MaterialButton.ICON_GRAVITY_END

                bind.btnPhonePermission.text=getString(coreRes.string.granted);bind.btnPhonePermission.isClickable=false
                bind.btnPhonePermission.setIconResource(coreRes.drawable.ic_check_rounded)
                bind.btnPhonePermission.iconGravity= MaterialButton.ICON_GRAVITY_END
                bind.btnFinish.visibility = View.VISIBLE
                bind.btnFinish.post {
                    bind.scrollViewContent.scrollTo(0,bind.btnFinish.bottom)
                }

            }else{
                permissions.forEach {(permission, granted)->
                    val button:MaterialButton=bind.root.findViewWithTag<Button>(permission) as MaterialButton
                    if(!granted){
                        button.text=getString(coreRes.string.grant)
                        button.isEnabled=true
                        button.setOnClickListener {
                            permissionStatusMap[permission]=false
                            launcherPermission.launch(permission)
                        }
                    }else{
                        button.text=getString(coreRes.string.granted);button.isClickable=false
                        button.setIconResource(coreRes.drawable.ic_check_rounded)
                        button.iconGravity= MaterialButton.ICON_GRAVITY_END
                    }
                    if(permission==Manifest.permission.READ_PHONE_STATE && granted){
                        // We initialize the call state listener to detect when a phone call is received and pause or continue playback.
                        musicPlayerService?.setupPhoneCallStateReceiver()
                    }
                }
            }
        }
    }

}