package net.nonylene.photolinkviewer.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Intent;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.SwitchPreference;

import net.nonylene.photolinkviewer.R;
import net.nonylene.photolinkviewer.dialog.BatchDialogFragment;

public class LTEFragment extends PreferenceSummaryFragment {
    OnWifiSwitchListener listener;

    // lister when switch changed
    public interface OnWifiSwitchListener {
        void onChanged(boolean checked);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.quality_setting_3g);
        // on click batch
        Preference preference = findPreference("quality_3g_batch");
        preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                // batch dialog
                BatchDialogFragment batchDialogFragment = new BatchDialogFragment();
                batchDialogFragment.setTargetFragment(LTEFragment.this, 1);
                batchDialogFragment.show(getFragmentManager(), "batch");
                return false;
            }
        });
        // on notes
        Preference note = findPreference("quality_note");
        note.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                // batch dialog
                NoteDialogFragment noteDialogFragment = new NoteDialogFragment();
                noteDialogFragment.show(getFragmentManager(), "batch");
                return false;
            }
        });
        // on switch changed
        SwitchPreference switchPreference = (SwitchPreference) findPreference("wifi_switch");
        switchPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                boolean value = Boolean.parseBoolean(newValue.toString());
                // go listener
                listener.onChanged(value);
                return true;
            }
        });
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        listener = (OnWifiSwitchListener) activity;
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
        ListPreference flickrPreference = (ListPreference) findPreference("flickr_quality_3g");
        ListPreference twitterPreference = (ListPreference) findPreference("twitter_quality_3g");
        ListPreference twipplePreference = (ListPreference) findPreference("twipple_quality_3g");
        ListPreference imglyPreference = (ListPreference) findPreference("imgly_quality_3g");
        ListPreference instagramPreference = (ListPreference) findPreference("instagram_quality_3g");
        ListPreference nicoPreference = (ListPreference) findPreference("nicoseiga_quality_3g");
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
                twipplePreference.setValue("large");
                imglyPreference.setValue("medium");
                instagramPreference.setValue("medium");
                nicoPreference.setValue("medium");
                break;
        }
    }

    public static class NoteDialogFragment extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(getString(R.string.notes_dialog_title))
                    .setMessage(getString(R.string.notes_about_quality))
                    .setPositiveButton(getString(android.R.string.ok), null);
            return builder.create();
        }

    }
}
