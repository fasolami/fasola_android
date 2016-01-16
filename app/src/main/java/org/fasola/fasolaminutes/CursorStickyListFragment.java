package org.fasola.fasolaminutes;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;

import se.emilsjolander.stickylistheaders.StickyListHeadersListView;

/**
 * A drop-in replacement for CursorListFragment that uses stickyListHeaders
 */
public abstract class CursorStickyListFragment extends CursorListFragment {
    protected StickyListHeadersListView mStickyList;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return createListView(inflater, container, savedInstanceState);
    }

    @Override
    protected View createListView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_sticky_list, container, false);
        mStickyList = (StickyListHeadersListView) view.findViewById(R.id.sticky_list);
        // ListFragment wants a specific ID for its ListView
        mStickyList.getWrappedList().setId(android.R.id.list);
        return view;
    }

    public void showHeaders(boolean show) {
        IndexedCursorAdapter adapter = getListAdapter();
        if (adapter != null)
            adapter.showHeaders(show);
        mStickyList.setAreHeadersSticky(show);
    }

    // A few ListFragment Overrides to make StickyList work
    @Override
    public IndexedCursorAdapter getListAdapter() {
        // ListFragment will look for the adapter in the actual ListView, not the stickylist
        // so fix that here.
        return (IndexedCursorAdapter) mStickyList.getAdapter();
    }

    @Override
    public void setListAdapter(ListAdapter adapter) {
        mStickyList.setAdapter((IndexedCursorAdapter)adapter);
    }
}
