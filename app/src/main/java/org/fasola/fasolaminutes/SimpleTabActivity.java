package org.fasola.fasolaminutes;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.SearchView;

import com.astuetz.PagerSlidingTabStrip;

import java.util.ArrayList;
import java.util.List;

/**
 *  A base class for an activity with tabs
 *  Extend this class, and override onCreateTabs()
 *  In this function, call addTab() to add a tab to your activity
 */
public abstract class SimpleTabActivity extends FragmentActivity {
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

        // Setup page change listener
        if (mTabStrip != null)
            mTabStrip.setOnPageChangeListener(mOwnPageChangeListener);
        else
            mViewPager.setOnPageChangeListener(mOwnPageChangeListener);

    }

    /**
     * Interface for fragments to detect when paging occurs
     */
    public interface FragmentPagerListener {
        void onPageFocused();
        void onPageBlurred();
    }

    ViewPager.OnPageChangeListener mPageChangeListener = null;

    public void setOnPageChangeListener(ViewPager.OnPageChangeListener listener) {
        mPageChangeListener = listener;
    }

    /**
     * Save a reference to a Fragment's SearchView so we can gracefully deactivate
     * any lingering handlers in the OnPageChangeListener
     */
    SearchView mSearchView = null;
    boolean mHasActivitySearchView = false;

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean ret = super.onPrepareOptionsMenu(menu);
        // Don't save the search view if it belongs to the Activity
        if (mHasActivitySearchView)
            return ret;
        MenuItem searchItem = menu.findItem(R.id.menu_search);
        if (getCurrentFragment() == null && searchItem != null)
            mHasActivitySearchView = true;
        else
            mSearchView = searchItem != null ? (SearchView)searchItem.getActionView() : null;
        return ret;
    }

    /**
     * Custom OnPageChangeListener that handles {@link FragmentPagerListener} events.
     * All events pass through to listener set with {@link #setOnPageChangeListener}
     */
    ViewPager.OnPageChangeListener mOwnPageChangeListener = new ViewPager.OnPageChangeListener() {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            if (mPageChangeListener != null)
                mPageChangeListener.onPageScrolled(position, positionOffset, positionOffsetPixels);
        }

        // Save old position for onPageBlurred
        int lastPos = 0;

        /**
         * Remove SearchView listeners (because the SearchView will be collapsed and
         * cleared) before the page changes, and call {@link FragmentPagerListener#onPageFocused()}
         * and {@link FragmentPagerListener#onPageBlurred()}
         */
        @Override
        public void onPageSelected(int position) {
            if (! mHasActivitySearchView && mSearchView != null)
                mSearchView.setOnQueryTextListener(null);
            Fragment from = getFragmentByPosition(lastPos);
            if (from instanceof FragmentPagerListener)
                ((FragmentPagerListener)from).onPageFocused();
            Fragment to = getFragmentByPosition(position);
            if (to instanceof FragmentPagerListener)
                ((FragmentPagerListener)to).onPageBlurred();
            lastPos = position;
            if (mPageChangeListener != null)
                mPageChangeListener.onPageSelected(position);
        }

        @Override
        public void onPageScrollStateChanged(int state) {
            if (mPageChangeListener != null)
                mPageChangeListener.onPageScrollStateChanged(state);
        }
    };

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
