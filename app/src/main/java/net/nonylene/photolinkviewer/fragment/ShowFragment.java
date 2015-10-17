package net.nonylene.photolinkviewer.fragment;

import android.app.DialogFragment;
import android.app.DownloadManager;
import android.app.LoaderManager;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Point;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.app.Fragment;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.TimingLogger;
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

import net.nonylene.photolinkviewer.R;
import net.nonylene.photolinkviewer.async.AsyncGetSizeType;
import net.nonylene.photolinkviewer.async.AsyncHttpBitmap;
import net.nonylene.photolinkviewer.dialog.SaveDialogFragment;
import net.nonylene.photolinkviewer.tool.Initialize;
import net.nonylene.photolinkviewer.tool.PLVUrl;
import net.nonylene.photolinkviewer.tool.ProgressBarListener;

import java.io.File;

public class ShowFragment extends Fragment {

    private View view;
    private ImageView imageView;
    private FrameLayout showFrameLayout;
    private ProgressBar progressBar;

    private SharedPreferences preferences;
    private float firstzoom = 1;
    private MyQuickScale quickScale;

    private TimingLogger mLogger;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mLogger = new TimingLogger("TEST", "plv");

        preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());

        view = inflater.inflate(R.layout.show_fragment, container, false);

        imageView = (ImageView) view.findViewById(R.id.imgview);
        showFrameLayout = (FrameLayout) view.findViewById(R.id.showframe);
        progressBar = (ProgressBar) view.findViewById(R.id.showprogress);

        final ScaleGestureDetector scaleGestureDetector = new ScaleGestureDetector(getActivity(), new simpleOnScaleGestureListener());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            scaleGestureDetector.setQuickScaleEnabled(false);
        }

        final GestureDetector gestureDetector = new GestureDetector(getActivity(), new simpleOnGestureListener());
        imageView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                scaleGestureDetector.onTouchEvent(event);
                if (!scaleGestureDetector.isInProgress()) {
                    gestureDetector.onTouchEvent(event);
                }
                if (quickScale != null) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_MOVE:
                            // image_view double_tap quick scale
                            quickScale.onMove(event);
                            break;
                        case MotionEvent.ACTION_UP:
                            quickScale = null;
                            break;
                    }
                }
                return true;
            }
        });

        if (!preferences.getBoolean("initialized39", false)) {
            Initialize.INSTANCE$.initialize39(getActivity());
        }

        if (getArguments().getBoolean("single_frag", false)) {
            showFrameLayout.setBackgroundResource(R.color.transparent);
            progressBar.setVisibility(View.GONE);
        }

        PLVUrl plvUrl = getArguments().getParcelable("plvurl");
        mLogger.addSplit("start");
        getAsyncGetSizeType(plvUrl).execute(plvUrl.getDisplayUrl());

        return view;
    }

    private AsyncGetSizeType getAsyncGetSizeType(final PLVUrl plvUrl) {

        return new AsyncGetSizeType(getActivity()) {
            @Override
            protected void onPostExecute(AsyncGetSizeType.Result result) {
                super.onPostExecute(result);
                mLogger.addSplit("end");
                mLogger.dumpToLog();
                if (!isAdded()) return;

                if (result.getType() != null) {
                    plvUrl.setType(result.getType());
                    plvUrl.setHeight(result.getHeight());
                    plvUrl.setWidth(result.getWidth());
                    addDLButton(plvUrl);

                    if ("gif".equals(result.getType())) {
                        addWebView(plvUrl);
                    } else {
                        AsyncExecute asyncExecute = new AsyncExecute();
                        asyncExecute.Start(plvUrl);
                    }

                } else {
                    Toast.makeText(getActivity(), getString(R.string.show_bitamap_error) +
                            (result.getErrorMessage() != null ? "\n" + result.getErrorMessage() : ""), Toast.LENGTH_LONG).show();
                }

            }
        };
    }


    class simpleOnGestureListener extends GestureDetector.SimpleOnGestureListener {
        boolean double_zoom;

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
            double_zoom = preferences.getBoolean("double_zoom", false);
            quickScale = new MyQuickScale(e, !double_zoom);
            if (!double_zoom) {
                doubleZoom(e);
            }
            return super.onDoubleTap(e);
        }

        @Override
        public boolean onDoubleTapEvent(MotionEvent e) {
            if (quickScale != null && e.getAction() == MotionEvent.ACTION_UP) {
                if (double_zoom && !quickScale.moved()) {
                    doubleZoom(e);
                }
                quickScale = null;
            }
            return false;
        }

        private void doubleZoom(MotionEvent e) {
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
        }
    }

    private class MyQuickScale {
        // quick scale zoom
        private float initialY;
        private float initialX;
        private float basezoom;
        private float old_zoom;
        private boolean moved = false;

        public MyQuickScale(MotionEvent e, boolean double_frag) {
            initialY = e.getY();
            initialX = e.getX();
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
            // double tap
            if (double_frag) {
                basezoom = basezoom * 2;
            }
            old_zoom = 1;
        }

        public void onMove(MotionEvent e) {
            moved = true;
            float touchY = e.getY();
            Matrix matrix = new Matrix();
            matrix.set(imageView.getImageMatrix());
            // adjust zoom speed
            // If using preference_fragment, value is saved to DefaultSharedPref.
            float zoomspeed = Float.parseFloat(preferences.getString("zoom_speed", "1.4"));
            float new_zoom = (float) Math.pow(touchY / initialY, zoomspeed * 2);
            // photo's zoom scale (is relative to old zoom value.)
            float scale = new_zoom / old_zoom;
            if (new_zoom > firstzoom / basezoom * 0.8) {
                old_zoom = new_zoom;
                matrix.postScale(scale, scale, initialX, initialY);
                imageView.setImageMatrix(matrix);
            }
        }

        public boolean moved() {
            return moved;
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
            old_zoom = 1;
            return super.onScaleBegin(detector);
        }

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            Matrix matrix = new Matrix();
            matrix.set(imageView.getImageMatrix());
            // adjust zoom speed
            // If using preference_fragment, value is saved to DefaultSharedPref.
            float zoomspeed = Float.parseFloat(preferences.getString("zoom_speed", "1.4"));
            float new_zoom = (float) Math.pow(detector.getScaleFactor(), zoomspeed);
            // photo's zoom scale (is relative to old zoom value.)
            float scale = new_zoom / old_zoom;
            if (new_zoom > firstzoom / basezoom * 0.8) {
                old_zoom = new_zoom;
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

    public class AsyncExecute implements LoaderManager.LoaderCallbacks<AsyncHttpBitmap.Result> {
        PLVUrl plvUrl;

        public void Start(PLVUrl plvUrl) {
            this.plvUrl = plvUrl;
            //there are some loaders, so restart(all has finished)
            Bundle bundle = new Bundle();
            bundle.putParcelable("plvurl", plvUrl);
            getLoaderManager().restartLoader(0, bundle, this);
        }

        @Override
        public Loader<AsyncHttpBitmap.Result> onCreateLoader(int id, Bundle bundle) {
            PLVUrl plvUrl = bundle.getParcelable("plvurl");
            int max_size = 2048;
            return new AsyncHttpBitmap(getActivity().getApplicationContext(), plvUrl, max_size);
        }

        @Override
        public void onLoadFinished(Loader<AsyncHttpBitmap.Result> loader, AsyncHttpBitmap.Result result) {
            //remove progressbar
            removeProgressBar();

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
                if (preferences.getBoolean("adjust_zoom", false) || zoom < 1) {
                    //zoom
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
                    if (zoom < 1) {
                        firstzoom = zoom;
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

                if (result.getIsResized()) {
                    Toast.makeText(view.getContext(),
                            getString(R.string.resize_message) + result.getOriginalWidth() + "x" + result.getOriginalHeight() ,
                            Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(view.getContext(), getString(R.string.show_bitamap_error), Toast.LENGTH_LONG).show();
            }
        }

        @Override
        public void onLoaderReset(Loader<AsyncHttpBitmap.Result> loader) {

        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        imageView.setImageBitmap(null);
        ImageButton dlButton = (ImageButton) getActivity().findViewById(R.id.dlbutton);
        FrameLayout dlLayout = (FrameLayout) getActivity().findViewById(R.id.dlbutton_frame);
        LinearLayout linearLayout = (LinearLayout) getActivity().findViewById(R.id.rotate_root);
        if (dlButton != null) {
            dlLayout.setVisibility(View.GONE);
        }
        if (linearLayout != null) {
            linearLayout.setVisibility(View.GONE);
        }
    }

    private Bundle getFileNames(PLVUrl plvUrl) {
        File dir;
        // get site, url and type from bundle
        String sitename = plvUrl.getSiteName();
        // set download directory
        String directory = preferences.getString("download_dir", "PLViewer");
        File root = Environment.getExternalStorageDirectory();
        // set filename (follow setting)
        String filename;
        if (preferences.getString("download_file", "mkdir").equals("mkdir")) {
            // make directory
            dir = new File(root, directory + "/" + sitename);
            filename = plvUrl.getFileName();
        } else {
            // not make directory
            dir = new File(root, directory);
            filename = sitename + "-" + plvUrl.getFileName();
        }
        filename += "." + plvUrl.getType();
        dir.mkdirs();

        Bundle bundle = new Bundle();
        bundle.putString("filename", filename);
        bundle.putString("dir", dir.toString());

        //check wifi connecting and setting or not
        boolean wifi = false;
        if (preferences.getBoolean("wifi_switch", false)) {
            // get wifi status
            ConnectivityManager manager = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo info = manager.getActiveNetworkInfo();
            if (info.getType() == ConnectivityManager.TYPE_WIFI) {
                wifi = true;
            }
        }

        boolean original;
        if (wifi) {
            original = preferences.getBoolean("original_switch_wifi", false);
        } else {
            original = preferences.getBoolean("original_switch_3g", false);
        }

        if (original) {
            bundle.putString("original_url", plvUrl.getBiggestUrl());
        } else {
            bundle.putString("original_url", plvUrl.getDisplayUrl());
        }

        return bundle;
    }

    private void save(Bundle bundle) {
        String url = bundle.getString("original_url");
        String dir_string = bundle.getString("dir");
        String filename = bundle.getString("filename");
        File path = new File(dir_string, filename);
        //save file
        Uri uri = Uri.parse(url);
        // use download manager
        DownloadManager downloadManager = (DownloadManager) getActivity().getSystemService(Context.DOWNLOAD_SERVICE);
        DownloadManager.Request request = new DownloadManager.Request(uri);
        request.setDestinationUri(Uri.fromFile(path));
        request.setTitle("PhotoLinkViewer");
        request.setDescription(filename);
        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_MOBILE | DownloadManager.Request.NETWORK_WIFI);
        // notify
        if (preferences.getBoolean("leave_notify", true)) {
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        }
        downloadManager.enqueue(request);
        Toast.makeText(getActivity(), getString(R.string.download_photo_title) + path.toString(), Toast.LENGTH_LONG).show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case 0:
                save(data.getBundleExtra("bundle"));
                break;
        }
    }

    private void addDLButton(final PLVUrl plvUrl) {
        // dl button visibility and click
        ImageButton dlButton = (ImageButton) getActivity().findViewById(R.id.dlbutton);

        dlButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // download direct
                if (preferences.getBoolean("skip_dialog", false)) {
                    save(getFileNames(plvUrl));
                } else {
                    // open dialog
                    DialogFragment dialogFragment = new SaveDialogFragment();
                    dialogFragment.setArguments(getFileNames(plvUrl));
                    dialogFragment.setTargetFragment(ShowFragment.this, 0);
                    dialogFragment.show(getFragmentManager(), "Save");
                }
            }
        });

        FrameLayout dlLayout = (FrameLayout) getActivity().findViewById(R.id.dlbutton_frame);
        dlLayout.setVisibility(View.VISIBLE);
    }

    private void addWebView(PLVUrl plvUrl) {
        int videoWidth = plvUrl.getWidth();
        int videoHeight = plvUrl.getHeight();

        Display display = getActivity().getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int dispWidth = size.x;
        int dispHeight = size.y;

        // gif view by web view
        WebView webView = new WebView(getActivity());
        webView.getSettings().setUseWideViewPort(true);
        webView.getSettings().setLoadWithOverviewMode(true);

        FrameLayout.LayoutParams layoutParams;
        String escaped = TextUtils.htmlEncode(plvUrl.getDisplayUrl());
        String html;
        if ((videoHeight > dispHeight * 0.9 && videoWidth * dispHeight / dispHeight < dispWidth) || dispWidth * videoHeight / videoWidth > dispHeight) {
            // if height of video > disp_height * 0.9, check whether calculated width > disp_width . if this is true,
            // give priority to width. and, if check whether calculated height > disp_height, give priority to height.
            int width = (int) (dispWidth * 0.9);
            int height = (int) (dispHeight * 0.9);
            layoutParams = new FrameLayout.LayoutParams(width, height);
            html = "<html><body><img style='display: block; margin: 0 auto' height='100%'src='" + escaped + "'></body></html>";
        } else {
            int width = (int) (dispWidth * 0.9);
            layoutParams = new FrameLayout.LayoutParams(width, width * videoHeight / videoWidth);
            html = "<html><body><img style='display: block; margin: 0 auto' width='100%'src='" + escaped + "'></body></html>";
        }
        layoutParams.gravity = Gravity.CENTER;
        webView.setLayoutParams(layoutParams);

        // html to centering
        webView.loadData(html, "text/html", "utf-8");
        webView.setBackgroundColor(0x00000000);
        WebSettings settings = webView.getSettings();
        settings.setBuiltInZoomControls(true);

        removeProgressBar();
        showFrameLayout.addView(webView);
    }

    private void removeProgressBar() {
        showFrameLayout.removeView(progressBar);
        if (getActivity() instanceof ProgressBarListener)
            ((ProgressBarListener) getActivity()).hideProgressBar();
    }
}