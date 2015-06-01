package net.nonylene.photolinkviewer;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteException;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.net.http.HttpResponseCache;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.text.TextPaint;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;
import com.android.volley.toolbox.Volley;

import net.nonylene.photolinkviewer.fragment.OptionFragment;
import net.nonylene.photolinkviewer.fragment.ShowFragment;
import net.nonylene.photolinkviewer.fragment.TwitterOptionFragment;
import net.nonylene.photolinkviewer.fragment.VideoShowFragment;
import net.nonylene.photolinkviewer.tool.AccountsList;
import net.nonylene.photolinkviewer.tool.BitmapCache;
import net.nonylene.photolinkviewer.tool.MyAsyncTwitter;
import net.nonylene.photolinkviewer.tool.PLVUrl;
import net.nonylene.photolinkviewer.tool.PLVUrlService;
import net.nonylene.photolinkviewer.tool.TwitterStatusAdapter;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import twitter4j.AsyncTwitter;
import twitter4j.ExtendedMediaEntity;
import twitter4j.Status;
import twitter4j.TwitterAdapter;
import twitter4j.TwitterException;
import twitter4j.TwitterListener;
import twitter4j.TwitterMethod;
import twitter4j.URLEntity;


public class TwitterDisplay extends Activity implements TwitterStatusAdapter.TwitterAdapterListener{
    private String url;
    private ImageLoader imageLoader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_twitter_display);

        //enable cache
        try {
            File httpCacheDir = new File(getApplicationContext().getCacheDir(), "http");
            long httpCacheSize = 10 * 1024 * 1024; // 10 MB
            HttpResponseCache.install(httpCacheDir, httpCacheSize);
        } catch (IOException e) {
            Log.d("cache", "HTTP response cache installation failed");
        }

        RequestQueue queue = Volley.newRequestQueue(getApplicationContext());
        imageLoader = new ImageLoader(queue, new BitmapCache());

        // get intent and purse url
        SharedPreferences sharedPreferences = getSharedPreferences("preference", Context.MODE_PRIVATE);
        if (Intent.ACTION_VIEW.equals(getIntent().getAction())) {
            Bundle bundle = new Bundle();
            Uri uri = getIntent().getData();
            url = uri.toString();
            bundle.putString("url", url);
            // option fragment
            FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
            OptionFragment optionFragment = new OptionFragment();
            optionFragment.setArguments(bundle);
            fragmentTransaction.add(R.id.root_layout, optionFragment).commit();
            if (url.contains("twitter.com")) {

                Pattern pattern = Pattern.compile("^https?://twitter\\.com/\\w+/status[es]*/(\\d+)");
                Matcher matcher = pattern.matcher(url);
                if (!matcher.find()) return;

                String id = matcher.group(1);
                Long id_long = Long.parseLong(id);
                if (sharedPreferences.getBoolean("authorized", false)) {
                    // oAuthed
                    try {
                        // get twitter async
                        AsyncTwitter twitter = MyAsyncTwitter.getAsyncTwitter(getApplicationContext());
                        twitter.addListener(twitterListener);
                        twitter.showStatus(id_long);
                        bundle.putLong("id_long", id_long);
                        TwitterOptionFragment twitterOptionFragment = new TwitterOptionFragment();
                        twitterOptionFragment.setArguments(bundle);
                        getFragmentManager().beginTransaction().add(R.id.buttons, twitterOptionFragment).commit();
                    } catch (SQLiteException e) {
                        Log.e("SQL", e.toString());
                    } catch (IndexOutOfBoundsException e) {
                        Log.e("twitter", e.toString());
                        Toast.makeText(getApplicationContext(), getString(R.string.twitter_async_select), Toast.LENGTH_LONG).show();
                        Intent intent = new Intent(this, TOAuth.class);
                        startActivity(intent);
                        finish();
                    }
                } else {
                    Toast.makeText(getApplicationContext(), getString(R.string.twitter_display_oauth), Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(this, TOAuth.class);
                    startActivity(intent);
                    finish();
                }
            }
        } else {
            Toast.makeText(this, "Intent Error!", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onShowFragmentRequired(PLVUrl plvUrl) {
        // go to show fragment
        Bundle bundle = new Bundle();
        bundle.putParcelable("plvurl", plvUrl);
        FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();

        ShowFragment showFragment = new ShowFragment();
        showFragment.setArguments(bundle);
        fragmentTransaction.replace(R.id.show_frag_replace, showFragment);

        // back to this screen when back pressed
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }

    @Override
    public void onVideoShowFragmentRequired(String fileUrl) {
        // go to show fragment
        Bundle bundle = new Bundle();
        bundle.putString("url", fileUrl);
        FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();

        VideoShowFragment showFragment = new VideoShowFragment();
        showFragment.setArguments(bundle);
        fragmentTransaction.replace(R.id.show_frag_replace, showFragment);

        // back to this screen when back pressed
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }

    public static class ChangeAccountDialog extends DialogFragment {

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AccountsList accountsList = MyAsyncTwitter.getAccountsList(getActivity());
            final ArrayList<String> screen_list = accountsList.getScreenList();
            final ArrayList<Integer> row_id_list = accountsList.getRowIdList();
            // get array for easy
            String[] screen_array = screen_list.toArray(new String[screen_list.size()]);
            // get current screen_name
            final SharedPreferences sharedPreferences = getActivity().getSharedPreferences("preference", MODE_PRIVATE);
            String current_name = sharedPreferences.getString("screen_name", null);
            int position = screen_list.indexOf(current_name);
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(getString(R.string.account_dialog_title))
                    .setNegativeButton(getString(android.R.string.cancel), null)
                    .setSingleChoiceItems(screen_array, position, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int position) {
                            int row_id = row_id_list.get(position);
                            String screen_name = screen_list.get(position);
                            // save rowid and screen name to preference
                            sharedPreferences.edit().putInt("account", row_id).apply();
                            sharedPreferences.edit().putString("screen_name", screen_name).apply();
                            //reload activity
                            startActivity(getActivity().getIntent());
                            dialog.dismiss();
                            getActivity().finish();
                        }
                    });
            return builder.create();
        }

    }

    private TwitterListener twitterListener = new TwitterAdapter() {

        @Override
        public void onException(TwitterException e, TwitterMethod twitterMethod) {
            Log.e("twitterException", e.toString());
            final String message = getString(R.string.twitter_error_toast) + ": " + e.getStatusCode() + "\n(" + e.getErrorMessage() + ")";
            switch (e.getErrorCode()) {
                case 179:
                case 88:
                    createDialog();
                    break;
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
                }
            });
        }

        public void createDialog() {
            ChangeAccountDialog accountDialog = new ChangeAccountDialog();
            accountDialog.show(getFragmentManager(), "account");
        }

        @Override
        public void gotShowStatus(final Status status) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // set media entity
                    ExtendedMediaEntity[] mediaEntities = status.getExtendedMediaEntities();
                    URLEntity[] urlEntities = status.getURLEntities();

                    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                    boolean show = sharedPreferences.getBoolean("disp_tweet", false);
                    // if number of media entity is one, show fragment directly
                    if (!show && url.contains("/photo") && mediaEntities.length == 1) {
                        ExtendedMediaEntity mediaEntity = mediaEntities[0];
                        Bundle bundle = new Bundle();
                        String type = mediaEntity.getType();

                        try {
                            FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();

                            if (("animated_gif").equals(type) || ("video").equals(type)) {
                                String file_url = getBiggestMp4Url(mediaEntity.getVideoVariants());
                                bundle.putString("url", file_url);
                                bundle.putBoolean("single_frag", true);
                                VideoShowFragment showFragment = new VideoShowFragment();
                                showFragment.setArguments(bundle);
                                fragmentTransaction.replace(R.id.show_frag_replace, showFragment);
                            } else {
                                bundle.putString("url", mediaEntity.getMediaURL());
                                ShowFragment showFragment = new ShowFragment();
                                showFragment.setArguments(bundle);
                                fragmentTransaction.replace(R.id.show_frag_replace, showFragment);
                            }

                            fragmentTransaction.commit();

                        } catch (IllegalStateException e) {
                            Log.e("error", e.toString());
                        }

                    } else {
                        // change background color
                        FrameLayout frameLayout = (FrameLayout) findViewById(R.id.root_layout);
                        frameLayout.setBackgroundResource(R.color.background);
                        ScrollView scrollView = (ScrollView) findViewById(R.id.twitterScrollView);
                        // transparent to f5
                        scrollView.setBackgroundColor(getResources().getColor(R.color.twitter_back));
                        // put status on text
                        TextView textView = (TextView) findViewById(R.id.twTxt);
                        TextView snView = (TextView) findViewById(R.id.twSN);
                        TextView dayView = (TextView) findViewById(R.id.twDay);
                        TextView favView = (TextView) findViewById(R.id.favCount);
                        TextView rtView = (TextView) findViewById(R.id.rtCount);
                        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

                        //retweet check
                        final Status finStatus;

                        if (status.isRetweet()) {
                            finStatus = status.getRetweetedStatus();
                        } else {
                            finStatus = status;
                        }

                        textView.setText(finStatus.getText());

                        final String screen = finStatus.getUser().getScreenName();
                        snView.setText(finStatus.getUser().getName() + " @" + screen);

                        if (finStatus.getUser().isProtected()) {
                            // add key icon
                            float dp = getResources().getDisplayMetrics().density;
                            // set size
                            int iconSize = (int) (17 * dp);
                            // resize app icon (bitmap_factory makes low-quality images)
                            Drawable protect = getResources().getDrawable(R.drawable.lock);
                            protect.setBounds(0, 0, iconSize, iconSize);
                            // set app-icon and bounds
                            snView.setCompoundDrawables(protect, null, null, null);
                        }

                        String statusDate = dateFormat.format(finStatus.getCreatedAt());
                        dayView.setText(statusDate);

                        // fav and rt
                        favView.setText("fav: " + String.valueOf(finStatus.getFavoriteCount()));
                        rtView.setText("RT: " + String.valueOf(finStatus.getRetweetCount()));

                        // set icon
                        NetworkImageView networkImageView = (NetworkImageView) findViewById(R.id.twImageView);
                        networkImageView.setImageUrl(finStatus.getUser().getBiggerProfileImageURL(), imageLoader);

                        // set background
                        networkImageView.setBackgroundResource(R.drawable.twitter_image_design);

                        //show user when tapped
                        networkImageView.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://twitter.com/" + screen));
                                startActivity(intent);
                            }
                        });

                        if (urlEntities.length > 0) {

                            LinearLayout urlLayout = (LinearLayout) findViewById(R.id.url_base);
                            urlLayout.setVisibility(View.VISIBLE);

                            final PhotoViewController controller = new PhotoViewController();
                            LinearLayout baseLayout = (LinearLayout) findViewById(R.id.url_photos);
                            controller.setBaseLayout(baseLayout);

                            for (URLEntity urlEntity : urlEntities) {
                                String url = urlEntity.getExpandedURL();
                                addUrl(url);

                                PLVUrlService service = new PLVUrlService(getApplicationContext());
                                service.setPLVUrlListener(getPLVUrlListener(controller));

                                service.requestGetPLVUrl(url);
                            }
                        }

                        if (mediaEntities.length > 0) {

                            LinearLayout urlLayout = (LinearLayout) findViewById(R.id.photo_base);
                            urlLayout.setVisibility(View.VISIBLE);

                            final PhotoViewController controller = new PhotoViewController();
                            LinearLayout baseLayout = (LinearLayout) findViewById(R.id.photos);
                            controller.setBaseLayout(baseLayout);

                            for (ExtendedMediaEntity mediaEntity : mediaEntities) {

                                String url = mediaEntity.getMediaURLHttps();

                                if (("animated_gif").equals(mediaEntity.getType()) || ("video").equals(mediaEntity.getType())) {
                                    String file_url = getBiggestMp4Url(mediaEntity.getVideoVariants());
                                    controller.setVideoUrl(controller.addImageView(), mediaEntity.getMediaURLHttps(), file_url);
                                } else {

                                    PLVUrlService service = new PLVUrlService(getApplicationContext());
                                    service.setPLVUrlListener(getPLVUrlListener(controller));

                                    service.requestGetPLVUrl(url);
                                }
                            }
                        }

                    }
                }

                private PLVUrlService.PLVUrlListener getPLVUrlListener(final PhotoViewController controller){
                    return new PLVUrlService.PLVUrlListener() {
                        int position;

                        @Override
                        public void onGetPLVUrlFinished(PLVUrl plvUrl) {
                            controller.setImageUrl(position, plvUrl);
                        }

                        @Override
                        public void onGetPLVUrlFailed(String text) {

                        }

                        @Override
                        public void onURLAccepted() {
                            position = controller.addImageView();
                        }
                    };
                }

                private void addUrl(final String url) {
                    LinearLayout urlLayout = (LinearLayout) findViewById(R.id.url_linear);
                    // prev is last linear_layout
                    TextView textView = (TextView) getLayoutInflater().inflate(R.layout.twitter_url, null);
                    textView.setText(url);
                    TextPaint textPaint = textView.getPaint();
                    textPaint.setUnderlineText(true);
                    textView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                            startActivity(intent);
                        }
                    });
                    urlLayout.addView(textView);
                }

                private String getBiggestMp4Url(ExtendedMediaEntity.Variant[] variants) {
                    int bitrate = 0;
                    String url = null;
                    for (ExtendedMediaEntity.Variant variant : variants) {
                        if (("video/mp4").equals(variant.getContentType()) && bitrate <= variant.getBitrate()) {
                            url = variant.getUrl();
                            bitrate = variant.getBitrate();
                        }
                    }
                    return url;
                }
            });
        }

    };

    @Override
    protected void onStop() {
        super.onStop();

        // flash cache
        HttpResponseCache cache = HttpResponseCache.getInstalled();
        if (cache != null) {
            cache.flush();
        }
    }

    private class PhotoViewController {
        private List<NetworkImageView> imageViewList = new ArrayList<>();
        private LinearLayout baseLayout;

        public void setBaseLayout(LinearLayout baseLayout) {
            this.baseLayout = baseLayout;
        }

        public int addImageView() {
            // prev is last linear_layout
            int size = imageViewList.size();

            FrameLayout frameLayout;
            if (size % 2 == 0) {
                // make new linear_layout and put below prev
                LinearLayout new_layout = (LinearLayout) getLayoutInflater().inflate(R.layout.twitter_photos, baseLayout, false);
                baseLayout.addView(new_layout);
                frameLayout = (FrameLayout) new_layout.getChildAt(0);
            } else {
                LinearLayout prevLayout = (LinearLayout) baseLayout.getChildAt(baseLayout.getChildCount() - 1);
                // put new photo below prev photo (not new linear_layout)
                frameLayout = (FrameLayout) prevLayout.getChildAt(1);
            }
            imageViewList.add((NetworkImageView) frameLayout.getChildAt(0));

            return size;
        }

        public void setImageUrl(int position, final PLVUrl plvUrl){
            NetworkImageView imageView = imageViewList.get(position);

            imageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // go to show fragment
                    Bundle bundle = new Bundle();
                    bundle.putParcelable("plvurl", plvUrl);
                    FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();

                    ShowFragment showFragment = new ShowFragment();
                    showFragment.setArguments(bundle);
                    fragmentTransaction.replace(R.id.show_frag_replace, showFragment);

                    // back to this screen when back pressed
                    fragmentTransaction.addToBackStack(null);
                    fragmentTransaction.commit();
                }
            });

            imageView.setImageUrl(plvUrl.getThumbUrl(), imageLoader);
        }

        public void setVideoUrl(int position, String thumbUrl, final String fileUrl){
            NetworkImageView imageView = imageViewList.get(position);
            FrameLayout frameLayout = (FrameLayout) imageView.getParent();
            ImageView video_icon = (ImageView) frameLayout.getChildAt(1);
            video_icon.setVisibility(View.VISIBLE);

            imageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // go to show fragment
                    Bundle bundle = new Bundle();
                    bundle.putString("url", fileUrl);
                    FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();

                    VideoShowFragment showFragment = new VideoShowFragment();
                    showFragment.setArguments(bundle);
                    fragmentTransaction.replace(R.id.show_frag_replace, showFragment);

                    // back to this screen when back pressed
                    fragmentTransaction.addToBackStack(null);
                    fragmentTransaction.commit();
                }
            });

            imageView.setImageUrl(thumbUrl, imageLoader);
        }
    }
}
