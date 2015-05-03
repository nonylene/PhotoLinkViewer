package net.nonylene.photolinkviewer;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;

import net.nonylene.photolinkviewer.fragment.LTEFragment;
import net.nonylene.photolinkviewer.fragment.WifiFragment;

public class ChangeQualityActivity extends AppCompatActivity implements LTEFragment.OnWifiSwitchListener {
    private QualityFragmentStateAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_quality);
        // set tabs
        ViewPager pager = (ViewPager) findViewById(R.id.quality_pager);

        adapter = new QualityFragmentStateAdapter(getFragmentManager());

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        adapter.setWifiEnabled(preferences.getBoolean("wifi_switch", false));

        pager.setAdapter(adapter);
    }

    @Override
    public void onChanged(boolean checked) {
        // lte fragment listener when switch changed
        adapter.setWifiEnabled(checked);
    }

    private class QualityFragmentStateAdapter extends FragmentPagerAdapter {
        private String[] titles;

        public QualityFragmentStateAdapter(FragmentManager fm){
            super(fm);
        }

        @Override
        public Fragment getItem(int i) {
            switch (i){
                case 0:
                    return new LTEFragment();
                case 1:
                    return new WifiFragment();
            }
            return null;
        }

        @Override
        public int getCount() {
            return titles.length;
        }

        @Override
        public int getItemPosition(Object object) {
            return POSITION_NONE;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return titles[position];
        }

        public void setWifiEnabled(boolean enabled){
            if (enabled){
                titles = new String[]{"LTE", "Wifi"};
            }else{
                titles = new String[]{"LTE / Wifi"};
            }
            notifyDataSetChanged();
        }
    }
}
