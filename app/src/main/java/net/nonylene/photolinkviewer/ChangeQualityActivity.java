package net.nonylene.photolinkviewer;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;

public class ChangeQualityActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_quality);
        ActionBar actionBar = getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        actionBar.addTab(actionBar.newTab()
                        .setText("LTE")
                        .setTabListener(new MyTabListener(new LTEFragment()))
        );
        actionBar.addTab(actionBar.newTab()
                        .setText("Wifi")
                        .setTabListener(new MyTabListener(new WifiFragment()))
        );

    }


    class MyTabListener implements ActionBar.TabListener {
        private Fragment fragment;

        public MyTabListener(Fragment fragment) {
            this.fragment = fragment;
        }

        @Override
        public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
            ft.add(R.id.content, fragment);
        }

        @Override
        public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction ft) {
            ft.remove(fragment);
        }

        @Override
        public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft) {

        }
    }

    public static class LTEFragment extends PreferenceSummaryFragment {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.quality_setting_3g);
            Preference preference = findPreference("quality_3g_batch");
            preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    BatchDialogFragment batchDialogFragment = new BatchDialogFragment();
                    batchDialogFragment.setTargetFragment(LTEFragment.this, 1);
                    batchDialogFragment.show(getFragmentManager(),"batch");
                    return false;
                }
            });
        }

        public static class BatchDialogFragment extends DialogFragment {
            @Override
            public Dialog onCreateDialog(Bundle savedInstanceState) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                String[] items = getResources().getStringArray(R.array.quality);
                builder.setTitle(getString(R.string.quality_setting_dialogtitle))
                        .setSingleChoiceItems(items, -1, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                getTargetFragment().onActivityResult(getTargetRequestCode(), which, null);
                                dialog.dismiss();
                            }
                        });
                return builder.create();
            }

        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);
            switch (requestCode){
                case 1:
                    batchSelected(resultCode);
                    break;
            }
        }

        private void batchSelected(int resultCode){
            ListPreference flickrPreference = (ListPreference) findPreference("flickr_quality_3g");
            ListPreference twitterPreference = (ListPreference) findPreference("twitter_quality_3g");
            ListPreference twipplePreference = (ListPreference) findPreference("twipple_quality_3g");
            ListPreference imglyPreference = (ListPreference) findPreference("imgly_quality_3g");
            ListPreference instagramPreference = (ListPreference) findPreference("instagram_quality_3g");
            switch (resultCode){
                case 0:
                    flickrPreference.setValue("original");
                    twitterPreference.setValue("original");
                    twipplePreference.setValue("original");
                    imglyPreference.setValue("full");
                    instagramPreference.setValue("large");
                    break;
                case 1:
                    flickrPreference.setValue("large");
                    twitterPreference.setValue("large");
                    twipplePreference.setValue("large");
                    imglyPreference.setValue("large");
                    instagramPreference.setValue("large");
                    break;
                case 2:
                    flickrPreference.setValue("medium");
                    twitterPreference.setValue("medium");
                    twipplePreference.setValue("thumb");
                    imglyPreference.setValue("medium");
                    instagramPreference.setValue("medium");
                    break;
            }


        }
    }


    public static class WifiFragment extends PreferenceSummaryFragment {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.settings);
        }
    }
}
