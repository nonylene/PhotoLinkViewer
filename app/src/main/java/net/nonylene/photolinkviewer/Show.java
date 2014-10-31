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

public class Show extends Activity implements ShowFragment.OnFragmentInteractionListener{

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
            bundle.putString("url",url);
            ShowFragment showFragment = new ShowFragment();
            FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
            showFragment.setArguments(bundle);
            fragmentTransaction.replace(android.R.id.content, showFragment);
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
                    .setMessage("対応サイト\nflickr, imgly, twipple, instagram, gyazo, twitter\n※gifには非対応\n\n操作方法\n" +
                            "URLのIntentから開いてください。ズームするのはいつか対応するつもり。" +
                            "saveを押すとPLViewerのディレクトリにpngとして保存されます。\n\n" +
                            "今のところSettingsは特に意味無いです。\n\n" +
                            "twitterの画像を取得するには認証が必要です。ただまだ複数画像に対応してないので認証する必要性は少ないです。認証しますか？")
                    .setNeutralButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent intent = new Intent(getActivity(), TOAuth.class);
                            startActivity(intent);
                        }
                    })
                    .setNegativeButton("やめて", null);
            return builder.create();
        }
    }

    public void onPurseFinished (Bundle bundle){
        OptionFragment optionFragment = new OptionFragment();
        FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
        optionFragment.setArguments(bundle);
        fragmentTransaction.add(android.R.id.content,optionFragment);
        fragmentTransaction.commit();
    }
}
