package com.barryzeha.ktmusicplayer.view.ui.activities

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.Menu
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.get
import androidx.viewpager2.widget.ViewPager2
import com.barryzeha.audioeffects.ui.activities.MainEqualizerActivity
import com.barryzeha.core.common.HOME_PLAYER
import com.barryzeha.core.common.LIST_PLAYER
import com.barryzeha.core.common.MAIN_FRAGMENT
import com.barryzeha.core.common.MyPreferences
import com.barryzeha.core.common.SONG_LIST_FRAGMENT
import com.barryzeha.core.common.startOrUpdateService
import com.barryzeha.core.model.ServiceSongListener
import com.barryzeha.core.model.entities.PlaylistEntity
import com.barryzeha.ktmusicplayer.databinding.ActivityMainBinding
import com.barryzeha.ktmusicplayer.databinding.MenuItemViewBinding
import com.barryzeha.ktmusicplayer.service.MusicPlayerService
import com.barryzeha.ktmusicplayer.view.ui.adapters.PageCollectionAdapter
import com.barryzeha.ktmusicplayer.view.ui.fragments.ListPlayerFragment
import com.barryzeha.ktmusicplayer.view.ui.fragments.MainPlayerFragment
import com.barryzeha.ktmusicplayer.view.viewmodel.MainViewModel
import com.google.android.material.bottomsheet.BottomSheetBehavior
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.barryzeha.core.R as coreRes
const val PLAYLIST_SUBMENU_ID = 25
const val PLAYLIST_DEFAULT_ID = 0
@AndroidEntryPoint
class MainActivity : AppCompatActivity(), ServiceConnection, MainPlayerFragment.OnFragmentReadyListener{
    internal lateinit var bind:ActivityMainBinding
    private var menu:Menu?=null
    private val mainViewModel: MainViewModel by viewModels()
    private var musicService: MusicPlayerService?=null
    private val launcherAudioEffectActivity = registerForActivityResult(MainEqualizerActivity.MainEqualizerContract()){}
    private var playlists:List<PlaylistEntity> = arrayListOf()
    @Inject
    lateinit var mPrefs:MyPreferences

    private var serviceSongListener:ServiceSongListener?=null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen().apply {
            CoroutineScope(Dispatchers.IO).launch {
                delay(1000)
                setKeepOnScreenCondition{false}
            }
        }
        bind= ActivityMainBinding.inflate(layoutInflater)
        enableEdgeToEdge()

        setContentView(bind.root)
        ViewCompat.setOnApplyWindowInsetsListener(bind.mainDrawerLayout) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        if(savedInstanceState==null){
           mainViewModel.fetchPlaylistWithSongsBy(mPrefs.playlistId,mPrefs.playListSortOption)
        }
        setUpViewPager()
        setUpObservers()
        setUpListeners()
        //mOnBackPressedDispatcher()
    }

    private fun setUpObservers(){

        mainViewModel.fetchSongState()
        mainViewModel.playLists.observe(this){lists->
            this.playlists = lists

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
                        //TODO remover el item
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

                    bind.mainDrawerLayout.closeDrawer(GravityCompat.START)
                }
            }
            true

        }
    }

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        val binder = service as MusicPlayerService.MusicPlayerServiceBinder
        musicService = binder.getService()
        musicService?.setActivity(this)
        serviceSongListener?.onServiceConnected(this,service)
        serviceSongListener?.let{serviceListener->registerSongListener(serviceListener)}

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
    }
    /*private fun mOnBackPressedDispatcher(){
        onBackPressedDispatcher.addCallback(this,object:OnBackPressedCallback(true){
            override fun handleOnBackPressed() {
                if(bind.mainDrawerLayout.isDrawerOpen(GravityCompat.START)){
                    bind.mainDrawerLayout.closeDrawer(GravityCompat.START)
                }else{
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
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
        unbindService(this)
    }
    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        if(bind.mainDrawerLayout.isOpen){
            bind.mainDrawerLayout.closeDrawer(GravityCompat.START)
        }else{
            checkedSelectedMenuDrawerItems()
            if(mPrefs.currentView == SONG_LIST_FRAGMENT){
                if (ListPlayerFragment.btmSheetIsExpanded)ListPlayerFragment.bottomSheetBehavior.state=BottomSheetBehavior.STATE_COLLAPSED
                else {
                    bind.mViewPager.setCurrentItem(MAIN_FRAGMENT, true)
                    bind.navView.menu[MAIN_FRAGMENT].setChecked(true)
                    mPrefs.currentView = MAIN_FRAGMENT
                }

            }else {
                super.onBackPressed()
            }
        }
    }
    override fun onResume() {
        super.onResume()
        checkedSelectedMenuDrawerItems()
    }


}