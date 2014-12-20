package net.nonylene.photolinkviewer;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteException;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
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
import net.nonylene.photolinkviewer.tool.MyAsyncTwitter;
import net.nonylene.photolinkviewer.tool.PLVImageView;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
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
    private AsyncTwitter twitter;
    private String url;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_twitter_display);
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
                        twitter = MyAsyncTwitter.getAsyncTwitter(getApplicationContext());
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

    private TwitterListener twitterListener = new TwitterAdapter() {

        @Override
        public void onException(TwitterException e, TwitterMethod twitterMethod) {
            Log.e("twitterException", e.toString());
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), getString(R.string.twitter_error_toast), Toast.LENGTH_LONG).show();
                }
            });
        }

        @Override
        public void gotShowStatus(final Status status) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // set media entity
                    MediaEntity[] mediaEntities = status.getExtendedMediaEntities();
                    URLEntity[] urlEntities = status.getURLEntities();

                    // if number of media entity is one, show fragment directly
                    if (url.contains("/photo") && mediaEntities.length == 1) {
                        Bundle bundle = new Bundle();
                        bundle.putString("url", mediaEntities[0].getMediaURL());
                        FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
                        ShowFragment showFragment = new ShowFragment();
                        showFragment.setArguments(bundle);
                        fragmentTransaction.replace(R.id.show_frag_replace, showFragment);
                        fragmentTransaction.commit();
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
                                String url = mediaEntity.getMediaURL();
                                addView(url);
                            }
                        }
                    }
                }

                public void addUrl(final String url) {
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

                public void addPhotoIcon() {
                    ImageView imageView = (ImageView) findViewById(R.id.photo_icon);
                    Drawable photoIcon = getResources().getDrawable(R.drawable.photo_icon);
                    imageView.setImageDrawable(photoIcon);
                }

                public void addView(final String url) {
                    try {
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
                                bundle.putString("url", url);
                                FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
                                ShowFragment showFragment = new ShowFragment();
                                showFragment.setArguments(bundle);
                                fragmentTransaction.replace(R.id.show_frag_replace, showFragment);
                                // back to this screen when back pressed
                                fragmentTransaction.addToBackStack(null);
                                fragmentTransaction.commit();
                            }
                        });
                        imageView.setUrl(new URL(url), size, size);
                        currentLayout.addView(imageView);
                    } catch (MalformedURLException e) {
                        Log.e("URLError", e.toString());
                    }
                }
            });
        }

    };

}
