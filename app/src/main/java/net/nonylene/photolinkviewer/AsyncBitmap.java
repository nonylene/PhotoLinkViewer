package net.nonylene.photolinkviewer;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class AsyncBitmap extends AsyncTask<URL, Integer, Bitmap> {
    private PLVImageView plvImageView;
    private int max_size;
    private int size = 0;

    public AsyncBitmap (PLVImageView plvImageView, int max_size){
        this.plvImageView = plvImageView;
        this.max_size = max_size;
    }

    public AsyncBitmap (PLVImageView plvImageView, int max_size, int size){
        this.plvImageView = plvImageView;
        this.max_size = max_size;
        this.size = size;
    }

    @Override
    protected Bitmap doInBackground(URL... url){
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
            }else{
                bitmap = BitmapFactory.decodeStream(inputStream);
            }
            inputStream.close();
            // scale bitmap
            if (size != 0){
                int width;
                int height;
                if (origheight < origwidth){
                    width = size;
                    height = size * origheight / origwidth;
                }else{
                    width = size * origwidth / origheight;
                    height = size;
                }
                bitmap = Bitmap.createScaledBitmap(bitmap,width,height, false );
            }
            return bitmap;
        } catch (IOException e) {
            return bitmap;
        }
    }


    protected void onPostExecute(Bitmap bitmap){
        plvImageView.setImageBitmap(bitmap);
    }

}
