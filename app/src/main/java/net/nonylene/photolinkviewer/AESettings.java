package net.nonylene.photolinkviewer;

import android.app.Activity;
import android.os.Bundle;

import net.nonylene.photolinkviewer.fragment.PreferenceSummaryFragment;

public class AESettings extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction().replace(android.R.id.content, new SettingsFragment()).commit();
    }

    public static class SettingsFragment extends PreferenceSummaryFragment {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.advanced_settings);
        }

    }
}