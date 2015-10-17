package net.nonylene.photolinkviewer.tool

import android.content.Context
import com.android.volley.RequestQueue
import com.android.volley.toolbox.Volley

public object VolleyManager {
    private var requestQueue: RequestQueue? = null

    public fun getRequestQueue(context: Context) : RequestQueue {
        if (requestQueue == null){
            requestQueue = Volley.newRequestQueue(context)
        }
        return requestQueue!!
    }
}
