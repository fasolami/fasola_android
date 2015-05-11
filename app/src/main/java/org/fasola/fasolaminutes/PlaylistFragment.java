package org.fasola.fasolaminutes;

import android.content.Context;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.mobeta.android.dslv.DragSortListView;

/**
 * A Fragment with a DragSortListView that shows a playlist
 */
public class PlaylistFragment extends ListFragment implements DragSortListView.DropListener {
    DragSortListView mList;
    Playlist mPlaylist;

    public PlaylistFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mList = (DragSortListView)inflater.inflate(R.layout.fragment_playlist, container, false);
        mPlaylist = Playlist.getInstance();
        return mList;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Setup listener
        mList.setDropListener(this);
        // Connect to the playback service and display the playlist
        setListAdapter(new PlaylistListAdapter(getActivity(), mPlaylist));
    }

    @Override
    public void onResume() {
        super.onResume();
        ((BaseAdapter)getListAdapter()).notifyDataSetChanged();
    }

    @Override
    public void drop(int from, int to) {
        int lastPos = mPlaylist.getPosition();
        mPlaylist.add(to, mPlaylist.remove(from));
        // Update now playing if we just moved the currently playing song
        // (otherwise Playlist handles it)
        if (lastPos == from)
            mPlaylist.moveToPosition(to);
        ((BaseAdapter)getListAdapter()).notifyDataSetChanged();
    }

    /**
     * Custom ListAdapter backed by the PlaybackService's playlist
     */
    static class PlaylistListAdapter extends BaseAdapter {
        Context mContext;
        LayoutInflater mInflater;
        Playlist mPlaylist;

        public PlaylistListAdapter(Context context, Playlist playlist) {
            mContext = context;
            mPlaylist = playlist;
            mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public int getCount() {
            return mPlaylist.size();
        }

        @Override
        public Object getItem(int i) {
            return mPlaylist.get(i);
        }

        public Playlist.Song getSong(int i) {
            return (Playlist.Song)getItem(i);
        }

        @Override
        public long getItemId(int i) {
            return getSong(i).leadId;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            if (view == null)
                view = mInflater.inflate(R.layout.playlist_list_item, viewGroup, false);
            // Text
            Playlist.Song song = getSong(i);
            ((TextView) view.findViewById(android.R.id.text1)).setText(song.name);
            ((TextView) view.findViewById(android.R.id.text2)).setText(song.singing);
            ((TextView) view.findViewById(R.id.text3)).setText(song.leaders);
            return view;
        }

        // Playlist change observers
        @Override
        public void registerDataSetObserver(DataSetObserver observer) {
            mPlaylist.registerDataSetObserver(observer);
            mPlaylist.registerPlayingObserver(observer);
            super.registerDataSetObserver(observer);
        }

        @Override
        public void unregisterDataSetObserver(DataSetObserver observer) {
            mPlaylist.unregisterDataSetObserver(observer);
            mPlaylist.unregisterPlayingObserver(observer);
            super.unregisterDataSetObserver(observer);
        }
    }
}
