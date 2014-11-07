package net.nonylene.photolinkviewer;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;


import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class AsyncHttp extends AsyncTaskLoader<Bitmap> {
    //get bitmap from url

    private URL url;
    private Context context = null;
    private Bitmap result;
    private int max_size;

    public AsyncHttp(Context context, URL url ,int max_size) {
        super(context);
        this.url = url;
        this.max_size = max_size;
    }

    @Override
    public Bitmap loadInBackground() {
        Bitmap bitmap = null;
        try {
            // get bitmap size
            InputStream inputStream = url.openStream();
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(inputStream, null, options);
            inputStream.close();
            inputStream = url.openStream();
            int origheight = options.outHeight;
            int origwidth = options.outWidth;
            // if bitmap size is bigger than limit, load small photo
            if (max_size < Math.max(origheight, origwidth)) {
                int size = Math.max(origwidth, origheight) / max_size + 1;
                BitmapFactory.Options options2 = new BitmapFactory.Options();
                options2.inSampleSize = size;
                bitmap = BitmapFactory.decodeStream(inputStream, null, options2);
            }else{
                bitmap = BitmapFactory.decodeStream(inputStream);
            }
            inputStream.close();
            return bitmap;
        } catch (IOException e) {
            return bitmap;
        }
    }

    @Override
    public void deliverResult(Bitmap bitmap) {
        if (isReset()) {
            if (this.result != null) {
                this.result = null;
            }
            return;
        }
        this.result = bitmap;
        if (isStarted()) {
            super.deliverResult(bitmap);
        }
    }

    @Override
    public void onStartLoading() {
        if (this.result != null) {
            deliverResult(this.result);
        }
        if (takeContentChanged() || this.result == null) {
            forceLoad();
        }
    }

    @Override
    public void onStopLoading() {
        super.onStopLoading();
        cancelLoad();
    }

    @Override
    public void onReset() {
        super.onReset();
        onStopLoading();
    }
}
