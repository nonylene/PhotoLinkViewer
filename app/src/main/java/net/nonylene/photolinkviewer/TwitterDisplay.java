package net.nonylene.photolinkviewer;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.net.http.HttpResponseCache;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.Volley;

import net.nonylene.photolinkviewer.fragment.OptionFragment;
import net.nonylene.photolinkviewer.fragment.ShowFragment;
import net.nonylene.photolinkviewer.fragment.TwitterOptionFragment;
import net.nonylene.photolinkviewer.fragment.VideoShowFragment;
import net.nonylene.photolinkviewer.tool.AccountsList;
import net.nonylene.photolinkviewer.tool.BitmapCache;
import net.nonylene.photolinkviewer.tool.MyAsyncTwitter;
import net.nonylene.photolinkviewer.tool.PLVUrl;
import net.nonylene.photolinkviewer.tool.PLVUrlService;
import net.nonylene.photolinkviewer.tool.ProgressBarListener;
import net.nonylene.photolinkviewer.tool.TwitterStatusAdapter;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import twitter4j.AsyncTwitter;
import twitter4j.ExtendedMediaEntity;
import twitter4j.Status;
import twitter4j.TwitterAdapter;
import twitter4j.TwitterException;
import twitter4j.TwitterListener;
import twitter4j.TwitterMethod;


public class TwitterDisplay extends Activity implements TwitterStatusAdapter.TwitterAdapterListener, ProgressBarListener {

