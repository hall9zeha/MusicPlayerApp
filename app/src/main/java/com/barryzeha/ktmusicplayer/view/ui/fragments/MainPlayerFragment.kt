package com.barryzeha.ktmusicplayer.view.ui.fragments

import android.animation.AnimatorInflater
import android.animation.AnimatorSet
import android.annotation.SuppressLint
import android.content.Context
import android.content.ServiceConnection
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.SeekBar
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import com.barryzeha.audioeffects.ui.activities.MainEqualizerActivity
import com.barryzeha.core.common.AB_LOOP
import com.barryzeha.core.common.CLEAR_MODE
import com.barryzeha.core.common.MAIN_FRAGMENT
import com.barryzeha.core.common.REPEAT_ALL
import com.barryzeha.core.common.REPEAT_ONE
import com.barryzeha.core.common.SHUFFLE
import com.barryzeha.core.common.createTime
import com.barryzeha.core.common.getBitmap
import com.barryzeha.core.common.getEmbeddedSyncedLyrics
import com.barryzeha.core.common.getSongMetadata
import com.barryzeha.core.common.loadImage
import com.barryzeha.core.common.startOrUpdateService
import com.barryzeha.core.model.entities.MusicState
import com.barryzeha.core.model.entities.SongEntity
import com.barryzeha.core.model.entities.SongMode
import com.barryzeha.ktmusicplayer.R
import com.barryzeha.ktmusicplayer.common.animateButtonsAbLoop
import com.barryzeha.ktmusicplayer.common.changeBackgroundColor
import com.barryzeha.ktmusicplayer.common.changeColorOfIcon
import com.barryzeha.ktmusicplayer.databinding.FragmentMainPlayerBinding
import com.barryzeha.ktmusicplayer.lyrics.CoverLrcView
import com.barryzeha.ktmusicplayer.service.MusicPlayerService
import com.barryzeha.ktmusicplayer.view.ui.activities.MainActivity
import com.barryzeha.ktmusicplayer.view.ui.dialog.SongInfoDialogFragment
import com.barryzeha.ktmusicplayer.view.ui.fragments.playlistFragment.ListFragment
import com.barryzeha.library.components.DiscCoverView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.barryzeha.core.R as coreRes

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

@AndroidEntryPoint
class MainPlayerFragment : BaseFragment(R.layout.fragment_main_player),ListFragment.OnFinishedLoadSongs {

