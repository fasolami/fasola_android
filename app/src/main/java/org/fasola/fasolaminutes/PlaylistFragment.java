package org.fasola.fasolaminutes;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.MediaController;
import android.widget.TextView;

import com.mobeta.android.dslv.DragSortListView;

/**
 * A Fragment with a DragSortListView that shows a playlist
 */
public class PlaylistFragment extends ListFragment
        implements DragSortListView.DropListener {

    DragSortListView mList;
    MediaController mController;
    PlaybackService.Control mPlayer;
    Playlist mPlaylist;

    public PlaylistFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        IntentFilter filter = new IntentFilter();
        filter.addAction(PlaybackService.BROADCAST_PREPARED);
        filter.addAction(PlaybackService.BROADCAST_COMPLETED);
        filter.addAction(PlaybackService.BROADCAST_ERROR);
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mReceiver, filter);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_playlist, container, false);
        mList = (DragSortListView)view.findViewById(android.R.id.list);
        mController = (MediaController)view.findViewById(R.id.media_controller);
        mPlaylist = Playlist.getInstance();
        mList.setEmptyView(view.findViewById(android.R.id.empty));
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setListAdapter(new PlaylistListAdapter(getActivity(), mPlaylist));
        // Setup listener
        mList.setDropListener(this);
        // Setup MediaController
        mPlayer = new PlaybackService.Control(getActivity());
        mController.setMediaPlayer(mPlayer);
        Playlist.getInstance().registerPlayingObserver(mPlaylistObserver);
        updateControls(); // Initial Setup
    }

    @Override
    public void onResume() {
        super.onResume();
        updateControls();
        ((BaseAdapter)getListAdapter()).notifyDataSetChanged();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Playlist.getInstance().unregisterPlayingObserver(mPlaylistObserver);
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mReceiver);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        mPlayer.start(position);
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

    protected void updateControls() {
        Playlist playlist = Playlist.getInstance();
        if (playlist.isEmpty()) {
            mController.setVisibility(View.GONE);
        }
        else {
            mController.setVisibility(View.VISIBLE);
            int pos = playlist.getPosition();
            mController.setPrevNextListeners(
                    pos < playlist.size() - 1 ? mPlayer.nextListener : null,
                    pos > 0 ? mPlayer.prevListener : null
            );
        }
    }

    // Receive PlaybackService broadcasts
    BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(PlaybackService.BROADCAST_PREPARED))
                mController.show(0);
        }
    };

    /**
     * Observer that sets or removes PrevNext listeners based on playlist state
     */
    DataSetObserver mPlaylistObserver = new DataSetObserver() {
        @Override
        public void onChanged() {
            updateControls();
        }
    };

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
            // Playing indicator
            int nowPlaying = 0;
            if (mPlaylist.getPosition() == i)
                nowPlaying = R.drawable.ic_play_indicator;
            ((TextView) view.findViewById(android.R.id.text1))
                    .setCompoundDrawablesWithIntrinsicBounds(nowPlaying, 0, 0, 0);
            return view;
        }

        // Playlist change observers
        @Override
        public void registerDataSetObserver(DataSetObserver observer) {
            mPlaylist.registerObserver(observer);
            mPlaylist.registerPlayingObserver(observer);
            super.registerDataSetObserver(observer);
        }

        @Override
        public void unregisterDataSetObserver(DataSetObserver observer) {
            mPlaylist.unregisterObserver(observer);
            mPlaylist.unregisterPlayingObserver(observer);
            super.unregisterDataSetObserver(observer);
        }
    }
}
