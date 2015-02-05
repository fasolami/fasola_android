package org.fasola.fasolaminutes;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import junit.framework.Assert;

/**
 * Simple framework for using queries as the base for a ListFragment
 * Must override getCursor() and return a Cursor
 *      Query should have _ID as the first column, and display columns afterwards
 * Set mItemLayoutId to a custom layout
 *      Layout should use R.id.text[1-n] to provide text views
 *      The number of TextViews must be >= the number of display columns
 * Set mIntentClass to provide an Activity that will be started when an item is clicked
 * In order to use an index and fastScroll, name a column IndexedCursorAdapter.INDEX_COLUMN
 *      This column will be excluded from display columns
 *      Set mAlphabet to use a custom alphabet
 */
public abstract class CursorListFragment extends ListFragment
        implements LoaderManager.LoaderCallbacks<Cursor> {
    protected int mItemLayoutId = android.R.layout.simple_list_item_1;
    protected Class<?> mIntentClass = null;
    protected String mAlphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    // Override and return a Cursor
    public abstract Cursor getCursor();

    // Shortcut to get the database
    public MinutesDb getDb() {
        return MinutesDb.getInstance(getActivity());
    }

    public static boolean hasIndex(Cursor cursor) {
        return cursor.getColumnIndex(IndexedCursorAdapter.INDEX_COLUMN) != -1;
    }

    // Return an array of columns from the cursor (excluding the ID column)
    public static String[] getFrom(Cursor cursor) {
        // Assemble the column names based on query columns
        // Check for an index column, which is not included as a display column
        String[] from = new String[cursor.getColumnCount() - (hasIndex(cursor) ? 2 : 1)];
        int fromIdx = 0;
        for (int i = 1; i < cursor.getColumnCount(); ++i) {
            String columnName = cursor.getColumnName(i);
            if (!columnName.equals(IndexedCursorAdapter.INDEX_COLUMN))
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

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getLoaderManager().initLoader(1, savedInstanceState, this);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        if (mIntentClass != null) {
            Intent intent = intent = new Intent(getActivity(), mIntentClass);
            intent.putExtra(MainActivity.EXTRA_ID, id);
            startActivity(intent);
        }
    }

    // LoaderCallbacks
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(getActivity()) {
            @Override
            public Cursor loadInBackground() {
                return getCursor();
            }
        };
    }

    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        // Create the cursor adapter
        String[] from = getFrom(cursor);
        int[] to = getTo(from.length);
        if (hasIndex(cursor)) {
            IndexedCursorAdapter adapter = new IndexedCursorAdapter(
                    getActivity(), mItemLayoutId, cursor, from, to, 0);
            // Create the indexer
            adapter.initIndexer(mAlphabet);
            // Attach adapter to the list
            setListAdapter(adapter);
            getListView().setFastScrollEnabled(true);
        } else {
            setListAdapter(new SimpleCursorAdapter(
                    getActivity(), mItemLayoutId, cursor, from, to, 0));
        }
    }

    public void onLoaderReset(Loader<Cursor> loader) {
        setListAdapter(null);
    }
}
