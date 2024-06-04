package com.barryzeha.ktmusicplayer.view.ui.activities

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.barryzeha.ktmusicplayer.databinding.ActivityMainBinding
import com.barryzeha.ktmusicplayer.service.MusicPlayerService
import com.barryzeha.ktmusicplayer.view.ui.adapters.PageCollectionAdapter
import com.barryzeha.ktmusicplayer.view.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), ServiceConnection {
    private lateinit var bind:ActivityMainBinding
    private val mainViewModel: MainViewModel by viewModels()
    private var musicPlayerService: MusicPlayerService?=null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bind= ActivityMainBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(bind.root)
        ViewCompat.setOnApplyWindowInsetsListener(bind.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setUpViewPager()
        setUpObservers()
    }
    private fun setUpObservers(){
        //mainViewModel.fetchAllSong()

        mainViewModel.musicState.observe(this){
            //Log.e("MAIN-ACTIVITY", it.toString() )
        }
    }
    private fun setUpViewPager(){
        val viewPagerAdapter= PageCollectionAdapter(this, listOf("HomePlayer", "listPlayer"))
        bind.mViewPager.adapter=viewPagerAdapter
    }

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        val binder = service as MusicPlayerService.MusicPlayerServiceBinder
        musicPlayerService = binder.getService()
        musicPlayerService?.setActivity(this)
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        musicPlayerService = null

    }
    private fun startOrUpdateService(): Intent {
        val serviceIntent = Intent (this, MusicPlayerService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(this, serviceIntent)
        } else startService(serviceIntent)

        return serviceIntent
    }
    override fun onStart() {
        super.onStart()
        //if(!requireContext().isServiceRunning(MusicPlayerService::class.java)) {
        bindService(startOrUpdateService(),
            this,
            Context.BIND_AUTO_CREATE
        )
        //}
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(this)
    }
}