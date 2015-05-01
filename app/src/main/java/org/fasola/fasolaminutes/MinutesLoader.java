package org.fasola.fasolaminutes;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;

/**
 * A helper class that handles LoaderManager callbacks for a CursorLoader
 * To handle onLoadFinished and onLoaderReset, pass an implementation of Callbacks,
 * which are simplified from LoaderManager.Callbacks<Cursor>.
 */

// Declared outside the class so it can implement this interface
interface _MinutesLoaderCallbacksInterface {
    void onLoadFinished(Cursor cursor);
    void onLoaderReset();
}

public class MinutesLoader implements LoaderManager.LoaderCallbacks<Cursor>,
                                      Loader.OnLoadCompleteListener<Cursor>,
                                      _MinutesLoaderCallbacksInterface {
    public interface Callbacks extends _MinutesLoaderCallbacksInterface {
    }

    // Simplified callbacks
    public static abstract class FinishedCallback implements _MinutesLoaderCallbacksInterface {
        @Override
        public void onLoaderReset() {
            // Do nothing
        }
    }

    SQL.Query mQuery;
    String[] mQueryArgs;
    _MinutesLoaderCallbacksInterface mCallbacks;

    public MinutesLoader(_MinutesLoaderCallbacksInterface callbacks) {
        mCallbacks = callbacks;
    }

    public MinutesLoader(_MinutesLoaderCallbacksInterface callbacks, SQL.Query query,
                         String... queryArgs) {
        this(callbacks);
        setQuery(query, queryArgs);
    }

    // Constructors without Callbacks param use this
    public MinutesLoader() {
        mCallbacks = this;
    }

    public MinutesLoader(SQL.Query query, String... queryArgs) {
        this();
        setQuery(query, queryArgs);
    }

    // Callbacks
    //----------------
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(MinutesApplication.getContext()) {
            @Override
            public Cursor loadInBackground() {
                return MinutesDb.getInstance().query(mQuery != null ? mQuery : "", mQueryArgs);
            }
        };
    }

    // Either override these using an anonymous subclass of MinutesLoader, or pass an
    // implementation of Callbacks in the constructor.
    @Override
    public void onLoadFinished(Cursor cursor) {
    }

    @Override
    public void onLoaderReset() {
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        // Reset cursor position to before the first row in case this is an automatic call
        // from initLoader (i.e. we are using an existing cursor)
        cursor.moveToPosition(-1);
        mCallbacks.onLoadFinished(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mCallbacks.onLoaderReset();
    }

    // For loading without LoaderManager (e.g. in a Service)
    // -----------------------------------------------------
    private CursorLoader mLoader;
    public void startLoading() {
        startLoading(this);
    }

    public void startLoading(final _MinutesLoaderCallbacksInterface callback) {
        release();
        mLoader = (CursorLoader)onCreateLoader(1, null);
        mLoader.registerListener(1, new Loader.OnLoadCompleteListener<Cursor>() {
            @Override
            public void onLoadComplete(Loader<Cursor> loader, Cursor data) {
                callback.onLoadFinished(data);
                // Assume this is a single-shot loader if it's called without a LoaderManager
                mLoader.unregisterListener(this);
                release();
            }
        });
        mLoader.startLoading();
    }

    public void release() {
        if (mLoader != null) {
            mLoader.cancelLoad();
            mLoader.stopLoading();
            mLoader = null;
        }
    }

    @Override
    public void onLoadComplete(Loader<Cursor> loader, Cursor data) {
        onLoadFinished(loader, data);
    }

    // Query getters/setters
    //----------------------
    public SQL.Query getQuery() {
        return mQuery;
    }

    public void setQuery(SQL.Query query) {
        mQuery = query;
    }

    public void setQuery(SQL.Query query, String... queryArgs) {
        setQuery(query);
        setQueryArgs(queryArgs);
    }

    public boolean hasQuery() {
        return mQuery != null;
    }

    public String[] getQueryArgs() {
        return mQueryArgs;
    }

    public void setQueryArgs(String... queryArgs) {
        mQueryArgs = queryArgs;
    }
}
