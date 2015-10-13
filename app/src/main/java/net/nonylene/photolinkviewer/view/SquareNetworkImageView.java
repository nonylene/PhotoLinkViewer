package net.nonylene.photolinkviewer.view;

import android.content.Context;
import android.util.AttributeSet;

import com.android.volley.toolbox.NetworkImageView;

public class SquareNetworkImageView extends NetworkImageView {

    public SquareNetworkImageView(Context context) {
        this(context, null);
    }

    public SquareNetworkImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SquareNetworkImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int width = getMeasuredWidth();
        setMeasuredDimension(width, width);
    }
}
