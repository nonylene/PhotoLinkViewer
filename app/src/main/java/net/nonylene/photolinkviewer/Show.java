package net.nonylene.photolinkviewer;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.net.Uri;
import android.net.http.HttpResponseCache;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import net.nonylene.photolinkviewer.fragment.OptionFragment;
import net.nonylene.photolinkviewer.fragment.ShowFragment;
import net.nonylene.photolinkviewer.fragment.VideoShowFragment;
import net.nonylene.photolinkviewer.tool.PLVUrl;
import net.nonylene.photolinkviewer.tool.PLVUrlService;
import net.nonylene.photolinkviewer.tool.ProgressBarListener;

import java.io.File;
import java.io.IOException;

public class Show extends Activity implements PLVUrlService.PLVUrlListener, ProgressBarListener {

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
        if (!Intent.ACTION_VIEW.equals(getIntent().getAction())) {
            Toast.makeText(this, "Intent Error!", Toast.LENGTH_LONG).show();
            return;
        }

        Uri uri = getIntent().getData();
        String url = uri.toString();

        Bundle bundle = new Bundle();
        bundle.putString("url", url);

        FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
        OptionFragment optionFragment = new OptionFragment();
        optionFragment.setArguments(bundle);
        fragmentTransaction.add(R.id.root_layout, optionFragment);
        fragmentTransaction.commit();

        PLVUrlService service = new PLVUrlService(this, this);
        service.requestGetPLVUrl(url);
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

    @Override
    public void onGetPLVUrlFinished(PLVUrl[] plvUrls) {
        Bundle bundle = new Bundle();
        bundle.putBoolean("single_frag", true);
        bundle.putParcelable("plvurl", plvUrls[0]);
        try {
            FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();

            if (plvUrls[0].isVideo()) {
                VideoShowFragment videoShowFragment = new VideoShowFragment();
                videoShowFragment.setArguments(bundle);
                fragmentTransaction.replace(R.id.show_frag_replace, videoShowFragment);
            } else {
                ShowFragment showFragment = new ShowFragment();
                showFragment.setArguments(bundle);
                fragmentTransaction.replace(R.id.show_frag_replace, showFragment);
            }
            fragmentTransaction.commit();

        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onGetPLVUrlFailed(String text) {
        Toast.makeText(Show.this, text, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onURLAccepted() {

    }

    @Override
    public void hideProgressBar() {
        findViewById(R.id.show_progress).setVisibility(View.GONE);
    }
}
