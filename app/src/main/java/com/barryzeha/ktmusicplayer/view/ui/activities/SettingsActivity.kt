package com.barryzeha.ktmusicplayer.view.ui.activities

import android.app.UiModeManager
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import androidx.preference.SwitchPreferenceCompat
import com.barryzeha.core.common.MAIN_FRAGMENT
import com.barryzeha.core.common.MyPreferences
import com.barryzeha.core.R as coreRes
import com.barryzeha.ktmusicplayer.R
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SettingsActivity : AppCompatActivity() {
    @Inject
    lateinit var mPrefs:MyPreferences

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


    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)
            setUpListeners()
        }
        private fun setUpListeners(){
            val uiMode = context?.getSystemService(UiModeManager::class.java)
            val themePref = findPreference<SwitchPreferenceCompat>("themeKey")
            themePref?.setOnPreferenceChangeListener {pref,newValue->
                if(newValue as Boolean){
                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        uiMode?.setApplicationNightMode(UiModeManager.MODE_NIGHT_YES)
                    }else{
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                    }
                }else{
                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        uiMode?.setApplicationNightMode(UiModeManager.MODE_NIGHT_NO)
                    }else {
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                    }
                }
                true
            }
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