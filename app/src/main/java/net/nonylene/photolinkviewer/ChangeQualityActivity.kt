package net.nonylene.photolinkviewer

import android.app.Fragment
import android.app.FragmentManager
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v13.app.FragmentPagerAdapter
import android.support.v4.view.PagerAdapter
import android.support.v4.view.PagerTabStrip
import android.support.v4.view.ViewPager
import android.support.v7.app.AppCompatActivity

import net.nonylene.photolinkviewer.fragment.LTEFragment
import net.nonylene.photolinkviewer.fragment.WifiFragment

class ChangeQualityActivity : AppCompatActivity(), LTEFragment.OnWifiSwitchListener {
    private val adapter: QualityFragmentStateAdapter = QualityFragmentStateAdapter(fragmentManager)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_change_quality)

        (findViewById(R.id.quality_tab_strip) as PagerTabStrip).setTabIndicatorColorResource(R.color.primary_color)

        adapter.setWifiEnabled(
                PreferenceManager.getDefaultSharedPreferences(this).getBoolean("wifi_switch", false))
        (findViewById(R.id.quality_pager) as ViewPager).adapter = adapter
    }

    override fun onChanged(checked: Boolean) {
        // lte fragment listener when switch changed
        adapter.setWifiEnabled(checked)
    }

    private inner class QualityFragmentStateAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {
        private var titles: Array<String>? = null

        override fun getItem(i: Int): Fragment? {
            return when (i) {
                0 -> LTEFragment()
                1 -> WifiFragment()
                else -> null
            }
        }

        override fun getCount(): Int {
            return titles!!.size
        }

        override fun getItemPosition(any : Any?): Int {
            return PagerAdapter.POSITION_NONE
        }

        override fun getPageTitle(position: Int): CharSequence {
            return titles!![position]
        }

        fun setWifiEnabled(enabled: Boolean) {
            titles = if (enabled) arrayOf("LTE", "WIFI") else arrayOf("LTE / WIFI")
            notifyDataSetChanged()
        }
    }
}
