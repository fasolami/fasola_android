package org.fasola.fasolaminutes;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.CursorAdapter;
import android.widget.ListView;

import junit.framework.Assert;

import java.util.ArrayList;

/**
 * Simple framework for using queries as the base for a ListFragment
 * Call setQuery() with a SQL.Query to update the list
 *      Query should have id as the first column, and display columns afterwards
 *      To use sections and fastScroll, name a column IndexedCursorAdapter.INDEX_COLUMN
 * Call setIndexer/setAlphabet/setBins/setIntSections to initialize an Indexer
 * Call setItemLayout() to use a custom layout for list items
 *      Layout should use R.id.text[1-n] to provide text views
 *      The number of TextViews must be >= the number of display columns
 * Call setIntentActivity() to provide an Activity that will be started when an item is clicked
 */
public class CursorListFragment extends ListFragment implements MinutesLoader.Callbacks {
    public final static String EXTRA_ID = "org.fasola.fasolaminutes.LIST_ID";

    protected int mItemLayoutId = android.R.layout.simple_list_item_1;
    protected Class<?> mIntentClass;
    protected MinutesLoader mMinutesLoader;
    protected String mSearchTerm = "";
    protected boolean mNeedsRangeIndexer;
    protected LetterIndexer mDeferredIndexer;
    private String BUNDLE_SEARCH = "SEARCH_TERM";
    private String LIST_STATE = "LIST_STATE";
    private Parcelable mListState;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            mSearchTerm = savedInstanceState.getString(BUNDLE_SEARCH, mSearchTerm);
            mListState = savedInstanceState.getParcelable(LIST_STATE);
        }
        // Setup the cursor loader
        mMinutesLoader = new MinutesLoader(this);
    }

    // Subclasses that want to override the default layout should provide a ViewStub with
    // id = android.R.id.list and call this method to inflate the default ListView
    protected void inflateList(LayoutInflater inflater, View container, Bundle savedInstanceState) {
        ViewStub stub = (ViewStub) container.findViewById(android.R.id.list);
        if (stub != null) {
            ViewGroup parent = (ViewGroup) stub.getParent();
            if (parent != null) {
                View listView = super.onCreateView(inflater, parent, savedInstanceState);
                // Replace the ViewStub with the ListView (code mostly lifted from ViewStub)
                final int index = parent.indexOfChild(stub);
                parent.removeViewInLayout(stub);
                final ViewGroup.LayoutParams layoutParams = stub.getLayoutParams();
                if (layoutParams != null)
                    parent.addView(listView, index, layoutParams);
                else
                    parent.addView(listView, index);
            }
        }
    }

    @Override
    public void onSaveInstanceState(final Bundle saveInstanceState) {
        super.onSaveInstanceState(saveInstanceState);
        saveInstanceState.putSerializable(BUNDLE_SEARCH, mSearchTerm);
        if (getView() != null) // Prevent IllegalStateException "Content view not yet created"
            saveInstanceState.putParcelable(LIST_STATE, getListView().onSaveInstanceState());
    }

    // Set the custom list item layout
    public void setItemLayout(int layoutId) {
        mItemLayoutId = layoutId;
    }

    // Set an Activity to start when an list item is clicked
    public void setIntentActivity(Class<?> cls) {
        mIntentClass = cls;
    }

    // Set the query and restart the loader
    public void setQuery(SQL.Query query, String... queryArgs) {
        mMinutesLoader.setQuery(query, queryArgs);
        // Apply the current search term
        if (! mSearchTerm.isEmpty()) {
            String term = mSearchTerm;
            mSearchTerm = ""; // Clear so setSearch knows we don't have an existing search term
            setSearch(term); // This will call restartLoader
        }
        else
            getLoaderManager().restartLoader(-1, null, mMinutesLoader);
    }

    public SQL.Query getQuery() {
        return mMinutesLoader.getQuery();
    }

    // Override and change the query string
    public void onSearch(SQL.Query query, String searchTerm) {
    }

    public void setSearch(String searchTerm) {
        if (mMinutesLoader.hasQuery()) {
            SQL.Query query = mMinutesLoader.getQuery();
            // Push or pop the new query filter
            if (! mSearchTerm.isEmpty())
                query = query.popFilter();
            if (! searchTerm.isEmpty()) {
                query = query.pushFilter();
                onSearch(query, searchTerm); // Update the query
            }
            // Set the new query and update
            mMinutesLoader.setQuery(query);
            getLoaderManager().restartLoader(-1, null, mMinutesLoader);
        }
        mSearchTerm = searchTerm;
    }

    public String getSearch() {
        return mSearchTerm;
    }

    public void setIndexer(LetterIndexer indexer) {
        mNeedsRangeIndexer = false;
        // Defer til onLoadFinished so we don't apply this indexer to the previous query
        mDeferredIndexer = indexer;
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

    public int getHighlight() {
        return getListAdapter().getHighlight();
    }

    public int setHighlight(Cursor cursor, String column, Object value) {
        if (cursor == null || ! cursor.moveToFirst())
            return -1;
        String val = value.toString();
        int columnIndex = cursor.getColumnIndex(column);
        do {
            if (cursor.getString(columnIndex).equals(val)) {
                setHighlight(cursor.getPosition());
                return cursor.getPosition();
            }
        } while (cursor.moveToNext());
        return -1;
    }

    public void setHighlight(int position) {
        ListView list = getListView();
        // I can't find another way to jump to a position without smooth-scrolling
        list.setSelection(position);
        getListAdapter().setHighlight(position);
        // Update the view's background if it is currently visible
        View view = list.getChildAt(position - list.getFirstVisiblePosition());
        if (view != null)
            getListAdapter().getView(position, view, getListView()); // NB: should reuse the view
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Create and attach an empty adapter
        IndexedCursorAdapter adapter = new IndexedCursorAdapter(
            getActivity(), mItemLayoutId, null, null, null, 0);
        setListAdapter(adapter);
        // Start loading the cursor in the background
        if (mMinutesLoader.hasQuery())
            getLoaderManager().initLoader(-1, null, mMinutesLoader);
    }

    protected void setFastScrollEnabled(boolean enabled) {
        getListView().setFastScrollEnabled(enabled);
    }

    // Cast to IndexedCursorAdapter
    @Override
    public IndexedCursorAdapter getListAdapter() {
        return (IndexedCursorAdapter) getListView().getAdapter();
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        if (mIntentClass != null) {
            Log.v("CursorListFragment", "Starting " + mIntentClass.getSimpleName() +
                                        " with id=" + String.valueOf(id));
            Intent intent = new Intent(getActivity(), mIntentClass);
            setIntentData(intent, position, id);
            startActivity(intent);
        }
    }

    // Override to add custom data to an intent
    protected void setIntentData(Intent intent, int position, long id) {
        intent.putExtra(EXTRA_ID, id);
    }

    //region Callbacks
    //-------------------------------------------------------------------------
    @Override
    public void onLoadFinished(Cursor cursor) {
        IndexedCursorAdapter adapter = getListAdapter();
        // Setup any deferred section indexers
        if (mNeedsRangeIndexer) {
            mDeferredIndexer = new RangeIndexer(cursor, IndexedCursorAdapter.getIndexColumn(cursor));
        }
        if (mDeferredIndexer != null) {
            adapter.setIndexer(mDeferredIndexer);
            mDeferredIndexer = null;
        }
        // Setup the CursorAdapter
        if (cursor.getColumnCount() == 0) {
            adapter.changeCursor(null);
            return;
        }
        String[] from = getFrom(cursor);
        int[] to = getTo(from.length);
        adapter.changeCursorAndColumns(cursor, from, to);
        // Set fastScroll if we have an index column
        setFastScrollEnabled(adapter.hasIndex() && adapter.hasIndexer());
        // Update list state
        if (mListState != null) {
            getListView().onRestoreInstanceState(mListState);
            mListState = null;
        }
    }

    @Override
    public void onLoaderReset() {
        getListAdapter().changeCursor(null);
        setListAdapter(null);
    }
    //endregion

    // Static helper functions

    // Return an array of columns from the cursor (excluding the ID column)
    public static String[] getFrom(Cursor cursor) {
        // Assemble the column names based on query columns
        // Check for an index column, which is not included as a display column
        ArrayList<String> from = new ArrayList<>();
        for (int i = 1; i < cursor.getColumnCount(); ++i) {
            String columnName = cursor.getColumnName(i);
            if (columnName.equals(SQL.INDEX_COLUMN)
                    || columnName.startsWith("__")) // Start excluded columns with __
                continue;
            from.add(columnName);
        }
        return from.toArray(new String[from.size()]);
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
