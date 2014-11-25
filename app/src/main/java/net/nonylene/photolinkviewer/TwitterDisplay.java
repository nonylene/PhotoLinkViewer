package net.nonylene.photolinkviewer;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.spec.SecretKeySpec;

import twitter4j.AsyncTwitter;
import twitter4j.AsyncTwitterFactory;
import twitter4j.MediaEntity;
import twitter4j.Status;
import twitter4j.TwitterAdapter;
import twitter4j.TwitterException;
import twitter4j.TwitterListener;
import twitter4j.TwitterMethod;
import twitter4j.auth.AccessToken;


public class TwitterDisplay extends Activity {
    Activity activity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_twitter_display);
        activity = this;
        // get intent and purse url
        SharedPreferences sharedPreferences = getSharedPreferences("preference", Context.MODE_PRIVATE);
        if (Intent.ACTION_VIEW.equals(getIntent().getAction())) {
            Bundle bundle = new Bundle();
            Uri uri = getIntent().getData();
            String url = uri.toString();
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
                // get twitter CK/CS/AT/AS
                String apikey = (String) getText(R.string.twitter_key);
                String apisecret = (String) getText(R.string.twitter_secret);
                String tokenkey = sharedPreferences.getString("key", null);
                if (sharedPreferences.getBoolean("authorized",false)) {
                    // oAuthed
                    byte[] keyboo = Base64.decode(tokenkey, Base64.DEFAULT);
                    SecretKeySpec key = new SecretKeySpec(keyboo, 0, keyboo.length, "AES");
                    byte[] token = Base64.decode(sharedPreferences.getString("ttoken", null), Base64.DEFAULT);
                    byte[] token_secret = Base64.decode(sharedPreferences.getString("ttokensecret", null), Base64.DEFAULT);
                    AccessToken accessToken = new AccessToken(Encryption.decrypt(token, key), Encryption.decrypt(token_secret, key));
                    // get twitter async
                    AsyncTwitter twitter = new AsyncTwitterFactory().getInstance();
                    twitter.setOAuthConsumer(apikey, apisecret);
                    twitter.setOAuthAccessToken(accessToken);
                    twitter.addListener(twitterListener);
                    twitter.showStatus(Long.parseLong(id));
                } else {
                    Toast.makeText(getApplicationContext(), getString(R.string.twitter_display_oauth), Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(this, TOAuth.class);
                    startActivity(intent);
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

                    // if number of media entity is one, show fragment directly
                    if (mediaEntities.length == 1) {
                        Bundle bundle = new Bundle();
                        bundle.putString("url", mediaEntities[0].getMediaURL());
                        FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
                        ShowFragment showFragment = new ShowFragment();
                        showFragment.setArguments(bundle);
                        fragmentTransaction.replace(R.id.show_frag_replace, showFragment).commit();
                    } else {
                        ScrollView scrollView = (ScrollView) findViewById(R.id.twitterScrollView);
                        // transparent to f5
                        scrollView.setBackgroundColor(Color.parseColor("#F5F5F5"));
                        // put status on text
                        TextView textView = (TextView) findViewById(R.id.twTxt);
                        TextView snView = (TextView) findViewById(R.id.twSN);
                        TextView dayView = (TextView) findViewById(R.id.twDay);
                        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                        URL iconUrl;

                        try {
                            //retweet check
                            if (status.isRetweet()) {
                                Status retweetedStatus = status.getRetweetedStatus();
                                textView.setText(retweetedStatus.getText());
                                snView.setText(retweetedStatus.getUser().getScreenName());
                                String statusDate = dateFormat.format(retweetedStatus.getCreatedAt());
                                dayView.setText(statusDate);
                                iconUrl = new URL(retweetedStatus.getUser().getBiggerProfileImageURL());
                            } else {
                                textView.setText(status.getText());
                                snView.setText(status.getUser().getScreenName());
                                String statusDate = dateFormat.format(status.getCreatedAt());
                                dayView.setText(statusDate);
                                iconUrl = new URL(status.getUser().getBiggerProfileImageURL());
                            }
                            // get icon
                            PLVImageView plvImageView = (PLVImageView) findViewById(R.id.twImageView);
                            // get dp
                            int size = plvImageView.getWidth();
                            plvImageView.setUrl(iconUrl, size, size);
                        } catch (MalformedURLException e) {
                            Log.e("URLError", e.toString());
                        }

                        for (MediaEntity mediaEntity : mediaEntities) {
                            final String url = mediaEntity.getMediaURL();
                            addView(url);
                        }
                    }
                }

                public void addView(final String url) {
                    try {
                        LinearLayout baseLayout = (LinearLayout) findViewById(R.id.baseLayout);
                        // prev is last linear_layout
                        LinearLayout prevLayout = (LinearLayout) baseLayout.getChildAt(baseLayout.getChildCount() - 1);
                        LinearLayout currentLayout;
                        if (prevLayout.getChildCount() > 1) {
                            // make new linear_layout and put below prev
                            currentLayout = new LinearLayout(TwitterDisplay.this);
                            currentLayout.setLayoutParams(new AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                            currentLayout.setOrientation(LinearLayout.HORIZONTAL);
                            baseLayout.addView(currentLayout);
                        } else {
                            // put new photo below prev photo (not new linear_layout)
                            currentLayout = prevLayout;
                        }
                        int width = baseLayout.getWidth();
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
                                fragmentTransaction.replace(R.id.show_frag_replace, showFragment).commit();
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
