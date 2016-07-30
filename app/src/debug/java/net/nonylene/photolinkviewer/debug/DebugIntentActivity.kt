package net.nonylene.photolinkviewer.debug

import android.animation.LayoutTransition
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.text.Editable
import android.text.TextWatcher
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import butterknife.bindView
import net.nonylene.photolinkviewer.R
import net.nonylene.photolinkviewer.core.PLVShowActivity

class DebugIntentActivity : AppCompatActivity() {

    val debugBaseLayout by bindView<LinearLayout>(R.id.debug_base)
    val editText by bindView<EditText>(R.id.debug_edit_text)
    val urlTextView by bindView<TextView>(R.id.url)
    val launchIntentButton by bindView<Button>(R.id.launch_default_intent)
    val launchIntentChooserButton by bindView<Button>(R.id.launch_intent_chooser)
    val launchPLVShowActivityButton by bindView<Button>(R.id.launch_in_plv_show)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_debug_intent)

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
            @Suppress
            debugBaseLayout.layoutTransition.enableTransitionType(LayoutTransition.CHANGING)
        }

        editText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val matcher = Patterns.WEB_URL.matcher(s)
                if (matcher.matches()) {
                    urlTextView.text = matcher.group()
                    changeButtonEnables(true)
                } else {
                    urlTextView.text = null
                    changeButtonEnables(false)
                }
            }
        })

        launchIntentButton.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(urlTextView.text.toString())))
        }

        launchIntentChooserButton.setOnClickListener {
            startActivity(Intent.createChooser(
                    Intent(Intent.ACTION_VIEW, Uri.parse(urlTextView.text.toString())
                    ), "Select")
            )
        }

        launchPLVShowActivityButton.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(urlTextView.text.toString()),
                    this@DebugIntentActivity, PLVShowActivity::class.java))
        }
    }

    internal fun changeButtonEnables(enable: Boolean) {
        launchIntentButton.isEnabled = enable
        launchIntentChooserButton.isEnabled = enable
        launchPLVShowActivityButton.isEnabled = enable
    }
}
