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
import twitter4j.auth.AccessToken;

public class MyAsyncTwitter {

    public static AsyncTwitter getAsyncTwitter(Context context) throws SQLiteException, CursorIndexOutOfBoundsException {
        return getAsyncTwitter(context, getRowId(context));
    }

    public static AsyncTwitter getAsyncTwitter(Context context, int row_id) throws SQLiteException, CursorIndexOutOfBoundsException {
        // sql
        MySQLiteOpenHelper sqLiteOpenHelper = new MySQLiteOpenHelper(context);
        SQLiteDatabase database = sqLiteOpenHelper.getReadableDatabase();
        AsyncTwitter twitter = getTwitterFromId(database, context, row_id);
        // sql close
        database.close();
        return twitter;
    }

    public static AsyncTwitter getAsyncTwitterFromDB(SQLiteDatabase database, Context context) throws SQLiteException, CursorIndexOutOfBoundsException {
        return getAsyncTwitterFromDB(database, context, getRowId(context));
    }

    public static AsyncTwitter getAsyncTwitterFromDB(SQLiteDatabase database, Context context, int row_id) throws SQLiteException, CursorIndexOutOfBoundsException {
        // sql not close
        return getTwitterFromId(database, context, row_id);
    }

    private static int getRowId(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("preference", Context.MODE_PRIVATE);
        return sharedPreferences.getInt("account", 1);
    }

    private static AsyncTwitter getTwitterFromId(SQLiteDatabase database, Context context, int row_id) {
        // oAuthed
        String apikey = BuildConfig.TWITTER_KEY;
        String apisecret = BuildConfig.TWITTER_SECRET;
        Cursor cursor = database.rawQuery("select token, token_secret, key from accounts where rowid = ?", new String[]{String.valueOf(row_id)});
        // rowid only one row
        cursor.moveToFirst();
        String tokenkey = cursor.getString(2);
        byte[] keyboo = Base64.decode(tokenkey, Base64.DEFAULT);
        SecretKeySpec key = new SecretKeySpec(keyboo, 0, keyboo.length, "AES");
        byte[] token = Base64.decode(cursor.getString(0), Base64.DEFAULT);
        byte[] token_secret = Base64.decode(cursor.getString(1), Base64.DEFAULT);
        AccessToken accessToken = new AccessToken(Encryption.decrypt(token, key), Encryption.decrypt(token_secret, key));
        AsyncTwitter twitter = new AsyncTwitterFactory().getInstance();
        twitter.setOAuthConsumer(apikey, apisecret);
        twitter.setOAuthAccessToken(accessToken);
        return twitter;
    }


    public static AccountsList getAccountsList(Context context) {
        // get account info from database
        SQLiteOpenHelper helper = new MySQLiteOpenHelper(context);
        SQLiteDatabase database = helper.getReadableDatabase();
        Cursor cursor = database.rawQuery("select rowid, userName from accounts", null);
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
        AccountsList list = new AccountsList();
        list.setRowIdList(row_id_list);
        list.setScreenList(screen_list);
        return list;
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
        Cursor cursor = database.rawQuery("select userName, token, token_secret, key from accounts", null);
        // lists
        if (cursor.moveToFirst()) {
            String tokenKey = cursor.getString(3);
            byte[] keyboo = Base64.decode(tokenKey, Base64.DEFAULT);
            SecretKeySpec key = new SecretKeySpec(keyboo, 0, keyboo.length, "AES");
            byte[] token = Base64.decode(cursor.getString(1), Base64.DEFAULT);
            byte[] token_secret = Base64.decode(cursor.getString(2), Base64.DEFAULT);
            PhotoLinkViewer.TwitterToken accessToken = new PhotoLinkViewer.TwitterToken(Encryption.decrypt(token, key), Encryption.decrypt(token_secret, key));
            tokenLinkedHashMap.put(cursor.getString(0), accessToken);

            while (cursor.moveToNext()) {
                String tKey = cursor.getString(3);
                byte[] kboo = Base64.decode(tKey, Base64.DEFAULT);
                SecretKeySpec keySpec = new SecretKeySpec(kboo, 0, kboo.length, "AES");
                byte[] aTokenByte = Base64.decode(cursor.getString(1), Base64.DEFAULT);
                byte[] aTokenSecretByte = Base64.decode(cursor.getString(2), Base64.DEFAULT);
                PhotoLinkViewer.TwitterToken aToken = new PhotoLinkViewer.TwitterToken(Encryption.decrypt(aTokenByte, keySpec), Encryption.decrypt(aTokenSecretByte, keySpec));
                tokenLinkedHashMap.put(cursor.getString(0), aToken);
            }
        }
        return tokenLinkedHashMap;
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
