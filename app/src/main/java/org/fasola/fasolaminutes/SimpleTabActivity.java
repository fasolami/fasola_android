package org.fasola.fasolaminutes;

import java.util.ArrayList;
import java.util.List;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;

import com.astuetz.PagerSlidingTabStrip;

/**
 *  A base class for an activity with tabs
 *  Extend this class, and override onCreateTabs()
 *  In this function, call addTab() to add a tab to your activity
 */
public class SimpleTabActivity extends FragmentActivity {
    /* Override to change the content view resource */
    protected int getLayoutId() {
        return R.layout.activity_tab;
    }

    /* Override to add tabs to the interface */
    protected void onCreateTabs() {
    }

    /* Call from onCreateTabs() to add tabs given a label and a fragment class */
    protected void addTab(String label, Class<? extends Fragment> fragmentClass) {
        mSectionsPagerAdapter.addTab(label, fragmentClass);
    }

    public Fragment getFragmentByPosition(int index) {
        String tag = "android:switcher:" + mViewPager.getId() + ":" + index;
        return getSupportFragmentManager().findFragmentByTag(tag);
    }

    public Fragment getCurrentFragment() {
        return getFragmentByPosition(mViewPager.getCurrentItem());
    }

    protected void removeTab(int index) {
        mSectionsPagerAdapter.removeTab(index);
    }

    // UI Stuff

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    ViewPager mViewPager;

    PagerSlidingTabStrip mTabStrip;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getLayoutId());

        // Create the adapter that will return a fragment for each of the
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        // Initialize tabs
        onCreateTabs();
        mSectionsPagerAdapter.notifyDataSetChanged();

        // If using tab strip, bind here
        mTabStrip = (PagerSlidingTabStrip) findViewById(R.id.tab_strip);
        if (mTabStrip != null) {
            mTabStrip.setViewPager(mViewPager);
        }
    }

    public void setOnPageChangeListener(ViewPager.OnPageChangeListener listener) {
        if (mTabStrip != null)
            mTabStrip.setOnPageChangeListener(listener);
        else
            mViewPager.setOnPageChangeListener(listener);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.menu_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */

    protected class SectionsPagerAdapter extends FragmentPagerAdapter {
        public List<Pair<String, Class<? extends Fragment>>> mTabs;

        public void addTab(String label, Class<? extends Fragment> fragmentClass) {
            mTabs.add(new Pair<String, Class<? extends Fragment>>(label, fragmentClass));
        }

        public void removeTab(int index) {
            mTabs.remove(index);
        }

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
            mTabs = new ArrayList<>();
        }

        @Override
        public Fragment getItem(int position) {
            try {
                return mTabs.get(position).second.newInstance();
            } catch (IndexOutOfBoundsException|InstantiationException|IllegalAccessException ex) {
                return null;
            }
        }

        @Override
        public int getCount() {
            return mTabs.size();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            try {
                return mTabs.get(position).first;
            } catch (IndexOutOfBoundsException ex) {
                return null;
            }
        }

        public Class<? extends Fragment> getFragmentClass(int position) {
            try {
                return mTabs.get(position).second;
            } catch (IndexOutOfBoundsException ex) {
                return null;
            }
        }

        public int getFragmentIndex(Class<? extends Fragment> fragmentClass) {
            for (int i = 0; i < mTabs.size(); i++)
                if (mTabs.get(i).second == fragmentClass)
                    return i;
            return -1;
        }
    }
}
