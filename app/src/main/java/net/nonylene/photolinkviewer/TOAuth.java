package net.nonylene.photolinkviewer;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.security.Key;

import twitter4j.AsyncTwitter;
import twitter4j.AsyncTwitterFactory;
import twitter4j.TwitterAdapter;
import twitter4j.TwitterException;
import twitter4j.TwitterListener;
import twitter4j.TwitterMethod;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;

public class TOAuth extends Activity {

    private AsyncTwitter twitter;
    private RequestToken requestToken;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.toauth);
        Button button1 = (Button) findViewById(R.id.oAuthButton);
        button1.setOnClickListener(new Button1ClickListener());
    }

    private TwitterListener twitterListener = new TwitterAdapter() {

        @Override
        public void onException (TwitterException exception, TwitterMethod method){
            Log.e("twitter",exception.toString());
        }

        @Override
        public void gotOAuthRequestToken(RequestToken token) {
            requestToken = token;
            Uri uri = Uri.parse(requestToken.getAuthorizationURL());
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            startActivity(intent);
        }

        @Override
        public void gotOAuthAccessToken(AccessToken token) {
            try {
                twitter.setOAuthAccessToken(token);
                SharedPreferences preferences = getSharedPreferences("preference", MODE_PRIVATE);
                Key key = Encryption.generate();
                String twitter_token = Encryption.encrypt(token.getToken().getBytes("UTF-8"), key);
                String twitter_tsecret = Encryption.encrypt(token.getTokenSecret().getBytes("UTF-8"), key);
                String keys = Base64.encodeToString(key.getEncoded(), Base64.DEFAULT);
                preferences.edit().putString("key", keys).apply();
                preferences.edit().putString("ttoken", twitter_token).apply();
                preferences.edit().putString("ttokensecret", twitter_tsecret).apply();
                preferences.edit().putBoolean("authorized", true).apply();
                //putting cue to UI Thread
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(TOAuth.this, getString(R.string.toauth_succeeded_token), Toast.LENGTH_LONG).show();
                        finish();
                    }
                });
            } catch (Exception e) {
                Log.e("gettoken", e.toString());
                //putting cue to UI Thread
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(TOAuth.this, getString(R.string.toauth_failed_token), Toast.LENGTH_LONG).show();
                    }
                });
            }
        }
    };


    class Button1ClickListener implements View.OnClickListener {
        public void onClick(View v) {
            try {
                twitter = new AsyncTwitterFactory().getInstance();
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
        if (oauth != null) {
            twitter.getOAuthAccessTokenAsync(requestToken, oauth);
        }else{
            Toast.makeText(TOAuth.this, getString(R.string.toauth_failed_token), Toast.LENGTH_LONG).show();
        }
    }
}