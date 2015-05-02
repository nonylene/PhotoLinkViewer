package net.nonylene.photolinkviewer.async;

import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class AsyncGetURL extends AsyncTask<String, Integer, String> {

    @Override
    protected String doInBackground(String... params) {
        String redirect = null;

        try {
            URL url = new URL(params[0]);

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

}
