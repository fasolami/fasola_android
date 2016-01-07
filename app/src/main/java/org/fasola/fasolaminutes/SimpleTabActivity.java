package org.fasola.fasolaminutes;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.util.Pair;
import android.view.InflateException;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewStub;
import android.widget.SearchView;

import com.astuetz.PagerSlidingTabStrip;

import java.util.ArrayList;
import java.util.List;

/**
 *  A base class for an activity with tabs.
 *  Use {@link android.support.v4.view.ViewPager} in the layout file, and define tabs using
 *  {@code <fragment android:title="label" android:name="FragmentClass">}
 */
public abstract class SimpleTabActivity extends BackActivity {
    /** PagerAdapter for ViewPager */
    SimplePagerAdapter mPagerAdapter;

    /** ViewPager (defined in layout) */
    ViewPager mViewPager;

    /** Optional Tab Strip header */
    PagerSlidingTabStrip mTabStrip;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getLayoutInflater().setFactory2(this); // Use our onCreateView function
        super.onCreate(savedInstanceState);
        mPagerAdapter = new SimplePagerAdapter(getSupportFragmentManager());
    }

    /**
     * Setup ViewPager and PagerSlidingTabStrip.
     * Remove temporary ViewStubs (=tabs) from ViewPager.
     */
    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);
        // Setup tabs
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.removeAllViews(); // Remove ViewStubs (added in custom onCreateView)
        mViewPager.setAdapter(mPagerAdapter);
        mPagerAdapter.notifyDataSetChanged();
        // Setup PagerSlidingTabStrip (if we have one)
        mTabStrip = (PagerSlidingTabStrip) findViewById(R.id.tab_strip);
        if (mTabStrip != null)
            mTabStrip.setViewPager(mViewPager);
        // Setup page change listener
        if (mTabStrip != null)
            mTabStrip.setOnPageChangeListener(mOwnPageChangeListener);
        else
            mViewPager.setOnPageChangeListener(mOwnPageChangeListener);
    }

    public Fragment getFragmentByPosition(int index) {
        String tag = "android:switcher:" + mViewPager.getId() + ":" + index;
        return getSupportFragmentManager().findFragmentByTag(tag);
    }

    public Fragment getCurrentFragment() {
        return getFragmentByPosition(mViewPager.getCurrentItem());
    }

    /**
     * Interface for fragments to respond to paging events.
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

    /**
     * Override to initialize a new fragment.  Defaults to setting Activity bundle args as extras.
     * @param fragment The new fragment.
     */
    public void onNewFragment(Fragment fragment) {
        fragment.setArguments(getIntent().getExtras());
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to one of the tabs.
     */
    protected class SimplePagerAdapter extends FragmentPagerAdapter {
        public List<Pair<String, Class<? extends Fragment>>> mTabs;

        public SimplePagerAdapter(FragmentManager fm) {
            super(fm);
            mTabs = new ArrayList<>();
        }

        public void addTab(String label, Class<? extends Fragment> fragmentClass) {
            mTabs.add(new Pair<String, Class<? extends Fragment>>(label, fragmentClass));
        }

        @Override
        public Fragment getItem(int position) {
            try {
                Fragment fragment = mTabs.get(position).second.newInstance();
                onNewFragment(fragment);
                return fragment;
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

        public int getFragmentIndex(Class<? extends Fragment> fragmentClass) {
            for (int i = 0; i < mTabs.size(); i++)
                if (mTabs.get(i).second == fragmentClass)
                    return i;
            return -1;
        }
    }

    /**
     * Use fragment tag to add Tabs to the ViewPager.
     */
    @Override
    @Nullable
    public View onCreateView(View parent, String name, @NonNull Context context, @NonNull AttributeSet attrs) {
        if (name.equals("fragment") && parent instanceof ViewPager) {
            String androidNamespace = "http://schemas.android.com/apk/res/android";
            String fname = attrs.getAttributeValue(androidNamespace, "name");
            int labelId = attrs.getAttributeResourceValue(androidNamespace, "title", 0);
            String label = labelId != 0
                    ? getResources().getString(labelId)
                    : attrs.getAttributeValue(androidNamespace, "title");
            try {
                mPagerAdapter.addTab(label, (Class<? extends Fragment>) Class.forName(fname));
            }
            catch (ClassNotFoundException e) {
                throw new InflateException(attrs.getPositionDescription()
                        + ": Invalid class: " + fname);
            }
            return new ViewStub(context);
        }
        return super.onCreateView(name, context, attrs);
    }

}
