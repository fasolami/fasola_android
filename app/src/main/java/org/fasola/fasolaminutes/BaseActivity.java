/*
 * This file is part of FaSoLa Minutes for Android.
 * Copyright (c) 2016 Mike Richards. All rights reserved.
 */

package org.fasola.fasolaminutes;

import android.app.ActionBar;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.widget.DrawerLayout;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SearchView;

import java.util.List;

/**
 * The base class for this app's Activities.
 *
 * <p>Changes Up button to work as Back button.
 *
 * <p>Notifies Fragments in drawers when the are open or closed using setUserVisibleHint.
 * Prevents user from opening drawers by sliding from the edge of the screen.
 */
public class BaseActivity extends FragmentActivity implements DrawerLayout.DrawerListener {
    /** Intent action used to prompt the user for streaming. */
    public static final String PROMPT_STREAMING = "org.fasola.fasolaminutes.PROMPT_STREAMING";

    DrawerLayout mDrawerLayout;
    Fragment mLeftFragment;
    Fragment mRightFragment;

    // Save a reference to the current Fragment's SearchView so we can gracefully
    // deactivate any lingering handlers in invalidateOptionsMenu.
    SearchView mSearchView = null;
    boolean mHasActivitySearchView = false;

    // Help text
    int mHelpResourceId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (! isTaskRoot() && getActionBar() != null)
            getActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        handleIntent(getIntent(), savedInstanceState == null);
        super.onPostCreate(savedInstanceState);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent, false);
    }

    /**
     * Handles intents dispatched from onPostCreate and onNewIntent.
     *
     * @param intent Intent
     * @param isFirst is this the first intent passed to the activity?
     *                True when the application starts.
     */
    protected void handleIntent(Intent intent, boolean isFirst) {
        // Check for streaming prompt
        if (! isFirst && PROMPT_STREAMING.equals(intent.getAction()))
            ConnectionStatus.promptStreaming(this);
    }

    public void setHelpResource(int id) {
        mHelpResourceId = id;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        boolean ret = super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        Debug.createOptionsMenu(inflater, menu);
        mHasActivitySearchView = menu.findItem(R.id.menu_search) != null;
        if (mHelpResourceId != -1)
            inflater.inflate(R.menu.menu_help, menu);
        return ret;
    }

    // Set mSearchView
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean ret = super.onPrepareOptionsMenu(menu);
        MenuItem searchItem = menu.findItem(R.id.menu_search);
        if (searchItem == null)
            return ret;
        // Setup expand/collapse listeners for search items
        if (searchItem.getGroupId() == R.id.group_search_items) {
            final Menu theMenu = menu;
            searchItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
                @Override
                public boolean onMenuItemActionExpand(MenuItem item) {
                    updateMenuVisibility(true, theMenu);
                    return true;
                }

                @Override
                public boolean onMenuItemActionCollapse(MenuItem item) {
                    updateMenuVisibility(false, theMenu);
                    return true;
                }
            });
            updateMenuVisibility(searchItem.isActionViewExpanded(), theMenu);
        }

        // Don't save the search view if it belongs to the Activity
        if (mHasActivitySearchView)
            mSearchView = null;
        else
            mSearchView = (SearchView)searchItem.getActionView();
        return ret;
    }

    // Toggle visibility of all items that are not in id/menu_search_items
    private void updateMenuVisibility(boolean search, Menu menu) {
        for (int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            if (item.getGroupId() != R.id.group_search_items)
                item.setVisible(!search);
        }
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
            onUpPressed();
            return true;
        } else if (Debug.onOptionsItemSelected(this, item)) {
            return true;
        } else if (item.getItemId() == R.id.menu_help) {
            if (onDrawerOptionsItemSelected(item))
                return true;
            return HelpActivity.start(this, mHelpResourceId);
        }
        return super.onOptionsItemSelected(item);
    }

    protected boolean onDrawerOptionsItemSelected(MenuItem item) {
        Fragment drawer = getVisibleDrawer();
        if (drawer != null)
            return drawer.onOptionsItemSelected(item);
        return false;
    }

    public void onUpPressed() {
        onBackPressed();
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
        mDrawerLayout.setDrawerListener(this);
        // Find fragments in left and right drawers
        List<Fragment> fragmentList = getSupportFragmentManager().getFragments();
        if (fragmentList != null) {
            for (Fragment fragment : fragmentList) {
                if (fragment == null)
                    continue; // This can happen, e.g. with a dialog fragment
                View view = fragment.getView();
                while (view != null && view != mDrawerLayout && view.getParent() instanceof View) {
                    if (view.getLayoutParams() instanceof DrawerLayout.LayoutParams) {
                        int gravity = ((DrawerLayout.LayoutParams)view.getLayoutParams()).gravity;
                        if ((gravity & Gravity.LEFT) == Gravity.LEFT) {
                            fragment.setMenuVisibility(false);
                            fragment.setUserVisibleHint(false);
                            mLeftFragment = fragment;
                        }
                        if ((gravity & Gravity.RIGHT) == Gravity.RIGHT) {
                            fragment.setMenuVisibility(false);
                            fragment.setUserVisibleHint(false);
                            mRightFragment = fragment;
                        }
                        break;
                    }
                    view = (View)view.getParent();
                }
                if (mLeftFragment != null && mRightFragment != null)
                    break;
            }
        }
    }

    // Is this fragment part of a drawer?
    public boolean isDrawer(Fragment fragment) {
        return fragment == mLeftFragment || fragment == mRightFragment;
    }

    // Is this fragment a visible drawer?
    public boolean isDrawerVisible(Fragment fragment) {
        if (mDrawerLayout == null)
            return false;
        if (fragment == mLeftFragment)
            return mDrawerLayout.isDrawerOpen(Gravity.LEFT);
        else if (fragment == mRightFragment)
            return mDrawerLayout.isDrawerOpen(Gravity.RIGHT);
        return false;
    }

    // Is this fragment a visible drawer?
    public Fragment getVisibleDrawer() {
        if (mDrawerLayout == null)
            return null;
        if (mLeftFragment != null && mDrawerLayout.isDrawerOpen(Gravity.LEFT))
            return mLeftFragment;
        else if (mRightFragment != null && mDrawerLayout.isDrawerOpen(Gravity.RIGHT))
            return mRightFragment;
        return null;
    }

    // region DrawerLayout listeners
    //---------------------------------------------------------------------------------------------

    // Setup listeners that
    // * Lock the drawer when it is closed; and
    // * Call setUserVisibleHint and setMenuVisibility on fragments within drawers

    boolean mWasUpEnabled; // Was the up button enabled before the drawer was opened?
    CharSequence mOldTitle;
    CharSequence mOldSubtitle;

    @Override
    public void onDrawerClosed(View drawerView) {
        if (getActionBar() != null) {
            if (! mWasUpEnabled)
                getActionBar().setDisplayHomeAsUpEnabled(false);
            setTitle(mOldTitle);
            getActionBar().setSubtitle(mOldSubtitle);
        }
        mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, drawerView);
        Fragment fragment = getDrawerFragment(drawerView);
        if (fragment != null) {
            fragment.setMenuVisibility(false);
            fragment.setUserVisibleHint(false);
        }
    }

    @Override
    public void onDrawerOpened(View drawerView) {
        mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, drawerView);
        if (getActionBar() != null) {
            mWasUpEnabled = (getActionBar().getDisplayOptions() & ActionBar.DISPLAY_HOME_AS_UP) != 0;
            if (! mWasUpEnabled)
                getActionBar().setDisplayHomeAsUpEnabled(true);
            mOldTitle = getTitle();
            mOldSubtitle = getActionBar().getSubtitle();
        }
        Fragment fragment = getDrawerFragment(drawerView);
        if (fragment != null) {
            fragment.setMenuVisibility(true);
            fragment.setUserVisibleHint(true);
        }
    }

    @Override
    public void onDrawerSlide(View drawerView, float slideOffset) {
    }

    @Override
    public void onDrawerStateChanged(int newState) {
    }

    //---------------------------------------------------------------------------------------------
    // endregion DrawerLayout listeners


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
