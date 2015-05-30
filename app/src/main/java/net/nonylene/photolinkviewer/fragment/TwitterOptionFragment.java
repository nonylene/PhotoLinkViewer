package net.nonylene.photolinkviewer.fragment;

import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import net.nonylene.photolinkviewer.tool.AccountsList;
import net.nonylene.photolinkviewer.tool.MyAsyncTwitter;
import net.nonylene.photolinkviewer.R;

import java.util.ArrayList;

import twitter4j.AsyncTwitter;
import twitter4j.Status;
import twitter4j.TwitterAdapter;
import twitter4j.TwitterException;
import twitter4j.TwitterMethod;

public class TwitterOptionFragment extends Fragment {
    private ImageButton retweet_button;
    private ImageButton favorite_button;
    private static final int FAVORITE_CODE = 1;
    private static final int RETWEET_CODE = 2;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final Bundle bundle = getArguments();
        // add twitter buttons
        View view = inflater.inflate(R.layout.twitter_option, container, false);
        retweet_button = (ImageButton) view.findViewById(R.id.retweet_button);
        retweet_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TwitterDialogFragment dialogFragment = new TwitterDialogFragment();
                dialogFragment.setArguments(bundle);
                dialogFragment.setTargetFragment(TwitterOptionFragment.this, RETWEET_CODE);
                dialogFragment.show(getFragmentManager(), "retweet");
            }
        });
        favorite_button = (ImageButton) view.findViewById(R.id.favorite_button);
        favorite_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TwitterDialogFragment dialogFragment = new TwitterDialogFragment();
                dialogFragment.setArguments(bundle);
                dialogFragment.setTargetFragment(TwitterOptionFragment.this, FAVORITE_CODE);
                dialogFragment.show(getFragmentManager(), "favorite");
            }
        });
        return view;
    }

    public static class TwitterDialogFragment extends DialogFragment {
        private int requestCode;

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // get request code
            requestCode = getTargetRequestCode();
            // get twitter id
            final Long id_long = getArguments().getLong("id_long");

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
            View view = View.inflate(getActivity(), R.layout.spinner_dialog, null);
            TextView textView = (TextView) view.findViewById(R.id.spinner_text);
            final Spinner spinner = (Spinner) view.findViewById(R.id.accounts_spinner);
            spinner.setAdapter(adapter);
            spinner.setSelection(screen_list.indexOf(screenName));

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            // change behave for request code
            switch (requestCode) {
                case FAVORITE_CODE:
                    textView.setText(getString(R.string.favorite_dialog_message));
                    builder.setTitle(getString(R.string.favorite_dialog_title));
                    break;
                case RETWEET_CODE:
                    textView.setText(getString(R.string.retweet_dialog_message));
                    builder.setTitle(getString(R.string.retweet_dialog_title));
            }
            builder.setView(view)
                    .setPositiveButton(getString(android.R.string.ok), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            int position = spinner.getSelectedItemPosition();
                            int row_id = row_id_list.get(position);
                            Intent intent = new Intent();
                            intent.putExtra("id_long", id_long);
                            getTargetFragment().onActivityResult(requestCode, row_id, intent);
                        }
                    })
                    .setNegativeButton(getString(android.R.string.cancel), null);
            return builder.create();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        long id_long = data.getLongExtra("id_long", -1);
        // request code > favorite or retweet
        // result code > row_id
        // intent > id_long
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
            public void createdFavorite(Status status) {
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getActivity(), getString(R.string.twitter_favorite_toast), Toast.LENGTH_LONG).show();
                    }
                });
            }

            @Override
            public void retweetedStatus(Status status) {
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getActivity(), getString(R.string.twitter_retweet_toast), Toast.LENGTH_LONG).show();
                    }
                });
            }
        });

        switch (requestCode) {
            case FAVORITE_CODE:
                twitter.createFavorite(id_long);
                break;

            case RETWEET_CODE:
                twitter.retweetStatus(id_long);
                break;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        retweet_button.setImageBitmap(null);
        favorite_button.setImageBitmap(null);
    }
}
