package net.nonylene.photolinkviewer;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import net.nonylene.photolinkviewer.fragment.PreferenceSummaryFragment;
import net.nonylene.photolinkviewer.tool.Initialize;

public class Settings extends AppCompatActivity {

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

        private SwitchPreference instagramSwitch;

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
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

            SharedPreferences preferences = getActivity().getSharedPreferences("preference", MODE_PRIVATE);
            instagramSwitch.setEnabled(preferences.getBoolean("instagram_authorized", false));

            return super.onCreateView(inflater, container, savedInstanceState);
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


        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);
            if (requestCode == 1) {
                // get instagram oauth result
                if (resultCode == IOAuthActivity.OAUTHED) {
                    instagramSwitch.setEnabled(true);
                    instagramSwitch.setEnabled(true);
                } else if (resultCode == IOAuthActivity.UNOAUTHED) {
                    instagramSwitch.setEnabled(false);
                    instagramSwitch.setChecked(false);
                }
            }
        }
    }
}