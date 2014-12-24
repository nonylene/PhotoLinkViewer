package net.nonylene.photolinkviewer.fragment;

import android.app.DialogFragment;
import android.app.LoaderManager;
import android.content.Context;
import android.content.Loader;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Point;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.app.Fragment;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import net.nonylene.photolinkviewer.tool.Base58;
import net.nonylene.photolinkviewer.tool.GIFException;
import net.nonylene.photolinkviewer.R;
import net.nonylene.photolinkviewer.async.AsyncHttp;
import net.nonylene.photolinkviewer.async.AsyncHttpResult;
import net.nonylene.photolinkviewer.async.AsyncJSON;
import net.nonylene.photolinkviewer.dialog.SaveDialogFragment;
import net.nonylene.photolinkviewer.tool.Initialize;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ShowFragment extends Fragment {

    private View view;
    private ImageView imageView;
    private SharedPreferences preferences;
    private float firstzoom = 1;

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
        if (!preferences.getBoolean("initialized19", false)) {
            Initialize.initialize19(getActivity());
        }
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

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            final float touchX = e.getX();
            final float touchY = e.getY();
            ScaleAnimation scaleAnimation = new ScaleAnimation(1, 2, 1, 2, touchX, touchY);
            scaleAnimation.setDuration(200);
            scaleAnimation.setFillEnabled(true);
            scaleAnimation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    final Matrix matrix = new Matrix();
                    matrix.set(imageView.getImageMatrix());
                    matrix.postScale(2, 2, touchX, touchY);
                    imageView.setImageMatrix(matrix);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }
            });
            imageView.startAnimation(scaleAnimation);
            // drag photo
            return super.onDoubleTap(e);
        }
    }

    class simpleOnScaleGestureListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        private float touchX;
        private float touchY;
        private float basezoom;
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
            Log.v("firstzoom", String.valueOf(firstzoom));
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

    public class AsyncExecute implements LoaderManager.LoaderCallbacks<AsyncHttpResult<Bitmap>> {

        public void Start(String url) {
            Bundle bundle = new Bundle();
            bundle.putString("url", url);
            //there are some loaders, so restart(all has finished)
            getLoaderManager().restartLoader(0, bundle, this);
        }

        @Override
        public Loader<AsyncHttpResult<Bitmap>> onCreateLoader(int id, Bundle bundle) {
            try {
                String c = bundle.getString("url");
                URL url = new URL(c);
                int max_size = 2048;
                return new AsyncHttp(getActivity().getApplicationContext(), url, max_size);
            } catch (IOException e) {
                Log.e("DrawableLoaderError", e.toString());
                return null;
            }
        }

        @Override
        public void onLoadFinished(Loader<AsyncHttpResult<Bitmap>> loader, AsyncHttpResult<Bitmap> result) {
            //remove progressbar
            FrameLayout frameLayout = (FrameLayout) view.findViewById(R.id.showframe);
            ProgressBar progressBar = (ProgressBar) view.findViewById(R.id.showprogress);
            frameLayout.removeView(progressBar);

            Bitmap bitmap = result.getBitmap();

            Display display = getActivity().getWindowManager().getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            int dispWidth = size.x;
            int dispHeight = size.y;

            if (bitmap != null) {
                //get bitmap size
                float origwidth = bitmap.getWidth();
                float origheight = bitmap.getHeight();
                //set image
                imageView.setImageBitmap(bitmap);
                //get matrix from imageview
                Matrix matrix = new Matrix();
                matrix.set(imageView.getMatrix());
                //get display size
                float wid = dispWidth / origwidth;
                float hei = dispHeight / origheight;
                float zoom = Math.min(wid, hei);
                float initX;
                float initY;
                if (zoom < 1) {
                    //zoom
                    firstzoom = zoom;
                    matrix.setScale(zoom, zoom);
                    if (wid < hei) {
                        //adjust width
                        initX = 0;
                        initY = (dispHeight - origheight * wid) / 2;
                    } else {
                        //adjust height
                        initX = (dispWidth - origwidth * hei) / 2;
                        initY = 0;
                    }
                } else {
                    //move
                    initX = (dispWidth - origwidth) / 2;
                    initY = (dispHeight - origheight) / 2;
                }

                matrix.postTranslate(initX, initY);
                imageView.setImageMatrix(matrix);
                LinearLayout linearLayout = (LinearLayout) getActivity().findViewById(R.id.rotate_root);
                linearLayout.setVisibility(View.VISIBLE);
            } else {
                if (result.getException() instanceof GIFException) {
                    // gif view by web view
                    WebView webView = new WebView(getActivity());
                    webView.getSettings().setUseWideViewPort(true);
                    webView.getSettings().setLoadWithOverviewMode(true);
                    FrameLayout.LayoutParams layoutParams;
                    int videoWidth = result.getWidth();
                    int videoHeight = result.getHeight();
                    if ((videoHeight > dispHeight * 0.9 && videoWidth * dispHeight / dispHeight < dispWidth) || dispWidth * videoHeight / videoWidth > dispHeight) {
                        // if height of video > disp_height * 0.9, check whether calculated width > disp_width . if this is true,
                        // give priority to width. and, if check whether calculated height > disp_height, give priority to height.
                        int height = (int) (dispHeight * 0.9);
                        layoutParams = new FrameLayout.LayoutParams(height * videoWidth / videoHeight, height);
                    } else {
                        int width = (int) (dispWidth * 0.9);
                        layoutParams = new FrameLayout.LayoutParams(width, width * videoHeight / videoWidth);
                    }
                    layoutParams.gravity = Gravity.CENTER;
                    webView.setLayoutParams(layoutParams);
                    webView.loadUrl(result.getUrl());
                    webView.setBackgroundColor(0x00000000);
                    WebSettings settings = webView.getSettings();
                    settings.setBuiltInZoomControls(true);
                    frameLayout.addView(webView);
                } else {
                    Toast.makeText(view.getContext(), getString(R.string.show_bitamap_error), Toast.LENGTH_LONG).show();
                }
            }
        }

        @Override
        public void onLoaderReset(Loader<AsyncHttpResult<Bitmap>> loader) {

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
                Log.e("AsyncJSONLoaderError", e.toString());
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
                JSONObject urls = new JSONObject(photo.getString("urls"));
                JSONArray urlArray = new JSONArray(urls.getString("url"));
                String url = urlArray.getJSONObject(0).getString("_content");
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
                String file_url = null;
                String secret = photo.getString("secret");

                // wifi check
                String quality;
                boolean wifi = wifiChecker(sharedPreferences);
                if (wifi) {
                    quality = sharedPreferences.getString("flickr_quality_wifi", "large");
                } else {
                    quality = sharedPreferences.getString("flickr_quality_3g", "large");
                }
                switch (quality) {
                    case "original":
                        String original_secret = photo.getString("originalsecret");
                        String original_format = photo.getString("originalformat");
                        file_url = "https://farm" + farm + ".staticflickr.com/" + server + "/" + id + "_" + original_secret + "_o." + original_format;
                        break;
                    case "large":
                        file_url = "https://farm" + farm + ".staticflickr.com/" + server + "/" + id + "_" + secret + "_b.jpg";
                        break;
                    case "medium":
                        file_url = "https://farm" + farm + ".staticflickr.com/" + server + "/" + id + "_" + secret + "_z.jpg";
                        break;
                }
                Log.v("URL", url);
                final Bundle bundle = new Bundle();
                bundle.putString("url", url);
                if (originalChecker(sharedPreferences, wifi)) {
                    String original_secrets = photo.getString("originalsecret");
                    String original_formats = photo.getString("originalformat");
                    file_url = "https://farm" + farm + ".staticflickr.com/" + server + "/" + id + "_" + original_secrets + "_o." + original_formats;
                    bundle.putString("file_url", file_url);
                } else {
                    bundle.putString("file_url", file_url);
                }
                bundle.putString("sitename", "flickr");
                bundle.putString("filename", id);
                AsyncExecute hoge = new AsyncExecute();
                hoge.Start(file_url);
                ImageButton dlButton = (ImageButton) getActivity().findViewById(R.id.dlbutton);
                if (dlButton != null) {
                    dlButton.setVisibility(View.VISIBLE);
                }
                dlButton.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        // open dialog
                        DialogFragment dialogFragment = new SaveDialogFragment();
                        dialogFragment.setArguments(bundle);
                        dialogFragment.show(getFragmentManager(), "Save");
                    }
                });
            } catch (JSONException e) {
                Log.e("JSONParseError", e.toString());
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(view.getContext(), getString(R.string.show_flickrjson_toast), Toast.LENGTH_LONG).show();
                    }
                });
            }
        }

        @Override
        public void onLoaderReset(Loader<JSONObject> loader) {

        }
    }

    public void URLPurser(String url) {
        //purse url

        //directory,filename to save
        String sitename;
        String filename;
        String file_url = null;
        String original_url = null;

        try {
            String id = null;

            if (url.contains("flickr.com") || url.contains("flic.kr")) {
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
                String api_key = (String) getText(R.string.flickr_key);
                String request = "https://api.flickr.com/services/rest/?method=flickr.photos.getInfo&format=json&api_key=" + api_key +
                        "&photo_id=" + id;
                Log.v("flickrAPI", request);
                AsyncJSONExecute hoge = new AsyncJSONExecute();
                hoge.Start(request);
            } else {

                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
                String quality;

                // wifi check
                boolean wifi = wifiChecker(sharedPreferences);

                if (url.contains("twimg.com/media/")) {
                    Log.v("twimg", url);
                    Pattern pattern = Pattern.compile("^https?://pbs\\.twimg\\.com/media/([^\\.]+)\\.");
                    Matcher matcher = pattern.matcher(url);
                    if (matcher.find()) {
                        Log.v("match", "success");
                    }
                    id = matcher.group(1);
                    sitename = "twitter";
                    filename = id;
                    if (wifi) {
                        quality = sharedPreferences.getString("twitter_quality_wifi", "large");
                    } else {
                        quality = sharedPreferences.getString("twitter_quality_3g", "large");
                    }
                    switch (quality) {
                        case "original":
                            file_url = url + ":orig";
                            break;
                        case "large":
                            file_url = url + ":large";
                            break;
                        case "medium":
                            file_url = url;
                            break;
                        case "small":
                            file_url = url + ":small";
                            break;
                    }
                    original_url = url + ":orig";
                } else if (url.contains("twipple.jp")) {
                    Log.v("twipple", url);
                    Pattern pattern = Pattern.compile("^https?://p\\.twipple\\.jp/(\\w+)");
                    Matcher matcher = pattern.matcher(url);
                    if (matcher.find()) {
                        Log.v("match", "success");
                    }
                    id = matcher.group(1);
                    sitename = "twipple";
                    filename = id;
                    if (wifi) {
                        quality = sharedPreferences.getString("twipple_quality_wifi", "large");
                    } else {
                        quality = sharedPreferences.getString("twipple_quality_3g", "large");
                    }
                    switch (quality) {
                        case "original":
                            file_url = "http://p.twipple.jp/show/orig/" + id;
                            break;
                        case "large":
                            file_url = "http://p.twipple.jp/show/large/" + id;
                            break;
                        case "thumb":
                            file_url = "http://p.twipple.jp/show/thumb/" + id;
                            break;
                    }
                    original_url = "http://p.twipple.jp/show/orig/" + id;
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
                    if (wifi) {
                        quality = sharedPreferences.getString("imgly_quality_wifi", "large");
                    } else {
                        quality = sharedPreferences.getString("imgly_quality_3g", "large");
                    }
                    switch (quality) {
                        case "full":
                            file_url = "http://img.ly/show/full/" + id;
                            break;
                        case "large":
                            file_url = "http://img.ly/show/large/" + id;
                            break;
                        case "medium":
                            file_url = "http://img.ly/show/medium/" + id;
                            break;
                    }
                    original_url = "http://img.ly/show/full/" + id;
                } else if (url.contains("instagram.com") || url.contains("instagr.am")) {
                    Log.v("instagram", url);
                    Pattern pattern = Pattern.compile("^https?://instagr\\.?am[\\.com]*/p/([^/]+)");
                    Matcher matcher = pattern.matcher(url);
                    if (matcher.find()) {
                        Log.v("match", "success");
                    }
                    id = matcher.group(1);
                    sitename = "instagram";
                    filename = id;
                    if (wifi) {
                        quality = sharedPreferences.getString("instagram_quality_wifi", "large");
                    } else {
                        quality = sharedPreferences.getString("instagram_quality_3g", "large");
                    }
                    switch (quality) {
                        case "large":
                            file_url = "http://instagram.com/p/" + id + "/media/?size=l";
                            break;
                        case "medium":
                            file_url = "http://instagram.com/p/" + id + "/media/?size=m";
                            break;
                    }
                    original_url = "http://instagram.com/p/" + id + "/media/?size=l";
                } else if (url.contains("gyazo.com")) {
                    Log.v("gyazo", url);
                    Pattern pattern = Pattern.compile("^https?://.*gyazo\\.com/(\\w+)");
                    Matcher matcher = pattern.matcher(url);
                    if (matcher.find()) {
                        Log.v("match", "success");
                    }
                    id = matcher.group(1);
                    sitename = "gyazo";
                    filename = id;
                    //redirect followed if new protocol is the same as old one.
                    original_url = file_url = "https://gyazo.com/" + id + "/raw";
                } else if (url.contains("imgur.com")) {
                    Log.v("gyazo", url);
                    Pattern pattern = Pattern.compile("^https?://.*imgur\\.com/([\\w^\\.]+)");
                    Matcher matcher = pattern.matcher(url);
                    if (matcher.find()) {
                        Log.v("match", "success");
                    }
                    id = matcher.group(1);
                    sitename = "imgur";
                    filename = id;
                    original_url = file_url = "http://i.imgur.com/" + id + ".jpg";
                } else {
                    Log.v("other", url);
                    Pattern pattern = Pattern.compile("/([^\\.]+)\\.\\w*$");
                    Matcher matcher = pattern.matcher(url);
                    if (matcher.find()) {
                        Log.v("match", "success");
                    }
                    id = matcher.group(1);
                    sitename = "other";
                    filename = id;
                    original_url = file_url = url;
                }
                Log.d("fileurl", file_url);
                AsyncExecute asyncExecute = new AsyncExecute();
                asyncExecute.Start(file_url);
                final Bundle bundle = new Bundle();
                bundle.putString("url", url);
                // get original photo
                if (originalChecker(sharedPreferences, wifi)) {
                    file_url = original_url;
                }
                bundle.putString("file_url", file_url);
                bundle.putString("sitename", sitename);
                bundle.putString("filename", filename);
                // dl button visibility and click
                ImageButton dlButton = (ImageButton) getActivity().findViewById(R.id.dlbutton);
                dlButton.setVisibility(View.VISIBLE);
                dlButton.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        // open dialog
                        DialogFragment dialogFragment = new SaveDialogFragment();
                        dialogFragment.setArguments(bundle);
                        dialogFragment.show(getFragmentManager(), "Save");
                    }
                });
            }
        } catch (IllegalStateException e) {
            // regex error
            Toast.makeText(view.getContext(), getString(R.string.url_purse_toast), Toast.LENGTH_LONG).show();
            Log.e("regex error", e.toString());
            // remove progressbar
            FrameLayout frameLayout = (FrameLayout) view.findViewById(R.id.showframe);
            ProgressBar progressBar = (ProgressBar) view.findViewById(R.id.showprogress);
            frameLayout.removeView(progressBar);
        }
    }

    public boolean wifiChecker(SharedPreferences sharedPreferences) {
        //check wifi connecting and setting or not
        boolean wifi = false;
        if (sharedPreferences.getBoolean("wifi_switch", false)) {
            // get wifi status
            ConnectivityManager manager = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo info = manager.getActiveNetworkInfo();
            if (info.getType() == ConnectivityManager.TYPE_WIFI) {
                wifi = true;
            }
        }
        Log.d("wifi", String.valueOf(wifi));
        return wifi;

    }

    public boolean originalChecker(SharedPreferences sharedPreferences, boolean wifi) {
        //check download original or not
        boolean orig;
        if (wifi) {
            orig = sharedPreferences.getBoolean("original_switch_wifi", false);
        } else {
            orig = sharedPreferences.getBoolean("original_switch_3g", false);
        }
        return orig;

    }

    @Override
    public void onDetach() {
        super.onDetach();
        imageView.setImageBitmap(null);
        ImageButton dlButton = (ImageButton) getActivity().findViewById(R.id.dlbutton);
        LinearLayout linearLayout = (LinearLayout) getActivity().findViewById(R.id.rotate_root);
        if (dlButton != null) {
            dlButton.setVisibility(View.GONE);
        }
        if (linearLayout != null) {
            linearLayout.setVisibility(View.GONE);
        }
    }
}