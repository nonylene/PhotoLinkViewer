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

    public AsyncHttp(Context context, URL url) {
        super(context);
        this.url = url;
    }

    @Override
    public Bitmap loadInBackground() {
        Bitmap bitmap = null;
        try {
            InputStream inputStream = url.openStream();
            bitmap = BitmapFactory.decodeStream(inputStream);
            inputStream.close();
            return bitmap;
        } catch (IOException e) {
            return bitmap;
        }
    }

    @Override
    public void deliverResult(Bitmap bitmap){
        if (isReset()){
            if(this.result != null ){
                this.result = null;
            }
            return;
        }
        this.result = bitmap;
        if (isStarted()){
            super.deliverResult(bitmap);
        }
    }

    @Override
    public void onStartLoading() {
        if (this.result != null){
            deliverResult(this.result);
        }
        if(takeContentChanged() || this.result == null){
            forceLoad();
        }
    }

    @Override
    public void onStopLoading(){
        super.onStopLoading();
        cancelLoad();
    }

    @Override
    public void onReset(){
        super.onReset();
        onStopLoading();
    }
}
