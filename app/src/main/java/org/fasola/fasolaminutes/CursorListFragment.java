package org.fasola.fasolaminutes;

import android.content.Intent;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.HeaderViewListAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;

import junit.framework.Assert;

import java.util.ArrayList;

/**
 * ListFragment that displays SQL queries.
 *
 * <p>Call {@link #setItemLayout} to set the layout for list items.
 *
 * <p>Call {@link #setQuery} to update the list.
 *
 * <p>Call any of the following to initialize a section indexer:
 * <ul><li>{@link #setIndexer}
 * <li>{@link #setAlphabet}
 * <li>{@link #setBins}
 * <li>{@link #setRangeIndexer}
 * <li>{@link #setStringIndexer}
 * </ul>
 *
 * <p>Call {@link #setIntentActivity} to set the Activity to start when an item is clicked.
 */
public class CursorListFragment extends ListFragment
                                implements MinutesLoader.Callbacks,
                                           View.OnClickListener,
                                           View.OnLongClickListener {
    public final static String EXTRA_ID = "org.fasola.fasolaminutes.LIST_ID";
    public static final String AUDIO_COLUMN = "__sql_audio_column";

    // Indexers that must be constructed after we have a Cursor
    public final static int NO_INDEXER = 0;
    public final static int RANGE_INDEXER = 1;
    public final static int STRING_INDEXER = 2;
    public final static int BIN_INDEXER = 3;
    private int mBinCount;

    protected final static int DEFAULT_LAYOUT = android.R.layout.simple_list_item_1;
    protected Class<?> mIntentClass;
    protected MinutesLoader mMinutesLoader;
    protected String mSearchTerm = "";
    protected SQL.Query mOriginalQuery;
    protected int mSortId = -1;
    protected View mRecordingCountView;
    protected int mMenuResourceId = -1;
    protected int mDeferredIndexerType = NO_INDEXER;
    protected LetterIndexer mDeferredIndexer;
    protected String[] mSectionLabels;
    protected boolean mUseFastScroll = false;

    private static final String BUNDLE_SEARCH = "SEARCH_TERM";
    private static final String BUNDLE_SORT = "SORT_ID";
    private static final String LIST_STATE = "LIST_STATE";
    private Parcelable mListState;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            mSearchTerm = savedInstanceState.getString(BUNDLE_SEARCH, mSearchTerm);
            mSortId = savedInstanceState.getInt(BUNDLE_SORT, mSortId);
            mListState = savedInstanceState.getParcelable(LIST_STATE);
        }
        // Setup the cursor loader
        mMinutesLoader = new MinutesLoader(this);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Create and attach an empty adapter
        IndexedCursorAdapter adapter = getListAdapter();
        if (adapter == null) {
            adapter = new IndexedCursorAdapter(getActivity(), DEFAULT_LAYOUT);
            setListAdapter(adapter);
        }
        // Setup listeners for the play button
        adapter.setPlayClickListeners(this, this);
        // Start loading the cursor in the background
        if (mMinutesLoader.hasQuery())
            getLoaderManager().initLoader(-1, null, mMinutesLoader);
    }

    // Subclasses that want to override the default layout should provide a ViewStub with
    // id = android.R.id.list and call this method to inflate the default ListView
    protected void inflateList(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewStub stub = (ViewStub) container.findViewById(android.R.id.list);
        if (stub != null) {
            ViewGroup parent = (ViewGroup) stub.getParent();
            if (parent != null) {
                View listView = createListView(inflater, parent, savedInstanceState);
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

    // Override in a subclass to return a custom list view (see CursorStickyListFragment)
    protected View createListView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onSaveInstanceState(final Bundle saveInstanceState) {
        super.onSaveInstanceState(saveInstanceState);
        saveInstanceState.putSerializable(BUNDLE_SEARCH, mSearchTerm);
        saveInstanceState.putSerializable(BUNDLE_SORT, mSortId);
        if (getView() != null) // Prevent IllegalStateException "Content view not yet created"
            saveInstanceState.putParcelable(LIST_STATE, getListView().onSaveInstanceState());
    }

    /**
     * Sets the layout to use for list items.
     *
     * <p>Layout should include up to three {@code TextViews} with the following ids:
     * <ul><li>{@code android.R.id.text1}
     * <li>{@code android.R.id.text2}
     * <li>{@code R.id.text3} (NB: text3 is *not* in the android namespace)
     * </ul>
     *
     * @param layoutId resource id for list items
     * @see #setQuery
     */
    public void setItemLayout(int layoutId) {
        getListAdapter().setViewResource(layoutId);
    }

    /**
     * Sets an Activity to start when a list item is clicked.
     *
     * <p>Activity is started with an Intent with {@link #EXTRA_ID} set to the list item id.
     *
     * @param cls Activity class
     */
    public void setIntentActivity(Class<?> cls) {
        mIntentClass = cls;
    }

    /**
     * Starts loading a new query.
     *
     * <p>{@code query} should {@code SELECT} ID column first.
     * Subsequent columns are displayed using {@code TextViews} in the list item layout.
     *
     * <p>A section index can be specified using {@code SELECT column AS {{SQL.INDEX_COLUMN}}}.
     *
     * <p>A recording icon will be displayed for any records that have a non-null {@link #AUDIO_COLUMN}.
     *
     * @param query query to execute.
     * @param queryArgs args to substitute using ? placeholders in {@code query}.
     * @see #setItemLayout(int)
     */
    public void setQuery(SQL.Query query, String... queryArgs) {
        mMinutesLoader.setQuery(query, queryArgs);
        getLoaderManager().restartLoader(-1, null, mMinutesLoader);
    }

    /**
     * Sets a new search term using the given query.
     *
     * @param searchTerm the search string
     * @see #onUpdateSearch
     */
    public void setSearch(String searchTerm) {
        mSearchTerm = searchTerm;
        if (searchTerm.isEmpty())
            updateQuery();
        else
            setQuery(onUpdateSearch(mOriginalQuery.copy(), searchTerm));
    }

    /** Gets the search term. */
    public String getSearch() {
        return mSearchTerm;
    }

    /**
     * Override to respond to search events.
     *
     * @param query a copy of the existing query
     * @param searchTerm search term (will not be empty)
     * @return query that incorporates the search term
     */
    public SQL.Query onUpdateSearch(SQL.Query query, String searchTerm) {
        return query;
    }

    /**
     * Updates the query and starts loading.
     *
     * <p>Calls {@link #setSearch(String)} if there is an existing search term.
     */
    public void updateQuery() {
        mOriginalQuery = onUpdateQuery();
        // Apply the current search term
        if (mSearchTerm.isEmpty())
            setQuery(mOriginalQuery);
        else
            setSearch(mSearchTerm); // Expect setSearch() to call setQuery()
    }

    /**
     * Override to reset query after cancelling a search.
     *
     * @return the new query
     */
    public SQL.Query onUpdateQuery() {
        return mOriginalQuery;
    }

    /**
     * Sets a menu resource to be inflated by onCreateOptionsMenu.
     *
     * <p>If this menu has a group named {@code R.id.menu_group_sort}, this group will
     * be used for sort menu items, which will be managed by this class.
     *
     * @param id resource id
     * @see #getSortId()
     */
    public void setMenuResource(int id) {
        mMenuResourceId = id;
        if (id != -1)
            setHasOptionsMenu(true);
    }

    /** Sets the default sort item id. */
    public void setDefaultSortId(int id) {
        if (mSortId == -1)
            mSortId = id;
    }

    /** Returns the id for the selected sort item. */
    public int getSortId() {
        return mSortId;
    }

    // Create the menu specified in setMenuResource
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (mMenuResourceId != -1)
            inflater.inflate(mMenuResourceId, menu);
        // Check the initial sort
        MenuItem item = menu.findItem(mSortId);
        if (item != null)
            item.setChecked(true);
        else if (mSortId != -1)
            Log.w("CursorListFragment", "Invalid sortId specified");
    }


    // Set sort id
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getGroupId() == R.id.menu_group_sort) {
            item.setChecked(true);
            mSortId = item.getItemId();
            updateQuery();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Sets SearchView callbacks if a SearchView is found in the menu.
     */
    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        // Get the SearchView
        final MenuItem searchItem = menu.findItem(R.id.menu_search);
        if (searchItem == null)
            return;
        final SearchView searchView = (SearchView) searchItem.getActionView();
        // Expand/collapse before setting callbacks, because both expanding and collapsing
        // reset search text to blank
        searchView.setIconifiedByDefault(true);
        if (mSearchTerm != null && ! mSearchTerm.isEmpty())
            searchItem.expandActionView();
        else
            searchItem.collapseActionView();
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
                setSearch(query);
                return true;
            }
        });
        // Set cached search text
        searchView.setQuery(mSearchTerm, false);
    }

    void updateRecordingCount() {
        int count = getRecordingCount(getListAdapter().getCursor());
        if (count > 0) {
            if (mRecordingCountView == null) {
                mRecordingCountView = View.inflate(
                        getActivity(), R.layout.list_header_recording_count, null);
                // Play/enqueue click handlers
                mRecordingCountView.findViewById(R.id.play_recordings).setOnClickListener(
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                PlaybackService.playSongs(getActivity(),
                                        PlaybackService.ACTION_PLAY_MEDIA,
                                        getListAdapter().getCursor());
                            }
                        });
                mRecordingCountView.findViewById(R.id.enqueue_recordings).setOnClickListener(
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                PlaybackService.playSongs(getActivity(),
                                        PlaybackService.ACTION_ENQUEUE_MEDIA,
                                        getListAdapter().getCursor());
                            }
                        });
            }
            ((TextView)mRecordingCountView.findViewById(R.id.play_recordings)).setText(
                getResources().getQuantityString(R.plurals.play_songs, count, count)
            );
            ((TextView)mRecordingCountView.findViewById(R.id.enqueue_recordings)).setText(
                getResources().getQuantityString(R.plurals.enqueue_songs, count, count)
            );
            if (! (getListView().getAdapter() instanceof HeaderViewListAdapter))
                addHeaderView(mRecordingCountView, null, false);
            mRecordingCountView.setVisibility(View.VISIBLE);
        }
        else if (mRecordingCountView != null) {
            mRecordingCountView.setVisibility(View.GONE);
        }
    }

    /**
     * Sets the list SectionIndexer
     * @param indexer {@link LetterIndexer} subclass
     */
    public void setIndexer(LetterIndexer indexer) {
        // Defer til onLoadFinished so we don't apply this indexer to the previous query
        setDeferredIndexer(NO_INDEXER);
        mDeferredIndexer = indexer;
    }

    /**
     * Sets an indexer that is deferred til the Cursor is loaded.
     *
     * @param type one of {@code NO_INDEXER}, {@code RANGE_INDEXER}, or {@code STRING_INDEXER}
     */
    protected void setDeferredIndexer(int type) {
        mDeferredIndexerType = type;
        // Clear custom labels any time a new indexer is set
        setSectionLabels();
    }

    /**
     * Sets a {@link LetterIndexer}.
     *
     * @param alphabet each character is used as a section
     */
    public void setAlphabet(CharSequence alphabet) {
        setIndexer(new LetterIndexer(null, -1, alphabet));
    }

    /** Sets LetterIndexer with {@code " ABCDEFGHIJKLMNOPQRSTUVWXYZ"}. */
    public void setAlphabetIndexer() {
        setAlphabet(" ABCDEFGHIJKLMNOPQRSTUVWXYZ");
    }

    /**
     * Sets a {@link BinIndexer}.
     *
     * @param bins array of the lower range for each bin
     */
    public void setBins(int... bins) {
        setIndexer(new BinIndexer(null, -1, bins));
    }

    /**
     * Sets a {@link BinIndexer} using equal intervals
     *
     * @param binCount number of bins
     */
    public void setBinCount(int binCount) {
        mBinCount = binCount;
        setDeferredIndexer(BIN_INDEXER);
    }

    /**
     * Sets a {@link RangeIndexer} using a known range.
     *
     * @param min minimum extent of range
     * @param max maximium extent of range
     * @see #setRangeIndexer()
     */
    public void setRangeIndexer(int min, int max) {
        setIndexer(new RangeIndexer(null, -1, min, max));
    }

    /**
     * Sets a {@link RangeIndexer} using values from the cursor.
     *
     * @see #setRangeIndexer(int, int)
     */
    public void setRangeIndexer() {
        setDeferredIndexer(RANGE_INDEXER);
    }

    /**
     * Sets a {@link StringIndexer} using values found in the Cursor.
     */
    public void setStringIndexer() {
        setDeferredIndexer(STRING_INDEXER);
    }

    /**
     * Sets alternate labels for section headers.
     *
     * @param sections list of labels (must be the same length as indexer sections)
     */
    public void setSectionLabels(String... sections) {
        mSectionLabels = sections;
    }

    /**
     * Gets the highlighted row.
     *
     * @return index of highlighted item
     */
    public int getHighlight() {
        return getListAdapter().getHighlight();
    }

    /**
     * Highlights the first matching item in the list.
     *
     * @param cursor cursor to search
     * @param column column to search
     * @param value value to search for
     * @return index of highlighted item or -1 if no item matches
     * @see #setHighlight(int)
     */
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

    /**
     * Highlights an item in the list.
     *
     * @param position index of the item to highlight
     * @see #setHighlight(Cursor, String, Object)
     */
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

    /** Force use of fast scroll */
    protected void setFastScrollEnabled(boolean enabled) {
        mUseFastScroll = enabled;
        getListView().setFastScrollEnabled(enabled);
    }

    /**
     * Returns list adapter casted to {@code IndexedCursorAdapter}.
     * Handles HeaderViewListAdapter wrapping.
     */
    @Override
    public IndexedCursorAdapter getListAdapter() {
        ListAdapter adapter = getListView().getAdapter();
        if (adapter instanceof HeaderViewListAdapter)
            adapter = ((HeaderViewListAdapter)adapter).getWrappedAdapter();
        return (IndexedCursorAdapter)adapter;
    }

    // Backport addHeaderView at any time to versions < KITKAT
    public void addHeaderView(View v, Object data, boolean isSelectable) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT &&
                ! (getListView().getAdapter() instanceof HeaderViewListAdapter)) {
            IndexedCursorAdapter oldAdapter = getListAdapter();
            setListAdapter(null);
            getListView().addHeaderView(v, data, isSelectable);
            setListAdapter(oldAdapter);
        }
        else {
            getListView().addHeaderView(v, data, isSelectable);
        }
    }

    // region Loader Callbacks
    //---------------------------------------------------------------------------------------------
    @Override
    public void onLoadFinished(Cursor cursor) {
        IndexedCursorAdapter adapter = getListAdapter();
        // Setup any deferred section indexers
        if (mDeferredIndexerType == RANGE_INDEXER)
            mDeferredIndexer = new RangeIndexer(cursor, IndexedCursorAdapter.getIndexColumn(cursor));
        else if (mDeferredIndexerType == STRING_INDEXER)
            mDeferredIndexer = new StringIndexer(cursor, IndexedCursorAdapter.getIndexColumn(cursor));
        else if (mDeferredIndexerType == BIN_INDEXER)
            mDeferredIndexer = BinIndexer.equalIntervals(cursor, IndexedCursorAdapter.getIndexColumn(cursor), mBinCount);
        // Set the new indexer
        if (mDeferredIndexer != null) {
            mDeferredIndexer.setSectionLabels(mSectionLabels);
            adapter.setIndexer(mDeferredIndexer);
            mDeferredIndexer = null;
            mSectionLabels = null;
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
        getListView().setFastScrollEnabled(
                mUseFastScroll || (adapter.hasIndex() && adapter.hasIndexer()));
        // Update list state
        if (mListState != null) {
            getListView().onRestoreInstanceState(mListState);
            mListState = null;
        }

        updateRecordingCount();
    }

    @Override
    public void onLoaderReset() {
        if (getView() != null) {
            getListAdapter().changeCursor(null);
            setListAdapter(null);
        }
    }

    @Override
    public void onDestroy() {
        if (getView() != null) {
            getListAdapter().changeCursor(null);
            setListAdapter(null);
        }
        super.onDestroy();
    }

    //---------------------------------------------------------------------------------------------
    // endregion Loader Callbacks

    @Override
    public void onClick(View v) {
        int pos = -1;
        try {
            pos = getListView().getPositionForView(v);
        }
        catch (NullPointerException ex) {
            Log.v("CursorListFragment", "NullPointerException in getPositionForView(); " +
                                        "must have been scrolled out of position");
        }
        if (pos != -1)
            onPlayClick(v, pos);
    }

    @Override
    public boolean onLongClick(View v) {
        int pos = -1;
        try {
            pos = getListView().getPositionForView(v);
        }
        catch (NullPointerException ex) {
            Log.v("CursorListFragment", "NullPointerException in getPositionForView(); " +
                                        "must have been scrolled out of position");
        }
        if (pos != -1)
            return onPlayLongClick(v, pos);
        return false;
    }

    /**
     * Handle click on the play/recording icon.
     *
     * <p>Defaults to playing the song or songs.
     *
     * @param v the clicked view
     * @param position the index of the record in the cursor
     */
    public void onPlayClick(View v, int position) {
        Cursor cursor = getListAdapter().getCursor();
        int urlColumn = cursor.getColumnIndex(AUDIO_COLUMN);
        if (! cursor.moveToPosition(position))
            return;
        // Send the intent
        PlaybackService.playSongs(
                getActivity(),
                PlaybackService.ACTION_PLAY_MEDIA,
                cursor.getString(urlColumn));
    }

    /**
     * Handle long click on the play/recording icon.
     *
     * <p>Defaults to enqueuing the song or songs.
     *
     * @param v the clicked view
     * @param position the index of the record in the cursor
     */
    public boolean onPlayLongClick(View v, int position) {
        Cursor cursor = getListAdapter().getCursor();
        int urlColumn = cursor.getColumnIndex(AUDIO_COLUMN);
        if (! cursor.moveToPosition(position))
            return true;
        // Send the intent
        PlaybackService.playSongs(
                getActivity(),
                PlaybackService.ACTION_ENQUEUE_MEDIA,
                cursor.getString(urlColumn));
        return true;
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

    /**
     * Gets an array of column names from the cursor.
     *
     * <p>Excludes ID, SQL.INDEX_COLUMN, and any column that starts with "__".
     *
     * @param cursor the data cursor
     * @return array of String column names
     */
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

    /**
     * Gets an array of view IDs.
     *
     * <p>The following ids are used: {@code android.R.id.text1}, {@code android.R.id.text2},
     * {@code R.id.text3}
     *
     * @param numberOfItems the number of text items in this view.
     * @return an array of text item ids.
     */
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

    /**
     * Gets the number of audioUrls in a cursor.
     *
     * <p>AUDIO_COLUMN can be used to display a count below the recording icon,
     * or it can have an actual url for a single tune.  This function ignores
     * total recording counts, and instead returns the total number of
     * audio urls in the cursor.
     *
     * @param cursor cursor with AUDIO_COLUMN as one of the columns
     * @return total number of audioUrls in the cursor
     */
    public static int getRecordingCount(Cursor cursor) {
        if (cursor == null)
            return 0;
        int audioCol = cursor.getColumnIndex(AUDIO_COLUMN);
        if (audioCol == -1)
            return 0;
        int count = 0;
        int pos = cursor.getPosition();
        if (! cursor.moveToFirst())
            return 0;
        do {
            String url = cursor.getString(audioCol);
            if (! (url == null || url.isEmpty() || Character.isDigit(url.charAt(0))))
                count += 1;
        } while (cursor.moveToNext());
        cursor.moveToPosition(pos);
        return count;
    }
}
