package net.nonylene.photolinkviewer;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import net.nonylene.photolinkviewer.fragment.PreferenceSummaryFragment;
import net.nonylene.photolinkviewer.tool.Initialize;

public class Settings extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (!preferences.getBoolean("initialized19", false)) {
            Initialize.initialize19(this);
        }
        getFragmentManager().beginTransaction().replace(android.R.id.content, new SettingsFragment()).commit();
    }

    public static class SettingsFragment extends PreferenceSummaryFragment {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.settings);
            Preference preference = findPreference("about_app_preference");
            preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    AboutDialogFragment dialogFragment = new AboutDialogFragment();
                    dialogFragment.show(getFragmentManager(), "about");
                    return false;
                }
            });
        }

        public static class AboutDialogFragment extends DialogFragment {
            // license etc

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
                } catch (PackageManager.NameNotFoundException e) {
                    Log.e("error", e.toString());
                    Toast.makeText(getActivity(), "Error occured", Toast.LENGTH_LONG).show();
                }
                return builder.create();
            }

        }
    }
}