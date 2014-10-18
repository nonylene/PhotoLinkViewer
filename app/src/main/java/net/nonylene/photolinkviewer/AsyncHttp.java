package net.nonylene.photolinkviewer;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.graphics.drawable.Drawable;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;


public class AsyncHttp extends AsyncTaskLoader<Drawable> {

    private URL url;
    private Context context = null;
    private Drawable result;

    public AsyncHttp(Context context, URL url) {
        super(context);
        this.url = url;
    }

    @Override
    public Drawable loadInBackground() {
        Drawable drawable = null;

        try {
            InputStream inputStream = url.openStream();
            drawable = Drawable.createFromStream(inputStream, "webimg");
            inputStream.close();
            return drawable;
        } catch (IOException e) {
            return drawable;
        }
    }

    @Override
    public void deliverResult(Drawable drawable){
        if (isReset()){
            if(this.result != null ){
                this.result = null;
            }
            return;
        }
        this.result = drawable;
        if (isStarted()){
            super.deliverResult(drawable);
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
