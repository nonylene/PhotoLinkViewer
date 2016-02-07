package net.nonylene.photolinkviewer;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import net.nonylene.photolinkviewer.core.fragment.PreferenceSummaryFragment;
import net.nonylene.photolinkviewer.core.tool.Initialize;
import net.nonylene.photolinkviewer.tool.AccountsList;
import net.nonylene.photolinkviewer.tool.MyAsyncTwitter;
import net.nonylene.photolinkviewer.tool.PLVUtils;

import java.util.ArrayList;

import twitter4j.AsyncTwitter;
import twitter4j.Status;
import twitter4j.TwitterAdapter;
import twitter4j.TwitterException;
import twitter4j.TwitterMethod;

public class Settings extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (!preferences.getBoolean("initialized39", false)) {
            Initialize.INSTANCE.initialize39(this);
        }
        getFragmentManager().beginTransaction().replace(android.R.id.content, new SettingsFragment()).commit();
    }

    public static class SettingsFragment extends PreferenceSummaryFragment {

        private static final int ABOUT_FRAGMENT = 100;
        private static final int TWITTER_FRAGMENT = 200;
        private static final int TWEET_CODE = 10;

        private SwitchPreference instagramSwitch;

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            addPreferencesFromResource(R.xml.settings);
            Preference preference = findPreference("about_app_preference");
            preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    AboutDialogFragment dialogFragment = new AboutDialogFragment();
                    dialogFragment.setTargetFragment(SettingsFragment.this, ABOUT_FRAGMENT);
                    dialogFragment.show(getFragmentManager(), "about");
                    return false;
                }
            });

            Preference instagramPreference = findPreference("instagram_preference");
            instagramPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent intent = new Intent(getActivity(), IOAuthActivity.class);
                    // get oauth result
                    startActivityForResult(intent, 1);
                    return false;
                }
            });

            instagramSwitch = (SwitchPreference) findPreference("instagram_api");

            instagramSwitch.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    PLVUtils.INSTANCE.refreshInstagramToken(getActivity(), (boolean) newValue);
                    return true;
                }
            });

            return super.onCreateView(inflater, container, savedInstanceState);
        }

        @Override
        public void onResume() {
            super.onResume();
            // IOAuthActivity
            instagramSwitch.setEnabled(getActivity().getSharedPreferences("preference", MODE_PRIVATE)
                    .getBoolean("instagram_authorized", false));
            instagramSwitch.setChecked(PreferenceManager.getDefaultSharedPreferences(getActivity())
                    .getBoolean("instagram_api", false));
        }

        // license etc
        public static class AboutDialogFragment extends DialogFragment {
            private int count = 0;

            @Override
            public Dialog onCreateDialog(Bundle savedInstanceState) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                try {
                    PackageManager manager = getActivity().getPackageManager();
                    PackageInfo info = manager.getPackageInfo(getActivity().getPackageName(), 0);
                    String version = info.versionName;
                    View view = View.inflate(getActivity(), R.layout.about_app, null);
                    TextView textView = (TextView) view.findViewById(R.id.about_version);
                    textView.append(version);
                    builder.setView(view)
                            .setTitle(getString(R.string.about_app_dialogtitle))
                            .setPositiveButton(getString(android.R.string.ok), null);

                    textView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            count++;
                            if (count == 5) {
                                getTargetFragment().onActivityResult(getTargetRequestCode(), TWEET_CODE, null);
                                count = 0;
                            }
                        }
                    });

                } catch (PackageManager.NameNotFoundException e) {
                    Log.e("error", e.toString());
                    Toast.makeText(getActivity(), "Error occured", Toast.LENGTH_LONG).show();
                }
                return builder.create();
            }
        }

        public static class TwitterDialogFragment extends DialogFragment {
            @Override
            public Dialog onCreateDialog(Bundle savedInstanceState) {
                // get screen_name
                SharedPreferences sharedPreferences = getActivity().getSharedPreferences("preference", Context.MODE_PRIVATE);
                String screenName = sharedPreferences.getString("screen_name", null);

                // get account_list
                AccountsList accountsList = MyAsyncTwitter.getAccountsList(getActivity());
                ArrayList<String> screen_list = accountsList.getScreenList();
                final ArrayList<Integer> row_id_list = accountsList.getRowIdList();
                // array_list to adapter
                ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, screen_list);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

                // get view
                View view = View.inflate(getActivity(), R.layout.spinner_tweet, null);
                final Spinner spinner = (Spinner) view.findViewById(R.id.accounts_spinner);
                spinner.setAdapter(adapter);
                spinner.setSelection(screen_list.indexOf(screenName));

                return new AlertDialog.Builder(getActivity())
                        .setTitle(getString(R.string.tweet))
                        .setView(view)
                        .setPositiveButton(getString(android.R.string.ok), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                int position = spinner.getSelectedItemPosition();
                                int row_id = row_id_list.get(position);
                                EditText editedText = (EditText) getDialog().findViewById(R.id.spinner_edit);
                                Intent intent = new Intent();
                                intent.putExtra("tweet_text", editedText.getText().toString());
                                getTargetFragment().onActivityResult(getTargetRequestCode(), row_id, intent);
                            }
                        })
                        .setNegativeButton(getString(android.R.string.cancel), null)
                        .create();
            }
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            if (requestCode == ABOUT_FRAGMENT && resultCode == TWEET_CODE) {
                TwitterDialogFragment dialogFragment = new TwitterDialogFragment();
                dialogFragment.setTargetFragment(this, TWITTER_FRAGMENT);
                dialogFragment.show(getFragmentManager(), "twitter");

            } else if (requestCode == TWITTER_FRAGMENT) {
                // result code > row_id
                // intent > tweet_text
                AsyncTwitter twitter = MyAsyncTwitter.getAsyncTwitter(getActivity(), resultCode);
                twitter.addListener(new TwitterAdapter() {

                    @Override
                    public void onException(TwitterException e, TwitterMethod twitterMethod) {
                        final String message = getString(R.string.twitter_error_toast) + ": " + e.getStatusCode() + "\n(" + e.getErrorMessage() + ")";
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getActivity(), message, Toast.LENGTH_LONG).show();
                            }
                        });
                    }

                    @Override
                    public void updatedStatus(Status status) {
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getActivity(), getString(R.string.twitter_tweet_toast), Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                });

                twitter.updateStatus(data.getStringExtra("tweet_text"));
            }

            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}