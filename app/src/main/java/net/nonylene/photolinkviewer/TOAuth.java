package net.nonylene.photolinkviewer;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import net.nonylene.photolinkviewer.dialog.DeleteDialogFragment;
import net.nonylene.photolinkviewer.tool.Encryption;
import net.nonylene.photolinkviewer.tool.MyAsyncTwitter;
import net.nonylene.photolinkviewer.tool.MyCursorAdapter;
import net.nonylene.photolinkviewer.tool.MySQLiteOpenHelper;

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
import twitter4j.User;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;

public class TOAuth extends AppCompatActivity implements DeleteDialogFragment.DeleteDialogCallBack{

    private AsyncTwitter twitter;
    private RequestToken requestToken;
    private MyCursorAdapter myCursorAdapter;
    private SQLiteDatabase database;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.toauth);
        // oauth button
        Button oAuthButton = (Button) findViewById(R.id.oAuthButton);
        oAuthButton.setOnClickListener(new Button1ClickListener());
        // update button
        Button update = (Button) findViewById(R.id.update_button);
        update.setOnClickListener(new UpdateButtonClickListener());
        MySQLiteOpenHelper sqLiteOpenHelper = new MySQLiteOpenHelper(getApplicationContext());
        // create table accounts
        database = sqLiteOpenHelper.getWritableDatabase();
        database.beginTransaction();
        database.execSQL("create table if not exists accounts (userName unique, userId integer unique, token, token_secret, key, icon)");
        database.setTransactionSuccessful();
        database.endTransaction();
        setListView();
    }

    private void setListView() {
        final ListView listView = (ListView) findViewById(R.id.accounts_list);
        // radio button
        listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        final SharedPreferences sharedPreferences = getSharedPreferences("preference", Context.MODE_PRIVATE);
        // choose account
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                changeAccount((int) id);
            }
        });
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                Cursor cursor = (Cursor) listView.getItemAtPosition(position);
                String screen_name = cursor.getString(cursor.getColumnIndex("userName"));
                Bundle bundle = new Bundle();
                bundle.putString("screen_name", screen_name);
                DeleteDialogFragment deleteDialogFragment = new DeleteDialogFragment();
                deleteDialogFragment.setDeleteDialogCallBack(TOAuth.this);
                deleteDialogFragment.setArguments(bundle);
                deleteDialogFragment.show(getFragmentManager(), "delete");
                return true;
            }

        });
        // not to lock
        try {
            Cursor cursor = database.rawQuery("select rowid _id, * from accounts", null);
            myCursorAdapter = new MyCursorAdapter(getApplicationContext(), cursor, true);
            listView.setAdapter(myCursorAdapter);
            // check current radio button
            for (int i = 0; i < listView.getCount(); i++) {
                Cursor c = (Cursor) listView.getItemAtPosition(i);
                if (c.getInt(c.getColumnIndex("_id")) == sharedPreferences.getInt("account", 0)) {
                    listView.setItemChecked(i, true);
                }
            }
        } catch (SQLiteException e) {
            Log.e("SQLite", e.toString());
        }
    }

    @Override
    public void onDeleteConfirmed(String userName) {
        String toastText = null;
        try {
            toastText = getString(R.string.delete_account_toast);
            database.beginTransaction();
            // quotation is required.
            database.delete("accounts", "userName = ?", new String[]{userName});
            // commit
            database.setTransactionSuccessful();
            final Cursor new_cursor = database.rawQuery("select rowid _id, * from accounts", null);
            database.endTransaction();
            // renew listView
            // i want to use content provider and cursor loader in future.
            myCursorAdapter.swapCursor(new_cursor);

        } catch (SQLiteException e) {
            toastText = getString(R.string.delete_failed_toast);
            e.printStackTrace();

        } finally {
            Toast.makeText(this, toastText, Toast.LENGTH_LONG).show();
        }
    }


    class Button1ClickListener implements View.OnClickListener {
        public void onClick(View v) {
            try {
                twitter = new AsyncTwitterFactory().getInstance();
                String apikey = BuildConfig.TWITTER_KEY;
                String apisecret = BuildConfig.TWITTER_SECRET;
                twitter.setOAuthConsumer(apikey, apisecret);
                twitter.addListener(twitterListener);
                twitter.getOAuthRequestTokenAsync("plvtwitter://callback");
            } catch (Exception e) {
                Log.e("twitter", e.toString());
            }
        }
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
                String apikey = BuildConfig.TWITTER_KEY;
                String apisecret = BuildConfig.TWITTER_SECRET;
                // get oauthed user_name and user_id and icon_url
                Twitter twitterNotAsync = new TwitterFactory().getInstance();
                twitterNotAsync.setOAuthConsumer(apikey, apisecret);
                twitterNotAsync.setOAuthAccessToken(token);
                Long myId = twitterNotAsync.getId();
                User user = twitterNotAsync.showUser(myId);
                final String screenName = user.getScreenName();
                String icon = user.getBiggerProfileImageURL();
                // encrypt twitter tokens by key
                Key key = Encryption.generate();
                String twitter_token = Encryption.encrypt(token.getToken().getBytes("UTF-8"), key);
                String twitter_tsecret = Encryption.encrypt(token.getTokenSecret().getBytes("UTF-8"), key);
                String keys = Base64.encodeToString(key.getEncoded(), Base64.DEFAULT);
                // save encrypted keys to database
                ContentValues values = new ContentValues();
                values.put("userName", screenName);
                values.put("userId", myId);
                values.put("token", twitter_token);
                values.put("token_secret", twitter_tsecret);
                values.put("key", keys);
                values.put("icon", icon);
                // open database
                database.beginTransaction();
                // if exists...
                database.delete("accounts", "userId = ?", new String[]{myId.toString()});
                // insert account information
                database.insert("accounts", null, values);
                // commit
                database.setTransactionSuccessful();
                database.endTransaction();
                Cursor cursorNew = database.rawQuery("select rowid _id, userId from accounts where userId = ?", new String[]{String.valueOf(myId)});
                cursorNew.moveToFirst();
                int account = cursorNew.getInt(cursorNew.getColumnIndex("_id"));
                // set oauth_completed frag
                SharedPreferences preferences = getSharedPreferences("preference", MODE_PRIVATE);
                preferences.edit().putBoolean("authorized", true).apply();
                preferences.edit().putString("screen_name", screenName).apply();
                preferences.edit().putInt("account", account).apply();
                //putting cue to UI Thread
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(TOAuth.this, getString(R.string.toauth_succeeded_token) + " " + screenName, Toast.LENGTH_LONG).show();
                        setListView();
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

    @Override
    protected void onNewIntent(Intent intent) {
        Uri uri = intent.getData();
        if (uri != null) {
            String oauth = uri.getQueryParameter("oauth_verifier");
            if (oauth != null) {
                twitter.getOAuthAccessTokenAsync(requestToken, oauth);
            } else {
                Toast.makeText(TOAuth.this, getString(R.string.toauth_failed_token), Toast.LENGTH_LONG).show();
            }
        }
    }

    class UpdateButtonClickListener implements View.OnClickListener {
        public void onClick(View v) {
            SharedPreferences sharedPreferences = getSharedPreferences("preference", MODE_PRIVATE);
            if (sharedPreferences.getBoolean("authorized", false)) {
                try {
                    AsyncTwitter twitter = MyAsyncTwitter.getAsyncTwitterFromDB(database, getApplicationContext());
                    Cursor cursor = database.rawQuery("select rowid _id, userId from accounts", null);
                    twitter.addListener(new TwitterAdapter() {
                        @Override
                        public void gotUserDetail(final User user) {
                            try {
                                ContentValues values = new ContentValues();
                                values.put("userName", user.getScreenName());
                                values.put("icon", user.getBiggerProfileImageURL());
                                // open database
                                database.beginTransaction();
                                database.update("accounts", values, "userId = ?", new String[]{String.valueOf(user.getId())});
                                database.setTransactionSuccessful();
                                database.endTransaction();
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        // renew ui
                                        Cursor cursor = database.rawQuery("select rowid _id, * from accounts", null);
                                        myCursorAdapter.swapCursor(cursor);
                                    }
                                });
                            } catch (SQLiteException e) {
                                Log.e("SQL", e.toString());
                            }
                        }
                    });
                    // move cursor focus
                    cursor.moveToFirst();
                    twitter.showUser(cursor.getLong(cursor.getColumnIndex("userId")));
                    while (cursor.moveToNext()) {
                        twitter.showUser(cursor.getLong(cursor.getColumnIndex("userId")));
                    }
                } catch (CursorIndexOutOfBoundsException e) {
                    Log.e("cursor", e.toString());
                    Toast.makeText(getApplicationContext(), getString(R.string.twitter_async_select), Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    private void changeAccount(int rowid) {
        // save rowid and screen name to preference
        SharedPreferences sharedPreferences = getSharedPreferences("preference", MODE_PRIVATE);
        sharedPreferences.edit().putInt("account", rowid).apply();
        Cursor cursor = database.rawQuery("select userName from accounts where rowid = ?", new String[]{String.valueOf(rowid)});
        cursor.moveToFirst();
        String screen_name = cursor.getString(cursor.getColumnIndex("userName"));
        sharedPreferences.edit().putString("screen_name", screen_name).apply();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        database.close();
    }
}