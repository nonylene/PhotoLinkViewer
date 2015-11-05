package net.nonylene.photolinkviewer.view

import android.content.Context
import android.util.AttributeSet
import android.widget.Checkable
import android.widget.CheckedTextView
import android.widget.LinearLayout

class CheckableLinearLayout(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs), Checkable {
    private var checkedTextView: CheckedTextView? = null

    override fun onFinishInflate() {
        super.onFinishInflate()
        (0..childCount).forEach { (getChildAt(it) as? CheckedTextView)?.let { checkedTextView = it } }
    }

    override fun isChecked(): Boolean {
        return checkedTextView != null && checkedTextView!!.isChecked
    }

    override fun setChecked(check: Boolean) {
        checkedTextView?.isChecked = check
    }

    override fun toggle() {
        checkedTextView?.toggle()
    }
}
