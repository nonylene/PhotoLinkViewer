package net.nonylene.photolinkviewer;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import net.nonylene.photolinkviewer.dialog.DeleteDialogFragment;
import net.nonylene.photolinkviewer.tool.BitmapCache;
import net.nonylene.photolinkviewer.tool.Encryption;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.security.Key;
import java.util.HashMap;
import java.util.Map;

public class IOAuthActivity extends AppCompatActivity implements DeleteDialogFragment.DeleteDialogCallBack {

    private SharedPreferences preferences;
    private NetworkImageView iconView;
    private TextView screenNameView;
    private LinearLayout unOAuthLayout;
    private RequestQueue queue;
    private ImageLoader loader;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_instagram);

        preferences = getSharedPreferences("preference", MODE_PRIVATE);
        queue = Volley.newRequestQueue(this);
        loader = new ImageLoader(queue, new BitmapCache());

        iconView = (NetworkImageView) findViewById(R.id.instagram_image_view);
        screenNameView = (TextView) findViewById(R.id.instagram_screen_name);
        unOAuthLayout = (LinearLayout) findViewById(R.id.instagram_unauth_layout);

        // oauth button
        Button oAuthButton = (Button) findViewById(R.id.instagram_oauth_button);
        oAuthButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Uri uri = new Uri.Builder().scheme("https")
                        .authority("api.instagram.com")
                        .path("/oauth/authorize/")
                        .appendQueryParameter("client_id", getString(R.string.instagram_key))
                        .appendQueryParameter("redirect_uri",
                                getString(R.string.instagram_callback_scheme) + "://callback")
                        .appendQueryParameter("response_type", "code")
                        .build();

                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
            }
        });

        // unoauth button
        Button unOAuthButton = (Button) findViewById(R.id.instagram_unauth_button);
        unOAuthButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bundle arguments = new Bundle();
                arguments.putString("screen_name",
                        preferences.getString("instagram_username", null));
                DeleteDialogFragment deleteDialogFragment = new DeleteDialogFragment();
                deleteDialogFragment.setArguments(arguments);
                deleteDialogFragment.setDeleteDialogCallBack(IOAuthActivity.this);
                deleteDialogFragment.show(getFragmentManager(), "delete");
            }
        });

        if (preferences.getBoolean("instagram_authorized", false)) {
            screenNameView.setText(preferences.getString("instagram_username", null));
            iconView.setImageUrl(preferences.getString("instagram_icon", null), loader);
        } else {
            iconView.setDefaultImageResId(R.drawable.instagram_logo);
            unOAuthLayout.setVisibility(View.GONE);
        }
    }

    public void getAccessToken(final String code) {

        // jsonRequest not understand parameters...
        StringRequest instagramRequest = new StringRequest(
                Request.Method.POST, "https://api.instagram.com/oauth/access_token",
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject jsonObject = new JSONObject(response);
                            onGetToken(jsonObject);
                        } catch (JSONException e) {
                            Toast.makeText(IOAuthActivity.this, getString(R.string.volley_error), Toast.LENGTH_LONG).show();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(IOAuthActivity.this, getString(R.string.volley_error), Toast.LENGTH_LONG).show();
                    }
                }
        ){
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("client_id", getString(R.string.instagram_key));
                params.put("client_secret", getString(R.string.instagram_secret));
                params.put("grant_type", "authorization_code");
                params.put("redirect_uri",
                        getString(R.string.instagram_callback_scheme) + "://callback");
                params.put("code", code);
                return params;
            }
        };
        queue.add(instagramRequest);
    }

    private void onGetToken(JSONObject json) {
        try {
            String accessToken = json.getString("access_token");
            JSONObject user = json.getJSONObject("user");
            String username = user.getString("username");
            String id = user.getString("id");
            String profile_picture = user.getString("profile_picture");

            // encrypt
            Key key = Encryption.generate();
            String accessTokenGen = Encryption.encrypt(accessToken.getBytes("UTF-8"), key);
            String keyString = Base64.encodeToString(key.getEncoded(), Base64.DEFAULT);

            preferences.edit().putString("instagram_token", accessTokenGen)
                    .putString("instagram_key", keyString)
                    .putString("instagram_id", id)
                    .putString("instagram_username", username)
                    .putString("instagram_icon", profile_picture)
                    .putBoolean("instagram_authorized", true)
                    .apply();

            screenNameView.setText(username);

            // if not null, icon would not updated
            iconView.setImageUrl(null, loader);
            iconView.setImageUrl(profile_picture, loader);
            unOAuthLayout.setVisibility(View.VISIBLE);

        } catch (JSONException | UnsupportedEncodingException e) {
            // toast
            Toast.makeText(this, getString(R.string.toauth_failed_token), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        // get callback
        Uri uri = intent.getData();
        if (uri != null) {
            String oauth = uri.getQueryParameter("code");
            if (oauth != null) {
                getAccessToken(oauth);
            } else {
                Toast.makeText(IOAuthActivity.this, getString(R.string.toauth_failed_token) + "\n("
                        + uri.getQueryParameter("error_reason") + ")", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onDeleteConfirmed(String userName) {
        preferences.edit().remove("instagram_token")
                .remove("instagram_key")
                .remove("instagram_id")
                .remove("instagram_username")
                .remove("instagram_icon")
                .remove("instagram_authorized")
                .apply();

        screenNameView.setText(R.string.instagram_not_authorized);
        iconView.setImageResource(R.drawable.instagram_logo);
        unOAuthLayout.setVisibility(View.GONE);
        Toast.makeText(this, getString(R.string.delete_account_toast), Toast.LENGTH_LONG).show();
    }
}
