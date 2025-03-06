package com.barryzeha.ktmusicplayer.view.ui.activities

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.os.IBinder
import android.view.Menu
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.get
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.viewpager2.widget.ViewPager2
import com.barryzeha.audioeffects.ui.activities.MainEqualizerActivity
import com.barryzeha.core.common.HOME_PLAYER
import com.barryzeha.core.common.LIST_PLAYER
import com.barryzeha.core.common.MAIN_FRAGMENT
import com.barryzeha.core.common.SONG_LIST_FRAGMENT
import com.barryzeha.core.common.checkPermissions
import com.barryzeha.core.common.getThemeResValue
import com.barryzeha.core.common.startOrUpdateService
import com.barryzeha.core.model.ServiceSongListener
import com.barryzeha.core.model.entities.PlaylistEntity
import com.barryzeha.ktmusicplayer.R
import com.barryzeha.ktmusicplayer.common.createNewPlayListDialog
import com.barryzeha.ktmusicplayer.databinding.ActivityMainBinding
import com.barryzeha.ktmusicplayer.databinding.MenuItemViewBinding
import com.barryzeha.ktmusicplayer.service.MusicPlayerService
import com.barryzeha.ktmusicplayer.view.ui.adapters.PageCollectionAdapter
import com.barryzeha.ktmusicplayer.view.ui.fragments.MainPlayerFragment
import com.barryzeha.ktmusicplayer.view.ui.fragments.playlistFragment.ListFragment
import com.barryzeha.ktmusicplayer.view.viewmodel.MainViewModel
import com.google.android.material.bottomsheet.BottomSheetBehavior
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.barryzeha.core.R as coreRes
const val PLAYLIST_SUBMENU_ID = 25
const val PLAYLIST_DEFAULT_ID = 0
@AndroidEntryPoint
class MainActivity : AbsMusicServiceActivity(), ServiceConnection, MainPlayerFragment.OnFragmentReadyListener{
    internal lateinit var bind:ActivityMainBinding
    private var menu:Menu?=null
    private val mainViewModel: MainViewModel by viewModels()
    private var musicService: MusicPlayerService?=null
    private val launcherAudioEffectActivity = registerForActivityResult(MainEqualizerActivity.MainEqualizerContract()){}
    private var playlists:List<PlaylistEntity> = arrayListOf()

    private var serviceSongListener:ServiceSongListener?=null
    private var loadedFinish = true
    private var mOnBackPressedCallback:OnBackPressedCallback?=null
    private var navController:NavController?=null
    private var currentTrackAvailable:Boolean = false

    private val permissionList:MutableList<String> =  if(VERSION.SDK_INT >= VERSION_CODES.TIRAMISU){
        mutableListOf(
            Manifest.permission.POST_NOTIFICATIONS,
            Manifest.permission.READ_MEDIA_AUDIO,
            Manifest.permission.READ_PHONE_STATE,
            // Se requiere para detectar los eventos de conexión y desconexión de dispositivos bluetooth
            // cuando el servicio bluetooth del móvil esté activo.
            Manifest.permission.BLUETOOTH_CONNECT)
    }else{
        mutableListOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(getThemeResValue())
        super.onCreate(savedInstanceState)
        installSplashScreen().apply{
            lifecycleScope.launch {
                delay(1500)
                setKeepOnScreenCondition{false}
            }
        }
        bind= ActivityMainBinding.inflate(layoutInflater)
        setContentView(bind.root)
        //enableEdgeToEdge()
         ViewCompat.setOnApplyWindowInsetsListener(bind.mainDrawerLayout) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)

