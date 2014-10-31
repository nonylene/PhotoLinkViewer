package net.nonylene.photolinkviewer;

import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

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
        view = inflater.inflate(R.layout.fragment_option, container, false);
        baseButton = (ImageButton) view.findViewById(R.id.basebutton);
        dlButton = (ImageButton) view.findViewById(R.id.dlbutton);
        setButton = (ImageButton) view.findViewById(R.id.setbutton);
        webButton = (ImageButton) view.findViewById(R.id.webbutton);
        baseButton.setOnClickListener(new BaseButtonClickListener());
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

}
