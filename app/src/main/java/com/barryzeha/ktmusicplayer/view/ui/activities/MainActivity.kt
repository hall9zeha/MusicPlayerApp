package com.barryzeha.ktmusicplayer.view.ui.activities

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.barryzeha.core.common.HOME_PLAYER
import com.barryzeha.core.common.LIST_PLAYER
import com.barryzeha.core.common.startOrUpdateService
import com.barryzeha.core.model.ServiceSongListener
import com.barryzeha.core.model.entities.MusicState
import com.barryzeha.ktmusicplayer.databinding.ActivityMainBinding
import com.barryzeha.ktmusicplayer.service.MusicPlayerService
import com.barryzeha.ktmusicplayer.view.ui.adapters.PageCollectionAdapter
import com.barryzeha.ktmusicplayer.view.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), ServiceConnection{
    private lateinit var bind:ActivityMainBinding
    private val mainViewModel: MainViewModel by viewModels()
    private var musicService: MusicPlayerService?=null


    private var serviceSongListener:ServiceSongListener?=null
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
        val viewPagerAdapter= PageCollectionAdapter(mainViewModel,this, listOf(HOME_PLAYER, LIST_PLAYER))
        bind.mViewPager.adapter=viewPagerAdapter
        // Para precargar el segundo fragmento mientras se muestra el primero
        bind.mViewPager.offscreenPageLimit=2
    }

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        val binder = service as MusicPlayerService.MusicPlayerServiceBinder
        musicService = binder.getService()
        musicService?.setActivity(this)
        serviceSongListener?.onServiceConnected(this,service)
        registerSongListener(serviceSongListener!!)
        mainViewModel.setServiceInstance(this,musicService!!)
    }
    override fun onServiceDisconnected(name: ComponentName?) {
         musicService = null
        serviceSongListener?.onServiceDisconnected()
    }
    fun registerSongListener(songListener: ServiceSongListener){
        this.serviceSongListener=songListener
        musicService?.setSongController(serviceSongListener!!)

    }
    fun unregisterSongListener(){
        musicService?.unregisterController()
    }

    override fun onStart() {
        super.onStart()
        startOrUpdateService(this,MusicPlayerService::class.java,this)
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(this)

    }

}