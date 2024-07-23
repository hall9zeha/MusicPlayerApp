package com.barryzeha.ktmusicplayer.view.ui.activities

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceFragmentCompat
import com.barryzeha.core.common.MAIN_FRAGMENT
import com.barryzeha.core.common.MyPreferences
import com.barryzeha.core.R as coreRes
import com.barryzeha.ktmusicplayer.R

class SettingsActivity : AppCompatActivity() {
    private lateinit var mPrefs:MyPreferences
    override fun onCreate(savedInstanceState: Bundle?) {
        setTitle(coreRes.string.settings)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, SettingsFragment())
                .commit()
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        mPrefs=MyPreferences(this)
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            android.R.id.home->{
                finish()
            }
            else->{}
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onStop() {
        super.onStop()
        mPrefs.currentView = MAIN_FRAGMENT
    }
}