package net.nonylene.photolinkviewer;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.LoaderManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
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

import javax.crypto.spec.SecretKeySpec;

import twitter4j.AsyncTwitter;
import twitter4j.AsyncTwitterFactory;
import twitter4j.MediaEntity;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterAdapter;
import twitter4j.TwitterFactory;
import twitter4j.TwitterListener;
import twitter4j.auth.AccessToken;


public class Show extends Activity {

    //directory,filename to save
    private String filename = "hoge";
    private String sitename = "hoge";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show);
        Button button1 = (Button) findViewById(R.id.button1);
        Button button2 = (Button) findViewById(R.id.button2);
        button1.setOnClickListener(new Button1ClickListener());
        button2.setOnClickListener(new Button2ClickListener());
        SharedPreferences preferences = getSharedPreferences("preference", MODE_PRIVATE);
        if (!preferences.getBoolean("Initialized", false)) {
            DialogFragment fragment = new InitDialogFragment();
            fragment.show(getFragmentManager(), "show");
            preferences.edit().putBoolean("Initialized", true).apply();
        }
        //receive intent
        if (Intent.ACTION_VIEW.equals(getIntent().getAction())) {
            Uri uri = getIntent().getData();
            String url = uri.toString();
            Log.v("url", url);
            PLVImageView plvImageView = (PLVImageView) findViewById(R.id.imgview);
            //set long-press url
            plvImageView.url = url;
            URLPurser(url);
        } else {
            Toast.makeText(this, "Intent Error!", Toast.LENGTH_LONG).show();
        }
    }

    class Button1ClickListener implements View.OnClickListener {
        public void onClick(View v) {
            //save file
            ImageView imageView = (ImageView) findViewById(R.id.imgview);
            //pickup bitmap
            Bitmap bitmap = ((BitmapDrawable) imageView.getDrawable()).getBitmap();
            File root = Environment.getExternalStorageDirectory();
            String directory = "PLViewer";
            File dir = new File(root, directory + "/" + sitename);
            Log.v("dir", dir.toString());
            //make directory
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
            //settings
            Intent intent = new Intent(Show.this, Settings.class);
            startActivity(intent);
        }
    }

    public class AsyncExecute implements LoaderManager.LoaderCallbacks<Drawable> {

        public void Start(String url) {
            Bundle bundle = new Bundle();
            bundle.putString("url", url);
            //there are some loaders, so restart(all has finished)
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
            //set image
            PLVImageView imageView = (PLVImageView) findViewById(R.id.imgview);
            imageView.setImageDrawable(drawable);
        }

        @Override
        public void onLoaderReset(Loader<Drawable> loader) {

        }
    }

    public class AsyncJSONExecute implements LoaderManager.LoaderCallbacks<JSONObject> {
        //get json from url

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
                //for flickr
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

    public static class InitDialogFragment extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle("Thank you for install")
                    .setMessage("対応サイト\nflickr, imgly, twipple, instagram, gyazo\n※gifには非対応\n\n操作方法\n" +
                            "URLのIntentから開いてください。ズームするのとtwitterはいつか対応するつもり。画像を長押しすることで元の" +
                            "URLに飛ぶことができます。saveを押すとPLViewerのディレクトリにpngとして保存されます。\n\n" +
                            "今のところSettingsは特に意味無いです。")
                    .setNeutralButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent intent = new Intent(getActivity(), TOAuth.class);
                            startActivity(intent);
                        }
                    });
            return builder.create();
        }
    }

    public void URLPurser(String url) {
        //purse url

        try {
            String id = null;
            if (url.contains("flic")) {
                Log.v("flickr", url);
                if (url.contains("flickr")) {
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
            } else if (url.contains("twitter")) {
                Log.v("twitter", url);
                Pattern pattern = Pattern.compile("^https?://twitter\\.com/\\w+/status/(\\d+)/photo");
                Matcher matcher = pattern.matcher(url);
                if (matcher.find()) {
                    Log.v("match", "success");
                }
                id = matcher.group(1);
                sitename = "twitter";
                filename = id;
                SharedPreferences sharedPreferences = getSharedPreferences("preference", MODE_PRIVATE);
                String apikey = (String) getText(R.string.twitter_key);
                String apisecret = (String) getText(R.string.twitter_secret);
                byte[] keyboo = Base64.decode(sharedPreferences.getString("key", null), Base64.DEFAULT);
                SecretKeySpec key = new SecretKeySpec(keyboo, 0, keyboo.length, "AES");
                byte[] token = Base64.decode(sharedPreferences.getString("ttoken", null), Base64.DEFAULT);
                byte[] token_secret = Base64.decode(sharedPreferences.getString("ttokensecret", null), Base64.DEFAULT);
                AccessToken accessToken = new AccessToken(Encryption.decrypt(token, key), Encryption.decrypt(token_secret, key));
                AsyncTwitter twitter = new AsyncTwitterFactory().getInstance();
                twitter.setOAuthConsumer(apikey, apisecret);
                twitter.setOAuthAccessToken(accessToken);
                twitter.addListener(twitterListener);
                twitter.showStatus(Long.parseLong(id));
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
                } else if (url.contains("gyazo")) {
                    Log.v("gyazo", url);
                    Pattern pattern = Pattern.compile("^https?://gyazo\\.com/(\\w+)");
                    Matcher matcher = pattern.matcher(url);
                    if (matcher.find()) {
                        Log.v("match", "success");
                    }
                    id = matcher.group(1);
                    sitename = "gyazo";
                    filename = id;
                    AsyncExecute hoge = new AsyncExecute();
                    //redirect followed if new protocol is the same as old one.
                    hoge.Start("https://gyazo.com/" + id + "/raw");
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

    private TwitterListener twitterListener = new TwitterAdapter() {
        @Override
        public void gotShowStatus(Status status) {
            MediaEntity[] mediaEntities = status.getMediaEntities();
            Log.v("media", mediaEntities[0].getMediaURL());
            String url = mediaEntities[0].getMediaURL();
            AsyncExecute hoge = new AsyncExecute();
            hoge.Start(url + ":orig");
        }
    };
}
