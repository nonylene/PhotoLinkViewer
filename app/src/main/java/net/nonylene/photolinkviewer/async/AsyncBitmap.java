package net.nonylene.photolinkviewer.async;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.AsyncTask;

import net.nonylene.photolinkviewer.tool.PLVImageView;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class AsyncBitmap extends AsyncTask<URL, Integer, Bitmap> {
    private PLVImageView plvImageView;
    private int max_size;
    private float width = 0;
    private float height = 0;
    private int picwidth;
    private int picheight;

    public AsyncBitmap(PLVImageView plvImageView, int max_size) {
        this.plvImageView = plvImageView;
        this.max_size = max_size;
    }

    public AsyncBitmap(PLVImageView plvImageView, int max_size, float width, float height) {
        this.plvImageView = plvImageView;
        this.max_size = max_size;
        this.width = width;
        this.height = height;

    }

    @Override
    protected Bitmap doInBackground(URL... url) {
        Bitmap bitmap = null;
        try {
            // get bitmap size
            InputStream inputStream = url[0].openStream();
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(inputStream, null, options);
            inputStream.close();
            inputStream = url[0].openStream();
            int origheight = options.outHeight;
            int origwidth = options.outWidth;
            // if bitmap size is bigger than limit, load small photo
            if (max_size < Math.max(origheight, origwidth)) {
                int size = Math.max(origwidth, origheight) / max_size + 1;
                BitmapFactory.Options options2 = new BitmapFactory.Options();
                options2.inSampleSize = size;
                bitmap = BitmapFactory.decodeStream(inputStream, null, options2);
            } else {
                bitmap = BitmapFactory.decodeStream(inputStream);
            }
            picwidth = bitmap.getWidth();
            picheight = bitmap.getHeight();
            inputStream.close();
            return bitmap;
        } catch (IOException e) {
            return bitmap;
        }
    }

    @Override
    protected void onPostExecute(Bitmap bitmap) {
        plvImageView.setImageBitmap(bitmap);
        if (width != 0) {
            //get matrix from imageview
            Matrix matrix = new Matrix();
            matrix.set(plvImageView.getMatrix());
            //get display size
            float wid = width / picwidth;
            float hei = height / picheight;
            float zoom = Math.min(wid, hei);
            float initX;
            float initY;
            matrix.setScale(zoom, zoom);
            if (wid < hei) {
                //adjust width
                initX = 0;
                initY = (height - picheight * wid) / 2;
            } else {
                //adjust height
                initX = (width - picwidth * hei) / 2;
                initY = 0;
            }
            matrix.postTranslate(initX, initY);
            plvImageView.setImageMatrix(matrix);
        }
    }

}
