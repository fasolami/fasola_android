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
    public void onLoadFinished(Cursor cursor);
    public void onLoaderReset();
}

public class MinutesLoader implements LoaderManager.LoaderCallbacks<Cursor>, _MinutesLoaderCallbacksInterface {
    public interface Callbacks extends _MinutesLoaderCallbacksInterface {
    }

    // Simplified callbacks
    public abstract class FinishedCallback implements _MinutesLoaderCallbacksInterface {
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
        mCallbacks.onLoadFinished(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mCallbacks.onLoaderReset();
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
