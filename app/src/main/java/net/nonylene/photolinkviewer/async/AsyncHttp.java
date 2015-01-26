package net.nonylene.photolinkviewer.async;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;


import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;

public class AsyncHttp extends AsyncTaskLoader<AsyncHttpResult<Bitmap>> {
    //get bitmap from url

    private URL url;
    private AsyncHttpResult<Bitmap> result;
    private int max_size;

    public AsyncHttp(Context context, URL url, int max_size) {
        super(context);
        this.url = url;
        this.max_size = max_size;
    }

    @Override
    public AsyncHttpResult<Bitmap> loadInBackground() {
        AsyncHttpResult<Bitmap> httpResult = new AsyncHttpResult<>();
        Bitmap bitmap;
        byte[] yaBinary = new byte[4];

        try {
            // get redirect url
            InputStream inputStream = url.openStream();
            // get bitmap size
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(inputStream, null, options);
            int origheight = options.outHeight;
            int origwidth = options.outWidth;
            inputStream.close();

            // read binary and get type
            inputStream = url.openStream();
            inputStream.read(yaBinary, 0, 4);
            inputStream.close();

            String type = getFileType(yaBinary);
            httpResult.setType(type);
            Log.v("type", type);

            if (type.equals("gif")) {
                httpResult.setUrl(url.toString());
                httpResult.setSize(origwidth, origheight);
            } else {
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
            Log.e("IOE", e.toString());
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

    private String getFileType(byte[] head) {
        // get file type from binary
        byte[] png = {(byte) 0x89, (byte) 0x50, (byte) 0x4E, (byte) 0x47};
        byte[] gif = {(byte) 0x47, (byte) 0x49, (byte) 0x46, (byte) 0x38};
        byte[] jpg = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
        byte[] bmp = {(byte) 0x42, (byte) 0x4D};

        if (Arrays.equals(Arrays.copyOfRange(head, 0, 4), png)) return "png";
        else if (Arrays.equals(Arrays.copyOfRange(head, 0, 4), gif)) return "gif";
        else if (Arrays.equals(Arrays.copyOfRange(head, 0, 3), jpg)) return "jpg";
        else if (Arrays.equals(Arrays.copyOfRange(head, 0, 2), bmp)) return "bmp";
        else return "unknown";
    }
}
