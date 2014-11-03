package net.nonylene.photolinkviewer;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class OptionFragment extends Fragment {
    private View view;
    private Boolean open = false;
    private ImageButton baseButton;
    private ImageButton dlButton;
    private ImageButton setButton;
    private ImageButton webButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.option_fragment, container, false);
        baseButton = (ImageButton) view.findViewById(R.id.basebutton);
        dlButton = (ImageButton) view.findViewById(R.id.dlbutton);
        setButton = (ImageButton) view.findViewById(R.id.setbutton);
        webButton = (ImageButton) view.findViewById(R.id.webbutton);
        baseButton.setOnClickListener(new BaseButtonClickListener());
        dlButton.setOnClickListener(new DlButtonClickListener());
        setButton.setOnClickListener(new SetButtonClickListener());
        webButton.setOnClickListener(new WebButtonClickListener());
        return view;
    }

    class BaseButtonClickListener implements View.OnClickListener{

        public void onClick(View v){
            if (open){
                baseButton.setImageResource(R.drawable.up_button);
                dlButton.setVisibility(View.GONE);
                setButton.setVisibility(View.GONE);
                webButton.setVisibility(View.GONE);
                open = false;
            }else{
                baseButton.setImageResource(R.drawable.down_button);
                dlButton.setVisibility(View.VISIBLE);
                setButton.setVisibility(View.VISIBLE);
                webButton.setVisibility(View.VISIBLE);
                open = true;
            }
        }
    }

    class DlButtonClickListener implements View.OnClickListener{

        public void onClick(View v) {
            String sitename = getArguments().getString("sitename");
            String filename = getArguments().getString("filename");
            //save file
            ImageView imageView = (ImageView) getActivity().findViewById(R.id.imgview);
            //pickup bitmap
            Bitmap bitmap = ((BitmapDrawable) imageView.getDrawable()).getBitmap();
            File root = Environment.getExternalStorageDirectory();
            String directory = "PLViewer";
            File dir = new File(root, directory + "/" + sitename);
            Log.v("dir", dir.toString());
            //make directory
            dir.mkdirs();
            File path = new File(dir, filename + ".png");
            try {
                FileOutputStream fo = new FileOutputStream(path);
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fo);
                fo.close();
                Toast.makeText(getActivity(), "file saved to " + path.toString(), Toast.LENGTH_LONG).show();
            } catch (IOException e) {
                Log.e("error", e.toString());
            }
        }
    }

    class SetButtonClickListener implements View.OnClickListener{

        public void onClick(View v){
            //settings
            Intent intent = new Intent(getActivity(), Settings.class);
            startActivity(intent);
        }
    }

    class WebButtonClickListener implements View.OnClickListener{

        public void onClick(View v){
            String url = getArguments().getString("url");
            Uri uri = Uri.parse(url);
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            startActivity(intent);
        }
    }

}
