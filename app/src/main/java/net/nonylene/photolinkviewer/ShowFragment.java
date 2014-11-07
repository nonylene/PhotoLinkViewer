package net.nonylene.photolinkviewer;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.Context;
import android.content.Loader;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Point;
import android.os.Bundle;
import android.app.Fragment;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.spec.SecretKeySpec;

import twitter4j.AsyncTwitter;
import twitter4j.AsyncTwitterFactory;
import twitter4j.MediaEntity;
import twitter4j.Status;
import twitter4j.TwitterAdapter;
import twitter4j.TwitterListener;
import twitter4j.auth.AccessToken;

public class ShowFragment extends Fragment {

    private View view;
    private ImageView imageView;
    private OnFragmentInteractionListener mListener;
    private SharedPreferences preferences;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.show_fragment, container, false);
        preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        imageView = (ImageView) view.findViewById(R.id.imgview);
        final ScaleGestureDetector scaleGestureDetector = new ScaleGestureDetector(getActivity(), new simpleOnScaleGestureListener());
        final GestureDetector GestureDetector = new GestureDetector(getActivity(), new simpleOnGestureListener());
        imageView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                scaleGestureDetector.onTouchEvent(event);
                if (!scaleGestureDetector.isInProgress()) {
                    GestureDetector.onTouchEvent(event);
                }
                return true;
            }
        });
        String url = getArguments().getString("url");
        URLPurser(url);
        return view;
    }

    class simpleOnGestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            // drag photo
            float[] values = new float[9];
            Matrix matrix = new Matrix();
            matrix.set(imageView.getImageMatrix());
            matrix.getValues(values);
            // move photo
            values[Matrix.MTRANS_X] = values[Matrix.MTRANS_X] - distanceX;
            values[Matrix.MTRANS_Y] = values[Matrix.MTRANS_Y] - distanceY;
            matrix.setValues(values);
            imageView.setImageMatrix(matrix);
            return super.onScroll(e1, e2, distanceX, distanceY);
        }
    }

    class simpleOnScaleGestureListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        private float touchX;
        private float touchY;
        private float basezoom;
        private float firstzoom;
        private boolean first = true;
        private float old_zoom;

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            //define zoom-base point
            touchX = detector.getFocusX();
            touchY = detector.getFocusY();
            //get current status
            float[] values = new float[9];
            Matrix matrix = new Matrix();
            matrix.set(imageView.getImageMatrix());
            matrix.getValues(values);
            //set base zoom param
            basezoom = values[Matrix.MSCALE_X];
            if (basezoom == 0) {
                basezoom = Math.abs(values[Matrix.MSKEW_X]);
            }
            if (first) {
                firstzoom = basezoom;
                first = false;
            }
            old_zoom = 1;
            return super.onScaleBegin(detector);
        }

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            Matrix matrix = new Matrix();
            imageView = (ImageView) view.findViewById(R.id.imgview);
            matrix.set(imageView.getImageMatrix());
            // adjust zoom speed
            // If using preference_fragment, value is saved to DefaultSharedPref.
            float zoomspeed = Float.parseFloat(preferences.getString("zoom_speed", "1.4"));
            float new_zoom = (float) Math.pow(detector.getScaleFactor(), zoomspeed);
            // photo's zoom scale (is relative to old zoom value.)
            float scale = new_zoom / old_zoom;
            old_zoom = new_zoom;
            if (new_zoom > firstzoom / basezoom * 0.8) {
                matrix.postScale(scale, scale, touchX, touchY);
                imageView.setImageMatrix(matrix);
            }
            return super.onScale(detector);
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            super.onScaleEnd(detector);
        }
    }

    public class AsyncExecute implements LoaderManager.LoaderCallbacks<Bitmap> {

        public void Start(String url) {
            Bundle bundle = new Bundle();
            bundle.putString("url", url);
            //there are some loaders, so restart(all has finished)
            getLoaderManager().restartLoader(0, bundle, this);
        }

        @Override
        public Loader<Bitmap> onCreateLoader(int id, Bundle bundle) {
            try {
                String c = bundle.getString("url");
                URL url = new URL(c);
                int max_size = 2048;
                if (preferences.getBoolean("view_4096", false)) {
                    max_size = 4096;
                }
                return new AsyncHttp(getActivity().getApplicationContext(), url, max_size);
            } catch (IOException e) {
                Log.e("DrawableLoaderError", e.toString());
                return null;
            }
        }

        @Override
        public void onLoadFinished(Loader<Bitmap> loader, Bitmap bitmap) {
            //remove progressbar
            FrameLayout frameLayout = (FrameLayout) view.findViewById(R.id.showframe);
            ProgressBar progressBar = (ProgressBar) view.findViewById(R.id.showprogress);
            frameLayout.removeView(progressBar);
            //get bitmap size
            float origwidth = bitmap.getWidth();
            float origheight = bitmap.getHeight();
            //set image
            imageView.setImageBitmap(bitmap);
            //get matrix from imageview
            Matrix matrix = new Matrix();
            matrix.set(imageView.getMatrix());
            //get display size
            Display display = getActivity().getWindowManager().getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            int dispwidth = size.x;
            int dispheight = size.y;
            float wid = dispwidth / origwidth;
            float hei = dispheight / origheight;
            float zoom = Math.min(wid, hei);
            float initX;
            float initY;
            if (zoom < 1) {
                //zoom
                matrix.setScale(zoom, zoom);
                if (wid < hei) {
                    //adjust width
                    initX = 0;
                    initY = (dispheight - origheight * wid) / 2;
                } else {
                    //adjust height
                    initX = (dispwidth - origwidth * hei) / 2;
                    initY = 0;
                }
            } else {
                //move
                initX = (dispwidth - origwidth) / 2;
                initY = (dispheight - origheight) / 2;
            }

            matrix.postTranslate(initX, initY);
            imageView.setImageMatrix(matrix);
        }

        @Override
        public void onLoaderReset(Loader<Bitmap> loader) {

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
                return new AsyncJSON(getActivity().getApplicationContext(), url);
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
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
                String url;
                if (sharedPreferences.getBoolean("flickr_original", false)) {
                    String original_secret = photo.getString("originalsecret");
                    String original_format = photo.getString("originalformat");
                    url = "https://farm" + farm + ".staticflickr.com/" + server + "/" + id + "_" + original_secret + "_o." + original_format;
                } else {
                    String secret = photo.getString("secret");
                    //license
                    url = "https://farm" + farm + ".staticflickr.com/" + server + "/" + id + "_" + secret + "_b.jpg";
                }
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

    public void URLPurser(String url) {
        //purse url

        //directory,filename to save
        String sitename = null;
        String filename = null;

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
                Pattern pattern = Pattern.compile("^https?://twitter\\.com/\\w+/status/(\\d+)");
                Matcher matcher = pattern.matcher(url);
                if (matcher.find()) {
                    Log.v("match", "success");
                }
                id = matcher.group(1);
                sitename = "twitter";
                filename = id;
                SharedPreferences sharedPreferences = getActivity().getSharedPreferences("preference", Context.MODE_PRIVATE);
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
                } else if (url.contains("imgur")) {
                    Log.v("gyazo", url);
                    Pattern pattern = Pattern.compile("^https?://.*imgur\\.com/([\\w^\\.]+)");
                    Matcher matcher = pattern.matcher(url);
                    if (matcher.find()) {
                        Log.v("match", "success");
                    }
                    id = matcher.group(1);
                    sitename = "imgur";
                    filename = id;
                    AsyncExecute hoge = new AsyncExecute();
                    hoge.Start("http://i.imgur.com/" + id + ".jpg");
                } else if (url.contains("twimg")) {
                    Log.v("twimg", url);
                    Pattern pattern = Pattern.compile("^https?://pbs\\.twimg\\.com/media/([^\\.]+)\\.");
                    Matcher matcher = pattern.matcher(url);
                    if (matcher.find()) {
                        Log.v("match", "success");
                    }
                    id = matcher.group(1);
                    sitename = "twitter";
                    filename = id;
                    AsyncExecute hoge = new AsyncExecute();
                    hoge.Start(url + ":orig");
                } else {
                    Log.v("other", url);
                    Pattern pattern = Pattern.compile("/([\\w^\\.]+)\\.\\w*$");
                    Matcher matcher = pattern.matcher(url);
                    if (matcher.find()) {
                        Log.v("match", "success");
                    }
                    id = matcher.group(1);
                    sitename = "other";
                    filename = id;
                    AsyncExecute hoge = new AsyncExecute();
                    hoge.Start(url);
                }
            }
            Bundle bundle = new Bundle();
            bundle.putString("url", url);
            bundle.putString("sitename", sitename);
            bundle.putString("filename", filename);
            mListener.onPurseFinished(bundle);
        } catch (Exception e) {
            Log.e("IOException", e.toString());
        }
    }

    //this is needed to return bundle
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    public interface OnFragmentInteractionListener {
        public void onPurseFinished(Bundle bundle);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        imageView.setImageBitmap(null);
    }
}