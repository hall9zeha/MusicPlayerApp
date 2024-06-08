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
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.barryzeha.core.common.HOME_PLAYER
import com.barryzeha.core.common.LIST_PLAYER
import com.barryzeha.core.common.isServiceRunning
import com.barryzeha.core.common.linkToService
import com.barryzeha.core.model.entities.MusicState
import com.barryzeha.core.model.entities.SongState
import com.barryzeha.core.model.entities.SongStateWithDetail
import com.barryzeha.ktmusicplayer.databinding.ActivityMainBinding
import com.barryzeha.ktmusicplayer.service.MusicPlayerService
import com.barryzeha.ktmusicplayer.view.ui.adapters.PageCollectionAdapter
import com.barryzeha.ktmusicplayer.view.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
        mainViewModel.fetchSongState()
        mainViewModel.musicState.observe(this){
            //Log.e("MAIN-ACTIVITY", it.toString() )
        }
        mainViewModel.songState.observe(this){songState->

        }
    }
    private fun setUpViewPager(){
        val viewPagerAdapter= PageCollectionAdapter(this, listOf(HOME_PLAYER, LIST_PLAYER))
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
   override fun onStart() {
        super.onStart()
        linkToService(this,MusicPlayerService::class.java,this)
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(this)
    }
}