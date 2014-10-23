package net.nonylene.photolinkviewer;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.security.Key;

import twitter4j.AsyncTwitter;
import twitter4j.AsyncTwitterFactory;
import twitter4j.TwitterAdapter;
import twitter4j.TwitterListener;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;

public class TOAuth extends Activity {

    private AsyncTwitter twitter;
    private RequestToken requestToken;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.toauth);
        twitter = new AsyncTwitterFactory().getInstance();
        Button button1 = (Button) findViewById(R.id.oAuthButton);
        button1.setOnClickListener(new ButtonClickListener());
    }

    private TwitterListener twitterListener = new TwitterAdapter() {

        @Override
        public void gotOAuthRequestToken(RequestToken token) {
            requestToken = token;
            Uri uri = Uri.parse(requestToken.getAuthorizationURL());
            Log.v("token", requestToken.getAuthorizationURL());
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            startActivity(intent);
        }

        @Override
        public void gotOAuthAccessToken(AccessToken token) {
            SharedPreferences preferences = getSharedPreferences("preference", MODE_PRIVATE);
            Key key = Encryption.generate();
            String twitter_token = Encryption.encrypt(token.getToken().getBytes(), key);
            String twitter_tsecret = Encryption.encrypt(token.getTokenSecret().getBytes(), key);
            preferences.edit().putString("keys", key.toString()).apply();
            preferences.edit().putString("ttoken", twitter_token).apply();
            preferences.edit().putString("ttokensecret", twitter_tsecret).apply();
        }
    };

    class ButtonClickListener implements View.OnClickListener {
        public void onClick(View v) {
            try {
                String apikey = (String) getText(R.string.twitter_key);
                String apisecret = (String) getText(R.string.twitter_secret);
                twitter.setOAuthConsumer(apikey, apisecret);
                twitter.addListener(twitterListener);
                twitter.getOAuthRequestTokenAsync("plviewer://callback");
            } catch (Exception e) {
                Log.e("twitter", e.toString());
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        Uri uri = intent.getData();
        String oauth = uri.getQueryParameter("oauth_verifier");
        twitter.getOAuthAccessTokenAsync(requestToken, oauth);
    }
}