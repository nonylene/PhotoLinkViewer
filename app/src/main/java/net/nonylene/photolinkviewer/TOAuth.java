package net.nonylene.photolinkviewer;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;
import java.security.Key;

import twitter4j.AsyncTwitter;
import twitter4j.AsyncTwitterFactory;
import twitter4j.Twitter;
import twitter4j.TwitterAdapter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
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
        public void onException(TwitterException exception, TwitterMethod method) {
            Log.e("twitter", exception.toString());
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
                String apikey = (String) getText(R.string.twitter_key);
                String apisecret = (String) getText(R.string.twitter_secret);
                Twitter twitterNotAsync = TwitterFactory.getSingleton();
                twitterNotAsync.setOAuthConsumer(apikey, apisecret);
                twitterNotAsync.setOAuthAccessToken(token);
                final String screenName = twitterNotAsync.getScreenName();
                Long myId = twitterNotAsync.getId();
                // encrypt twitter tokens by key
                Key key = Encryption.generate();
                String twitter_token = Encryption.encrypt(token.getToken().getBytes("UTF-8"), key);
                String twitter_tsecret = Encryption.encrypt(token.getTokenSecret().getBytes("UTF-8"), key);
                String keys = Base64.encodeToString(key.getEncoded(), Base64.DEFAULT);
                // save encrypted keys
                ContentValues values = new ContentValues();
                values.put("user_name", screenName);
                values.put("user_id", myId);
                values.put("token", twitter_token);
                values.put("token_secret", twitter_tsecret);
                values.put("key", keys);
                // open database
                MySQLiteOpenHelper sqLiteOpenHelper = new MySQLiteOpenHelper(getApplicationContext());
                SQLiteDatabase database = sqLiteOpenHelper.getWritableDatabase();
                database.execSQL("create table if not exists accounts (user_name unique, user_id integer unique, token, token_secret, key)");
                database.beginTransaction();
                database.delete("accounts", "user_id = " + String.valueOf(myId), null);
                database.insert("accounts", null, values);
                database.setTransactionSuccessful();
                database.endTransaction();
                // set oauth_completed frag
                SharedPreferences preferences = getSharedPreferences("preference", MODE_PRIVATE);
                preferences.edit().putBoolean("authorized", true).apply();
                preferences.edit().putInt("account", 1).apply();
                preferences.edit().putString("screen_name", screenName).apply();
                //putting cue to UI Thread
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(TOAuth.this, getString(R.string.toauth_succeeded_token) + " " + screenName, Toast.LENGTH_LONG).show();
                        finish();
                    }
                });
            } catch (UnsupportedEncodingException e) {
                Log.e("gettoken", e.toString());
                //putting cue to UI Thread
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(TOAuth.this, getString(R.string.toauth_failed_encode), Toast.LENGTH_LONG).show();
                    }
                });
                finish();
            } catch (TwitterException e) {
                Log.e("gettoken", e.toString());
                //putting cue to UI Thread
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(TOAuth.this, getString(R.string.toauth_failed_twitter4j), Toast.LENGTH_LONG).show();
                    }
                });
                finish();
            } catch (SQLiteException e) {
                Log.w("SQLite", e.toString());
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
        } else {
            Toast.makeText(TOAuth.this, getString(R.string.toauth_failed_token), Toast.LENGTH_LONG).show();
        }
    }
}