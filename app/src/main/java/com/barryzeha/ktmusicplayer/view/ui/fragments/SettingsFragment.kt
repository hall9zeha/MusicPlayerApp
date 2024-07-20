package com.barryzeha.ktmusicplayer.view.ui.fragments

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.barryzeha.ktmusicplayer.R

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
    }
}