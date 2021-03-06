package net.nonylene.photolinkviewer.tool;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Base64;

import net.nonylene.photolinkviewer.BuildConfig;
import net.nonylene.photolinkviewer.core.PhotoLinkViewer;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import javax.crypto.spec.SecretKeySpec;

import twitter4j.AsyncTwitter;
import twitter4j.AsyncTwitterFactory;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;

public class MyAsyncTwitter {

    public static AsyncTwitter getAsyncTwitter(Context context) throws SQLiteException, CursorIndexOutOfBoundsException {
        return getAsyncTwitter(context, getRowId(context));
    }

    public static AsyncTwitter getAsyncTwitter(Context context, int row_id) throws SQLiteException, CursorIndexOutOfBoundsException {
        // sql
        MySQLiteOpenHelper sqLiteOpenHelper = new MySQLiteOpenHelper(context);
        SQLiteDatabase database = sqLiteOpenHelper.getReadableDatabase();
        AsyncTwitter twitter = getTwitterFromId(database, row_id);
        // sql close
        database.close();
        return twitter;
    }

    public static AsyncTwitter getAsyncTwitterFromDB(SQLiteDatabase database, Context context) throws SQLiteException, CursorIndexOutOfBoundsException {
        return getAsyncTwitterFromDB(database, getRowId(context));
    }

    public static AsyncTwitter getAsyncTwitterFromDB(SQLiteDatabase database, int row_id) throws SQLiteException, CursorIndexOutOfBoundsException {
        // sql not close
        return getTwitterFromId(database, row_id);
    }

    private static int getRowId(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("preference", Context.MODE_PRIVATE);
        return sharedPreferences.getInt("account", 1);
    }

    private static AsyncTwitter getTwitterFromId(SQLiteDatabase database, int row_id) {
        // oAuthed
        String apiKey = BuildConfig.TWITTER_KEY;
        String apiSecret = BuildConfig.TWITTER_SECRET;
        Cursor cursor = database.rawQuery("select token, token_secret, key from accounts where rowid = ?", new String[]{String.valueOf(row_id)});
        // rowid only one row
        cursor.moveToFirst();
        PhotoLinkViewer.TwitterToken token = getAccessToken(cursor);

        Configuration twitterConfiguration = new ConfigurationBuilder()
                .setTweetModeExtended(true)
                .setOAuthConsumerKey(apiKey)
                .setOAuthConsumerSecret(apiSecret)
                .setOAuthAccessToken(token.getAccessToken())
                .setOAuthAccessTokenSecret(token.getAccessTokenSecret())
                .build();

        return new AsyncTwitterFactory(twitterConfiguration).getInstance();
    }


    public static AccountsList getAccountsList(Context context) {
        // get account info from database
        SQLiteOpenHelper helper = new MySQLiteOpenHelper(context);
        SQLiteDatabase database = helper.getReadableDatabase();
        Cursor cursor = database.rawQuery("select rowid, userName from accounts order by rowid", null);
        // lists
        ArrayList<String> screen_list = new ArrayList<>();
        ArrayList<Integer> row_id_list = new ArrayList<>();
        cursor.moveToFirst();
        screen_list.add(cursor.getString(cursor.getColumnIndex("userName")));
        row_id_list.add(cursor.getInt(cursor.getColumnIndex("rowid")));
        while (cursor.moveToNext()) {
            screen_list.add(cursor.getString(cursor.getColumnIndex("userName")));
            row_id_list.add(cursor.getInt(cursor.getColumnIndex("rowid")));
        }
        database.close();
        return new AccountsList(screen_list, row_id_list);
    }

    public static LinkedHashMap<String, PhotoLinkViewer.TwitterToken> getAccountsTokenList(Context context) {
        // sql
        MySQLiteOpenHelper sqLiteOpenHelper = new MySQLiteOpenHelper(context);
        SQLiteDatabase database = sqLiteOpenHelper.getReadableDatabase();
        LinkedHashMap<String, PhotoLinkViewer.TwitterToken> map = getAccountsTokenList(database);
        database.close();
        return map;
    }

    public static LinkedHashMap<String, PhotoLinkViewer.TwitterToken> getAccountsTokenList(SQLiteDatabase database) {
        LinkedHashMap<String, PhotoLinkViewer.TwitterToken> tokenLinkedHashMap = new LinkedHashMap<>();
        Cursor cursor = database.rawQuery("select userName, token, token_secret, key from accounts order by rowid", null);
        // lists
        if (cursor.moveToFirst()) {
            tokenLinkedHashMap.put(cursor.getString(cursor.getColumnIndex("userName")), getAccessToken(cursor));
            while (cursor.moveToNext()) {
                tokenLinkedHashMap.put(cursor.getString(cursor.getColumnIndex("userName")), getAccessToken(cursor));
            }
        }
        return tokenLinkedHashMap;
    }

    private static PhotoLinkViewer.TwitterToken getAccessToken(Cursor cursor) {
        String tokenKey = cursor.getString(cursor.getColumnIndex("key"));
        byte[] keyboo = Base64.decode(tokenKey, Base64.DEFAULT);
        SecretKeySpec key = new SecretKeySpec(keyboo, 0, keyboo.length, "AES");
        byte[] token = Base64.decode(cursor.getString(cursor.getColumnIndex("token")), Base64.DEFAULT);
        byte[] token_secret = Base64.decode(cursor.getString(cursor.getColumnIndex("token_secret")), Base64.DEFAULT);
        return new PhotoLinkViewer.TwitterToken(Encryption.decrypt(token, key), Encryption.decrypt(token_secret, key));
    }

    public static void createTwitterTable(Context context) {
        // create table accounts
        MySQLiteOpenHelper sqLiteOpenHelper = new MySQLiteOpenHelper(context);
        SQLiteDatabase database = sqLiteOpenHelper.getReadableDatabase();
        database.beginTransaction();
        database.execSQL("create table if not exists accounts (userName unique, userId integer unique, token, token_secret, key, icon)");
        database.setTransactionSuccessful();
        database.endTransaction();
        database.close();
    }
}
