package org.fasola.fasolaminutes;

import android.app.SearchManager;
import android.app.SearchableInfo;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.SearchView;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;


public class MainActivity extends SimpleTabActivity {
    public final static String EXTRA_ID = "org.fasola.fasolaminutes.ID";
    public final static String ACTIVITY_POSITION = "org.fasola.fasolaminutes.POSITION";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Save all the pages since the queries may take some time to run
        mViewPager.setOffscreenPageLimit(mSectionsPagerAdapter.getCount());
        // Change title and FaSoLa tabs when the page changes
        final FasolaTabView tabs = (FasolaTabView) findViewById(R.id.fasola_tabs);
        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                setTitle(mSectionsPagerAdapter.getPageTitle(position));
                tabs.setSelection(position);
            }
        });
        // Initial settings
        setTitle(mSectionsPagerAdapter.getPageTitle(0));
        tabs.setSelection(0);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        // Get the SearchView and set the searchable configuration
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        final MenuItem searchItem = menu.findItem(R.id.menu_search);
        final SearchView searchView = (SearchView) searchItem.getActionView();
        // Collapse the search box when it is cleared
        searchView.findViewById(R.id.search_close_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                searchItem.collapseActionView();
            }
        });
        // Update search results as you type
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                searchView.clearFocus(); // Hide the keyboard
                onQueryTextChange(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String query) {
                CursorListFragment fragment = (CursorListFragment) getCurrentFragment();
                fragment.setSearch(query);
                return true;
            }

        });
        SearchableInfo info = searchManager.getSearchableInfo(getComponentName());
        searchView.setSearchableInfo(info);
        searchView.setIconifiedByDefault(false); // Do not iconify the widget; expand it by default
        return true;
    }

    public static class LeaderListFragment extends CursorStickyListFragment {
        protected int mSortId;
        protected final static String BUNDLE_SORT = "SORT_ID";

        public LeaderListFragment() {
            mIntentClass = LeaderActivity.class;
            mItemLayoutId = R.layout.leader_list_item;
        }

        // Change query/index based on the selected sort column
        public void updateQuery() {
            switch(mSortId) {
                case R.id.menu_leader_sort_count:
                    setQuery(C.Leader.selectList(C.Leader.fullName, C.Leader.leadCount.format("'(' || {column} || ')'"))
                                        .sectionIndex(C.Leader.leadCount, "DESC"));
                    setBins(0, 10, 50, 100, 500, 1000);
                    showHeaders(false);
                    break;
                case R.id.menu_leader_sort_entropy:
                    setQuery(C.Leader.selectList(C.Leader.fullName, C.Leader.entropy.format("'(' || ROUND({column}, 4) || ')'"))
                                     .sectionIndex(C.Leader.entropy.format("CAST({column} * 100 AS INT)"), "DESC"));
                    setBins(0,10,20,30,40,50,60,70,80,90);
                    showHeaders(false);
                    break;
                case R.id.menu_leader_sort_first_name:
                    setQuery(C.Leader.selectList(C.Leader.fullName, C.Leader.leadCount.format("'(' || {column} || ')'"))
                                  .sectionIndex(C.Leader.fullName, "ASC"));
                    setAlphabet("ABCDEFGHIJKLMNOPQRSTUVWXYZ");
                    showHeaders(true);
                    break;
                case R.id.menu_leader_sort_name:
                default:
                    setQuery(C.Leader.selectList(C.Leader.fullName, C.Leader.leadCount.format("'(' || {column} || ')'"))
                                  .sectionIndex(C.Leader.lastName, "ASC"));
                    setAlphabet("ABCDEFGHIJKLMNOPQRSTUVWXYZ");
                    showHeaders(true);
                    break;
            }
        }

        @Override
        public void onSearch(String query) {
            mQuery.leftJoin(C.Leader, C.LeaderAlias)
                .where(C.Leader.fullName, "LIKE", "%" + query + "%")
                .or(C.LeaderAlias.alias, "LIKE", "%" + query + "%");
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            setHasOptionsMenu(true);
            if (savedInstanceState != null)
                mSortId = savedInstanceState.getInt(BUNDLE_SORT, R.id.menu_leader_sort_name);
            else
                mSortId = R.id.menu_leader_sort_name;
            updateQuery();
        }

        @Override
        public void onSaveInstanceState(final Bundle saveInstanceState) {
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
    }

    public static class SongListFragment extends CursorStickyListFragment {
        protected int mSortId;
        protected final static String BUNDLE_SORT = "SORT_ID";

        public SongListFragment() {
            mIntentClass = SongActivity.class;
            mItemLayoutId = R.layout.song_list_item;
        }

        // Change query/index based on the selected sort column
        public void updateQuery() {
            switch(mSortId) {
                case R.id.menu_song_sort_title:
                    setQuery(C.Song.selectList(C.Song.number, C.Song.title,
                                      C.SongStats.leadCount.sum().format("'(' || {column} || ')'"))
                                .sectionIndex(C.Song.title, "ASC"));
                    setAlphabet("ABCDEFGHIJKLMNOPQRSTUVWXYZ");
                    showHeaders(true);
                    break;
                case R.id.menu_song_sort_leads:
                    setQuery(C.Song.selectList(C.Song.number, C.Song.title,
                                      C.SongStats.leadCount.sum().format("'(' || {column} || ')'"))
                                .sectionIndex(C.SongStats.leadCount.sum(), "DESC"));
                    setBins(100, 500, 1000, 1500, 2000, 2500, 3000);
                    showHeaders(false);
                    break;
                case R.id.menu_song_sort_page:
                default:
                    setQuery(C.Song.selectList(C.Song.number, C.Song.title,
                                      C.SongStats.leadCount.sum().format("'(' || {column} || ')'"))
                                .sectionIndex(C.Song.pageSort, "ASC"));
                    setBins(0, 100, 200, 300, 400, 500);
                    showHeaders(false);
                    break;
            }
        }

        @Override
        public void onSearch(String query) {
            mQuery.where(C.Song.fullName, "LIKE", "%" + query + "%")
                    .or(C.Song.lyrics, "LIKE", "%" + query + "%");
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            setHasOptionsMenu(true);
            if (savedInstanceState != null)
                mSortId = savedInstanceState.getInt(BUNDLE_SORT, R.id.menu_song_sort_page);
            else
                mSortId = R.id.menu_song_sort_page;
            updateQuery();
        }

        @Override
        public void onSaveInstanceState(final Bundle saveInstanceState) {
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
    }

    public static class SingingListFragment extends CursorStickyListFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            mIntentClass = SingingActivity.class;
            mItemLayoutId = android.R.layout.simple_list_item_2;
            mQuery = C.Singing.selectList(C.Singing.name, C.Singing.location)
                                .sectionIndex(C.Singing.year);
            setRangeIndexer();
        }

        @Override
        public void onSearch(String query) {
            mQuery.where(C.Singing.name, "LIKE", "%" + query + "%")
                    .or(C.Singing.location, "LIKE", "%" + query + "%");
        }
    }


    public static class SearchListFragment extends CursorStickyListFragment {
        public SearchListFragment() {
            mIntentClass = LeaderActivity.class;
            mItemLayoutId = android.R.layout.simple_list_item_1;
            mQuery = new SQL.Query("", "");
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
            mQuery = SQL.union(leaderQuery, singingQuery, songQuery).order(SQL.INDEX_COLUMN);
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
            intent.putExtra(MainActivity.EXTRA_ID, id);
            startActivity(intent);
        }
    }
}