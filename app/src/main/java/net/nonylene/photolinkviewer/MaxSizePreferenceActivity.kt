package net.nonylene.photolinkviewer

import android.graphics.*
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v7.app.AppCompatActivity
import android.widget.*

class MaxSizePreferenceActivity : AppCompatActivity() {

    private var imageView: ImageView? = null
    private var textView: TextView? = null
    private var seekBar: SeekBar? = null
    private var setButton: Button? = null

    private val blackPaint = Paint().apply {
        color = Color.BLACK
    }
    private val bluePaint = Paint().apply {
        color = Color.parseColor("#4cb6ed")
    }
    private val greenPaint = Paint().apply {
        color = Color.parseColor("#46d249")
    }
    private val redPaint = Paint().apply {
        color = Color.parseColor("#f4554f")
    }
    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 200f
        textAlign = Paint.Align.CENTER
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_max_preference)

        imageView = findViewById(R.id.imageView) as ImageView?
        textView = findViewById(R.id.textView) as TextView?
        seekBar = findViewById(R.id.seekBar) as SeekBar?
        setButton = findViewById(R.id.setButton) as Button?

        seekBar!!.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                textView!!.text = "${(progress + 1) * 1024}px"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                setButton!!.isEnabled = false
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                updateImageView(seekBar!!.progress)
                setButton!!.isEnabled = true
            }
        })

        val sharedPref = PreferenceManager.getDefaultSharedPreferences(this)

        setButton!!.setOnClickListener {
            val size = seekBar!!.progress + 1
            sharedPref.edit().putInt("imageview_max_size", size).apply()
            Toast.makeText(this.applicationContext, "Set max size to ${size * 1024}px!", Toast.LENGTH_LONG).show()
        }

        val index = sharedPref.getInt("imageview_max_size", 2) - 1;
        updateImageView(index)
        seekBar!!.progress = index
    }

    private fun updateImageView(index: Int) {
        imageView!!.setImageBitmap(null)
        val size = (index + 1) * 1024
        val s2 = size / 2f
        val scaledBitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(scaledBitmap)
        // background
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), blackPaint)
        // circle
        canvas.drawCircle(s2, s2, size / 3f, bluePaint)
        // rhombus
        canvas.drawPath(Path().apply {
            val s5 = size / 5f
            moveTo(s2, s5)
            lineTo(s5, s2)
            lineTo(s2, s5 * 4)
            lineTo(s5 * 4, s2)
            close()
        }, greenPaint)
        canvas.drawCircle(s2, s2, size / 5f, redPaint)
        canvas.drawText("${size}px", s2, s2, textPaint)
        imageView!!.setImageBitmap(scaledBitmap)
    }

}
