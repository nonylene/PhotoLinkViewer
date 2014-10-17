package nonylene.net.photolinkviewer;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.Intent;
import android.content.Loader;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;


public class Show extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show);
        Button button1 = (Button) findViewById(R.id.button1);
        Button button2 = (Button) findViewById(R.id.button2);
        button1.setOnClickListener(new Button1ClickListener());
        button2.setOnClickListener(new Button2ClickListener());
        if (Intent.ACTION_VIEW.equals(getIntent().getAction())) {
            Uri uri = getIntent().getData();
            String url = uri.toString();
            String a = URLPurser(url);
            AsyncExecute hoge = new AsyncExecute();
            hoge.Start(a);
        }else{
            Toast.makeText(this,"Intent Error!", Toast.LENGTH_LONG).show();
        }
    }

    public String URLPurser(String url){
        if (url.contains("twipple")){
            Log.v("twipple", url);
            return("http://p.twipple.jp/show/orig/" + url.substring(url.indexOf(".jp/") + 4));
        }else{
            Log.v("default","hoge");
            return("https://pbs.twimg.com/media/Bz1FnXUCEAAkVGt.png:orig");
        }
    }


    class Button1ClickListener implements View.OnClickListener {
        public void onClick(View v) {
            ImageView imageView = (ImageView) findViewById(R.id.imgview);
            Bitmap bitmap = ((BitmapDrawable) imageView.getDrawable()).getBitmap();
            File root = Environment.getExternalStorageDirectory();
            String directory = "GetImgTst";
            String Name = "hoge.png";
            File dir = new File(root, directory);
            Log.v("dir", dir.toString());
            dir.mkdirs();
            File path = new File(dir, Name);
            try {
                FileOutputStream fo = new FileOutputStream(path);
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fo);
                fo.close();
            } catch (IOException e) {
                Log.e("error", e.toString());
            }
        }
    }

    class Button2ClickListener implements View.OnClickListener {
        public void onClick(View v) {
            Log.v("save", "clicked");
        }
    }

    public class AsyncExecute implements LoaderManager.LoaderCallbacks<Drawable> {


        public void Start(String url) {
            Bundle bundle = new Bundle();
            bundle.putString("url", url);
            getLoaderManager().initLoader(0, bundle, this);
        }

        @Override
        public Loader<Drawable> onCreateLoader(int id, Bundle bundle) {
            try {
                String c = bundle.getString("url");
                URL url = new URL(c);
                return new AsyncHttp(getApplicationContext(), url);

            } catch (IOException e) {
                Toast.makeText(Show.this, "fuck", Toast.LENGTH_LONG).show();
                return null;
            }
        }

        @Override
        public void onLoadFinished(Loader<Drawable> loader, Drawable drawable) {
            ImageView imageView = (ImageView) findViewById(R.id.imgview);
            imageView.setImageDrawable(drawable);
        }

        @Override
        public void onLoaderReset(Loader<Drawable> loader) {

        }
    }

}
