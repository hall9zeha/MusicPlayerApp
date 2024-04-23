package com.barryzeha.ktmusicplayer.view.activities

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.barryzeha.ktmusicplayer.R
import com.barryzeha.ktmusicplayer.databinding.ActivityMainBinding
import com.barryzeha.ktmusicplayer.view.adapters.PageCollectionAdapter

class MainActivity : AppCompatActivity() {
    private lateinit var bind:ActivityMainBinding
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
    }
    private fun setUpViewPager(){
        val viewPagerAdapter= PageCollectionAdapter(this, listOf("HomePlayer", "listPlayer"))
        bind.mViewPager.adapter=viewPagerAdapter
    }
}