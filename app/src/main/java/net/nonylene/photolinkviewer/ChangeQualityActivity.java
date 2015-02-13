package net.nonylene.photolinkviewer;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;

import net.nonylene.photolinkviewer.fragment.LTEFragment;
import net.nonylene.photolinkviewer.fragment.WifiFragment;

public class ChangeQualityActivity extends Activity implements LTEFragment.OnWifiSwitchListener {
    private ActionBar actionBar;
    private ActionBar.Tab lteTab;
    private ActionBar.Tab wifiTab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_quality);
        // set tabs
        actionBar = getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        lteTab = actionBar.newTab();
        lteTab.setTabListener(new MyTabListener(new LTEFragment()));
        wifiTab = actionBar.newTab();
        wifiTab.setText("Wifi")
                .setTabListener(new MyTabListener(new WifiFragment()));
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (preferences.getBoolean("wifi_switch", false)) {
            lteTab.setText("LTE");
            actionBar.addTab(lteTab);
            actionBar.addTab(wifiTab);
        } else {
            lteTab.setText("LTE / Wifi");
            actionBar.addTab(lteTab);
        }
    }

    @Override
    public void onChanged(boolean checked) {
        // lte fragment listener when switch changed
        if (checked) {
            if (actionBar.getTabCount() == 1) {
                lteTab.setText("LTE");
                actionBar.addTab(wifiTab);
            }
        } else {
            if (actionBar.getTabCount() == 2) {
                lteTab.setText("LTE / Wifi");
                actionBar.removeTab(wifiTab);
            }
        }
    }

    class MyTabListener implements ActionBar.TabListener {
        private Fragment fragment;

        public MyTabListener(Fragment fragment) {
            this.fragment = fragment;
        }

        @Override
        public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
            ft.add(R.id.content, fragment);
        }

        @Override
        public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction ft) {
            ft.remove(fragment);
        }

        @Override
        public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft) {

        }
    }

}
