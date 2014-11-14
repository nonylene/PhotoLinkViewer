package net.nonylene.photolinkviewer;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;

import java.net.URL;

public class PLVImageView extends ImageView {

    public PLVImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PLVImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setUrl(URL url, int width, int height) {
        AsyncBitmap asyncBitmap = new AsyncBitmap(this, 2048, width, height);
        asyncBitmap.execute(url);
    }

    public void setUrl(URL url) {
        AsyncBitmap asyncBitmap = new AsyncBitmap(this, 2048);
        asyncBitmap.execute(url);
    }
}
