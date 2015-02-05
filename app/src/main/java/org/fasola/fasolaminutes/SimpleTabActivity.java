package org.fasola.fasolaminutes;

import java.util.ArrayList;
import java.util.List;

import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.FragmentPagerAdapter;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;

/**
 *  A base class for an activity with tabs
 *  Extend this class, and override onCreateTabs()
 *  In this function, call addTab() to add a tab to your activity
 */
public class SimpleTabActivity extends ActionBarActivity implements ActionBar.TabListener {
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

    /**
     * Call from onCreateTabs() to show tabs in the ActionBar
     * If you want a TabPagerStrip, you must add it to a custom layout and override getLayoutId()
     */
    protected void showTabs() {
        // Set up the action bar.
        final ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        // When swiping between different sections, select the corresponding
        // tab. We can also use ActionBar.Tab#select() to do this if we have
        // a reference to the Tab.
        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
            actionBar.setSelectedNavigationItem(position);
            }
        });

        // For each of the sections in the app, add a tab to the action bar.
        for (int i = 0; i < mSectionsPagerAdapter.getCount(); i++) {
            // Create a tab with text corresponding to the page title defined by
            // the adapter. Also specify this Activity object, which implements
            // the TabListener interface, as the callback (listener) for when
            // this tab is selected.
            actionBar.addTab(
                    actionBar.newTab()
                            .setText(mSectionsPagerAdapter.getPageTitle(i))
                            .setTabListener(this));
        }
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getLayoutId());

        // Create the adapter that will return a fragment for each of the
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Initialize tabs
        onCreateTabs();

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
        // When the given tab is selected, switch to the corresponding page in
        // the ViewPager.
        mViewPager.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
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

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
            mTabs = new ArrayList<Pair<String, Class<? extends Fragment>>>();
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
    }
}
