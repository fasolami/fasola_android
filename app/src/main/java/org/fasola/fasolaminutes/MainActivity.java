package org.fasola.fasolaminutes;

import android.app.SearchManager;
import android.app.SearchableInfo;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.content.Loader;
import android.support.v7.widget.SearchView;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;


public class MainActivity extends SimpleTabActivity {
    public final static String EXTRA_ID = "org.fasola.fasolaminutes.ID";

    @Override
    protected void onCreateTabs() {
        addTab(getString(R.string.tab_leaders).toUpperCase(), LeaderListFragment.class);
        addTab(getString(R.string.tab_songs).toUpperCase(), SongListFragment.class);
        addTab(getString(R.string.tab_singings).toUpperCase(), SingingListFragment.class);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        // Get the SearchView and set the searchable configuration
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) menu.findItem(R.id.menu_search).getActionView();
        // Assumes current activity is the searchable activity
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
        public SingingListFragment() {
            mIntentClass = SingingActivity.class;
            mItemLayoutId = android.R.layout.simple_list_item_2;
            mQuery = C.Singing.selectList(C.Singing.name, C.Singing.location)
                                .sectionIndex(C.Singing.year).toString();
        }

        public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
            super.onLoadFinished(loader, cursor);
            // Find the index column
            IndexedCursorAdapter adapter = getListAdapter();
            int indexColumn = adapter.getIndexColumn();
            // Find the min and max values (assume sorted)
            cursor.moveToFirst();
            int min = cursor.getInt(indexColumn);
            cursor.moveToLast();
            int max = cursor.getInt(indexColumn);
            cursor.moveToFirst();
            // Create sections
            String[] sections = new String[max-min+1];
            for (int i = min; i <= max; i++)
                sections[i-min] = Integer.toString(i);
            // Set the indexer
            setIndexer(new StringIndexer(cursor, indexColumn, sections));
        }
    }
}
