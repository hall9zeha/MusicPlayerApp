package com.barryzeha.ktmusicplayer.view.ui.activities

import android.app.Activity
import android.app.UiModeManager
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import androidx.preference.SwitchPreferenceCompat
import com.barryzeha.core.common.MAIN_FRAGMENT
import com.barryzeha.core.common.MyPreferences
import com.barryzeha.core.common.SettingsKeys
import com.barryzeha.core.common.getThemeResValue
import com.barryzeha.core.common.getThemeWithActionBarResValue
import com.barryzeha.ktmusicplayer.MyApp
import com.barryzeha.ktmusicplayer.MyApp.Companion.mPrefs
import com.barryzeha.core.R as coreRes
import com.barryzeha.ktmusicplayer.R
import com.barryzeha.mfilepicker.common.Preferences
import com.google.android.material.color.DynamicColors
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SettingsActivity : AppCompatActivity() {
    @Inject
    lateinit var mPrefs:MyPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(getThemeWithActionBarResValue())
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
            val themePref = findPreference<SwitchPreferenceCompat>(SettingsKeys.DEFAULT_THEME.value)
            val materialYouTheme = findPreference<SwitchPreferenceCompat>(SettingsKeys.MATERIAL_YOU_THEME.value)
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
            materialYouTheme?.setOnPreferenceChangeListener { preference, newValue ->
                if(newValue as Boolean){
                    DynamicColors.applyToActivityIfAvailable(requireActivity())
                    mPrefs.globalTheme=SettingsKeys.MATERIAL_YOU_THEME.ordinal
                    restartActivity()
                }else{
                    requireActivity().setTheme(coreRes.style.Theme_KTMusicPlayer_Material3)
                    mPrefs.globalTheme=SettingsKeys.DEFAULT_THEME.ordinal
                    restartActivity()
                }
                mPrefs.themeChanged = true
                true
            }
        }
        private fun restartActivity(){
            requireActivity().recreate()
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