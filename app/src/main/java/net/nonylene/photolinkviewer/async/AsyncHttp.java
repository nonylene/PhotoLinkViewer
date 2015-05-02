package net.nonylene.photolinkviewer.async;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import net.nonylene.photolinkviewer.tool.PLVUrl;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class AsyncHttp extends AsyncTaskLoader<AsyncHttpResult<Bitmap>> {
    //get bitmap from url

    private PLVUrl plvUrl;
    private AsyncHttpResult<Bitmap> result;
    private int max_size;

    public AsyncHttp(Context context, PLVUrl plvUrl, int max_size) {
        super(context);
        this.plvUrl = plvUrl;
        this.max_size = max_size;
    }

    @Override
    public AsyncHttpResult<Bitmap> loadInBackground() {
        AsyncHttpResult<Bitmap> httpResult = new AsyncHttpResult<>();

        try {

            URL url = new URL(plvUrl.getDisplayUrl());

            InputStream inputStream = url.openStream();

            // if bitmap size is bigger than limit, load small photo
            Bitmap bitmap;
            if (max_size < Math.max(plvUrl.getHeight(), plvUrl.getWidth())) {
                int size = Math.max(plvUrl.getHeight(), plvUrl.getWidth()) / max_size + 1;
                BitmapFactory.Options options2 = new BitmapFactory.Options();
                options2.inSampleSize = size;
                bitmap = BitmapFactory.decodeStream(inputStream, null, options2);
            } else {
                bitmap = BitmapFactory.decodeStream(inputStream);
            }
            inputStream.close();
            httpResult.setBitmap(bitmap);

        } catch (IOException e) {
            e.printStackTrace();
        }
        return httpResult;
    }

    @Override
    public void deliverResult(AsyncHttpResult<Bitmap> httpResult) {
        if (isReset()) {
            if (this.result != null) {
                this.result = null;
            }
            return;
        }
        this.result = httpResult;
        if (isStarted()) {
            super.deliverResult(httpResult);
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
