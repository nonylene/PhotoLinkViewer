package net.nonylene.photolinkviewer.tool;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import net.nonylene.photolinkviewer.R;

import org.json.JSONObject;

public class MyJsonObjectRequest extends JsonObjectRequest {

    public MyJsonObjectRequest(int method, String url, JSONObject jsonRequest,
                               Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {
        super(method, url, jsonRequest, listener, errorListener);
    }

    public MyJsonObjectRequest(final Context context, String url, Response.Listener<JSONObject> listener) {
        this(Method.GET, url, null, listener,
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e("error", error.toString());
                        Toast.makeText(context, context.getString(R.string.volley_error), Toast.LENGTH_LONG).show();
                    }
                }
        );
    }
}
