package net.nonylene.photolinkviewer

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.database.CursorIndexOutOfBoundsException
import android.database.sqlite.SQLiteException
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Base64
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.ListView
import android.widget.Toast
import butterknife.bindView
import com.twitter.sdk.android.core.Callback
import com.twitter.sdk.android.core.Result
import com.twitter.sdk.android.core.TwitterSession
import com.twitter.sdk.android.core.identity.TwitterLoginButton

import net.nonylene.photolinkviewer.dialog.DeleteDialogFragment
import net.nonylene.photolinkviewer.tool.Encryption
import net.nonylene.photolinkviewer.tool.MyAsyncTwitter
import net.nonylene.photolinkviewer.tool.MyCursorAdapter
import net.nonylene.photolinkviewer.tool.MySQLiteOpenHelper

import twitter4j.AsyncTwitter
import twitter4j.AsyncTwitterFactory
import twitter4j.TwitterAdapter
import twitter4j.TwitterException
import twitter4j.TwitterFactory
import twitter4j.TwitterMethod
import twitter4j.User
import twitter4j.auth.AccessToken
import twitter4j.auth.RequestToken

class TwitterOAuthActivity : AppCompatActivity(), DeleteDialogFragment.DeleteDialogCallBack {

    private var twitter: AsyncTwitter? = null
    private var requestToken: RequestToken? = null
    private var myCursorAdapter: MyCursorAdapter? = null
    private val database by lazy { MySQLiteOpenHelper(applicationContext).writableDatabase }
    private val preferences by lazy { getSharedPreferences("preference", Context.MODE_PRIVATE) }

    private val listView : ListView by bindView(R.id.accounts_list)
    private val twitterOAuthButton : TwitterLoginButton by bindView(R.id.twitter_oauth_button)

