package net.nonylene.photolinkviewer;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import net.nonylene.photolinkviewer.fragment.OptionFragment;
import net.nonylene.photolinkviewer.fragment.ShowFragment;
import net.nonylene.photolinkviewer.fragment.VideoShowFragment;

public class Show extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show);
        //receive intent
        if (Intent.ACTION_VIEW.equals(getIntent().getAction())) {
            Bundle bundle = new Bundle();
            Uri uri = getIntent().getData();
            String url = uri.toString();
            bundle.putString("url", url);
            FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
            if (url.contains("vine.co")) {
                VideoShowFragment videoShowFragment = new VideoShowFragment();
                videoShowFragment.setArguments(bundle);
                fragmentTransaction.replace(android.R.id.content, videoShowFragment);
            } else {
                OptionFragment optionFragment = new OptionFragment();
                optionFragment.setArguments(bundle);
                fragmentTransaction.add(R.id.root_layout, optionFragment);
                ShowFragment showFragment = new ShowFragment();
                showFragment.setArguments(bundle);
                fragmentTransaction.replace(R.id.show_frag_replace, showFragment);
            }
            fragmentTransaction.commit();
        } else {
            Toast.makeText(this, "Intent Error!", Toast.LENGTH_LONG).show();
        }
    }

}
