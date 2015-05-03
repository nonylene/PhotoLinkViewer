package net.nonylene.photolinkviewer.tool;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckedTextView;
import android.widget.CursorAdapter;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;
import com.android.volley.toolbox.Volley;

import net.nonylene.photolinkviewer.R;

public class MyCursorAdapter extends CursorAdapter {

    public MyCursorAdapter(Context context, Cursor cursor, boolean autoRequery) {
        super(context, cursor, autoRequery);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
        LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        return layoutInflater.inflate(R.layout.account_list, viewGroup, false);
    }

    @Override
    public void bindView(final View view, final Context context, Cursor cursor) {
        try {
            String screen_name = cursor.getString(cursor.getColumnIndex("userName"));
            CheckedTextView textView = (CheckedTextView) view.findViewById(R.id.screen);
            textView.setText(screen_name);

            String icon_url = cursor.getString(cursor.getColumnIndex("icon"));

            NetworkImageView imageView = (NetworkImageView) view.findViewById(R.id.icon);
            BitmapCache bitmapCache = new BitmapCache();
            RequestQueue queue = Volley.newRequestQueue(context);
            imageView.setImageUrl(icon_url, new ImageLoader(queue, bitmapCache));
        } catch (SQLiteException e) {
            Log.e("SQL", e.toString());
        }
    }

}
