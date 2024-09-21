package com.barryzeha.audioeffects.ui.activities


import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContract
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.get
import com.barryzeha.core.R as coreRes
import com.barryzeha.audioeffects.common.EffectsPreferences
import com.barryzeha.audioeffects.databinding.ActivityMainEqualizerBinding
import com.barryzeha.core.common.CHANNEL_OR_SESSION_ID_EXTRA
import com.un4seen.bass.BASS
import com.un4seen.bass.BASS.BASS_DX8_PARAMEQ
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject


@AndroidEntryPoint
class MainEqualizerActivity : AppCompatActivity() {

    @Inject
    lateinit var mPrefs:EffectsPreferences


    private lateinit var bind:ActivityMainEqualizerBinding
    private val fxArray:IntArray = IntArray(11)
    private var fxchan: Int = 0
    private var chan:Int=0
    private var channelIntent=0

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
            channelIntent=intent.getIntExtra(CHANNEL_OR_SESSION_ID_EXTRA,0)
        }
    }
    private fun setUpEqualizer(){


    }

    private fun setUpListeners()=with(bind){
        bind.output.setOnClickListener { outputClicked() }

    }
    private fun updateFX(sb: SeekBar){
            val  v = sb.progress;
            val n = Integer.parseInt(sb.tag.toString())
            if (n < fxArray.size -1) { // EQ
                val p:BASS.BASS_DX8_PARAMEQ = BASS.BASS_DX8_PARAMEQ()
                BASS.BASS_FXGetParameters(fxArray[n], p);
                p.fGain = (v - 10).toFloat()
                BASS.BASS_FXSetParameters(fxArray[n], p);
            } else if (n == fxArray.size -1) { // reverb
                val p:BASS.BASS_DX8_REVERB  = BASS.BASS_DX8_REVERB()
                BASS.BASS_FXGetParameters(fxArray[n], p);
                p.fReverbMix = (if(v != 0)  (Math.log(v / 20.0) * 20).toFloat() else (-96).toFloat())
                BASS.BASS_FXSetParameters(fxArray[n], p);
            } else // volume
                BASS.BASS_ChannelSetAttribute(chan, BASS.BASS_ATTRIB_VOL, v / 10f);

    }
    private fun setupFX() {
        // setup the effects
        val ch: Int =if (fxchan != 0) fxchan else chan // set on output stream if enabled, else file stream
        for (i in fxArray.indices-1) {

            fxArray[i] = BASS.BASS_ChannelSetFX(ch, BASS.BASS_FX_DX8_PARAMEQ, 0)
            val p = BASS_DX8_PARAMEQ()
            p.fGain = 0f
            p.fBandwidth = 18f
            p.fCenter = (125f * Math.pow(2.0, (i - 1).toDouble())).toFloat() // Frecuencias centradas
            BASS.BASS_FXSetParameters(fxArray[i], p)
            val childView= bind.lnContent[i]
            if(childView is SeekBar)updateFX(childView)
        }
        fxArray[fxArray.size-1] = BASS.BASS_ChannelSetFX(ch, BASS.BASS_FX_DX8_REVERB, 0)
        updateFX(bind.lnContent.findViewById(coreRes.id.reverb))

    }
   private  fun outputClicked() {

        val ch = if (fxchan != 0) fxchan else chan
        for (i in fxArray.indices) {
            BASS.BASS_ChannelRemoveFX(ch, fxArray[i])
        }
        // remove reverb effect
        BASS.BASS_ChannelRemoveFX(ch, fxArray[fxArray.size-1])
        if (bind.output.isChecked) {
            fxchan= BASS.BASS_StreamCreate(0, 0, 0, BASS.STREAMPROC_DEVICE, null)
            chan=channelIntent
        } else {
            fxchan=0
            chan=0
        }
        setupFX()
    }
    private fun enableAndDisableViews(isEnable:Boolean){

    }
    private fun createView(effectType:Int){
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
            topMargin = 24
            bottomMargin = 24
        }

        val osbcl: OnSeekBarChangeListener = object : OnSeekBarChangeListener {
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                updateFX(seekBar)
            }
        }

        val frequencies = arrayOf("125 Hz", "1 kHz", "8 kHz", "16 kHz", "32 kHz", "64 kHz", "125 kHz", "250 kHz", "500 kHz", "1 MHz", "")
        fxArray.forEachIndexed{ i, e->
           val seekBar = SeekBar(this@MainEqualizerActivity)
            seekBar.apply {
                id=i
                tag=i
                max=20
                progress=10
                setOnSeekBarChangeListener(osbcl)
            }
            val textView = TextView(this@MainEqualizerActivity)

            textView.gravity = Gravity.CENTER // Centrar el texto
            textView.layoutParams=layoutParams0
            textView.text=frequencies[i]
            bind.lnContent.addView(seekBar)
            bind.lnContent.addView(textView)
        }

        val reverbSeekBar = SeekBar(this@MainEqualizerActivity).apply {
            id = coreRes.id.reverb
            tag = fxArray.size-1
            max = 20
            progress = 0
            layoutParams = layoutParams1
            setOnSeekBarChangeListener(osbcl)
        }
        val reverbTextView = TextView(this@MainEqualizerActivity).apply {
            text = "Reverb"
            layoutParams=layoutParams1
            gravity = Gravity.CENTER
        }

        bind.lnContent.addView(reverbSeekBar)
        bind.lnContent.addView(reverbTextView)

        // Agregar SeekBar para volumen
        val volumeSeekBar = SeekBar(this@MainEqualizerActivity).apply {
            id = coreRes.id.volume
            tag = 11
            max = 20
            progress = 10
            layoutParams = layoutParams1
            setOnSeekBarChangeListener(osbcl)
        }

        val volumeTextView = TextView(this@MainEqualizerActivity).apply {
            text = "Volume"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER
                topMargin = 10
            }
            gravity = Gravity.CENTER
        }

        bind.lnContent.addView(volumeSeekBar)
        bind.lnContent.addView(volumeTextView)
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