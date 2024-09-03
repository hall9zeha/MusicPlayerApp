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
import com.barryzeha.core.common.EXOPLAYER_SESSION_ID_EXTRA
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import com.barryzeha.core.R as coreRes

@AndroidEntryPoint
class MainEqualizerActivity : AppCompatActivity() {

    @Inject
    lateinit var mPrefs:EffectsPreferences

    private var numberFrequencyBands: Short?=null
    private var lowerEqualizerBandLevel: Short?=null
    private var upperEqualizerBandLevel: Short?=null
    private lateinit var bind:ActivityMainEqualizerBinding
    private  var sessionId:Int?=null
    private var effectTypeForSave=0

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
        handleIntent()
        setUpEqualizer()
        createView(mPrefs.effectType)
        setUpListeners()
        
    }
    private fun handleIntent(){
        intent?.let{
            sessionId=intent.getIntExtra(EXOPLAYER_SESSION_ID_EXTRA,-1)

        }
    }
    private fun setUpEqualizer(){
        sessionId?.let{
           EqualizerManager.initEqualizer(it)
        }

        // setup FX

        bind.main.setPadding(0, 0, 0, 20)
        //        get number frequency bands supported by the equalizer engine
        //numberFrequencyBands = mEq!!.getNumberOfBands()
        numberFrequencyBands = EqualizerManager.mEq?.getNumberOfBands()
        //        get the level ranges to be used in setting the band level
//        get lower limit of the range in milliBels
        lowerEqualizerBandLevel = EqualizerManager.mEq!!.getBandLevelRange()[0]

        //        get the upper limit of the range in millibels
        upperEqualizerBandLevel = EqualizerManager.mEq!!.getBandLevelRange()[1]

    }
    private fun setUpListeners(){
        var swIsChecked=mPrefs.effectsIsEnabled
        enableAndDisableViews(mPrefs.effectsIsEnabled)
        bind.swEnableEffects.isChecked = mPrefs.effectsIsEnabled
        bind.swEnableEffects.setOnCheckedChangeListener { buttonView, isChecked ->
            if(isChecked){
                EqualizerManager.setEnabled(true)
                swIsChecked = true
               enableAndDisableViews(true)
            }else{
                EqualizerManager.setEnabled(false)
                swIsChecked= false
               enableAndDisableViews(false)
            }
        }
        bind.btnApplyEffects.setOnClickListener {
            mPrefs.effectsIsEnabled = swIsChecked
            mPrefs.effectType = effectTypeForSave
            Snackbar.make(bind.root,"Applied success", Snackbar.LENGTH_SHORT).show()
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
            mPrefs.clearPreference()
            bind.contentBands.removeAllViews()
            createView(mPrefs.effectType)
        }
    }
    private fun enableAndDisableViews(isEnable:Boolean){
        bind.btnResetEffects.isEnabled=isEnable

        for (i in 0 until bind.chipGroupEffects.childCount) {
            val chip = bind.chipGroupEffects.getChildAt(i) as Chip
            chip.isEnabled = isEnable
        }
        for(i in 0 until bind.contentBands.childCount){
            val child = bind.contentBands.getChildAt(i)
            child.isEnabled = isEnable
            if (bind.contentBands.getChildAt(i) is LinearLayout) {
                val ln = bind.contentBands.getChildAt(i) as LinearLayout
                for (j in 0 until ln.childCount) {
                    val lnChild = ln.getChildAt(j)
                    lnChild.isEnabled = isEnable
                }
            }
        }
    }
    private fun createView(effectType:Int){
        effectTypeForSave=effectType
        for (i in 0 until numberFrequencyBands!!) {
            val equalizerBandIndex = i.toShort()

            //            frequency header for each seekBar
            val frequencyHeaderTextview = TextView(this)
            frequencyHeaderTextview.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            frequencyHeaderTextview.gravity = Gravity.CENTER_HORIZONTAL
            frequencyHeaderTextview
                .setText((EqualizerManager.mEq!!.getCenterFreq(equalizerBandIndex) / 1000).toString() + " Hz")
            bind.contentBands.addView(frequencyHeaderTextview)

            //            set up linear layout to contain each seekBar
            val seekBarRowLayout = LinearLayout(this)
            seekBarRowLayout.orientation = LinearLayout.HORIZONTAL

            //            set up lower level textview for this seekBar
            val lowerEqualizerBandLevelTextview = TextView(this)
            lowerEqualizerBandLevelTextview.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            lowerEqualizerBandLevelTextview.text =
                (lowerEqualizerBandLevel!! / 100).toString() + " dB"
            lowerEqualizerBandLevelTextview.rotation = 90f
            //            set up upper level textview for this seekBar
            val upperEqualizerBandLevelTextview = TextView(this)
            upperEqualizerBandLevelTextview.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            upperEqualizerBandLevelTextview.text =
                (upperEqualizerBandLevel!! / 100).toString() + " dB"
            upperEqualizerBandLevelTextview.rotation = 90f


            //            **********  the seekBar  **************
//            set the layout parameters for the seekbar
            val layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT - 60,
                120
            )
            layoutParams.weight = 1f

            //            create a new seekBar
            val seekBar = SeekBar(this)
            //            give the seekBar an ID
            seekBar.id = i.toInt()
            val realThemeColor = "#25d24d"
            var seekbg = ColorDrawable(Color.parseColor(realThemeColor))
            seekbg.alpha = 90
            //            seekBar.setBackground(new ColorDrawable(Color.rgb(201, 224, 203)));
            seekBar.background = seekbg
            seekBar.setPadding(35, 15, 35, 15)

            seekBar.layoutParams = layoutParams
            seekBar.max = (upperEqualizerBandLevel!!) - (lowerEqualizerBandLevel!!)


            //            set the progress for this seekBar
            val seek_id = i.toInt()
            val progressBar: Int = mPrefs.getSeekBandValue(effectType,seek_id)

            if (progressBar != 1500) {
                seekBar.progress = progressBar
                EqualizerManager.setBand(equalizerBandIndex,(progressBar + lowerEqualizerBandLevel!!).toShort())
            } else {

                    val bandValue = getEqualizerBandPreConfig(effectType, seek_id)

                    seekBar.progress = bandValue
                    EqualizerManager.setBand(equalizerBandIndex,(bandValue + lowerEqualizerBandLevel!!).toShort())
            }
            //            change progress as its changed by moving the sliders
            seekBar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar, progress: Int,
                    fromUser: Boolean
                ) {
                    EqualizerManager.setBand(equalizerBandIndex,(equalizerBandIndex + lowerEqualizerBandLevel!!).toShort())
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) {
                    //not used
                }

                override fun onStopTrackingTouch(seekBar: SeekBar) {
                    //not used
                    mPrefs.setSeekBandValue(effectType,seek_id,seekBar.progress)
                    //properties.edit_preferences.putInt("position", 0).commit()
                }
            })

            val equalizer = ContextCompat.getDrawable(this,coreRes.drawable.ic_square_minus)

            seekBar.thumb = equalizer
            seekBar.progressDrawable = ColorDrawable(Color.rgb(56, 60, 62))
            // seekbar row layout settings. The layout is rotated at 270 so left=>bottom, Right=>top and so on
            val seekBarLayout = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            //seekBarLayout.weight = 1f
            seekBarLayout.setMargins(5, 0, 5, 0)
            seekBarRowLayout.layoutParams = seekBarLayout

            //            add the lower and upper band level textviews and the seekBar to the row layout
            seekBarRowLayout.addView(lowerEqualizerBandLevelTextview)
            seekBarRowLayout.addView(seekBar)
            seekBarRowLayout.addView(upperEqualizerBandLevelTextview)

            bind.contentBands.addView(seekBarRowLayout)
   }
        //bind.main.rotation = 270F
    }
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