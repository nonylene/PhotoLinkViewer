package net.nonylene.photolinkviewer;

import android.app.FragmentTransaction;
import android.content.Intent;
import android.net.Uri;
import android.net.http.HttpResponseCache;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import net.nonylene.photolinkviewer.fragment.OptionFragment;
import net.nonylene.photolinkviewer.fragment.ShowFragment;
import net.nonylene.photolinkviewer.fragment.VideoShowFragment;

import java.io.File;
import java.io.IOException;

public class Show extends AppCompatActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show);

        //enable cache
        try {
            File httpCacheDir = new File(getApplicationContext().getCacheDir(), "http");
            long httpCacheSize = 10 * 1024 * 1024; // 10 MB
            HttpResponseCache.install(httpCacheDir, httpCacheSize);
        } catch (IOException e) {
            Log.d("cache", "HTTP response cache installation failed");
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
            if (url.contains("vine.co")) {
                bundle.putBoolean("single_frag", true);
                VideoShowFragment videoShowFragment = new VideoShowFragment();
                videoShowFragment.setArguments(bundle);
                fragmentTransaction.replace(R.id.show_frag_replace, videoShowFragment);
            } else {
                ShowFragment showFragment = new ShowFragment();
                showFragment.setArguments(bundle);
                fragmentTransaction.replace(R.id.show_frag_replace, showFragment);
            }
            fragmentTransaction.commit();
        } else {
            Toast.makeText(this, "Intent Error!", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        // flash cache
        HttpResponseCache cache = HttpResponseCache.getInstalled();
        if (cache != null) {
            cache.flush();
        }
    }

}
