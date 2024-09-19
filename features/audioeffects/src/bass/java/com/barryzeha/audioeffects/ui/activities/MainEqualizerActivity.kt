package com.barryzeha.audioeffects.ui.activities


import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContract
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.barryzeha.audioeffects.common.BASS
import com.barryzeha.audioeffects.common.CLASSICAL
import com.barryzeha.audioeffects.common.CUSTOM
import com.barryzeha.audioeffects.common.ELECTRONIC
import com.barryzeha.audioeffects.common.EffectsPreferences
import com.barryzeha.audioeffects.common.EqualizerManager
import com.barryzeha.audioeffects.common.FLAT
import com.barryzeha.audioeffects.common.FULL_SOUND
import com.barryzeha.audioeffects.common.HIP_HOP
import com.barryzeha.audioeffects.common.JAZZ
import com.barryzeha.audioeffects.common.POP
import com.barryzeha.audioeffects.common.ROCK
import com.barryzeha.audioeffects.common.getEqualizerBandPreConfig
import com.barryzeha.audioeffects.databinding.ActivityMainEqualizerBinding
import com.barryzeha.core.common.COLOR_PRIMARY
import com.barryzeha.core.common.EXOPLAYER_SESSION_ID_EXTRA
import com.barryzeha.core.common.mColorList
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import com.barryzeha.core.R as coreRes

@AndroidEntryPoint
class MainEqualizerActivity : AppCompatActivity() {

    @Inject
    lateinit var mPrefs:EffectsPreferences


    private lateinit var bind:ActivityMainEqualizerBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bind = ActivityMainEqualizerBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(bind.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(bind.main.id)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        Toast.makeText(this, "Holi bass", Toast.LENGTH_SHORT).show()
        handleIntent()
        setUpEqualizer()
        createView(mPrefs.effectType)
        setUpListeners()
        
    }
    private fun handleIntent(){

    }
    private fun setUpEqualizer(){


    }
    private fun setUpListeners(){
        var swIsChecked=mPrefs.effectsIsEnabled
        enableAndDisableViews(mPrefs.effectsIsEnabled)
        bind.swEnableEffects.isChecked = mPrefs.effectsIsEnabled
        bind.swEnableEffects.setOnCheckedChangeListener { buttonView, isChecked ->
            if(isChecked){

            }else{

            }
        }
        bind.btnApplyEffects.setOnClickListener {

        }
        bind.chipGroupEffects.isSingleSelection=true
        bind.chipGroupEffects.setOnCheckedStateChangeListener { group, checkedIds ->
            if(checkedIds.isNotEmpty()){
            val chip = group.findViewById<Chip>(checkedIds[0])
            when(group.indexOfChild(chip)){
            CUSTOM->{bind.contentBands.removeAllViews(); createView(CUSTOM)}
            ROCK->{bind.contentBands.removeAllViews(); createView(ROCK)}
            POP->{bind.contentBands.removeAllViews(); createView(POP)}
            BASS->{bind.contentBands.removeAllViews(); createView(BASS)}
            FLAT->{bind.contentBands.removeAllViews(); createView(FLAT)}
            JAZZ->{bind.contentBands.removeAllViews(); createView(JAZZ)}
            CLASSICAL->{bind.contentBands.removeAllViews(); createView(CLASSICAL)}
            HIP_HOP->{bind.contentBands.removeAllViews(); createView(HIP_HOP)}
            ELECTRONIC->{bind.contentBands.removeAllViews(); createView(ELECTRONIC)}
            FULL_SOUND->{bind.contentBands.removeAllViews(); createView(FULL_SOUND)}
        }
}
        }
        bind.btnResetEffects.setOnClickListener {

        }
    }
    private fun enableAndDisableViews(isEnable:Boolean){

    }
    private fun createView(effectType:Int){

   }
        //  bind.main.rotation = 270F

    // Por ahora no debe retornar nada
    class MainEqualizerContract:ActivityResultContract<Int,Unit>(){
        override fun createIntent(context: Context, sessionId: Int): Intent {
                return Intent(context,MainEqualizerActivity::class.java).apply {
                    putExtra(EXOPLAYER_SESSION_ID_EXTRA,sessionId)
                }
        }

        override fun parseResult(resultCode: Int, intent: Intent?) {
            // Por ahora no debe retornar nada
        }
    }
}