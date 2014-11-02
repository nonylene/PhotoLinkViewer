package net.nonylene.photolinkviewer;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class AsyncJSON extends AsyncTaskLoader<JSONObject> {
    //get json from url, asynctaskloader

    private URL url;
    private Context context = null;
    private JSONObject result;

    public AsyncJSON(Context context, URL url) {
        super(context);
        this.url = url;
    }

    @Override
    public JSONObject loadInBackground() {
        JSONObject json = null;

        try {
            InputStream inputStream = url.openStream();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, Charset.defaultCharset()));
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line);
            }
            inputStream.close();
            String hoge = stringBuilder.toString();
            //flickr returns function for javascript, so purse for json
            Pattern pattern = Pattern.compile("^jsonFlickrApi\\((.+)\\)$");
            Matcher matcher = pattern.matcher(hoge);
            if (matcher.find()) {
                Log.v("match", "success");
                hoge = matcher.group(1);
            }
            Log.v("text", hoge);
            json = new JSONObject(hoge);
            return json;
        } catch (IOException e) {
            Log.e("IOExc", e.toString());
            return json;
        } catch (JSONException e) {
            Log.e("JSOMExc", e.toString());
            return json;
        }
    }

    @Override
    public void deliverResult(JSONObject json) {
        if (isReset()) {
            if (this.result != null) {
                this.result = null;
            }
            return;
        }
        this.result = json;
        if (isStarted()) {
            super.deliverResult(json);
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
