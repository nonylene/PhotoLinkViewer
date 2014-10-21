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

    private String filename = "hoge";
    private String sitename = "hoge";

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

    class Button1ClickListener implements View.OnClickListener {
        public void onClick(View v) {
            ImageView imageView = (ImageView) findViewById(R.id.imgview);
            Bitmap bitmap = ((BitmapDrawable) imageView.getDrawable()).getBitmap();
            File root = Environment.getExternalStorageDirectory();
            String directory = "PLViewer";
            File dir = new File(root, directory + "/" + sitename);
            Log.v("dir", dir.toString());
            dir.mkdirs();
            File path = new File(dir, filename + ".png");
            try {
                FileOutputStream fo = new FileOutputStream(path);
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fo);
                fo.close();
                Toast.makeText(Show.this, "file saved to " + path.toString(), Toast.LENGTH_LONG).show();
            } catch (IOException e) {
                Log.e("error", e.toString());
            }
        }
    }

    class Button2ClickListener implements View.OnClickListener {
        public void onClick(View v) {
            Log.v("settings", "clicked");
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
                Log.e("DrawableLoaderError", e.toString());
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
                Log.e("JSONLoaderError", e.toString());
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
                String url = "https://farm" + farm + ".staticflickr.com/" + server + "/" + id + "_" + secret + "_b.jpg";
                Log.v("URL", url);
                AsyncExecute hoge = new AsyncExecute();
                hoge.Start(url);
            } catch (JSONException e) {
                Log.e("JSONError", e.toString());
            }
        }

        @Override
        public void onLoaderReset(Loader<JSONObject> loader) {

        }
    }

    public void URLPurser(String url) {
        try {
            String id = null;
            if (url.contains("flic")) {
                Log.v("flickr", url);
                if (url.contains("w.flickr")) {
                    Pattern pattern = Pattern.compile("^https?://[wm]w*\\.flickr\\.com/?#?/photos/[\\w@]+/(\\d+)");
                    Matcher matcher = pattern.matcher(url);
                    if (matcher.find()) {
                        Log.v("match", "success");
                    }
                    id = matcher.group(1);
                } else if (url.contains("flic.kr")) {
                    Pattern pattern = Pattern.compile("^https?://flic\\.kr/p/(\\w+)");
                    Matcher matcher = pattern.matcher(url);
                    if (matcher.find()) {
                        Log.v("match", "success");
                    }
                    id = Base58.decode(matcher.group(1));
                }
                sitename = "flickr";
                filename = id;
                String api_key = (String) getText(R.string.flickr_key);
                String request = "https://api.flickr.com/services/rest/?method=flickr.photos.getInfo&format=json&api_key=" + api_key +
                "&photo_id=" + id;
                Log.v("flickrAPI", request);
                AsyncJSONExecute hoge = new AsyncJSONExecute();
                hoge.Start(request);
            } else {
                if (url.contains("twipple")) {
                    Log.v("twipple", url);
                    Pattern pattern = Pattern.compile("^https?://p\\.twipple\\.jp/(\\w+)");
                    Matcher matcher = pattern.matcher(url);
                    if (matcher.find()) {
                        Log.v("match", "success");
                    }
                    id = matcher.group(1);
                    sitename = "twipple";
                    filename = id;
                    AsyncExecute hoge = new AsyncExecute();
                    hoge.Start("http://p.twipple.jp/show/orig/" + id);
                } else if (url.contains("img.ly")) {
                    Log.v("img.ly", url);
                    Pattern pattern = Pattern.compile("^https?://img\\.ly/(\\w+)");
                    Matcher matcher = pattern.matcher(url);
                    if (matcher.find()) {
                        Log.v("match", "success");
                    }
                    id = matcher.group(1);
                    sitename = "img.ly";
                    filename = id;
                    AsyncExecute hoge = new AsyncExecute();
                    hoge.Start("http://img.ly/show/full/" + id);
                } else if (url.contains("instagr")) {
                    Log.v("instagram", url);
                    Pattern pattern = Pattern.compile("^https?://instagr\\.?am[\\.com]*/p/(\\w+)");
                    Matcher matcher = pattern.matcher(url);
                    if (matcher.find()) {
                        Log.v("match", "success");
                    }
                    id = matcher.group(1);
                    sitename = "instagram";
                    filename = id;
                    AsyncExecute hoge = new AsyncExecute();
                    hoge.Start("http://instagram.com/p/" + id + "/media/?size=l");
                } else {
                    Log.v("default", "hoge");
                    AsyncExecute hoge = new AsyncExecute();
                    Toast.makeText(Show.this, "this url is incompatible, so showing a sample picture.", Toast.LENGTH_LONG).show();
                    hoge.Start("https://pbs.twimg.com/media/Bz1FnXUCEAAkVGt.png:orig");
                }
            }
        } catch (Exception e) {
            Log.e("IOException", e.toString());
        }
    }
}
