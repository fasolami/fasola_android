package org.fasola.fasolaminutes;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;

/**
 * A helper class for CursorLoaders and LoaderManagers
 * To handle onLoadFinished and onLoaderReset, pass an implementation of LoaderCallbacks
 * in the second constructor.  NB: These are simplifed from LoaderManager.LoaderCallbacks<Cursor>
 */
public class MinutesCursorLoader implements LoaderManager.LoaderCallbacks<Cursor> {
    // Simplified callbacks
    public interface LoaderCallbacks {
        public void onLoadFinished(Cursor cursor);
        public void onLoaderReset();
    }

    Context mContext;
    LoaderManager mLoaderManager;
    SQL.Query mQuery;
    String[] mQueryParams;
    LoaderCallbacks mCallbacks;


    public MinutesCursorLoader(Context context, LoaderManager loaderManager) {
        mContext = context;
        mLoaderManager = loaderManager;
    }

    public MinutesCursorLoader(Context context, LoaderManager loaderManager, LoaderCallbacks callbacks) {
        this(context, loaderManager);
        mCallbacks = callbacks;
    }

    public void initLoader() {
        mLoaderManager.initLoader(1, null, this);
    }

    public void restartLoader() {
        mLoaderManager.restartLoader(1, null, this);
    }

    public MinutesDb getDb() {
        return MinutesDb.getInstance(mContext);
    }

    // LoaderCallbacks
    //----------------
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(mContext) {
            @Override
            public Cursor loadInBackground() {
                return getDb().query(mQuery != null ? mQuery : "", mQueryParams);
            }
        };
    }

    // Either override these using an anonymous subclass of MinutesCursorLoader, or pass an
    // implementation of LoaderCallbacks in the constructor.
    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (mCallbacks != null)
            mCallbacks.onLoadFinished(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        if (mCallbacks != null)
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

    public void setQuery(SQL.Query query, String... queryParams) {
        setQuery(query);
        setQueryParams(queryParams);
    }

    public void setQueryAndLoad(SQL.Query query, String... queryParams) {
        boolean doInit = mQuery == null;
        setQuery(query, queryParams);
        if (doInit)
            initLoader();
        else
            restartLoader();
    }

    public boolean hasQuery() {
        return mQuery != null;
    }

    public String[] getQueryParams() {
        return mQueryParams;
    }

    public void setQueryParams(String... queryParams) {
        mQueryParams = queryParams;
    }
}
