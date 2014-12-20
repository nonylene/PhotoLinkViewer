package net.nonylene.photolinkviewer;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.view.View;

public class PreferenceSummaryFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        summaryView();
    }

    private void summaryView() {
        PreferenceScreen screen = getPreferenceScreen();
        for (int i = 0; i < screen.getPreferenceCount(); i++) {
            Preference preference = screen.getPreference(i);
            if (preference instanceof PreferenceCategory) {
                // if category in preference, check recursive
                PreferenceCategory childCategory = (PreferenceCategory) preference;
                loopOnCategory(childCategory);
            } else {
                if (preference.getSummary() == null) {
                    setSummary(preference);
                }
            }
        }
    }

    private void loopOnCategory(PreferenceCategory category) {
        // if category in preference, check recursive
        for (int i = 0; i < category.getPreferenceCount(); i++) {
            Preference preference = category.getPreference(i);
            if (preference instanceof PreferenceCategory) {
                PreferenceCategory childCategory = (PreferenceCategory) preference;
                loopOnCategory(childCategory);
            } else {
                if (preference.getSummary() == null) {
                    setSummary(preference);
                }
            }
        }
    }

    private void setSummary(Preference preference) {
        if (preference.hasKey()) {
            if (preference instanceof ListPreference) {
                ListPreference listPreference = (ListPreference) preference;
                listPreference.setSummary(listPreference.getEntry());
            } else if (!(preference instanceof CheckBoxPreference) && !(preference instanceof SwitchPreference)) {
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
                preference.setSummary(preferences.getString(preference.getKey(), null));
            }
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Preference preference = findPreference(key);
        setSummary(preference);
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }
}
