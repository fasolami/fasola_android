package org.fasola.fasolaminutes;

import android.app.SearchManager;
import android.app.SearchableInfo;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.widget.SearchView;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;

public class MainActivity extends SimpleTabActivity {
    public final static String ACTIVITY_POSITION = "org.fasola.fasolaminutes.POSITION";
    SearchView mSearchView;
    MenuItem mSearchItem;
    boolean mAllowSearchUpdates;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Save all the pages since the queries may take some time to run
        mViewPager.setOffscreenPageLimit(mSectionsPagerAdapter.getCount());
        // Change title, FaSoLa tabs, and search when the page changes
        final FasolaTabView tabs = (FasolaTabView) findViewById(R.id.fasola_tabs);
        setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                setTitle(mSectionsPagerAdapter.getPageTitle(position));
                tabs.setSelection(position);
                // Android will expand/collapse and change the SearchView text several times
                // in the process of rebuilding the menu (due to the newly focused Fragment).
                // Here we set an internal flag so that changes to the SearchView will not
                // be forwarded to the Fragment (and trigger a new db query).
                // The menu update ends with onPreparePanel(), at which point this flag is
                // turned back on, and subsequent searches will go through to the Fragment.
                mAllowSearchUpdates = false;
            }
        });
        // Initial settings
        setTitle(mSectionsPagerAdapter.getPageTitle(0));
        tabs.setSelection(0);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Kill the service
        stopService(new Intent(this, PlaybackService.class));
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // Change to the requested fragment (by position)
        int position = intent.getIntExtra(ACTIVITY_POSITION, -1);
        if (position != -1)
            mViewPager.setCurrentItem(position, true);
    }


    @Override
    protected void onCreateTabs() {
        addTab(getString(R.string.tab_leaders), LeaderListFragment.class);
        addTab(getString(R.string.tab_songs), SongListFragment.class);
        addTab(getString(R.string.tab_singings), SingingListFragment.class);
        //addTab("SEARCH", SearchListFragment.class);
    }

    public boolean onPreparePanel(int featureId, View view, Menu menu) {
        boolean ret = super.onPreparePanel(featureId, view, menu);
        // Update the SearchView with the searchTerm from the current Fragment
        final CursorListFragment fragment = (CursorListFragment) getCurrentFragment();
        if (fragment != null) {
            String searchTerm = fragment.getSearch();
            if (searchTerm != null && ! searchTerm.isEmpty())
                mSearchItem.expandActionView();
            else
                mSearchItem.collapseActionView();
            mSearchView.setQuery(searchTerm, false);
        }
        // Forward searches to the Fragment (see onPageSelected for a full explanation)
        mAllowSearchUpdates = true;
        return ret;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        // Get the SearchView and set the searchable configuration
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        mSearchItem = menu.findItem(R.id.menu_search);
        mSearchView = (SearchView) mSearchItem.getActionView();
        // Update search results as you type
        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                mSearchView.clearFocus(); // Hide the keyboard
                onQueryTextChange(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String query) {
                CursorListFragment fragment = (CursorListFragment) getCurrentFragment();
                if (mAllowSearchUpdates)
                    fragment.setSearch(query);
                return true;
            }

        });
        SearchableInfo info = searchManager.getSearchableInfo(getComponentName());
        mSearchView.setSearchableInfo(info);
        mSearchView.setIconifiedByDefault(true); // Do not iconify the widget; expand it by default
        return true;
    }

    public static class LeaderListFragment extends CursorStickyListFragment {
        protected int mSortId = R.id.menu_leader_sort_name;
        protected final static String BUNDLE_SORT = "SORT_ID";

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setIntentActivity(LeaderActivity.class);
            setItemLayout(R.layout.leader_list_item);
            if (savedInstanceState != null)
                mSortId = savedInstanceState.getInt(BUNDLE_SORT, mSortId);
            setHasOptionsMenu(true);
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            updateQuery();
        }

        @Override
        public void onSaveInstanceState(final Bundle saveInstanceState) {
            super.onSaveInstanceState(saveInstanceState);
            saveInstanceState.putSerializable(BUNDLE_SORT, mSortId);
        }

        @Override
        public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
            inflater.inflate(R.menu.menu_leader_fragment, menu);
            // Check the initial sort
            MenuItem item = menu.findItem(mSortId);
            if (item != null)
                item.setChecked(true);
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            // Sort
            if (item.getGroupId() == R.id.menu_group_leader_sort) {
                item.setChecked(true);
                mSortId = item.getItemId();
                updateQuery();
                return true;
            }
            return super.onOptionsItemSelected(item);
        }

        // Change query/index based on the selected sort column
        public void updateQuery() {
            switch(mSortId) {
                case R.id.menu_leader_sort_count:
                    setBins(0, 10, 50, 100, 500, 1000);
                    showHeaders(false);
                    setQuery(C.Leader.selectList(C.Leader.fullName, C.Leader.leadCount.format("'(' || {column} || ')'"))
                                        .sectionIndex(C.Leader.leadCount, "DESC"));
                    break;
                case R.id.menu_leader_sort_entropy:
                    setBins(0,10,20,30,40,50,60,70,80,90);
                    showHeaders(false);
                    setQuery(C.Leader.selectList(C.Leader.fullName, C.Leader.entropyDisplay.format("'(' || {column} || ')'"))
                                     .sectionIndex(C.Leader.entropy.format("CAST({column} * 100 AS INT)"), "DESC"));
                    break;
                case R.id.menu_leader_sort_first_name:
                    setAlphabet("ABCDEFGHIJKLMNOPQRSTUVWXYZ");
                    showHeaders(true);
                    setQuery(C.Leader.selectList(C.Leader.fullName, C.Leader.leadCount.format("'(' || {column} || ')'"))
                                  .sectionIndex(C.Leader.fullName, "ASC"));
                    break;
                case R.id.menu_leader_sort_name:
                default:
                    setAlphabet("ABCDEFGHIJKLMNOPQRSTUVWXYZ");
                    showHeaders(true);
                    setQuery(C.Leader.selectList(C.Leader.fullName, C.Leader.leadCount.format("'(' || {column} || ')'"))
                                  .sectionIndex(C.Leader.lastName, "ASC"));
                    break;
            }
        }

        @Override
        public void onSearch(SQL.Query query, String searchTerm) {
            query.where(C.Leader.fullName, "LIKE", "%" + searchTerm + "%")
                    .or(C.LeaderAlias.alias, "LIKE", "%" + searchTerm + "%");
        }
    }

    public static class SongListFragment extends CursorStickyListFragment {
        protected int mSortId = R.id.menu_song_sort_page;
        protected final static String BUNDLE_SORT = "SORT_ID";

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setIntentActivity(SongActivity.class);
            setItemLayout(R.layout.song_list_item);
            if (savedInstanceState != null) {
                mSortId = savedInstanceState.getInt(BUNDLE_SORT, mSortId);
            }
            setHasOptionsMenu(true);
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            updateQuery();
        }

        @Override
        public void onSaveInstanceState(final Bundle saveInstanceState) {
            super.onSaveInstanceState(saveInstanceState);
            saveInstanceState.putSerializable(BUNDLE_SORT, mSortId);
        }

        @Override
        public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
            inflater.inflate(R.menu.menu_song_fragment, menu);
            // Check the initial sort
            MenuItem item = menu.findItem(mSortId);
            if (item != null)
                item.setChecked(true);
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            // Sort
            if (item.getGroupId() == R.id.menu_group_song_sort) {
                item.setChecked(true);
                mSortId = item.getItemId();
                updateQuery();
                return true;
            }
            return super.onOptionsItemSelected(item);
        }

        // Change query/index based on the selected sort column
        public void updateQuery() {
            switch(mSortId) {
                case R.id.menu_song_sort_title:
                    setAlphabet("ABCDEFGHIJKLMNOPQRSTUVWXYZ");
                    showHeaders(true);
                    setQuery(C.Song.selectList(C.Song.number, C.Song.title,
                                      C.SongStats.leadCount.sum().format("'(' || {column} || ')'"))
                                .sectionIndex(C.Song.title, "ASC"));
                    break;
                case R.id.menu_song_sort_leads:
                    setBins(100, 500, 1000, 1500, 2000, 2500, 3000);
                    showHeaders(false);
                    setQuery(C.Song.selectList(C.Song.number, C.Song.title,
                                      C.SongStats.leadCount.sum().format("'(' || {column} || ')'"))
                                .sectionIndex(C.SongStats.leadCount.sum(), "DESC"));
                    break;
                case R.id.menu_song_sort_page:
                default:
                    setBins(0, 100, 200, 300, 400, 500);
                    showHeaders(false);
                    setQuery(C.Song.selectList(C.Song.number, C.Song.title,
                                      C.SongStats.leadCount.sum().format("'(' || {column} || ')'"))
                                .sectionIndex(C.Song.pageSort, "ASC"));
                    break;
            }
        }

        @Override
        public void onSearch(SQL.Query query, String searchTerm) {
            query.where(C.Song.fullName, "LIKE", "%" + searchTerm + "%")
                    .or(C.Song.lyrics, "LIKE", "%" + searchTerm + "%");
        }
    }

    public static class SingingListFragment extends CursorStickyListFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setIntentActivity(SingingActivity.class);
            setItemLayout(R.layout.singing_list_item);
            setQuery(C.Singing.selectList(C.Singing.name, C.Singing.startDate, C.Singing.location)
                                .sectionIndex(C.Singing.year));
            setRangeIndexer();
        }

        @Override
        public void onSearch(SQL.Query query, String searchTerm) {
            query.where(C.Singing.name, "LIKE", "%" + searchTerm + "%")
                    .or(C.Singing.location, "LIKE", "%" + searchTerm + "%");
        }
    }


    public static class SearchListFragment extends CursorStickyListFragment {
        public SearchListFragment() {
            mIntentClass = LeaderActivity.class;
            mItemLayoutId = android.R.layout.simple_list_item_1;
            setQuery(new SQL.Query("", ""));
        }

        public void onSearch(String query) {
            SQL.Query leaderQuery =
                C.Leader.selectList(C.Leader.fullName)
                    .sectionIndex(new SQL.QueryColumn("'Leader'"))
                    .where(C.Leader.fullName, "LIKE", "%" + query + "%");
            SQL.Query songQuery =
                C.Song.selectList(C.Song.fullName)
                    .sectionIndex(new SQL.QueryColumn("'Song'"))
                    .where(C.Song.title, "LIKE", "%" + query + "%");
            SQL.Query singingQuery =
                C.Singing.selectList(C.Singing.name)
                    .sectionIndex(new SQL.QueryColumn("'Singing'"))
                    .where(C.Singing.name, "LIKE", "%" + query + "%").or(C.Singing.location, "LIKE", "%" + query + "%");
            setQuery(SQL.union(leaderQuery, singingQuery, songQuery).order(SQL.INDEX_COLUMN));
            setBins("Leader", "Singing", "Song");
        }

        @Override
        public void onListItemClick(ListView l, View v, int position, long id) {
            Cursor cursor = getListAdapter().getCursor();
            cursor.moveToPosition(position);
            Intent intent;
            switch(cursor.getString(2)) {
                case "Singing":
                    intent = new Intent(getActivity(), SingingActivity.class);
                    break;
                case "Song":
                    intent = new Intent(getActivity(), SongActivity.class);
                    break;
                case "Leader":
                default:
                    intent = new Intent(getActivity(), LeaderActivity.class);
                    break;
            }
            intent.putExtra(EXTRA_ID, id);
            startActivity(intent);
        }
    }
}