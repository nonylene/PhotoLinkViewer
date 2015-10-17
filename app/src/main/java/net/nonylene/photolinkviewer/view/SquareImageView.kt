package net.nonylene.photolinkviewer.view

import android.content.Context
import android.util.AttributeSet
import android.widget.ImageView

public class SquareImageView(context: Context?, attrs: AttributeSet?) : ImageView(context, attrs) {

    override protected fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        setMeasuredDimension(measuredWidth, measuredWidth);
    }
}
