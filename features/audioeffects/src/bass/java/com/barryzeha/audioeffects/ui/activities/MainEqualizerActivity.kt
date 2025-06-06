package com.barryzeha.audioeffects.ui.activities


import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
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
import androidx.core.view.forEach
import androidx.core.view.get
import com.barryzeha.audioeffects.common.BASS_PRESET
import com.barryzeha.audioeffects.common.CLASSICAL_PRESET
import com.barryzeha.audioeffects.common.CUSTOM_PRESET
import com.barryzeha.audioeffects.common.ELECTRONIC_PRESET
import com.barryzeha.core.R as coreRes
import com.barryzeha.audioeffects.common.EffectsPreferences
import com.barryzeha.audioeffects.common.EqualizerManager
import com.barryzeha.audioeffects.common.FLAT_PRESET
import com.barryzeha.audioeffects.common.FULL_BASS_AND_TREBLE_PRESET
import com.barryzeha.audioeffects.common.FULL_SOUND_PRESET
import com.barryzeha.audioeffects.common.HEAD_PHONE
import com.barryzeha.audioeffects.common.HIP_HOP_PRESET
import com.barryzeha.audioeffects.common.JAZZ_PRESET
import com.barryzeha.audioeffects.common.POP_PRESET
import com.barryzeha.audioeffects.common.ROCK_PRESET
import com.barryzeha.audioeffects.common.getEqualizerBandPreConfig
import com.barryzeha.audioeffects.databinding.ActivityMainEqualizerBinding
import com.barryzeha.core.common.CHANNEL_OR_SESSION_ID_EXTRA
import com.barryzeha.core.common.getThemeResValue
import com.google.android.material.chip.Chip
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import androidx.core.graphics.drawable.toDrawable


@AndroidEntryPoint
class MainEqualizerActivity : AppCompatActivity() {

    @Inject
    lateinit var mPrefs:EffectsPreferences
    private lateinit var bind:ActivityMainEqualizerBinding
    private val fxArray:IntArray = IntArray(11)
    private var channelIntent=0

