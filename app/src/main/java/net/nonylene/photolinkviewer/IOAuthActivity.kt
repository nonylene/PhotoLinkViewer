package net.nonylene.photolinkviewer

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v7.app.AppCompatActivity
import android.util.Base64
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

import com.android.volley.AuthFailureError
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.squareup.picasso.Picasso

import net.nonylene.photolinkviewer.dialog.DeleteDialogFragment
import net.nonylene.photolinkviewer.tool.Encryption
import net.nonylene.photolinkviewer.tool.OkHttpManager
import net.nonylene.photolinkviewer.tool.PLVUtils

import org.json.JSONException
import org.json.JSONObject

import java.io.UnsupportedEncodingException
import java.util.HashMap

class IOAuthActivity : AppCompatActivity(), DeleteDialogFragment.DeleteDialogCallBack {

    private var preferences: SharedPreferences? = null
    private var iconView: ImageView? = null
    private var screenNameView: TextView? = null
    private var unOAuthLayout: LinearLayout? = null
    private var queue: RequestQueue? = null
    private var picasso: Picasso? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_instagram)

        preferences = getSharedPreferences("preference", Context.MODE_PRIVATE)
        queue = Volley.newRequestQueue(this)

        iconView = findViewById(R.id.instagram_image_view) as ImageView
        screenNameView = findViewById(R.id.instagram_screen_name) as TextView
        unOAuthLayout = findViewById(R.id.instagram_unauth_layout) as LinearLayout

        picasso = OkHttpManager.getPicasso(this)

        // oauth button
        findViewById(R.id.instagram_oauth_button).setOnClickListener{
            val uri = Uri.Builder()
                    .scheme("https")
                    .authority("api.instagram.com")
                    .path("/oauth/authorize/")
                    .appendQueryParameter("client_id", BuildConfig.INSTAGRAM_KEY)
                    .appendQueryParameter("redirect_uri", BuildConfig.INSTAGRAM_CALLBACK_SCHEME + "://callback")
                    .appendQueryParameter("response_type", "code")
                    .build()
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        }

        // unOauth button
        findViewById(R.id.instagram_unauth_button).setOnClickListener {
            val arguments = Bundle().apply {
                putString("screen_name",
                        preferences!!.getString("instagram_username", null))
            }

            DeleteDialogFragment().apply {
                setArguments(arguments)
                setDeleteDialogCallBack(this@IOAuthActivity)
                show(this@IOAuthActivity.fragmentManager, "delete")
            }
        }

        if (preferences!!.getBoolean("instagram_authorized", false)) {
            screenNameView!!.text = preferences!!.getString("instagram_username", null)
            picasso!!.load(preferences!!.getString("instagram_icon", null)).into(iconView)
        } else {
            iconView!!.setImageResource(R.drawable.instagram_logo)
            unOAuthLayout!!.visibility = View.GONE
        }
    }

    private fun getAccessToken(code: String) {
        // jsonRequest not understand parameters...
        val instagramRequest = object : StringRequest(Request.Method.POST, "https://api.instagram.com/oauth/access_token", { response ->
            try {
                val jsonObject = JSONObject(response)
                onGetToken(jsonObject)
            } catch (e: JSONException) {
                Toast.makeText(this@IOAuthActivity, getString(R.string.volley_error), Toast.LENGTH_LONG).show()
            }
        }, {
            Toast.makeText(this@IOAuthActivity, getString(R.string.volley_error), Toast.LENGTH_LONG).show()
        }) {
            @Throws(AuthFailureError::class)
            override fun getParams(): Map<String, String> {
                val params = HashMap<String, String>()
                params.put("client_id", BuildConfig.INSTAGRAM_KEY)
                params.put("client_secret", BuildConfig.INSTAGRAM_SECRET)
                params.put("grant_type", "authorization_code")
                params.put("redirect_uri", BuildConfig.INSTAGRAM_CALLBACK_SCHEME + "://callback")
                params.put("code", code)
                return params
            }
        }
        queue!!.add(instagramRequest)
    }

    private fun onGetToken(json: JSONObject) {
        try {
            val user = json.getJSONObject("user")
            val username = user.getString("username")
            val id = user.getString("id")
            val profile_picture = user.getString("profile_picture")

            // encrypt
            val key = Encryption.generate()
            val accessTokenGen = Encryption.encrypt(json.getString("access_token").toByteArray(), key)
            val keyString = Base64.encodeToString(key.encoded, Base64.DEFAULT)

            preferences!!.edit()
                    .putString("instagram_token", accessTokenGen)
                    .putString("instagram_key", keyString)
                    .putString("instagram_id", id)
                    .putString("instagram_username", username)
                    .putString("instagram_icon", profile_picture)
                    .putBoolean("instagram_authorized", true)
                    .apply()

            PreferenceManager.getDefaultSharedPreferences(this).edit()
                    .putBoolean("instagram_api", true).apply()

            PLVUtils.refreshInstagramToken(this)

            screenNameView!!.text = username

            // if not null, icon would not updated
            picasso!!.load(profile_picture).into(iconView)
            unOAuthLayout!!.visibility = View.VISIBLE

        } catch (e: JSONException) {
            // toast
            Toast.makeText(this, getString(R.string.toauth_failed_token), Toast.LENGTH_LONG).show()
        } catch (e: UnsupportedEncodingException) {
            Toast.makeText(this, getString(R.string.toauth_failed_token), Toast.LENGTH_LONG).show()
        }

    }

    override fun onNewIntent(intent: Intent) {
        // get callback
        intent.data?.let { uri ->
            val oauth = uri.getQueryParameter("code")
            if (oauth != null) {
                getAccessToken(oauth)
            } else {
                Toast.makeText(this@IOAuthActivity, getString(R.string.toauth_failed_token) + "\n(" + uri.getQueryParameter("error_reason") + ")", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDeleteConfirmed(userName: String) {
        preferences!!.edit()
                .remove("instagram_token")
                .remove("instagram_key")
                .remove("instagram_id")
                .remove("instagram_username")
                .remove("instagram_icon")
                .remove("instagram_authorized")
                .apply()

        PreferenceManager.getDefaultSharedPreferences(this).edit()
                .putBoolean("instagram_api", false).apply()

        PLVUtils.refreshInstagramToken(this)

        screenNameView!!.setText(R.string.instagram_not_authorized)
        iconView!!.setImageResource(R.drawable.instagram_logo)
        unOAuthLayout!!.visibility = View.GONE

        Toast.makeText(this, getString(R.string.delete_account_toast), Toast.LENGTH_LONG).show()
    }
}
