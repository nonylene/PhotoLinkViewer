package net.nonylene.photolinkviewer;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteException;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.net.http.HttpResponseCache;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextPaint;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import net.nonylene.photolinkviewer.fragment.OptionFragment;
import net.nonylene.photolinkviewer.fragment.ShowFragment;
import net.nonylene.photolinkviewer.fragment.TwitterOptionFragment;
import net.nonylene.photolinkviewer.fragment.VideoShowFragment;
import net.nonylene.photolinkviewer.tool.AccountsList;
import net.nonylene.photolinkviewer.tool.MyAsyncTwitter;
import net.nonylene.photolinkviewer.tool.PLVImageView;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import twitter4j.AsyncTwitter;
import twitter4j.MediaEntity;
import twitter4j.Status;
import twitter4j.TwitterAdapter;
import twitter4j.TwitterException;
import twitter4j.TwitterListener;
import twitter4j.TwitterMethod;
import twitter4j.URLEntity;


public class TwitterDisplay extends Activity {
    private String url;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_twitter_display);

        //enable cache
        try {
            File httpCacheDir = new File(getApplicationContext().getCacheDir(), "http");
            long httpCacheSize = 10 * 1024 * 1024; // 15 MB
            HttpResponseCache.install(httpCacheDir, httpCacheSize);
        } catch (IOException e) {
            Log.d("cache", "HTTP response cache installation failed");
        }

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
                Log.v("twitter", url);
                Pattern pattern = Pattern.compile("^https?://twitter\\.com/\\w+/status[es]*/(\\d+)");
                Matcher matcher = pattern.matcher(url);
                if (matcher.find()) {
                    Log.v("match", "success");
                }
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
            final String message;
            switch (e.getErrorCode()) {
                case 34:
                    message = "404 not found";
                    break;
                case 130:
                    message = "Over capacity";
                    break;
                case 179:
                    message = getString(R.string.twitter_error_authorize);
                    createDialog();
                    break;
                case 88:
                    message = "Rate limit exceeded";
                    createDialog();
                    break;
                default:
                    message = getString(R.string.twitter_error_toast);
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
                    MediaEntity[] mediaEntities = status.getExtendedMediaEntities();
                    URLEntity[] urlEntities = status.getURLEntities();

                    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                    boolean show = sharedPreferences.getBoolean("disp_tweet", false);
                    // if number of media entity is one, show fragment directly
                    if (!show && url.contains("/photo") && mediaEntities.length == 1) {
                        Bundle bundle = new Bundle();
                        bundle.putString("url", mediaEntities[0].getMediaURL());
                        try {
                            FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
                            ShowFragment showFragment = new ShowFragment();
                            showFragment.setArguments(bundle);
                            fragmentTransaction.replace(R.id.show_frag_replace, showFragment);
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
                        scrollView.setBackgroundColor(Color.parseColor("#F5F5F5"));
                        // put status on text
                        TextView textView = (TextView) findViewById(R.id.twTxt);
                        TextView snView = (TextView) findViewById(R.id.twSN);
                        TextView dayView = (TextView) findViewById(R.id.twDay);
                        TextView favView = (TextView) findViewById(R.id.favCount);
                        TextView rtView = (TextView) findViewById(R.id.rtCount);
                        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                        URL iconUrl;

                        try {
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
                            iconUrl = new URL(finStatus.getUser().getBiggerProfileImageURL());
                            // fav and rt
                            favView.setText("fav: " + String.valueOf(finStatus.getFavoriteCount()));
                            rtView.setText("RT: " + String.valueOf(finStatus.getRetweetCount()));
                            // get icon
                            PLVImageView plvImageView = (PLVImageView) findViewById(R.id.twImageView);
                            // get icon size
                            int size = plvImageView.getWidth();
                            int padding = plvImageView.getPaddingTop();
                            // set icon
                            plvImageView.setUrl(iconUrl, size - padding * 2, size - padding * 2);
                            plvImageView.setBackgroundResource(R.drawable.twitter_image_design);
                            //show user when tapped
                            plvImageView.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://twitter.com/" + screen));
                                    startActivity(intent);
                                }
                            });
                        } catch (MalformedURLException e) {
                            Log.e("URLError", e.toString());
                        }

                        for (URLEntity urlEntity : urlEntities) {
                            Log.d("url", urlEntity.toString());
                            String url = urlEntity.getExpandedURL();
                            addUrl(url);
                        }

                        if (mediaEntities.length > 0) {

                            addPhotoIcon();

                            for (MediaEntity mediaEntity : mediaEntities) {
                                addView(mediaEntity);
                            }
                        }
                    }
                }

                private void addUrl(final String url) {
                    LinearLayout urlLayout = (LinearLayout) findViewById(R.id.url_linear);
                    // prev is last linear_layout
                    TextView textView = (TextView) LayoutInflater.from(getApplicationContext()).inflate(R.layout.twitter_url, null);
                    textView.setText(url);
                    TextPaint textPaint = textView.getPaint();
                    textPaint.setUnderlineText(true);
                    Drawable linkIcon = getResources().getDrawable(R.drawable.link_icon);
                    // get dp
                    float dp = getResources().getDisplayMetrics().density;
                    // set size
                    int iconSize = (int) (20 * dp);
                    int paddingSize = (int) (5 * dp);
                    linkIcon.setBounds(0, 0, iconSize, iconSize);
                    // set app-icon and bounds
                    textView.setCompoundDrawables(linkIcon, null, null, null);
                    textView.setCompoundDrawablePadding(paddingSize);
                    textView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                            startActivity(intent);
                        }
                    });
                    urlLayout.addView(textView);
                }

                private String getBiggestMp4Url(MediaEntity.Variant[] variants) {
                    int bitrate = 0;
                    String url = null;
                    for (MediaEntity.Variant variant : variants) {
                        if (("video/mp4").equals(variant.getContentType()) && bitrate <= variant.getBitrate()) {
                            url = variant.getUrl();
                            bitrate = variant.getBitrate();
                        }
                    }
                    return url;
                }

                private void addPhotoIcon() {
                    ImageView imageView = (ImageView) findViewById(R.id.photo_icon);
                    Drawable photoIcon = getResources().getDrawable(R.drawable.photo_icon);
                    imageView.setImageDrawable(photoIcon);
                }

                private void addView(MediaEntity mediaEntity) {
                    try {
                        final String url = mediaEntity.getMediaURL();
                        final String type = mediaEntity.getType();
                        final String file_url;
                        if (("animated_gif").equals(type) || ("video").equals(type)) {
                            file_url = getBiggestMp4Url(mediaEntity.getVideoVariants());
                        } else {
                            file_url = url;
                        }
                        LinearLayout baseLayout = (LinearLayout) findViewById(R.id.photos);
                        // prev is last linear_layout
                        LinearLayout prevLayout = (LinearLayout) baseLayout.getChildAt(baseLayout.getChildCount() - 1);
                        LinearLayout currentLayout;
                        if (prevLayout == null || prevLayout.getChildCount() > 1) {
                            // make new linear_layout and put below prev
                            currentLayout = new LinearLayout(TwitterDisplay.this);
                            currentLayout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                            currentLayout.setOrientation(LinearLayout.HORIZONTAL);
                            baseLayout.addView(currentLayout);
                        } else {
                            // put new photo below prev photo (not new linear_layout)
                            currentLayout = prevLayout;
                        }
                        int width = baseLayout.getWidth();
                        Log.d("hoge", String.valueOf(width));
                        // get dp
                        float dp = getResources().getDisplayMetrics().density;
                        // set padding and margin
                        int padding = (int) (2 * dp);
                        int margin = (int) (1 * dp);
                        // photo size
                        int size = width / 2 - padding * 2 - margin * 2;
                        // imgview size
                        int layoutsize = width / 2 - margin * 2;
                        // new imgview
                        PLVImageView imageView = new PLVImageView(TwitterDisplay.this);
                        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(layoutsize, layoutsize);
                        layoutParams.setMargins(margin, margin, margin, margin);
                        imageView.setLayoutParams(layoutParams);
                        imageView.setPadding(padding, padding, padding, padding);
                        imageView.setScaleType(ImageView.ScaleType.MATRIX);
                        // background (tap to gray)
                        imageView.setBackgroundResource(R.drawable.twitter_image_design);
                        imageView.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                // go to show fragment
                                Bundle bundle = new Bundle();
                                bundle.putString("url", file_url);
                                FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
                                if (("animated_gif").equals(type) || ("video").equals(type)) {
                                    VideoShowFragment showFragment = new VideoShowFragment();
                                    showFragment.setArguments(bundle);
                                    fragmentTransaction.replace(R.id.show_frag_replace, showFragment);
                                } else {
                                    ShowFragment showFragment = new ShowFragment();
                                    showFragment.setArguments(bundle);
                                    fragmentTransaction.replace(R.id.show_frag_replace, showFragment);
                                }
                                // back to this screen when back pressed
                                fragmentTransaction.addToBackStack(null);
                                fragmentTransaction.commit();
                            }
                        });
                        imageView.setUrl(new URL(url + ":small"), size, size);
                        currentLayout.addView(imageView);
                    } catch (MalformedURLException e) {
                        Log.e("URLError", e.toString());
                    }
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
}
