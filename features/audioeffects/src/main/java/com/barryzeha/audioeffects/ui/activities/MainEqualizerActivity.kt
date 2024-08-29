package com.barryzeha.audioeffects.ui.activities


import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.media.AudioManager
import android.media.audiofx.Equalizer
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.barryzeha.audioeffects.databinding.ActivityMainEqualizerBinding
import com.barryzeha.core.R as coreRes


class MainEqualizerActivity : AppCompatActivity() {
    private var mEq:Equalizer ?=null
    private var numberFrequencyBands: Short?=null
    private var lowerEqualizerBandLevel: Short?=null
    private var upperEqualizerBandLevel: Short?=null
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
        setUpEqualizer()
        createView()
    }
    private fun setUpEqualizer(){
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val audioSessionId = audioManager.generateAudioSessionId()
        //in On Create
        mEq = Equalizer(0, audioSessionId)
        mEq!!.setEnabled(true)
        Log.e("NUM-BAND", mEq!!.numberOfBands.toString() )

// setup FX

        bind.main.setPadding(0, 0, 0, 20)
        //        get number frequency bands supported by the equalizer engine
        numberFrequencyBands = mEq!!.getNumberOfBands()
        //        get the level ranges to be used in setting the band level
//        get lower limit of the range in milliBels
        lowerEqualizerBandLevel = mEq!!.getBandLevelRange()[0]

        //        get the upper limit of the range in millibels
        upperEqualizerBandLevel = mEq!!.getBandLevelRange()[1]
    }
    private fun createView(){
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
                .setText((mEq!!.getCenterFreq(equalizerBandIndex) / 1000).toString() + " Hz")
            bind.main.addView(frequencyHeaderTextview)

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
            seekBar.max = upperEqualizerBandLevel!! - lowerEqualizerBandLevel!!
            //            set the progress for this seekBar
            val seek_id = i.toInt()
            val progressBar: Int = 1500
                /*properties.preferences.getInt("seek_$seek_id", 1500)*/
            //            Log.i("storedOld_seek_"+seek_id,":"+ progressBar);
            if (progressBar != 1500) {
                seekBar.progress = progressBar
                mEq?.setBandLevel(
                    equalizerBandIndex,
                    (progressBar + lowerEqualizerBandLevel!!).toShort()
                )
            } else {
                seekBar.progress = mEq!!.getBandLevel(equalizerBandIndex).toInt()
                mEq?.setBandLevel(
                    equalizerBandIndex,
                    (progressBar + lowerEqualizerBandLevel!!).toShort()
                )
            }
            //            change progress as its changed by moving the sliders
            seekBar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar, progress: Int,
                    fromUser: Boolean
                ) {
                    mEq?.setBandLevel(
                        equalizerBandIndex,
                        (progress + lowerEqualizerBandLevel!!).toShort()
                    )
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) {
                    //not used
                }

                override fun onStopTrackingTouch(seekBar: SeekBar) {
                    //not used
                    /*properties.edit_preferences.putInt("seek_$seek_id", seekBar.progress).commit()
                    properties.edit_preferences.putInt("position", 0).commit()*/
                }
            })

            val equalizer = ContextCompat.getDrawable(this,coreRes.drawable.ic_square_minus)

            seekBar.thumb = equalizer
            seekBar.progressDrawable = ColorDrawable(Color.rgb(56, 60, 62))
            // seekbar row layout settings. The layout is rotated at 270 so left=>bottom, Right=>top and so on
            val seekBarLayout = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            seekBarLayout.weight = 1f
            seekBarLayout.setMargins(5, 0, 5, 0)
            seekBarRowLayout.layoutParams = seekBarLayout

            //            add the lower and upper band level textviews and the seekBar to the row layout
            seekBarRowLayout.addView(lowerEqualizerBandLevelTextview)
            seekBarRowLayout.addView(seekBar)
            seekBarRowLayout.addView(upperEqualizerBandLevelTextview)

            bind.main.addView(seekBarRowLayout)



        }
        //bind.main.rotation = 270F
    }
}