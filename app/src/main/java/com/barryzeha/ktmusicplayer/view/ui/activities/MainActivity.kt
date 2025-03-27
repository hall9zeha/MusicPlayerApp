package com.barryzeha.ktmusicplayer.view.ui.activities

import android.Manifest
import android.content.Intent
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.view.Menu
import android.widget.Toast
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
import com.barryzeha.core.model.entities.PlaylistEntity
import com.barryzeha.ktmusicplayer.R
import com.barryzeha.ktmusicplayer.common.createNewPlayListDialog
import com.barryzeha.ktmusicplayer.databinding.ActivityMainBinding
import com.barryzeha.ktmusicplayer.databinding.MenuItemViewBinding
import com.barryzeha.ktmusicplayer.view.ui.adapters.ViewPagerAdapter
import com.barryzeha.ktmusicplayer.view.ui.fragments.MainPlayerFragment
import com.barryzeha.ktmusicplayer.view.ui.fragments.playlistFragment.ListFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.barryzeha.core.R as coreRes

const val PLAYLIST_SUBMENU_ID = 25
const val PLAYLIST_DEFAULT_ID = 0
@AndroidEntryPoint
class MainActivity : AbsMusicServiceActivity(),  MainPlayerFragment.OnFragmentReadyListener{
    internal lateinit var bind:ActivityMainBinding
    private var menu:Menu?=null
    private val launcherAudioEffectActivity = registerForActivityResult(MainEqualizerActivity.MainEqualizerContract()){}
    private var playlists:List<PlaylistEntity> = arrayListOf()

    private var navController:NavController?=null
    private var currentTrackAvailable:Boolean = false

    private val permissionList:MutableList<String> =  if(VERSION.SDK_INT >= VERSION_CODES.TIRAMISU){
        mutableListOf(
            Manifest.permission.POST_NOTIFICATIONS,
            Manifest.permission.READ_MEDIA_AUDIO,
            Manifest.permission.READ_PHONE_STATE,
            // It is required to detect connection and disconnection events of Bluetooth devices when the mobile Bluetooth service is active.
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
        val viewPagerAdapter= ViewPagerAdapter(mainViewModel,this, listOf(HOME_PLAYER, LIST_PLAYER))
        bind.mViewPager.adapter=viewPagerAdapter

        // To preload the second fragment while displaying the first one
        // bind.mViewPager.offscreenPageLimit=2
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
            // Item to add new playlist
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
            val subMenuPlaylist = subMenu?.add(Menu.NONE, PLAYLIST_DEFAULT_ID,Menu.NONE,"default")
            subMenuPlaylist?.setOnMenuItemClickListener {
                mainViewModel.fetchPlaylistWithSongsBy(subMenuPlaylist.itemId,mPrefs.playListSortOption)
                bind.mViewPager.setCurrentItem(SONG_LIST_FRAGMENT, true)
                mPrefs.currentView = SONG_LIST_FRAGMENT
                bind.navView.menu[MAIN_FRAGMENT].setChecked(false)
                bind.mainDrawerLayout.closeDrawer(GravityCompat.START)
              true
            }
            val existId = mutableSetOf<Int>()

            subMenuPlaylist?.setIcon(coreRes.drawable.ic_playlist_select)
            playlists.forEachIndexed { index, playlist ->
                // We check if the item already exists through its id
                if (!existId.contains(playlist.idPlaylist.toInt())) {
                    val itemView = MenuItemViewBinding.inflate(layoutInflater)
                    val menuItemPlaylist = subMenu?.add(
                        Menu.NONE,
                        playlist.idPlaylist.toInt(),
                        Menu.NONE,
                        playlist.playListName
                    )
                    menuItemPlaylist?.setActionView(itemView.root)
                    menuItemPlaylist?.setIcon(coreRes.drawable.ic_playlist_select)
                    existId.add(playlist.idPlaylist.toInt())

                    menuItemPlaylist?.setOnMenuItemClickListener {
                        mainViewModel.fetchPlaylistWithSongsBy(menuItemPlaylist.itemId, mPrefs.playListSortOption)
                        bind.mViewPager.setCurrentItem(SONG_LIST_FRAGMENT, true)
                        mPrefs.currentView = SONG_LIST_FRAGMENT
                        bind.navView.menu[MAIN_FRAGMENT].setChecked(false)
                        bind.mainDrawerLayout.closeDrawer(GravityCompat.START)
                        true
                    }
                    itemView.menuItemIcon.setOnClickListener {
                        mainViewModel.deletePlayList(menuItemPlaylist?.itemId!!.toLong())
                        subMenu?.removeItem(menuItemPlaylist.itemId)
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

    // We use navigation without the navigation component since we need the viewPager to scroll and
    // only the menu to be able to show the viewPager indexes programmatically
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
                    musicPlayerService?.let{service->
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
    /*
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

    // We wait for the first fragment to load completely before loading the second one.
    override fun onFragmentReady() {
        CoroutineScope(Dispatchers.Main).launch {
            // We delayed the loading of the second fragment by 1.5 seconds
            delay(1500)
            bind.mViewPager.offscreenPageLimit = 2
        }
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