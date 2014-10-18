package net.nonylene.photolinkviewer;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.Intent;
import android.content.Loader;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class Show extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show);
        Button button1 = (Button) findViewById(R.id.button1);
        Button button2 = (Button) findViewById(R.id.button2);
        button1.setOnClickListener(new Button1ClickListener());
        button2.setOnClickListener(new Button2ClickListener());
        if (Intent.ACTION_VIEW.equals(getIntent().getAction())) {
            Uri uri = getIntent().getData();
            String url = uri.toString();
            Log.v("url", url);
            URLPurser(url);
        } else {
            Toast.makeText(this, "Intent Error!", Toast.LENGTH_LONG).show();
        }
    }

    public void URLPurser(String url) {
        try {
            if (url.contains("twipple")) {
                Log.v("twipple", url);
                AsyncExecute hoge = new AsyncExecute();
                hoge.Start("http://p.twipple.jp/show/orig/" + url.substring(url.indexOf(".jp/") + 4));
            } else if (url.contains("flic")) {
                String id = null;
                if (url.contains("flickr")) {
                    Log.v("flickr", url);
                    Pattern pattern = Pattern.compile("^https?://www\\.flickr\\.com/photos/[\\w@]+/(\\d+)");
                    Matcher matcher = pattern.matcher(url);
                    if (matcher.find()) {
                        Log.v("march", "success");
                    }
                    id = matcher.group(1);
                } else if (url.contains("flic.kr")) {
                    Log.v("flic.kr", url);
                    Pattern pattern = Pattern.compile("^https?://flic\\.kr/p/(\\w+)");
                    Matcher matcher = pattern.matcher(url);
                    if (matcher.find()) {
                        Log.v("march", "success");
                    }
                    id = Base58.decode(matcher.group(1));
                }
                String request = "https://api.flickr.com/services/rest/?method=flickr.photos.getInfo&format=json&api_key=<API_KEY>&photo_id="
                        + id;
                Log.v("flickrAPI", request);
                AsyncJSONExecute hoge = new AsyncJSONExecute();
                hoge.Start(request);
            } else {
                Log.v("default", "hoge");
                AsyncExecute hoge = new AsyncExecute();
                hoge.Start("https://pbs.twimg.com/media/Bz1FnXUCEAAkVGt.png:orig");
            }
        } catch (Exception e) {
            Log.e("IOerror", e.toString());
        }
    }


    class Button1ClickListener implements View.OnClickListener {
        public void onClick(View v) {
            ImageView imageView = (ImageView) findViewById(R.id.imgview);
            Bitmap bitmap = ((BitmapDrawable) imageView.getDrawable()).getBitmap();
            File root = Environment.getExternalStorageDirectory();
            String directory = "GetImgTst";
            String Name = "hoge.png";
            File dir = new File(root, directory);
            Log.v("dir", dir.toString());
            dir.mkdirs();
            File path = new File(dir, Name);
            try {
                FileOutputStream fo = new FileOutputStream(path);
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fo);
                fo.close();
            } catch (IOException e) {
                Log.e("error", e.toString());
            }
        }
    }

    class Button2ClickListener implements View.OnClickListener {
        public void onClick(View v) {
            Log.v("save", "clicked");
        }
    }

    public class AsyncExecute implements LoaderManager.LoaderCallbacks<Drawable> {


        public void Start(String url) {
            Bundle bundle = new Bundle();
            bundle.putString("url", url);
            getLoaderManager().restartLoader(0, bundle, this);
        }

        @Override
        public Loader<Drawable> onCreateLoader(int id, Bundle bundle) {
            try {
                String c = bundle.getString("url");
                URL url = new URL(c);
                return new AsyncHttp(getApplicationContext(), url);

            } catch (IOException e) {
                Toast.makeText(Show.this, "fuck", Toast.LENGTH_LONG).show();
                return null;
            }
        }

        @Override
        public void onLoadFinished(Loader<Drawable> loader, Drawable drawable) {
            ImageView imageView = (ImageView) findViewById(R.id.imgview);
            imageView.setImageDrawable(drawable);
        }

        @Override
        public void onLoaderReset(Loader<Drawable> loader) {

        }
    }

    public class AsyncJSONExecute implements LoaderManager.LoaderCallbacks<JSONObject> {


        public void Start(String url) {
            Bundle bundle = new Bundle();
            bundle.putString("url", url);
            getLoaderManager().restartLoader(0, bundle, this);
        }

        @Override
        public Loader<JSONObject> onCreateLoader(int id, Bundle bundle) {
            try {
                String c = bundle.getString("url");
                URL url = new URL(c);
                return new AsyncJSON(getApplicationContext(), url);

            } catch (IOException e) {
                Toast.makeText(Show.this, "fuck", Toast.LENGTH_LONG).show();
                return null;
            }
        }

        @Override
        public void onLoadFinished(Loader<JSONObject> loader, JSONObject json) {

            try {
                Log.v("json", json.toString(2));
                JSONObject photo = new JSONObject(json.getString("photo"));
                String farm = photo.getString("farm");
                String server = photo.getString("server");
                String id = photo.getString("id");
                String secret = photo.getString("secret");
                //license
                Log.v("URL", "https://farm" + farm + ".staticflickr.com/" + server + "/" + id + "_" + secret + "_b.jpg");
                AsyncExecute hoge = new AsyncExecute();
                hoge.Start("https://farm" + farm + ".staticflickr.com/" + server + "/" + id + "_" + secret + "_b.jpg");
            } catch (JSONException e) {
                Log.e("JSONError", e.toString());
            }
        }

        @Override
        public void onLoaderReset(Loader<JSONObject> loader) {

        }
    }

}
