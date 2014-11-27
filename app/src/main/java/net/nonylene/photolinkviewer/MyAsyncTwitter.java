package net.nonylene.photolinkviewer;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Base64;

import javax.crypto.spec.SecretKeySpec;

import twitter4j.AsyncTwitter;
import twitter4j.AsyncTwitterFactory;
import twitter4j.auth.AccessToken;

public class MyAsyncTwitter {
       public static AsyncTwitter getAsyncTwitter(Context context) throws SQLiteException{
           // oAuthed
           SharedPreferences sharedPreferences  = context.getSharedPreferences("preference", Context.MODE_PRIVATE);
           String apikey = (String) context.getText(R.string.twitter_key);
           String apisecret = (String) context.getText(R.string.twitter_secret);
           int account = sharedPreferences.getInt("account", 0);
           // sql
           MySQLiteOpenHelper sqLiteOpenHelper = new MySQLiteOpenHelper(context);
           SQLiteDatabase database = sqLiteOpenHelper.getReadableDatabase();
           Cursor cursor = database.rawQuery("select token, token_secret, key from accounts where rowid = ?", new String[]{String.valueOf(account)});
           // rowid only one row
           cursor.moveToFirst();
           String tokenkey = cursor.getString(2);
           byte[] keyboo = Base64.decode(tokenkey, Base64.DEFAULT);
           SecretKeySpec key = new SecretKeySpec(keyboo, 0, keyboo.length, "AES");
           byte[] token = Base64.decode(cursor.getString(0), Base64.DEFAULT);
           byte[] token_secret = Base64.decode(cursor.getString(1), Base64.DEFAULT);
           AccessToken accessToken = new AccessToken(Encryption.decrypt(token, key), Encryption.decrypt(token_secret, key));
           AsyncTwitter twitter = new AsyncTwitterFactory().getInstance();
           twitter.setOAuthConsumer(apikey,apisecret);
           twitter.setOAuthAccessToken(accessToken);
           return twitter;
       }
}
