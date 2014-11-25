package net.nonylene.photolinkviewer;

import android.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;

import javax.crypto.spec.SecretKeySpec;

import twitter4j.AsyncTwitter;
import twitter4j.AsyncTwitterFactory;
import twitter4j.Status;
import twitter4j.TwitterAdapter;
import twitter4j.TwitterException;
import twitter4j.TwitterListener;
import twitter4j.TwitterMethod;
import twitter4j.auth.AccessToken;

public class TwitterOptionFragment extends Fragment {
    private View view;
    private ImageButton retweet_button;
    private AsyncTwitter twitter;
    private SharedPreferences sharedPreferences;
    private Long id_long;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.twitter_option_fragment, container, false);
        id_long = getArguments().getLong("id_long");
        sharedPreferences = getActivity().getSharedPreferences("preference", Context.MODE_PRIVATE);
        // get twitter CK/CS/AT/AS
        String apikey = (String) getText(R.string.twitter_key);
        String apisecret = (String) getText(R.string.twitter_secret);
        String tokenkey = sharedPreferences.getString("key", null);
        // oAuthed
        byte[] keyboo = Base64.decode(tokenkey, Base64.DEFAULT);
        SecretKeySpec key = new SecretKeySpec(keyboo, 0, keyboo.length, "AES");
        byte[] token = Base64.decode(sharedPreferences.getString("ttoken", null), Base64.DEFAULT);
        byte[] token_secret = Base64.decode(sharedPreferences.getString("ttokensecret", null), Base64.DEFAULT);
        AccessToken accessToken = new AccessToken(Encryption.decrypt(token, key), Encryption.decrypt(token_secret, key));
        // get twitter async
        twitter = new AsyncTwitterFactory().getInstance();
        twitter.setOAuthConsumer(apikey, apisecret);
        twitter.setOAuthAccessToken(accessToken);
        twitter.addListener(twitterListener);
        retweet_button = (ImageButton) view.findViewById(R.id.retweet_button);
        retweet_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                twitter.retweetStatus(id_long);
            }
        });
        return view;
    }

    private TwitterListener twitterListener = new TwitterAdapter(){
        @Override
        public void onException(TwitterException e, TwitterMethod twitterMethod) {
            Log.e("twitterException", e.toString());
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(view.getContext(), getString(R.string.twitter_error_toast), Toast.LENGTH_LONG).show();
                }
            });
        }

        @Override
        public void retweetedStatus(Status status){
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(view.getContext(), "retweeted!", Toast.LENGTH_LONG).show();
                }
            });

        }
    };
}
