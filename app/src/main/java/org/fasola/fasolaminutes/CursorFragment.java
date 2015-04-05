package org.fasola.fasolaminutes;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;

/**
 * Simple framework for using queries as the base for a Fragment
 * Call setQuery() with a SQL.Query
 * Override onLoaderFinished() to update the layout
 */
public class CursorFragment extends Fragment implements MinutesCursorLoader.LoaderCallbacks {
    protected MinutesCursorLoader mCursorLoader;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Setup the cursor loader
        mCursorLoader = new MinutesCursorLoader(getActivity(), getLoaderManager(), this);
    }

    // Set the query and start the loader
    public void setQuery(SQL.Query query, String... params) {
        mCursorLoader.setQueryAndLoad(query, params);
    }

    public SQL.Query getQuery() {
        return mCursorLoader.getQuery();
    }

    // Loader callbacks: override in the subclass
    @Override
    public void onLoadFinished(Cursor cursor) {
    }

    @Override
    public void onLoaderReset() {
    }
}