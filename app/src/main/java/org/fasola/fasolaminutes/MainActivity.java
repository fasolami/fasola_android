package org.fasola.fasolaminutes;

import android.app.SearchManager;
import android.app.SearchableInfo;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.content.Loader;
import android.support.v7.widget.SearchView;
import android.view.Menu;
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
        SearchView searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
        // Assumes current activity is the searchable activity
        SearchableInfo info = searchManager.getSearchableInfo(getComponentName());
        searchView.setSearchableInfo(info);
        searchView.setIconifiedByDefault(false); // Do not iconify the widget; expand it by default
        return true;
    }

    public static class LeaderListFragment extends CursorListFragment {
        public LeaderListFragment() {
            mIntentClass = LeaderActivity.class;
            mItemLayoutId = R.layout.leader_list_item;
        }

        @Override
        public Cursor getCursor() {
            return getDb().query(MinutesDb.LEADER_LIST_QUERY, null);
        }
    }

    public static class SongListFragment extends CursorListFragment {
        public SongListFragment() {
            mIntentClass = SongActivity.class;
            mItemLayoutId = R.layout.song_list_item;
            mAlphabet = "012345";
        }

        @Override
        public Cursor getCursor() {
            return getDb().query(MinutesDb.SONG_LIST_QUERY, null);
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
            super.onLoadFinished(loader, cursor);
            // Set custom labels for the indices
            IndexedCursorAdapter adapter = (IndexedCursorAdapter) getListAdapter();
            adapter.setSections("0-100", "100", "200", "300", "400", "500");
            // Must call this to update the view with new sections
            adapter.notifyDataSetChanged();
        }
    }

    public static class SingingListFragment extends CursorListFragment {
        public SingingListFragment() {
            mIntentClass = SingingActivity.class;
            mItemLayoutId = android.R.layout.simple_list_item_2;
        }

        @Override
        public Cursor getCursor() {
            return getDb().query(MinutesDb.SINGING_LIST_QUERY, null);
        }
    }
}