    // For seekbar
    private val maxProgressRange:Int=300
    private val seekMinVal:Float = -150F
    private val seekMaxVal:Float = 150F

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(getThemeResValue())
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
        createView(mPrefs.effectType)
        enableAndDisableViews(mPrefs.effectsIsEnabled)
        setUpListeners()
        setUpEqualizer()

    }
    private fun handleIntent(){
        intent?.let{
            channelIntent=intent.getIntExtra(CHANNEL_OR_SESSION_ID_EXTRA,0)
        }
    }
    private fun setUpEqualizer(){
        enableOrDisableEffects()
    }

    private fun setUpListeners()=with(bind){

        if(mPrefs.effectsIsEnabled) output.performClick()
        if(mPrefs.effectsIsEnabled){
            when(mPrefs.effectType){
                CUSTOM_PRESET->{chipGroupFocused(CUSTOM_PRESET)}
                ROCK_PRESET->{chipGroupFocused(ROCK_PRESET)}
                POP_PRESET->{chipGroupFocused(POP_PRESET)}
                BASS_PRESET->{chipGroupFocused(BASS_PRESET)}
                FLAT_PRESET->{chipGroupFocused(FLAT_PRESET)}
                JAZZ_PRESET->{chipGroupFocused(JAZZ_PRESET)}
                CLASSICAL_PRESET->{chipGroupFocused(CLASSICAL_PRESET)}
                HIP_HOP_PRESET->{chipGroupFocused(HIP_HOP_PRESET)}
                ELECTRONIC_PRESET->{chipGroupFocused(ELECTRONIC_PRESET)}
                FULL_SOUND_PRESET->{chipGroupFocused(FULL_SOUND_PRESET)}
                FULL_BASS_AND_TREBLE_PRESET->{chipGroupFocused(FULL_BASS_AND_TREBLE_PRESET)}
                HEAD_PHONE->{chipGroupFocused(HEAD_PHONE)}
            }
        }
        output.setOnClickListener {
            enableOrDisableEffects()  }

        chipGroupEffects.setOnCheckedStateChangeListener { group, checkedIds ->
            // Al hacer click en otro chip quitar la selección y el focus si ya tenemos un efecto guardado en preferencias

            chipGroupEffects.forEach {v->v.clearFocus();v.isSelected=false }
            if(checkedIds.isNotEmpty()){

            val chip = group.findViewById<Chip>(checkedIds[0])
            when(group.indexOfChild(chip)){
            CUSTOM_PRESET->{lnContentBands.removeAllViews(); createView(CUSTOM_PRESET);setEffect()}
            ROCK_PRESET->{lnContentBands.removeAllViews(); createView(ROCK_PRESET);setEffect()}
            POP_PRESET->{lnContentBands.removeAllViews(); createView(POP_PRESET);setEffect()}
            BASS_PRESET->{lnContentBands.removeAllViews(); createView(BASS_PRESET);setEffect()}
            FLAT_PRESET->{lnContentBands.removeAllViews(); createView(FLAT_PRESET);setEffect()}
            JAZZ_PRESET->{lnContentBands.removeAllViews(); createView(JAZZ_PRESET);setEffect()}
            CLASSICAL_PRESET->{lnContentBands.removeAllViews(); createView(CLASSICAL_PRESET);setEffect()}
            HIP_HOP_PRESET->{lnContentBands.removeAllViews(); createView(HIP_HOP_PRESET);setEffect()}
            ELECTRONIC_PRESET->{lnContentBands.removeAllViews(); createView(ELECTRONIC_PRESET);setEffect()}
            FULL_SOUND_PRESET->{lnContentBands.removeAllViews(); createView(FULL_SOUND_PRESET);setEffect()}
            FULL_BASS_AND_TREBLE_PRESET->{lnContentBands.removeAllViews();createView(FULL_BASS_AND_TREBLE_PRESET);setEffect()}
            HEAD_PHONE->{lnContentBands.removeAllViews();createView(HEAD_PHONE);setEffect()}
        }
        }

        }
        btnResetEffects.setOnClickListener{
            mPrefs.clearSeekBandPreferences()
            lnContentBands.removeAllViews()
            createView(CUSTOM_PRESET)
            setEffect()
        }
    }
    private fun chipGroupFocused(effectType:Int){
        val chip = bind.chipGroupEffects[effectType]
        bind.horizontalScrollView.post {
            bind.horizontalScrollView.scrollTo(chip.left,0)
            chip.isSelected=true
            chip.requestFocus()
        }
    }

   private  fun enableOrDisableEffects() {

        EqualizerManager.enableOrDisableEffects(bind.output.isChecked){isEnable ->
            if(isEnable){
                enableAndDisableViews(true)
                chipGroupFocused(mPrefs.effectType)
            }else{
                enableAndDisableViews(false)
                bind.chipGroupEffects.forEach { v->
                    v.clearFocus()
                }
            }
        }
        EqualizerManager.setupFX { fxIndex->
            // Ya que tenemos en el mismo linear layout tanto textviews como seekbars
            //buscarlos por el index no nos devolverá todos los seekbar que tenemos, entonces los buscamos por su tag
            val childView= bind.lnContentBands.findViewWithTag<SeekBar>(fxIndex)
            if(childView is SeekBar) {
                when (childView.id) {
                    coreRes.id.volume -> {
                        EqualizerManager.updateFX(childView.tag.toString().toInt(), childView.progress.toFloat())
                    }
                    else -> {
                        EqualizerManager.updateFX(childView.tag.toString().toInt(), convertBandValueToEqualizer(childView.progress.toFloat()))
                    }
                }
            }
        }
        val reverbSeek: SeekBar = bind.lnContentBands.findViewById(coreRes.id.reverb)
        EqualizerManager.updateFX(reverbSeek.tag.toString().toInt(),reverbSeek.progress.toFloat())
    }
    private fun setEffect(){
        EqualizerManager.setEffect(bind.output.isChecked)
        EqualizerManager.setupFX { fxIndex->
            val childView= bind.lnContentBands.findViewWithTag<SeekBar>(fxIndex)
            if(childView is SeekBar)
                when (childView.id) {
                    coreRes.id.volume -> {
                        EqualizerManager.updateFX(childView.tag.toString().toInt(), childView.progress.toFloat())
                    }
                    else -> {
                        EqualizerManager.updateFX(childView.tag.toString().toInt(), convertBandValueToEqualizer(childView.progress.toFloat()))
                    }
                }
        }
        val reverbSeek: SeekBar = bind.lnContentBands.findViewById(coreRes.id.reverb)
        EqualizerManager.updateFX(reverbSeek.tag.toString().toInt(),reverbSeek.progress.toFloat())

    }
    private fun enableAndDisableViews(isEnable:Boolean){
        bind.btnResetEffects.isEnabled=isEnable
        mPrefs.effectsIsEnabled=isEnable

        for (i in 0 until bind.chipGroupEffects.childCount) {
            val chip = bind.chipGroupEffects.getChildAt(i) as Chip
            chip.isEnabled = isEnable
        }
        for(i in 0 until bind.lnContentBands.childCount){
            val child = bind.lnContentBands.getChildAt(i)
            child.isEnabled = isEnable

        }
    }
    private fun createView(effectType:Int){
        if(mPrefs.effectsIsEnabled) mPrefs.effectType = effectType

        val layoutParams0 = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER // Centrar el texto
                bottomMargin = 28 // Margen superior opcional
            }
        val layoutParams1 = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.CENTER
            topMargin = 8
            bottomMargin = 8
        }
        val osbcl: OnSeekBarChangeListener = object : OnSeekBarChangeListener {
            override fun onStopTrackingTouch(seekBar: SeekBar) {
                when (seekBar.id) {
                    coreRes.id.reverb -> {
                        mPrefs.setSeekBandValue(mPrefs.effectType,seekBar.id,seekBar.progress.toFloat())
                    }
                    coreRes.id.volume -> {
                        mPrefs.setSeekBandValue(mPrefs.effectType,seekBar.id,seekBar.progress.toFloat())
                    }
                    else -> {
                        mPrefs.setSeekBandValue(mPrefs.effectType,seekBar.id,convertBandValueToEqualizer(seekBar.progress.toFloat()))
                    }
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                when (seekBar.id) {
                    coreRes.id.reverb -> {
                        EqualizerManager.updateFX(seekBar.tag.toString().toInt(),progress.toFloat())
                    }
                    coreRes.id.volume -> {
                        EqualizerManager.updateFX(seekBar.tag.toString().toInt(),progress.toFloat())
                    }
                    else -> {
                        EqualizerManager.updateFX(seekBar.tag.toString().toInt(),convertBandValueToEqualizer(progress.toFloat()))
                    }
                }

               val textViewDb =  bind.lnContentBands.findViewWithTag<TextView>("Dbs${seekBar.tag}")
               textViewDb?.text = getBandValue(seekBar.progress.toFloat())

            }
        }

        val frequencies = arrayOf("32 Hz", "64 Hz", "125 Hz", "250 Hz", "500 Hz", "1 kHz", "2 kHz", "4 kHz", "8 kHz", "16 kHz", "")
        for(i in 0 until fxArray.size-1){
            val seekId=i
            val seekProgress=mPrefs.getSeekBandValue(mPrefs.effectType,seekId)
            val linearLayoutSeekbar = LinearLayout(this).apply{
                orientation = LinearLayout.HORIZONTAL
            }

            val layoutParamsForSeekbar = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                0.12f
            ).apply {
                topMargin = 2
                bottomMargin = 2
            }
            val layoutParamsForTvDbs = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                0.88f
            ).apply {
                topMargin = 2
                bottomMargin = 2
            }

           val seekBar = CustomSeekBar(this@MainEqualizerActivity, progressRange = maxProgressRange)
            seekBar.apply {
                id=seekId
                tag=seekId
                max=maxProgressRange
                progress= if(seekProgress.toInt() != 30) convertBandValueToSeekbar(seekProgress) else convertBandValueToSeekbar(getEqualizerBandPreConfig(mPrefs.effectType,seekId).toFloat())
                thumb= ContextCompat.getDrawable(this@MainEqualizerActivity,coreRes.drawable.seekbar_thumb)
                progressDrawable= Color.TRANSPARENT.toDrawable()
                setOnSeekBarChangeListener(osbcl)
            }

            val textViewFreq = TextView(this@MainEqualizerActivity)
            textViewFreq.gravity = Gravity.CENTER // Centrar el texto
            textViewFreq.layoutParams=layoutParams0
            textViewFreq.text=frequencies[i]
            textViewFreq.textSize = 10f

            val textViewDb = TextView(this@MainEqualizerActivity)
            textViewDb.gravity = Gravity.CENTER // Centrar el texto
            textViewDb.layoutParams=layoutParamsForTvDbs
            textViewDb.text=getBandValueToString(if(seekProgress.toInt() != 30) seekProgress else getEqualizerBandPreConfig(mPrefs.effectType,seekId).toFloat().toFloat())
            textViewDb.tag="Dbs$seekId"
            textViewDb.textSize = 10f
            seekBar.layoutParams=layoutParamsForSeekbar
            linearLayoutSeekbar.addView(seekBar)
            linearLayoutSeekbar.addView(textViewDb)
            bind.lnContentBands.addView(linearLayoutSeekbar)
            bind.lnContentBands.addView(textViewFreq)
        }
        // Seekbar par reverberación
        val reverbProgress = mPrefs.getReverbSeekBandValue(mPrefs.effectType, coreRes.id.reverb)
        val reverbSeekBar = SeekBar(this@MainEqualizerActivity).apply {
            id = coreRes.id.reverb
            tag = fxArray.size-1
            max = 30
            progress = if(reverbProgress !=0f) reverbProgress.toInt() else 0
            thumb= ContextCompat.getDrawable(this@MainEqualizerActivity,coreRes.drawable.seekbar_thumb)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER
                topMargin = 16
                bottomMargin = 8
            }
            setOnSeekBarChangeListener(osbcl)

        }
        val reverbTextView = TextView(this@MainEqualizerActivity).apply {
            text = getString(coreRes.string.reverb)
            layoutParams=layoutParams1
            gravity = Gravity.CENTER
        }

        bind.lnContentBands.addView(reverbTextView)
        bind.lnContentBands.addView(reverbSeekBar)

        // SeekBar para volumen
        val volumeProgress = mPrefs.getVolumeSeekBandValue(effectType,coreRes.id.volume)
        val volumeSeekBar = CustomSeekBar(this@MainEqualizerActivity).apply {
            id = coreRes.id.volume
            tag = 11
            max = 30
            progress = if(volumeProgress !=15f) volumeProgress.toInt() else 15
            thumb= ContextCompat.getDrawable(this@MainEqualizerActivity,coreRes.drawable.seekbar_thumb)
            layoutParams = layoutParams1
            progressDrawable=ColorDrawable(Color.TRANSPARENT)
            setOnSeekBarChangeListener(osbcl)
        }
        val volumeTextView = TextView(this@MainEqualizerActivity).apply {
            text = getString(coreRes.string.volume)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER
                topMargin = 10
            }
            gravity = Gravity.CENTER
        }
        bind.lnContentBands.addView(volumeTextView)
        bind.lnContentBands.addView(volumeSeekBar)

    }
    private fun convertBandValueToSeekbar(value:Float):Int{
        // Convertir el valor flotante (-15 a 15) a un valor del SeekBar (0 a 300)
        val progress = ((value + 15) / 30 * maxProgressRange).toInt()  // (valor entre -15 a 15)
        return progress.coerceIn(0, maxProgressRange)
    }
    private fun convertBandValueToEqualizer(value:Float):Float{
        // Convertir el valor del SeekBar (0 a 300) a un valor flotante entre -15 y 15
        val result = ((value.toFloat() / maxProgressRange) * 30) - 15
        return (Math.round(result * 10) / 10.0f)
    }
    private fun getBandValueToString(value:Float):String{
        val toSeekbarValue = convertBandValueToSeekbar(value)
        val toEQValue = convertBandValueToEqualizer(toSeekbarValue.toFloat())
        return "${toEQValue}db"
    }
    @SuppressLint("DefaultLocale")
    private fun getBandValue(value:Float):String{
        return "${String.format("%.1f",convertBandValueToEqualizer(value))}db"
    }
    // Por ahora no debe retornar nada
    class MainEqualizerContract:ActivityResultContract<Int,Unit>(){
        override fun createIntent(context: Context, sessionId: Int): Intent {
                return Intent(context,MainEqualizerActivity::class.java).apply {
                    putExtra(CHANNEL_OR_SESSION_ID_EXTRA,sessionId)
                }
        }
        override fun parseResult(resultCode: Int, intent: Intent?) {
            // Por ahora no debe retornar nada
        }
    }
}