    private var param1: String? = null
    private var param2: String? = null
    private var bind:FragmentMainPlayerBinding ? = null
    private var currentMusicState = MusicState()
    private val launcherAudioEffectActivity: ActivityResultLauncher<Int> = registerForActivityResult(MainEqualizerActivity.MainEqualizerContract()){}
    private var isFavorite:Boolean = false
    private var listener: OnFragmentReadyListener? = null
    // Forward and rewind
    private var fastForwardingOrRewind = false
    private var fastForwardOrRewindHandler: Handler? = null
    private var forwardOrRewindRunnable:Runnable?=null
    private var coverViewClicked=false
    private var frontAnimator:AnimatorSet?=null
    private var backAnimator:AnimatorSet?=null
    private var listFragmentInstance:ListFragment?=null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
            instance=this
            bind=FragmentMainPlayerBinding.bind(view)
            // Important is necessary setSelected to textview for able marquee autoscroll when text is long than textView size
            //setUpScrollOnTextViews()
            setUpObservers()
            setUpListeners()
            setupAnimator()
            listener?.onFragmentReady()
            bind?.tvNumberSong?.text = String.format("#%s/%s",if (mPrefs.currentIndexSong > -1) mPrefs.currentIndexSong else 0,mPrefs.totalItemSongs)
    }
    /*private fun tryBlurBackground(){
        bind?.colorBackground?.let {
            Glide.with(this)
                .load(currentMusicState.albumArt)
                .transform(BlurTransformation.Builder(requireContext()).blurRadius(20f).build())
                .error(Glide.with(this).load(ColorDrawable(Color.DKGRAY)).fitCenter())
                .into(bind?.colorBackground!!)
        }
    }*/
    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as? OnFragmentReadyListener
    }
    private fun setUpScrollOnTextViews()=with(bind){
        this?.let{
            tvSongArtist.setSelected(true)
            tvSongAlbum.setSelected(true)
            tvSongDescription.setSelected(true);
        }
    }
    private fun discCoverViewIsEnable():Boolean{
        return defaultPrefs.getBoolean("coverStyle",false)
    }
    @SuppressLint("ResourceType")
    private fun checkPlayerSongModePreferences()=with(bind){
        this?.let {
            val mode = mPrefs.songMode
            val colorOn = changeColorOfIcon(requireContext(), true)
            val colorOff = changeColorOfIcon(requireContext(), false)

            btnRepeat.setIconResource(coreRes.drawable.ic_repeat_all)
            btnRepeat.iconTint = colorOff
            btnShuffle.iconTint = colorOff
            btnAbLoop.iconTint = colorOff
            when (mode) {
                SongMode.RepeatOne.ordinal -> {
                    btnRepeat.setIconResource(coreRes.drawable.ic_repeat_one)
                    btnRepeat.iconTint = colorOn
                }
                SongMode.RepeatAll.ordinal ->  btnRepeat.iconTint = colorOn
                SongMode.Shuffle.ordinal ->  btnShuffle.iconTint = colorOn
                SongMode.AbLoop.ordinal ->   btnAbLoop.iconTint = colorOn
                else -> {

                }
            }
        }
    }
    private fun setUpObservers(){
        (bind?.ivDiscMusicCover as ImageView).loadImage(coreRes.mipmap.ic_launcher)
        (bind?.ivMusicCover as ImageView).loadImage(coreRes.mipmap.ic_launcher)
        mainViewModel.fragmentInstance.observe(viewLifecycleOwner){instance->
            if(instance is ListFragment) listFragmentInstance = instance as ListFragment
        }
        mainViewModel.fetchAllSongFromMain()
        mainViewModel.allSongFromMain.observe(viewLifecycleOwner){songs->
            CoroutineScope(Dispatchers.IO).launch {
                 withContext(Dispatchers.Main) {
                    setNumberOfTrack(mPrefs.idSong)
                }
            }
        }
        mainViewModel.currentTrack.observe(viewLifecycleOwner){
           it?.let{currentTrack->
               mainViewModel.checkIfIsFavorite(currentTrack.idSong)
               updateUIOnceTime(currentTrack)
               setNumberOfTrack(mPrefs.idSong)
           }
        }
        mainViewModel.musicState.observe(viewLifecycleOwner){
            updateUI(it)
        }
        mainViewModel.isPlaying.observe(viewLifecycleOwner){statePlay->
            checkIfDiscCoverViewIsRotating(statePlay)
            if (statePlay) {
                bind?.btnMainPlay?.setIconResource(coreRes.drawable.ic_circle_pause)
            }else{
                bind?.btnMainPlay?.setIconResource(coreRes.drawable.ic_play)
            }
        }
        mainViewModel.currentSongListPosition.observe(viewLifecycleOwner){currentPosition->
            musicPlayerService?.setCurrentSongPosition(currentPosition)
        }

        mainViewModel.isFavorite.observe(viewLifecycleOwner){isFavorite->
            this.isFavorite = isFavorite
            bind?.btnFavorite?.setIconResource(if(isFavorite)coreRes.drawable.ic_favorite_fill else coreRes.drawable.ic_favorite)
            //For change state of the favorite action in media notify
            musicPlayerService?.checkIfSongIsFavorite(mPrefs.idSong)

        }
        mainViewModel.deletedRow.observe(viewLifecycleOwner){deleteRow->
            if(deleteRow>0){
                setNumberOfTrack(currentMusicState.idSong)
            }
        }
        mainViewModel.deleteAllRows.observe(viewLifecycleOwner){deleteAllRows->
            if(deleteAllRows>0){
                setNumberOfTrack(currentMusicState.idSong)
            }
        }
        mainViewModel.isSongTagEdited.observe(viewLifecycleOwner){songEntity->
            songEntity?.let {song->
                val meta = getSongMetadata(requireContext(),song.pathLocation)
                meta?.let {
                    val updateSongInfo = currentMusicState.copy(
                        title = meta.title,
                        album = meta.album,
                        artist = meta.artist)
                    mainViewModel.setCurrentTrack(updateSongInfo)
                    musicPlayerService?.updateNotify(updateSongInfo)
                }
            }
        }
    }
    private fun updateUIOnceTime(musicState: MusicState)=with(bind){
        this?.let {
            val albumArt = getBitmap(requireContext(),musicState.songPath)
            tvSongAlbum.text = musicState.album
            tvSongArtist.text = musicState.artist
            tvSongDescription.text = musicState.title
            (ivDiscMusicCover as ImageView).loadImage(albumArt!!,musicState.nextOrPrev)
            (ivMusicCover as ImageView).loadImage(albumArt!!,musicState.nextOrPrev)

            mainSeekBar.max = musicState.duration.toInt()
            tvSongTimeRest.text = createTime(musicState.currentDuration).third
            tvSongTimeCompleted.text = createTime(musicState.duration).third
            currentMusicState = musicState

            //tryBlurBackground()
            mainViewModel.saveStatePlaying(musicPlayerService?.playingState()!!)
            updateService()
            if(discCoverViewIsEnable()) {
                // We stop the animation for each song change so that the image appears correctly and not rotated.
                (bind?.ivDiscMusicCover as DiscCoverView).end()
                CoroutineScope(Dispatchers.IO).launch{
                    delay(500)
                    withContext(Dispatchers.Main) {
                        // If it was playing, we start the animation again when changing the song.
                        if (mPrefs.isPlaying) (bind?.ivDiscMusicCover as DiscCoverView).start()
                    }
                }
            }
            if(mPrefs.songMode != AB_LOOP){
                bind?.btnAbLoop?.backgroundTintList=changeBackgroundColor(requireContext(),false)}
        }
    }
    private fun updateUI(musicState: MusicState)=with(bind){
        this?.let {
            currentMusicState = musicState
            mPrefs.currentPosition = musicState.currentDuration
            mainSeekBar.progress = musicState.currentDuration.toInt()
            tvSongTimeRest.text = createTime(musicState.currentDuration).third
            lrcView?.updateTime(musicState.currentDuration)
        }
    }
    private fun checkIfDiscCoverViewIsRotating(isPlaying:Boolean){
        if(discCoverViewIsEnable()) {
                    if (isPlaying) (bind?.ivDiscMusicCover as DiscCoverView).resume()
                    else (bind?.ivDiscMusicCover as DiscCoverView).pause()
                }
    }
    fun setNumberOfTrack(songId:Long? = null){
        CoroutineScope(Dispatchers.IO).launch {
        if(songId != null && songId >-1) {
                val song = listFragmentInstance?.musicListAdapter?.getSongById(songId.toLong())
               song?.let {
               val (itemNumOnList, _) = listFragmentInstance?.musicListAdapter?.getPositionByItem(song as SongEntity)
                    ?: Pair(0, 0)
                withContext(Dispatchers.Main) {
                    bind?.tvNumberSong?.text = String.format(
                        "#%s/%s",
                        if (mPrefs.currentIndexSong > -1) itemNumOnList else 0,
                        musicPlayerService?.getSongsList()?.count()
                    )
                }
            }
        }
        }
    }
    private fun checkCoverViewStyle()=with(bind){
        this?.let {
            if (discCoverViewIsEnable()) {
                cardCoverView.visibility = View.GONE
                ivDiscMusicCover.visibility = View.VISIBLE
                checkIfDiscCoverViewIsRotating(mPrefs.isPlaying)
            } else {
                ivDiscMusicCover.visibility = View.GONE
                cardCoverView.visibility = View.VISIBLE
            }
        }
    }
    private fun loadLyric(){
        val lyricsString = getEmbeddedSyncedLyrics(currentMusicState.songPath)
        lyricsString?.let{lyric->
            bind?.lrcView?.loadLrc(lyric)
            bind?.lrcView?.apply {
                setCurrentColor(ContextCompat.getColor(context, coreRes.color.primaryColor))
                setTimeTextColor(ContextCompat.getColor(context, coreRes.color.primaryColor))
                setTimelineColor(ContextCompat.getColor(context, coreRes.color.lrc_timeline_color))
                setTimelineTextColor(ContextCompat.getColor(context, coreRes.color.lrc_timeline_text_color))
                setDraggable(true, CoverLrcView.OnPlayClickListener {
                    return@OnPlayClickListener true
                })
            }
        }
    }
    private fun showLyricView(enable:Boolean){
        if(enable) {
            if (discCoverViewIsEnable()) bind?.ivDiscMusicCover?.visibility = View.GONE
            else bind?.ivMusicCover?.visibility = View.GONE

        }else{
            if (discCoverViewIsEnable()) bind?.ivDiscMusicCover?.visibility = View.VISIBLE
            else bind?.ivMusicCover?.visibility = View.VISIBLE
        }
    }
    private fun setupAnimator(){
        frontAnimator= AnimatorInflater.loadAnimator(requireContext(),coreRes.anim.front_animator) as AnimatorSet
        backAnimator = AnimatorInflater.loadAnimator(requireContext(),coreRes.anim.back_animator) as AnimatorSet
    }

    private fun setAlbumCoverViewAnimator(frontView:Any?, backView:Any?){
        if(!coverViewClicked){
            (backView as? View)?.visibility = View.VISIBLE
            frontAnimator?.setTarget(frontView)
            backAnimator?.setTarget(backView)
            frontAnimator?.start()
            backAnimator?.start()
        }else{
            frontAnimator?.setTarget(backView)
            backAnimator?.setTarget(frontView)
            backAnimator?.start()
            frontAnimator?.start()
            (backView as? View)?.visibility = View.GONE
        }
    }
    @SuppressLint("ResourceType", "ClickableViewAccessibility")
    private fun setUpListeners()=with(bind){
        this?.let {
            checkCoverViewStyle()
            btnLyric?.setOnClickListener{
                lrcView?.let {
                    if (!coverViewClicked) {
                        showLyricView(true)
                        loadLyric()
                        setAlbumCoverViewAnimator(if(discCoverViewIsEnable())ivDiscMusicCover else cardCoverView, lrcView)
                        coverViewClicked = true
                    } else {
                        showLyricView(false)
                        setAlbumCoverViewAnimator(if(discCoverViewIsEnable())ivDiscMusicCover else cardCoverView, lrcView)
                        coverViewClicked = false
                    }
                }
            }
            ivMusicCover.setOnClickListener{
                lrcView?.let {
                    if (!coverViewClicked) {
                        setAlbumCoverViewAnimator(cardCoverView, lrcView)
                        showLyricView(true)
                        loadLyric()
                        coverViewClicked = true
                    }
                }
            }
            ivDiscMusicCover.setOnClickListener{
                lrcView?.let {
                    if (!coverViewClicked) {
                        showLyricView(true)
                        loadLyric()
                        setAlbumCoverViewAnimator(ivDiscMusicCover, lrcView)
                        coverViewClicked = true
                    }
                }
            }
            // When lrcView is full, the view's click event can be used.
            lrcView?.setOnClickListener{
                showLyricView(false)
                setAlbumCoverViewAnimator(if(discCoverViewIsEnable())ivDiscMusicCover else cardCoverView,lrcView)
                coverViewClicked=false
            }
            // When lrcView is empty only the click event on rootView will work
            contentCover.setOnClickListener{
                showLyricView(false)
                setAlbumCoverViewAnimator(if(discCoverViewIsEnable())ivDiscMusicCover else cardCoverView,lrcView)
                coverViewClicked=false
            }

            btnMainMenu?.setOnClickListener {
                (activity as MainActivity).bind.mainDrawerLayout.openDrawer(GravityCompat.START)
            }
            btnMainPlay.setOnClickListener {
                if (musicPlayerService?.getSongsList()?.size!! > 0) {
                    if (!currentMusicState.isPlaying && currentMusicState.duration <= 0) {
                        musicPlayerService?.getSongsList()?.get(0)?.let{song->
                            musicPlayerService?.startPlayer(song)
                            btnMainPlay.setIconResource(coreRes.drawable.ic_circle_pause)
                            mainViewModel.saveStatePlaying(musicPlayerService?.playingState()!!)
                        }

                    } else {
                        if (musicPlayerService?.playingState()!!) {
                            musicPlayerService?.pausePlayer()
                            mainViewModel.saveStatePlaying(musicPlayerService?.playingState()!!)
                            if(discCoverViewIsEnable()) (bind?.ivDiscMusicCover as DiscCoverView).pause()

                        } else {
                            musicPlayerService?.resumePlayer()
                            mainViewModel.saveStatePlaying(musicPlayerService?.playingState()!!)
                            if(discCoverViewIsEnable()) (bind?.ivDiscMusicCover as DiscCoverView).resume()
                        }
                    }
                }
            }
            btnMainPrevious.setOnClickListener {
                checkCoverViewStyle()
                if (musicPlayerService?.getCurrentSongPosition()!! > 0) {
                       musicPlayerService?.prevSong()
                }
            }
            btnMainNext.setOnClickListener {
                checkCoverViewStyle()
                if (musicPlayerService?.getCurrentSongPosition()!! < listFragmentInstance?.musicListAdapter?.itemCount!! - 1) {
                    musicPlayerService?.nextSong()
                } else {
                    getSongOfList(0)?.let{song->
                        musicPlayerService?.startPlayer(song)
                    }
                }
            }
            btnMainNext.setOnLongClickListener {
                fastForwardOrRewind(isForward = true)
                true
            }
            btnMainPrevious.setOnLongClickListener {
                fastForwardOrRewind(isForward=false)
                true
            }
            mainSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                var userSelectPosition = 0
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    if (fromUser) {
                        tvSongTimeRest.text = createTime(progress.toLong()).third
                        userSelectPosition = progress
                    }
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    musicPlayerService?.setPlayerProgress(seekBar?.progress?.toLong()!!)
                    mainSeekBar.progress = userSelectPosition

                }
            })
            btnRepeat.setOnClickListener {
                    musicPlayerService?.stopAbLoop()
                    when (mPrefs.songMode) {
                        SongMode.RepeatOne.ordinal -> {
                            //  Third: deactivate modes
                            mPrefs.songMode = CLEAR_MODE
                            checkPlayerSongModePreferences()
                        }
                        SongMode.RepeatAll.ordinal -> {
                            // Second: repeat one
                            mPrefs.songMode = REPEAT_ONE
                            checkPlayerSongModePreferences()
                        }
                        else -> {
                            // First: active repeat All
                            mPrefs.songMode= REPEAT_ALL
                            checkPlayerSongModePreferences()
                        }
                    }
            }
            btnShuffle.setOnClickListener {
                musicPlayerService?.stopAbLoop()
                when(mPrefs.songMode){
                        SongMode.Shuffle.ordinal->{
                            mPrefs.songMode = CLEAR_MODE
                            checkPlayerSongModePreferences()
                    }
                    else->{
                        mPrefs.songMode= SHUFFLE
                        checkPlayerSongModePreferences()
                    }
                }
            }
            btnFavorite.setOnClickListener {
                if(!isFavorite){
                    mainViewModel.updateFavoriteSong(true,mPrefs.idSong)
                    //isFavorite=true
                }else{
                    mainViewModel.updateFavoriteSong(false,mPrefs.idSong)
                    //isFavorite=false
                }
            }
            btnAbLoop.setOnClickListener{
                //
                if(mPrefs.songMode == AB_LOOP){
                    musicPlayerService?.stopAbLoop()
                    mPrefs.songMode = CLEAR_MODE
                    checkPlayerSongModePreferences()
                }else {
                    btnAbLoop.visibility = View.GONE
                    btnALoop.visibility = View.VISIBLE
                    animateButtonsAbLoop(btnALoop)
                }
            }
            btnALoop.setOnClickListener{
                btnALoop.clearAnimation()
                btnALoop.visibility = View.GONE
                btnBLoop.visibility = View.VISIBLE
                musicPlayerService?.setStartPositionForAbLoop()
                animateButtonsAbLoop(btnBLoop)
            }
            btnBLoop.setOnClickListener{
                btnBLoop.clearAnimation()
                btnBLoop.visibility = View.GONE
                btnAbLoop.visibility = View.VISIBLE
                musicPlayerService?.setEndPositionAbLoop()
                mPrefs.songMode = AB_LOOP
                checkPlayerSongModePreferences()
            }
            btnMainEq?.setOnClickListener{
                launcherAudioEffectActivity.launch(musicPlayerService?.getSessionOrChannelId()!!)
            }
            btnInfo.setOnClickListener{
                SongInfoDialogFragment.newInstance(SongEntity(id = currentMusicState.idSong, pathLocation =currentMusicState.songPath))
                    .show(parentFragmentManager,SongInfoDialogFragment::class.simpleName)
            }
        }
    }
    private fun fastForwardOrRewind(isForward:Boolean){
        fastForwardOrRewindHandler = Handler(Looper.getMainLooper())
        forwardOrRewindRunnable = Runnable{
            if(isForward)fastForwardingOrRewind = bind?.btnMainNext?.isPressed!!
            else  fastForwardingOrRewind = bind?.btnMainPrevious?.isPressed!!
            if(fastForwardingOrRewind){
                if(isForward){musicPlayerService?.fastForward()}
                else{ musicPlayerService?.fastRewind() }
            }else{fastForwardOrRewindHandler?.removeCallbacks(forwardOrRewindRunnable!!) }
            fastForwardOrRewindHandler?.postDelayed(forwardOrRewindRunnable!!,200)
        }
        fastForwardOrRewindHandler?.post(forwardOrRewindRunnable!!)
    }
    private fun getSongOfList(position:Int): SongEntity?{
        if(mPrefs.currentIndexSong>-1) {
            mPrefs.currentIndexSong = position.toLong()
            mainViewModel.setCurrentPosition(position)
            musicPlayerService?.getSongsList()?.let{songsList->
               return songsList[position]
           }?:run{
                return null
            }
        }else{
            mainViewModel.setCurrentPosition(0)
            musicPlayerService?.getSongsList()?.let{songsList->
                return songsList[0]
            }?:run { return null }
       }
    }
    private fun updateService(){
        serviceConnection?.let{
        startOrUpdateService(requireContext(),MusicPlayerService::class.java,it,currentMusicState)
        }
    }

    override fun play() {
        super.play()
        bind?.btnMainPlay?.setIconResource(coreRes.drawable.ic_circle_pause)
        musicPlayerService?.resumePlayer()
        mainViewModel.saveStatePlaying(true)
    }
    override fun pause() {
        super.pause()
        bind?.btnMainPlay?.setIconResource(coreRes.drawable.ic_play)
        musicPlayerService?.pausePlayer()
        mainViewModel.saveStatePlaying(false)
    }
    override fun next() {
        super.next()
        bind?.btnMainNext?.performClick()
    }
    override fun previous() {
        super.previous()
        bind?.btnMainPrevious?.performClick()
    }
    override fun stop() {
        super.stop()
        activity?.finish()
    }
    override fun musicState(musicState: MusicState?) {
        super.musicState(musicState)
        musicState?.let{
            mainViewModel.setMusicState(musicState)
        }
    }
    override fun currentTrack(musicState: MusicState?) {
        super.currentTrack(musicState)
        musicState?.let{
            mainViewModel.setCurrentTrack(musicState)
        }
    }
    override fun onServiceConnected(conn: ServiceConnection, service: IBinder?) {
        super.onServiceConnected(conn, service)
        val bind = service as MusicPlayerService.MusicPlayerServiceBinder
        musicPlayerService = bind.getService()
        this.serviceConnection=conn
    }
    override fun onServiceDisconnected() {
        super.onServiceDisconnected()
        musicPlayerService=null
    }
    override fun onResume() {
        super.onResume()
        setNumberOfTrack(mPrefs.idSong)
        if(!coverViewClicked)checkCoverViewStyle()
        checkPlayerSongModePreferences()
        mainViewModel.checkIfIsFavorite(currentMusicState.idSong)
        mainViewModel.reloadSongInfo()
    }
    override fun onStop() {
        super.onStop()
        mPrefs.currentView = MAIN_FRAGMENT
        mainViewModel.saveCurrentStateSong(currentMusicState)
    }
   companion object {
       var instance:MainPlayerFragment?=null
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            MainPlayerFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
    interface OnFragmentReadyListener{
        fun onFragmentReady()
    }

    override fun onFinishLoad() {
        setUpScrollOnTextViews()
    }
}