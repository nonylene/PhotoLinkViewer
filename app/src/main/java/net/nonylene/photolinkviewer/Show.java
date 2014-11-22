package net.nonylene.photolinkviewer;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

public class Show extends Activity implements ShowFragment.OnFragmentInteractionListener {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show);
        SharedPreferences preferences = getSharedPreferences("preference", MODE_PRIVATE);
        if (!preferences.getBoolean("Initialized", false)) {
            DialogFragment fragment = new InitDialogFragment();
            fragment.show(getFragmentManager(), "show");
            preferences.edit().putBoolean("Initialized", true).apply();
        }
        //receive intent
        if (Intent.ACTION_VIEW.equals(getIntent().getAction())) {
            Bundle bundle = new Bundle();
            Uri uri = getIntent().getData();
            String url = uri.toString();
            bundle.putString("url", url);
            FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
            OptionFragment optionFragment = new OptionFragment();
            optionFragment.setArguments(bundle);
            fragmentTransaction.add(R.id.root_layout, optionFragment);
            ShowFragment showFragment = new ShowFragment();
            showFragment.setArguments(bundle);
            fragmentTransaction.replace(R.id.show_frag_replace, showFragment);
            fragmentTransaction.commit();
        } else {
            Toast.makeText(this, "Intent Error!", Toast.LENGTH_LONG).show();
        }
    }

    public static class InitDialogFragment extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle("Thank you for install")
                    .setMessage(getString(R.string.initial_dialog))
                    .setNeutralButton(getString(R.string.initial_dialog_ok), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent intent = new Intent(getActivity(), TOAuth.class);
                            startActivity(intent);
                        }
                    })
                    .setNegativeButton(getString(R.string.initial_dialog_no), null);
            return builder.create();
        }
    }

    public void onPurseFinished(final Bundle bundle) {
        // write process when called from fragment
    }

}
