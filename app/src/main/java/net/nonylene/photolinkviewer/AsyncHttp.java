package net.nonylene.photolinkviewer;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;


import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AsyncHttp extends AsyncTaskLoader<AsyncHttpResult<Bitmap>> {
    //get bitmap from url

    private URL url;
    private Context context = null;
    private AsyncHttpResult<Bitmap> result;
    private int max_size;

    public AsyncHttp(Context context, URL url ,int max_size) {
        super(context);
        this.url = url;
        this.max_size = max_size;
    }

    @Override
    public AsyncHttpResult<Bitmap> loadInBackground() {
        AsyncHttpResult<Bitmap> httpResult = new AsyncHttpResult<>();
        try {
            Bitmap bitmap;
            // get bitmap size
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            InputStream inputStream = connection.getInputStream();
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(inputStream, null, options);
            inputStream.close();
            // get redirected url
            String redirect = connection.getURL().toString();
            connection.disconnect();
            int origheight = options.outHeight;
            int origwidth = options.outWidth;
            Pattern pattern = Pattern.compile("^https?://.*\\.gif$");
            Matcher matcher = pattern.matcher(redirect);
            if (matcher.find()) {
                Log.d("redirect", redirect);
                httpResult.setException(new GIFException("gif file"));
                httpResult.setUrl(redirect);
                httpResult.setSize(origwidth,origheight);
            }else {
                inputStream = url.openStream();
                // if bitmap size is bigger than limit, load small photo
                if (max_size < Math.max(origheight, origwidth)) {
                    int size = Math.max(origwidth, origheight) / max_size + 1;
                    BitmapFactory.Options options2 = new BitmapFactory.Options();
                    options2.inSampleSize = size;
                    bitmap = BitmapFactory.decodeStream(inputStream, null, options2);
                } else {
                    bitmap = BitmapFactory.decodeStream(inputStream);
                }
                inputStream.close();
                httpResult.setBitmap(bitmap);
            }
        } catch (IOException e) {
            httpResult.setException(e);
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