            insets
        }
        setUpViewPager()
        setUpObservers()
        setUpListeners()
        initCheckPermission()
        //mOnBackPressedDispatcher()
    }
    private fun initCheckPermission(){
        checkPermissions(this,permissionList){isGranted,_->
            val activity:Intent
            if(!isGranted){
                activity=Intent(this,MainPermissionsActivity::class.java)
                startActivity(activity)
                finish()
            }
        }
    }
    private fun setUpObservers(){
        mainViewModel.fetchSongState()
        mainViewModel.currentTrack.observe(this){
            currentTrackAvailable =true
        }
        mainViewModel.serviceInstance.observe(this){(serviceConn, serviceInst)->
            if(!currentTrackAvailable)serviceInst.getStateSaved()
        }
        mainViewModel.playLists.observe(this){lists->
            this.playlists = lists
            addItemOnMenuDrawer(playlists)
        }
        mainViewModel.createdPlayList.observe(this) { insertedRow ->
            if (insertedRow > 0) {
                Toast.makeText(
                    this,
                    com.barryzeha.core.R.string.playlistCreatedMsg,
                    Toast.LENGTH_SHORT
                ).show()
                mainViewModel.fetchPlaylists()
            }
        }
        mainViewModel.navControllerInstance.observe(this){instance->
            navController=instance
        }
    }
    private fun setUpViewPager(){
        val viewPagerAdapter= PageCollectionAdapter(mainViewModel,this, listOf(HOME_PLAYER, LIST_PLAYER))
        bind.mViewPager.adapter=viewPagerAdapter

        // Para precargar el segundo fragmento mientras se muestra el primero
        //bind.mViewPager.offscreenPageLimit=2
        bind.mViewPager.registerOnPageChangeCallback(object: ViewPager2.OnPageChangeCallback(){
              override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                when(position){
                    0->mPrefs.currentView= MAIN_FRAGMENT
                    1->mPrefs.currentView= SONG_LIST_FRAGMENT
                }
                bind.navView.menu[position].setChecked(true)
            }
        })
        menu = bind.navView.menu

    }
   fun addItemOnMenuDrawer(playlists:List<PlaylistEntity>){
        menu?.let{menu->
            var subMenu = menu.findItem(PLAYLIST_SUBMENU_ID)?.subMenu
            if(subMenu==null) {
                subMenu = menu.addSubMenu(Menu.NONE, PLAYLIST_SUBMENU_ID, Menu.NONE, "Playlists")
                subMenu.setHeaderIcon(coreRes.drawable.ic_playlist_select)
            }
            subMenu?.clear()
            // Item para agregar nueva playlist
            val menuItemAdd = subMenu?.add(Menu.NONE,-1,Menu.NONE,"")
            val itemViewAdd = MenuItemViewBinding.inflate(layoutInflater)
            itemViewAdd.menuItemIcon.setImageResource(coreRes.drawable.ic_add)
            menuItemAdd?.setActionView(itemViewAdd.root)
            itemViewAdd.menuItemIcon.setOnClickListener {
                createNewPlayListDialog(this) { playlistName ->
                    mainViewModel.createPlayList(PlaylistEntity(playListName = playlistName))
                }
                bind.mainDrawerLayout.closeDrawer(GravityCompat.START)
            }
            val m = subMenu?.add(Menu.NONE, PLAYLIST_DEFAULT_ID,Menu.NONE,"default")
            m?.setOnMenuItemClickListener {
                mainViewModel.fetchPlaylistWithSongsBy(m.itemId,mPrefs.playListSortOption)
                bind.mViewPager.setCurrentItem(SONG_LIST_FRAGMENT, true)
                mPrefs.currentView = SONG_LIST_FRAGMENT
                bind.navView.menu[MAIN_FRAGMENT].setChecked(false)
                bind.mainDrawerLayout.closeDrawer(GravityCompat.START)
              true
            }
            val existId = mutableSetOf<Int>()

            m?.setIcon(coreRes.drawable.ic_playlist_select)
            playlists.forEachIndexed { index, playlist ->
                // Comprobamos si el item ya existe a través de su id
                if (!existId.contains(playlist.idPlaylist.toInt())) {
                    val itemView = MenuItemViewBinding.inflate(layoutInflater)
                    val m = subMenu?.add(
                        Menu.NONE,
                        playlist.idPlaylist.toInt(),
                        Menu.NONE,
                        playlist.playListName
                    )
                    m?.setActionView(itemView.root)
                    m?.setIcon(coreRes.drawable.ic_playlist_select)
                    existId.add(playlist.idPlaylist.toInt())

                    m?.setOnMenuItemClickListener {
                        mainViewModel.fetchPlaylistWithSongsBy(m.itemId, mPrefs.playListSortOption)
                        bind.mViewPager.setCurrentItem(SONG_LIST_FRAGMENT, true)
                        mPrefs.currentView = SONG_LIST_FRAGMENT
                        bind.navView.menu[MAIN_FRAGMENT].setChecked(false)
                        bind.mainDrawerLayout.closeDrawer(GravityCompat.START)
                        true
                    }
                    itemView.menuItemIcon.setOnClickListener {
                        mainViewModel.deletePlayList(m?.itemId!!.toLong())
                        subMenu?.removeItem(m.itemId)
                    }
                    bind.navView.invalidate()
                }
            }
        }
    }
    fun removeMenuItemDrawer(itemId:Int){
        menu?.let{menu->
            val subMenu = menu.findItem(PLAYLIST_SUBMENU_ID)?.subMenu
            subMenu?.removeItem(itemId)
        }
    }

    // Usamos  la navegación sin el componente de navegación ya que necesitamos el viewPager para deslizarnos
    // y solo el menú para poder mostrar los índices del viewPager programaticamente
    private fun setUpListeners(){
        bind.navView.setNavigationItemSelectedListener {menuItem->
            when(menuItem.itemId){
                coreRes.id.home->{
                    bind.mViewPager.setCurrentItem(MAIN_FRAGMENT,true)
                    bind.mainDrawerLayout.closeDrawer(GravityCompat.START)
                }
                coreRes.id.music_list->{
                    bind.mViewPager.setCurrentItem(SONG_LIST_FRAGMENT,true)
                    bind.mainDrawerLayout.closeDrawer(GravityCompat.START)
                    mPrefs.currentView = SONG_LIST_FRAGMENT
                }
                coreRes.id.settings->{
                    startActivity(Intent(this, SettingsActivity::class.java))
                    bind.mainDrawerLayout.closeDrawer(GravityCompat.START)
                }
                coreRes.id.equalizer->{
                    musicService?.let{service->
                        bind.mainDrawerLayout.closeDrawer(GravityCompat.START)
                        launcherAudioEffectActivity.launch(service.getSessionOrChannelId())
                   }
                }
            }
            true
        }
        bind.navFooter.setNavigationItemSelectedListener { menuItem->
            when(menuItem.itemId) {
                coreRes.id.aboutThis -> {
                    startActivity(Intent(this,AboutThisActivity::class.java))
                    bind.mainDrawerLayout.closeDrawer(GravityCompat.START)
                }
            }
            true
        }
    }
    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        val binder = service as MusicPlayerService.MusicPlayerServiceBinder
        musicService = binder.getService()
        musicPlayerService = musicService
        musicService?.setActivity(this)
        mainViewModel.setServiceInstance(this,musicService!!)
        serviceSongListener?.onServiceConnected(this,service)
        musicService?.setSongController(this)
        if(ContextCompat.checkSelfPermission(this,Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED){
             musicService?.setupPhoneCallStateReceiver()
        }
    }
    override fun onServiceDisconnected(name: ComponentName?) {
         musicService = null
        serviceSongListener?.onServiceDisconnected()
    }

    private fun checkedSelectedMenuDrawerItems(){
        when(mPrefs.currentView){
            MAIN_FRAGMENT->{
                if(bind.mViewPager.currentItem == SONG_LIST_FRAGMENT){
                    bind.navView.menu[SONG_LIST_FRAGMENT].setChecked(true)
                    mPrefs.currentView = SONG_LIST_FRAGMENT
                }else{
                    bind.navView.menu[MAIN_FRAGMENT].setChecked(true)
                }
            }else->{

            }
        }
    }/*
    private fun mOnBackPressedDispatcher(){
        mOnBackPressedCallback = object: OnBackPressedCallback(true){
            override fun handleOnBackPressed() {
                if(bind.mainDrawerLayout.isOpen){
                    bind.mainDrawerLayout.closeDrawer(GravityCompat.START)
                }
                else if(ListFragment.isFiltering){
                    ListFragment.instance?.hideSearchBar()
                }
                else{
                    checkedSelectedMenuDrawerItems()
                    if(mPrefs.currentView == SONG_LIST_FRAGMENT){
                        if (ListFragment.btmSheetIsExpanded)ListFragment.bottomSheetBehavior.state=BottomSheetBehavior.STATE_COLLAPSED
                        else {
                            bind.mViewPager.setCurrentItem(MAIN_FRAGMENT, true)
                            bind.navView.menu[MAIN_FRAGMENT].setChecked(true)
                            mPrefs.currentView = MAIN_FRAGMENT
                        }

                    }else {
                        finish()
                    }
                }
            }
        }
        onBackPressedDispatcher.addCallback(this,mOnBackPressedCallback!!)
    }*/
    // Esperamos a que el primer fragmento cargue completamente para cargar el segundo
    override fun onFragmentReady() {
        CoroutineScope(Dispatchers.Main).launch {
            // Retrasamos 1.5 segundos la carga del segundo fragmento
            delay(1500)
            bind.mViewPager.offscreenPageLimit = 2
        }
    }
    override fun onStart() {
        super.onStart()
        startOrUpdateService(this,MusicPlayerService::class.java,this)
    }
    override fun onDestroy() {
        super.onDestroy()
        musicService?.let{unbindService(this)}
    }
    @Suppress("DEPRECATION")
    override fun onBackPressed() {

        if(bind.mainDrawerLayout.isOpen){
            bind.mainDrawerLayout.closeDrawer(GravityCompat.START)
        }
        else if(ListFragment.isFiltering){
            ListFragment.instance?.hideSearchBar()
        }
        else if(mPrefs.saveFragmentOfNav > 0){
            navController?.navigate(R.id.playlistFragment)
        }
        else{
            checkedSelectedMenuDrawerItems()
            if(mPrefs.currentView == SONG_LIST_FRAGMENT){
                    bind.mViewPager.setCurrentItem(MAIN_FRAGMENT, true)
                    bind.navView.menu[MAIN_FRAGMENT].setChecked(true)
                    mPrefs.currentView = MAIN_FRAGMENT
            }else {
                super.onBackPressed()
            }
        }
    }
    override fun onResume() {
        super.onResume()
        if(mPrefs.themeChanged){
            mPrefs.themeChanged=false
            recreate()
        }
        checkedSelectedMenuDrawerItems()
    }


}