package com.barryzeha.ktmusicplayer.view.ui.activities

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.barryzeha.ktmusicplayer.BuildConfig
import com.barryzeha.ktmusicplayer.R
import com.barryzeha.ktmusicplayer.databinding.ActivityAboutThisBinding
import com.barryzeha.core.R as coreRes

class AboutThisActivity : AppCompatActivity() {
    private lateinit var bind:ActivityAboutThisBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(coreRes.style.Theme_KTMusicPlayer)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        bind = ActivityAboutThisBinding.inflate(layoutInflater)
        setContentView(bind.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(bind.main.id)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        setupInfo()
    }
    private fun setupInfo()=with(bind){
        tvAppName.text=applicationInfo.loadLabel(packageManager).toString()
        tvVersion.text= BuildConfig.VERSION_NAME
    }
}