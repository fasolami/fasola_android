package org.fasola.fasolaminutes;

import android.app.ActionBar;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.widget.DrawerLayout;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SearchView;

import java.util.List;

/**
 *  The base class for this app's Activities.
 *
 * <p>Changes Up button to work as Back button.
 *
 * <p>Notifies Fragments in drawers when the are open or closed using setUserVisibleHint.
 * Prevents user from opening drawers by sliding from the edge of the screen.
 */
public class BackActivity extends FragmentActivity {
    DrawerLayout mDrawerLayout;
    Fragment mLeftFragment;
    Fragment mRightFragment;

    // Save a reference to the current Fragment's SearchView so we can gracefully
    // deactivate any lingering handlers in invalidateOptionsMenu.
    SearchView mSearchView = null;
    boolean mHasActivitySearchView = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (! isTaskRoot() && getActionBar() != null)
            getActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        boolean ret = super.onCreateOptionsMenu(menu);
        SQLiteDebugActivity.createOptionsMenu(getMenuInflater(), menu);
        mHasActivitySearchView = menu.findItem(R.id.menu_search) != null;
        return ret;
    }

    // Set mSearchView
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean ret = super.onPrepareOptionsMenu(menu);
        // Don't save the search view if it belongs to the Activity
        if (mHasActivitySearchView) {
            mSearchView = null;
            return ret;
        }
        MenuItem searchItem = menu.findItem(R.id.menu_search);
        if (searchItem == null)
            mSearchView = null;
        else
            mSearchView = (SearchView)searchItem.getActionView();
        return ret;
    }

    // Remove SearchView listeners because the SearchView will be collapsed and
    // cleared when the menu is invalidated.
    @Override
    public void invalidateOptionsMenu() {
        if (! mHasActivitySearchView && mSearchView != null)
            mSearchView.setOnQueryTextListener(null);
        super.invalidateOptionsMenu();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home && getActionBar() != null &&
                (getActionBar().getDisplayOptions() & ActionBar.DISPLAY_HOME_AS_UP) != 0) {
            // Use up button as back button
            onBackPressed();
            return true;
        }
        else if (SQLiteDebugActivity.handleOptionsItemSelected(this, item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        // Close drawers first
        if (mDrawerLayout != null) {
            if (mDrawerLayout.isDrawerOpen(Gravity.LEFT))
                mDrawerLayout.closeDrawer(Gravity.LEFT);
            else if (mDrawerLayout.isDrawerOpen(Gravity.RIGHT))
                mDrawerLayout.closeDrawer(Gravity.RIGHT);
            else
                super.onBackPressed();
        }
        else
            super.onBackPressed();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Setup drawer
        View drawerLayout = findViewById(R.id.drawer_layout);
        if (drawerLayout == null)
            return;
        mDrawerLayout = (DrawerLayout)drawerLayout;
        mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        // Setup listeners that
        // (A) Lock the drawer when it is closed; and
        // (B) Call setUserVisibleHint on fragments within drawers
        mDrawerLayout.setDrawerListener(new DrawerLayout.SimpleDrawerListener() {
            @Override
            public void onDrawerClosed(View drawerView) {
                Fragment fragment = getDrawerFragment(drawerView);
                if (fragment != null)
                    fragment.setUserVisibleHint(false);
                mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, drawerView);
                invalidateOptionsMenu();
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                Fragment fragment = getDrawerFragment(drawerView);
                if (fragment != null)
                    fragment.setUserVisibleHint(true);
                mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, drawerView);
                invalidateOptionsMenu();
            }
        });
        // Find fragments in left and right drawers
        List<Fragment> fragmentList = getSupportFragmentManager().getFragments();
        if (fragmentList != null) {
            for (Fragment fragment : fragmentList) {
                View view = fragment.getView();
                while (view != null && view != mDrawerLayout && view.getParent() instanceof View) {
                    if (view.getLayoutParams() instanceof DrawerLayout.LayoutParams) {
                        int gravity = ((DrawerLayout.LayoutParams)view.getLayoutParams()).gravity;
                        if ((gravity & Gravity.LEFT) != 0)
                            mLeftFragment = fragment;
                        if ((gravity & Gravity.RIGHT) != 0)
                            mRightFragment = fragment;
                        break;
                    }
                    view = (View)view.getParent();
                }
                if (mLeftFragment != null && mRightFragment != null)
                    break;
            }
        }
    }

    /** Returns the fragment in the given drawer (or null if none). */
    protected Fragment getDrawerFragment(int gravity) {
        return ((gravity & Gravity.RIGHT) != 0) ? mRightFragment : mLeftFragment;
    }

    /** Returns the fragment in a drawer given a drawer view (or null if none). */
    protected Fragment getDrawerFragment(View drawerView) {
        ViewGroup.LayoutParams lp = drawerView.getLayoutParams();
        if (lp instanceof DrawerLayout.LayoutParams)
            return getDrawerFragment(((DrawerLayout.LayoutParams) lp).gravity);
        return null;
    }
}