    private val twitterListener = object : TwitterAdapter() {
        override fun onException(exception: TwitterException?, method: TwitterMethod?) {
            Log.e("twitter", exception!!.toString())
        }

        override fun gotOAuthRequestToken(token: RequestToken?) {
            requestToken = token
            val uri = Uri.parse(requestToken!!.authorizationURL)
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        }

        override fun gotOAuthAccessToken(token: AccessToken) {
            fetchAndSaveUserData(token)
        }
    }


    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.toauth)

        twitterOAuthButton.callback = object: Callback<TwitterSession>() {
            override fun failure(e: com.twitter.sdk.android.core.TwitterException) {
                e.printStackTrace()
                Toast.makeText(this@TwitterOAuthActivity, getString(R.string.toauth_failed_token), Toast.LENGTH_LONG).show()
            }

            override fun success(result: Result<TwitterSession>) {
                result.data.authToken.let {
                    Thread(Runnable {
                        fetchAndSaveUserData(AccessToken(it.token, it.secret))
                    }).start()
                }
            }
        }

        // create table accounts
        with(database) {
            beginTransaction()
            execSQL("create table if not exists accounts (userName unique, userId integer unique, token, token_secret, key, icon)")
            setTransactionSuccessful()
            endTransaction()
        }

        setListView()
        updateProfiles()
    }

    private fun setListView() {
        // radio button
        listView.choiceMode = ListView.CHOICE_MODE_SINGLE
        // choose account
        listView.setOnItemClickListener { parent, view, position, id ->
            changeAccount(id.toInt())
        }
        listView.setOnItemLongClickListener { parent, view, position, id ->
            val cursor = listView.getItemAtPosition(position) as Cursor
            with(DeleteDialogFragment()) {
                setDeleteDialogCallBack(this@TwitterOAuthActivity)
                arguments = Bundle().apply {
                    putString("screen_name", cursor.getString(cursor.getColumnIndex("userName")))
                }
                show(this@TwitterOAuthActivity.fragmentManager, "delete")
            }
            true
        }

        // not to lock
        try {
            val cursor = database.rawQuery("select rowid _id, * from accounts", null)
            myCursorAdapter = MyCursorAdapter(applicationContext, cursor, true)
            listView.adapter = myCursorAdapter
            // check current radio button
            val accountPosition = preferences.getInt("account", 0)
            (0..listView.count - 1).forEach {
                val c = listView.getItemAtPosition(it) as Cursor
                if (c.getInt(c.getColumnIndex("_id")) == accountPosition) {
                    listView.setItemChecked(it, true)
                }
            }
        } catch (e: SQLiteException) {
            Log.e("SQLite", e.toString())
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.twitter_oauth_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.twitter_old_oauth -> {
                try {
                    twitter = AsyncTwitterFactory().instance.apply {
                        setOAuthConsumer(BuildConfig.TWITTER_KEY, BuildConfig.TWITTER_SECRET)
                        addListener(twitterListener)
                        getOAuthRequestTokenAsync("plvtwitter://callback")
                    }
                } catch (e: Exception) {
                    Log.e("twitter", e.toString())
                }
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDeleteConfirmed(userName: String) {
        var toastText: String? = null
        try {
            toastText = getString(R.string.delete_account_toast)
            with(database) {
                beginTransaction()
                // quotation is required.
                delete("accounts", "userName = ?", arrayOf(userName))
                // commit
                setTransactionSuccessful()
                endTransaction()
            }
            val new_cursor = database.rawQuery("select rowid _id, * from accounts", null)
            // renew listView
            // i want to use content provider and cursor loader in future.
            myCursorAdapter!!.swapCursor(new_cursor)

        } catch (e: SQLiteException) {
            toastText = getString(R.string.delete_failed_toast)
            e.printStackTrace()

        } finally {
            Toast.makeText(this, toastText, Toast.LENGTH_LONG).show()
        }
    }

    private fun updateProfiles() {
        if (preferences.getBoolean("authorized", false)) {
            try {
                val twitter = MyAsyncTwitter.getAsyncTwitterFromDB(database, applicationContext)
                twitter.addListener(object : TwitterAdapter() {
                    override fun gotUserDetail(user: User) {
                        try {
                            val values = ContentValues()
                            values.put("userName", user.screenName)
                            values.put("icon", user.biggerProfileImageURL)
                            // open database
                            with(database) {
                                beginTransaction()
                                update("accounts", values, "userId = ?", arrayOf(user.id.toString()))
                                setTransactionSuccessful()
                                endTransaction()
                            }
                            runOnUiThread {
                                // renew ui
                                myCursorAdapter!!.swapCursor(database.rawQuery("select rowid _id, * from accounts", null))
                            }
                        } catch (e: SQLiteException) {
                            Log.e("SQL", e.toString())
                        }
                    }
                })
                // move cursor focus
                val cursor = database.rawQuery("select rowid _id, userId from accounts", null)
                cursor.moveToFirst()
                twitter.showUser(cursor.getLong(cursor.getColumnIndex("userId")))
                while (cursor.moveToNext()) {
                    twitter.showUser(cursor.getLong(cursor.getColumnIndex("userId")))
                }
            } catch (e: CursorIndexOutOfBoundsException) {
                Log.e("cursor", e.toString())
                Toast.makeText(applicationContext, getString(R.string.twitter_async_select), Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        intent.data?.let {
            val oauth = it.getQueryParameter("oauth_verifier")
            if (oauth != null) {
                twitter!!.getOAuthAccessTokenAsync(requestToken, oauth)
            } else {
                Toast.makeText(this@TwitterOAuthActivity, getString(R.string.toauth_failed_token), Toast.LENGTH_LONG).show()
            }
        }
    }

    // this method must be run asynchronously.
    private fun fetchAndSaveUserData(token: AccessToken) {
        try {
            // get oauthed user_name and user_id and icon_url
            val twitterNotAsync = TwitterFactory().instance.apply {
                setOAuthConsumer(BuildConfig.TWITTER_KEY, BuildConfig.TWITTER_SECRET)
                oAuthAccessToken = token
            }
            val myId = twitterNotAsync.id
            val user = twitterNotAsync.showUser(myId)
            val screenName = user.screenName
            val icon = user.biggerProfileImageURL
            // encrypt twitter tokens by key
            // save encrypted keys to database
            val values = ContentValues().apply {
                put("userName", screenName)
                put("userId", myId)
                put("icon", icon)

                val key = Encryption.generate()
                put("token", Encryption.encrypt(token.token.toByteArray("UTF-8"), key))
                put("token_secret", Encryption.encrypt(token.tokenSecret.toByteArray("UTF-8"), key))
                put("key", Base64.encodeToString(key.encoded, Base64.DEFAULT))
            }
            // open database
            with(database) {
                beginTransaction()
                // if exists...
                delete("accounts", "userId = ?", arrayOf(myId.toString()))
                // insert account information
                insert("accounts", null, values)
                // commit
                setTransactionSuccessful()
                endTransaction()
            }
            val cursorNew = database.rawQuery("select rowid _id, userId from accounts where userId = ?",
                    arrayOf(myId.toString())).apply {
                moveToFirst()
            }
            val account = cursorNew.getInt(cursorNew.getColumnIndex("_id"))
            // set oauth_completed frag
            preferences.edit()
                    .putBoolean("authorized", true)
                    .putString("screen_name", screenName)
                    .putInt("account", account)
                    .apply()
            //putting cue to UI Thread
            runOnUiThread {
                Toast.makeText(this@TwitterOAuthActivity, getString(R.string.toauth_succeeded_token) + " @" + screenName, Toast.LENGTH_LONG).show()
                setListView()
            }
        } catch (e: TwitterException) {
            e.printStackTrace()
            //putting cue to UI Thread
            runOnUiThread { Toast.makeText(this@TwitterOAuthActivity, getString(R.string.toauth_failed_twitter4j), Toast.LENGTH_LONG).show() }
            finish()
        } catch (e: SQLiteException) {
            e.printStackTrace()
        }
    }

    private fun changeAccount(rowid: Int) {
        // save rowid and screen name to preference
        val cursor = database.rawQuery("select userName from accounts where rowid = ?",
                arrayOf(rowid.toString())).apply {
            moveToFirst()
        }
        preferences.edit()
                .putInt("account", rowid)
                .putString("screen_name", cursor.getString(cursor.getColumnIndex("userName")))
                .apply()
    }

    override fun onDestroy() {
        super.onDestroy()
        database.close()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // Pass the activity result to the login button.
        twitterOAuthButton.onActivityResult(requestCode, resultCode, data);
    }
}