    private String url;
    private ProgressBar progressBar;
    private FrameLayout rootLayout;
    private TwitterStatusAdapter statusAdapter;
    private AsyncTwitter twitter;
    private boolean isSingle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_twitter_display);

        //enable cache
        try {
            File httpCacheDir = new File(getApplicationContext().getCacheDir(), "http");
            long httpCacheSize = 10 * 1024 * 1024; // 10 MB
            HttpResponseCache.install(httpCacheDir, httpCacheSize);
        } catch (IOException e) {
            Log.d("cache", "HTTP response cache installation failed");
        }

        // get intent and purse url
        SharedPreferences sharedPreferences = getSharedPreferences("preference", Context.MODE_PRIVATE);

        if (!Intent.ACTION_VIEW.equals(getIntent().getAction())) {
            Toast.makeText(this, "Intent Error!", Toast.LENGTH_LONG).show();
            return;
        }

        Bundle bundle = new Bundle();
        Uri uri = getIntent().getData();
        url = uri.toString();
        bundle.putString("url", url);

        // option fragment
        FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
        OptionFragment optionFragment = new OptionFragment();
        optionFragment.setArguments(bundle);
        fragmentTransaction.add(R.id.root_layout, optionFragment).commit();

        if (!url.contains("twitter.com")) return;

        RequestQueue queue = Volley.newRequestQueue(getApplicationContext());
        statusAdapter = new TwitterStatusAdapter(this, new ImageLoader(queue, new BitmapCache()));
        statusAdapter.setTwitterAdapterListener(this);

        ListView listView = (ListView) findViewById(R.id.twitter_list_view);
        listView.setAdapter(statusAdapter);

        progressBar = (ProgressBar) findViewById(R.id.show_progress);

        Pattern pattern = Pattern.compile("^https?://twitter\\.com/\\w+/status[es]*/(\\d+)");
        Matcher matcher = pattern.matcher(url);

        if (!matcher.find()) return;

        String id = matcher.group(1);
        Long id_long = Long.parseLong(id);

        if (!sharedPreferences.getBoolean("authorized", false)) {
            Toast.makeText(getApplicationContext(), getString(R.string.twitter_display_oauth), Toast.LENGTH_LONG).show();
            Intent intent = new Intent(this, TOAuth.class);
            startActivity(intent);
            finish();
            return;
        }

        // oAuthed
        try {
            // get twitter async
            twitter = MyAsyncTwitter.getAsyncTwitter(getApplicationContext());
            twitter.addListener(twitterListener);
            twitter.showStatus(id_long);

            bundle.putLong("id_long", id_long);
            TwitterOptionFragment twitterOptionFragment = new TwitterOptionFragment();
            twitterOptionFragment.setArguments(bundle);
            getFragmentManager().beginTransaction().add(R.id.buttons, twitterOptionFragment).commit();

        } catch (SQLiteException e) {
            e.printStackTrace();

        } catch (IndexOutOfBoundsException e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), getString(R.string.twitter_async_select), Toast.LENGTH_LONG).show();
            Intent intent = new Intent(this, TOAuth.class);
            startActivity(intent);
            finish();
        }
    }

    @Override
    public void onShowFragmentRequired(PLVUrl plvUrl) {
        onFragmentRequired(new ShowFragment(), plvUrl);
    }

    @Override
    public void onVideoShowFragmentRequired(PLVUrl plvUrl) {
        onFragmentRequired(new VideoShowFragment(), plvUrl);
    }

    private void onFragmentRequired(Fragment fragment, PLVUrl plvUrl) {
        try {
            // go to show fragment
            Bundle bundle = new Bundle();
            bundle.putParcelable("plvurl", plvUrl);
            bundle.putBoolean("single_frag", isSingle);
            FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();

            fragment.setArguments(bundle);
            fragmentTransaction.replace(R.id.show_frag_replace, fragment);

            if (!isSingle) {
                // back to this screen when back pressed
                fragmentTransaction.addToBackStack(null);
            }
            fragmentTransaction.commit();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onReadMoreClicked() {
        long replyId = statusAdapter.getLastStatus().getInReplyToStatusId();
        twitter.showStatus(replyId);
    }

    @Override
    public void hideProgressBar() {
        progressBar.setVisibility(View.GONE);
    }

    public static class ChangeAccountDialog extends DialogFragment {

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AccountsList accountsList = MyAsyncTwitter.getAccountsList(getActivity());
            final ArrayList<String> screen_list = accountsList.getScreenList();
            final ArrayList<Integer> row_id_list = accountsList.getRowIdList();
            // get array for easy
            String[] screen_array = screen_list.toArray(new String[screen_list.size()]);
            // get current screen_name
            final SharedPreferences sharedPreferences = getActivity().getSharedPreferences("preference", MODE_PRIVATE);
            String current_name = sharedPreferences.getString("screen_name", null);
            int position = screen_list.indexOf(current_name);
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(getString(R.string.account_dialog_title))
                    .setNegativeButton(getString(android.R.string.cancel), null)
                    .setSingleChoiceItems(screen_array, position, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int position) {
                            int row_id = row_id_list.get(position);
                            String screen_name = screen_list.get(position);
                            // save rowid and screen name to preference
                            sharedPreferences.edit().putInt("account", row_id).apply();
                            sharedPreferences.edit().putString("screen_name", screen_name).apply();
                            //reload activity
                            startActivity(getActivity().getIntent());
                            dialog.dismiss();
                            getActivity().finish();
                        }
                    });
            return builder.create();
        }

    }

    private TwitterListener twitterListener = new TwitterAdapter() {

        @Override
        public void onException(TwitterException e, TwitterMethod twitterMethod) {
            Log.e("twitterException", e.toString());
            final String message = getString(R.string.twitter_error_toast) + ": " + e.getStatusCode() + "\n(" + e.getErrorMessage() + ")";
            switch (e.getErrorCode()) {
                case 179:
                case 88:
                    createDialog();
                    break;
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
                }
            });
        }

        public void createDialog() {
            ChangeAccountDialog accountDialog = new ChangeAccountDialog();
            accountDialog.show(getFragmentManager(), "account");
        }

        @Override
        public void gotShowStatus(final Status status) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (statusAdapter.isEmpty()) {
                        // set media entity
                        ExtendedMediaEntity[] mediaEntities = status.getExtendedMediaEntities();

                        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                        boolean show = sharedPreferences.getBoolean("disp_tweet", false);
                        // if number of media entity is one, show fragment directly
                        if (!show && (url.contains("/photo") || url.contains("/video")) && mediaEntities.length == 1) {
                            isSingle = true;

                            ExtendedMediaEntity mediaEntity = mediaEntities[0];
                            String type = mediaEntity.getType();

                            PLVUrlService plvUrlService = new PLVUrlService(TwitterDisplay.this, new PLVUrlService.PLVUrlListener() {
                                @Override
                                public void onGetPLVUrlFinished(@NotNull PLVUrl[] plvUrls) {
                                    PLVUrl plvUrl = plvUrls[0];
                                    if (plvUrl.isVideo()) {
                                        onVideoShowFragmentRequired(plvUrl);
                                    } else {
                                        onShowFragmentRequired(plvUrl);
                                    }
                                }

                                @Override
                                public void onGetPLVUrlFailed(@NotNull String text) {

                                }

                                @Override
                                public void onURLAccepted() {

                                }
                            });

                            if (("animated_gif").equals(type) || ("video").equals(type)) {
                                String file_url = getBiggestMp4Url(mediaEntity.getVideoVariants());
                                PLVUrl plvUrl = new PLVUrl(url);
                                plvUrl.setDisplayUrl(file_url);
                                onVideoShowFragmentRequired(plvUrl);
                            } else {
                                plvUrlService.requestGetPLVUrl(mediaEntity.getMediaURLHttps());
                            }
                            return;

                        } else {
                            hideProgressBar();

                            isSingle = false;
                            // change background color
                            rootLayout = (FrameLayout) findViewById(R.id.root_layout);
                            rootLayout.setBackgroundResource(R.color.background);
                        }
                    }

                    statusAdapter.addItem(status);

                    if (status.getInReplyToScreenName() == null) {
                        statusAdapter.removeLoadingItem();
                    } else {
                        // auto pager
                        if (statusAdapter.getCount() % 4 != 2) {
                            onReadMoreClicked();
                        } else {
                            statusAdapter.setRequesting(false);
                        }
                    }

                    statusAdapter.notifyDataSetChanged();
                }

                private String getBiggestMp4Url(ExtendedMediaEntity.Variant[] variants) {
                    int bitrate = 0;
                    String url = null;
                    for (ExtendedMediaEntity.Variant variant : variants) {
                        if (("video/mp4").equals(variant.getContentType()) && bitrate <= variant.getBitrate()) {
                            url = variant.getUrl();
                            bitrate = variant.getBitrate();
                        }
                    }
                    return url;
                }
            });
        }

    };

    @Override
    protected void onStop() {
        super.onStop();

        // flash cache
        HttpResponseCache cache = HttpResponseCache.getInstalled();
        if (cache != null) {
            cache.flush();
        }
    }
}
