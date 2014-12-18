package net.nonylene.photolinkviewer;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.view.View;
import android.widget.Checkable;

public class PreferenceSummaryFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener{


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        PreferenceScreen screen = getPreferenceScreen();
        for (int i = 0;i < screen.getPreferenceCount() ; i++){
            Preference preference = screen.getPreference(i);
            if (preference.hasKey() && preference.getSummary() == null) {
                if (preference instanceof ListPreference) {
                    ListPreference listPreference = (ListPreference) preference;
                    listPreference.setSummary(listPreference.getEntry());
                } else if (!(preference instanceof Checkable)) {
                    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
                    preference.setSummary(preferences.getString(preference.getKey(), null));
                }
            }
        }

    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Preference preference = getPreferenceScreen().findPreference(key);
        if (preference instanceof ListPreference){
            ListPreference listPreference = (ListPreference) preference;
            listPreference.setSummary(listPreference.getEntry());
        }else if(!(preference instanceof Checkable)){
            preference.setSummary(sharedPreferences.getString(key,null));
        }
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
