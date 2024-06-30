package com.barryzeha.ktmusicplayer.view.ui.activities

import android.content.ComponentName
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
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
import java.security.Provider.Service

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), ServiceConnection, ServiceSongListener {
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
        val viewPagerAdapter= PageCollectionAdapter(mainViewModel,this, listOf(HOME_PLAYER, LIST_PLAYER))
        bind.mViewPager.adapter=viewPagerAdapter
        // Para precargar el segundo fragmento mientras se muestra el primero
        bind.mViewPager.offscreenPageLimit=2
    }

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
       this@MainActivity.onServiceConnected(this,service)

        /*val binder = service as MusicPlayerService.MusicPlayerServiceBinder
        musicPlayerService = binder.getService()
        musicPlayerService?.setActivity(this)*/

    }

    override fun onServiceDisconnected(name: ComponentName?) {
        this@MainActivity.onServiceDisconnected()
        //musicPlayerService = null

    }
    fun registerSongListener(songListener: ServiceSongListener){
        musicPlayerService?.setSongController(songListener)
    }
    fun unregisterSongListener(){
        musicPlayerService?.unregisterController()
    }
    override fun play() {
    }

    override fun pause() {
    }

    override fun next() {
    }

    override fun previous() {
    }

    override fun stop() {
    }

    override fun musicState(musicState: MusicState?) {
    }

    override fun currentTrack(musicState: MusicState?) {
    }

    override fun onServiceConnected(conn: ServiceConnection,service: IBinder?) {
        val binder = service as MusicPlayerService.MusicPlayerServiceBinder
        musicPlayerService = binder.getService()
        musicPlayerService?.setActivity(this)
    }

    override fun onServiceBinder(binder: IBinder?) {
        Toast.makeText(this, "Hola", Toast.LENGTH_SHORT).show()
    }

    override fun onServiceDisconnected() {
        musicPlayerService = null
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