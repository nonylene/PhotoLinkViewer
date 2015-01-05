package net.nonylene.photolinkviewer.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;

import net.nonylene.photolinkviewer.R;
import net.nonylene.photolinkviewer.dialog.BatchDialogFragment;

public class WifiFragment extends PreferenceSummaryFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.quality_setting_wifi);
        // on click batch
        Preference preference = findPreference("quality_wifi_batch");
        preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                // batch dialog
                BatchDialogFragment batchDialogFragment = new BatchDialogFragment();
                batchDialogFragment.setTargetFragment(WifiFragment.this, 1);
                batchDialogFragment.show(getFragmentManager(), "batch");
                return false;
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // batch dialog listener
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case 1:
                batchSelected(resultCode);
                break;
        }
    }

    private void batchSelected(int resultCode) {
        // change preferences in a lump
        ListPreference flickrPreference = (ListPreference) findPreference("flickr_quality_wifi");
        ListPreference twitterPreference = (ListPreference) findPreference("twitter_quality_wifi");
        ListPreference twipplePreference = (ListPreference) findPreference("twipple_quality_wifi");
        ListPreference imglyPreference = (ListPreference) findPreference("imgly_quality_wifi");
        ListPreference instagramPreference = (ListPreference) findPreference("instagram_quality_wifi");
        ListPreference nicoPreference = (ListPreference) findPreference("nicoseiga_quality_wifi");
        switch (resultCode) {
            case 0:
                flickrPreference.setValue("original");
                twitterPreference.setValue("original");
                twipplePreference.setValue("original");
                imglyPreference.setValue("full");
                instagramPreference.setValue("large");
                nicoPreference.setValue("original");
                break;
            case 1:
                flickrPreference.setValue("large");
                twitterPreference.setValue("large");
                twipplePreference.setValue("large");
                imglyPreference.setValue("large");
                instagramPreference.setValue("large");
                nicoPreference.setValue("large");
                break;
            case 2:
                flickrPreference.setValue("medium");
                twitterPreference.setValue("medium");
                twipplePreference.setValue("thumb");
                imglyPreference.setValue("medium");
                instagramPreference.setValue("medium");
                nicoPreference.setValue("medium");
                break;
        }


    }
}
