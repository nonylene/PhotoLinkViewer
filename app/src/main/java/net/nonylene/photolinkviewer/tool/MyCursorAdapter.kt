package net.nonylene.photolinkviewer.tool

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteException
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckedTextView
import android.widget.CursorAdapter
import android.widget.ImageView

import net.nonylene.photolinkviewer.R
import net.nonylene.photolinkviewer.core.tool.OkHttpManager

class MyCursorAdapter(context: Context, cursor: Cursor, autoRequery: Boolean) : CursorAdapter(context, cursor, autoRequery) {

    override fun newView(context: Context, cursor: Cursor, viewGroup: ViewGroup): View {
        val layoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        return layoutInflater.inflate(R.layout.account_list, viewGroup, false)
    }

    override fun bindView(view: View, context: Context, cursor: Cursor) {
        try {
            val screen_name = cursor.getString(cursor.getColumnIndex("userName"))
            val textView = view.findViewById(R.id.screen) as CheckedTextView
            textView.text = screen_name

            val icon_url = cursor.getString(cursor.getColumnIndex("icon"))

            val imageView = view.findViewById(R.id.icon) as ImageView
            OkHttpManager.getPicasso(context).load(icon_url).into(imageView)
        } catch (e: SQLiteException) {
            Log.e("SQL", e.toString())
        }

    }

}
