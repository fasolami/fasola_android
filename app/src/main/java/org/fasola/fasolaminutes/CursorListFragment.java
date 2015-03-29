package org.fasola.fasolaminutes;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.View;
import android.widget.CursorAdapter;
import android.widget.ListView;

import junit.framework.Assert;

/**
 * Simple framework for using queries as the base for a ListFragment
 * Call setQuery() with a String or SQL.Query to update the list
 *      Query should have id as the first column, and display columns afterwards
 *      To use sections and fastScroll, name a column IndexedCursorAdapter.INDEX_COLUMN
 * Call setIndexer/setAlphabet/setBins/setIntSections to initialize an Indexer
 * Call setItemLayout() to use a custom layout for list items
 *      Layout should use R.id.text[1-n] to provide text views
 *      The number of TextViews must be >= the number of display columns
 * Call setIntentActivity() to provide an Activity that will be started when an item is clicked
 */
public class CursorListFragment extends ListFragment
        implements LoaderManager.LoaderCallbacks<Cursor> {
    protected int mItemLayoutId = android.R.layout.simple_list_item_1;
    protected Class<?> mIntentClass;
    protected SQL.Query mQuery;
    protected String mSearchTerm = "";
    protected String[] mQueryParams;
    protected boolean mNeedsRangeIndexer;

    // Set the custom list item layout
    public void setItemLayout(int layoutId) {
        mItemLayoutId = layoutId;
    }

    // Set an Activity to start when an list item is clicked
    public void setIntentActivity(Class<?> cls) {
        mIntentClass = cls;
    }

    // Set the query and restart the loader (if the query has changed)
    public void setQuery(SQL.Query query, String... params) {
        boolean restartLoader = mQuery != null;
        mQuery = query;
        mQueryParams = params;
        if (! mSearchTerm.isEmpty()) { // Apply the current search
            String term = mSearchTerm;
            mSearchTerm = ""; // Clear so setSearch knows we don't have an existing search
            setSearch(term); // This will call restartLoader
        }
        else if (restartLoader)
            getLoaderManager().restartLoader(1, null, this);

    }

    // Override and change the query string
    public void onSearch(String query) {
    }

    public void setSearch(String query) {
        if (mQuery != null) {
            if (! mSearchTerm.isEmpty())
                mQuery = mQuery.popFilter();
            if (! query.isEmpty()) {
                mQuery = mQuery.pushFilter();
                onSearch(query); // Update the query
            }
            mSearchTerm = query;
            getLoaderManager().restartLoader(1, null, this);
        }
    }

    public void clearSearch() {
        setSearch("");
    }

    public SQL.Query getQuery() {
        return mQuery;
    }

    // Set an indexer
    public void setIndexer(LetterIndexer indexer) {
        mNeedsRangeIndexer = false;
        IndexedCursorAdapter adapter = ((IndexedCursorAdapter) getListAdapter());
        adapter.setIndexer(indexer);
        // Update fastscroll when the indexer changes
        if (adapter.getCursor() != null)
            setFastScrollEnabled(adapter.hasIndex() && adapter.hasIndexer());
    }

    // Indexer shortcuts
    public void setAlphabet(CharSequence alphabet) {
        setIndexer(new LetterIndexer(null, -1, alphabet));
    }

    public void setBins(int... bins) {
        setIndexer(new BinIndexer(null, -1, bins));
    }

    public void setBins(String... sections) {
        setIndexer(new StringIndexer(null, -1, sections));
    }

    // Set using a known range
    public void setRangeIndexer(int min, int max) {
        setIndexer(new RangeIndexer(null, -1, min, max));
    }

    // Delay until the query is performed and we can find a range
    public void setRangeIndexer() {
        mNeedsRangeIndexer = true;
    }

    // Get the minutes database
    public MinutesDb getDb() {
        return MinutesDb.getInstance(getActivity());
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Create and attach an empty adapter
        IndexedCursorAdapter adapter = new IndexedCursorAdapter(
            getActivity(), mItemLayoutId, null, null, null, 0);
        setListAdapter(adapter);
        // Start loading the cursor in the background
        getLoaderManager().initLoader(1, savedInstanceState, this);
    }

    protected void setFastScrollEnabled(boolean enabled) {
        getListView().setFastScrollEnabled(enabled);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        if (mIntentClass != null) {
            Intent intent = new Intent(getActivity(), mIntentClass);
            intent.putExtra(MainActivity.EXTRA_ID, id);
            startActivity(intent);
        }
    }

    // LoaderCallbacks
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(getActivity()) {
            @Override
            public Cursor loadInBackground() {
                return getDb().query(mQuery != null ? mQuery : "", mQueryParams);
            }
        };
    }

    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        // Setup any deferred section indexers
        if (mNeedsRangeIndexer) {
            setIndexer(new RangeIndexer(cursor, IndexedCursorAdapter.getIndexColumn(cursor)));
            mNeedsRangeIndexer = true; // setIndexer sets this to false
        }
        // Setup the CursorAdapter
        IndexedCursorAdapter adapter = (IndexedCursorAdapter) getListAdapter();
        if (cursor.getColumnCount() == 0) {
            adapter.changeCursor(null);
            return;
        }
        String[] from = getFrom(cursor);
        int[] to = getTo(from.length);
        adapter.changeCursorAndColumns(cursor, from, to);
        // Set fastScroll if we have an index column
        setFastScrollEnabled(adapter.hasIndex() && adapter.hasIndexer());
    }

    // For some reason this never gets called...
    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        ((CursorAdapter)getListAdapter()).changeCursor(null);
        setListAdapter(null);
    }

    // Static helper functions

    // Return an array of columns from the cursor (excluding the ID column)
    public static String[] getFrom(Cursor cursor) {
        // Assemble the column names based on query columns
        // Check for an index column, which is not included as a display column
        String[] from = new String[cursor.getColumnCount() - (IndexedCursorAdapter.hasIndex(cursor) ? 2 : 1)];
        int fromIdx = 0;
        for (int i = 1; i < cursor.getColumnCount(); ++i) {
            String columnName = cursor.getColumnName(i);
            if (! columnName.equals(SQL.INDEX_COLUMN))
                from[fromIdx++] = columnName;
        }
        return from;
    }

    // Return an array of View ids (using the pattern R.id.text[n])
    public static int[] getTo(int numberOfItems) {
        int[] to = new int[numberOfItems];
        for (int i = 0; i < numberOfItems; ++i) {
            switch (i) {
                case 0:
                    to[i] = android.R.id.text1;
                    break;
                case 1:
                    to[i] = android.R.id.text2;
                    break;
                // android doesn't include ids past text2, so this is a custom id
                case 2:
                    to[i] = R.id.text3;
                    break;
                default:
                    Assert.fail(String.format("ID: R.id.text%d does not exist", i));
            }
        }
        return to;
    }
}
