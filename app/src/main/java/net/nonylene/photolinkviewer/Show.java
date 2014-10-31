package net.nonylene.photolinkviewer;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

public class Show extends Activity {

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
            showFragment.setArguments(bundle);
            getFragmentManager().beginTransaction().replace(android.R.id.content,showFragment).commit();
            Log.v("showfrag","completed");
        } else {
            Toast.makeText(this, "Intent Error!", Toast.LENGTH_LONG).show();
        }
    }

    public static class InitDialogFragment extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle("Thank you for install")
                    .setMessage("対応サイト\nflickr, imgly, twipple, instagram, gyazo\n※gifには非対応\n\n操作方法\n" +
                            "URLのIntentから開いてください。ズームするのとtwitterはいつか対応するつもり。画像を長押しすることで元の" +
                            "URLに飛ぶことができます。saveを押すとPLViewerのディレクトリにpngとして保存されます。\n\n" +
                            "今のところSettingsは特に意味無いです。")
                    .setNeutralButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent intent = new Intent(getActivity(), TOAuth.class);
                            startActivity(intent);
                        }
                    });
            return builder.create();
        }
    }
}
