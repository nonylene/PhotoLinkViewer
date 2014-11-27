package net.nonylene.photolinkviewer;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;

import java.net.MalformedURLException;
import java.net.URL;

import twitter4j.AsyncTwitter;
import twitter4j.TwitterAdapter;
import twitter4j.User;

public class MyCursorAdapter extends CursorAdapter {

    public MyCursorAdapter(Context context, Cursor cursor, boolean autoRequery) {
        super(context, cursor, autoRequery);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
        LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = layoutInflater.inflate(R.layout.account_list, viewGroup, false);
        return view;
    }

    @Override
    public void bindView(final View view, final Context context, Cursor cursor) {
        final Long id_long = cursor.getLong(cursor.getColumnIndex("userId"));
        Log.d("id", String.valueOf(id_long));
        AsyncTwitter asyncTwitter = MyAsyncTwitter.getAsyncTwitter(context);
        asyncTwitter.addListener(new TwitterAdapter() {
            @Override
            public void gotUserDetail(final User user) {
                try {
                    PLVImageView plvImageView = (PLVImageView) view.findViewById(R.id.icon);
                    URL url = new URL(user.getBiggerProfileImageURL());
                    plvImageView.setUrl(url);
                    TextView textView = (TextView) view.findViewById(R.id.screen);
                    textView.setText(user.getScreenName());
                    ContentValues values = new ContentValues();
                    values.put("userName", user.getScreenName());
                    // open database
                    MySQLiteOpenHelper sqLiteOpenHelper = new MySQLiteOpenHelper(context);
                    SQLiteDatabase database = sqLiteOpenHelper.getWritableDatabase();
                    database.beginTransaction();
                    database.update("accounts", values, "userId = ?", new String[]{String.valueOf(id_long)});
                    database.setTransactionSuccessful();
                    database.endTransaction();
                    database.close();
                } catch (MalformedURLException e) {
                    Log.e("URL", e.toString());
                } catch (SQLiteException e) {
                    Log.e("SQL", e.toString());
                }
            }
        });
        asyncTwitter.showUser(id_long);
    }

}
