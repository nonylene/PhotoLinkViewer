package net.nonylene.photolinkviewer.async;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class AsyncGetURL extends AsyncTaskLoader<String> {
    //get json from url, asynctaskloader

    private URL url;
    private Context context = null;
    private String result;

    public AsyncGetURL(Context context, URL url) {
        super(context);
        this.url = url;
    }

    @Override
    public String loadInBackground() {
        String redirect = null;

        try {
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            InputStream inputStream = connection.getInputStream();
            inputStream.close();
            // get redirected url
            redirect = connection.getURL().toString();
            connection.disconnect();
        } catch (IOException e) {
            Log.e("IOExc", e.toString());
        }

        return redirect;
    }

    @Override
    public void deliverResult(String redirect) {
        if (isReset()) {
            if (this.result != null) {
                this.result = null;
            }
            return;
        }
        this.result = redirect;
        if (isStarted()) {
            super.deliverResult(redirect);
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
