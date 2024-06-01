package com.barryzeha.ktmusicplayer.view.ui.activities

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.viewModels
import com.barryzeha.ktmusicplayer.R
import com.barryzeha.ktmusicplayer.databinding.ActivityMainBinding
import com.barryzeha.ktmusicplayer.view.ui.adapters.PageCollectionAdapter
import com.barryzeha.ktmusicplayer.view.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private lateinit var bind:ActivityMainBinding
    private val mainViewModel: MainViewModel by viewModels()
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
}