package org.fasola.fasolaminutes;

import android.content.Context;
import android.database.DataSetObserver;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.MediaController;
import android.widget.TextView;

import com.mobeta.android.dslv.DragSortListView;

/**
 * A Fragment with a DragSortListView that shows a playlist
 */
public class PlaylistFragment extends ListFragment
        implements DragSortListView.DropListener,
                   MediaController.MediaPlayerControl {

    DragSortListView mList;
    MediaController mController;
    Playlist mPlaylist;

    public PlaylistFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_playlist, container, false);
        mList = (DragSortListView)view.findViewById(android.R.id.list);
        mController = (MediaController)view.findViewById(R.id.media_controller);
        mPlaylist = Playlist.getInstance();
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setListAdapter(new PlaylistListAdapter(getActivity(), mPlaylist));
        // Setup listener
        mList.setDropListener(this);
        // Setup MediaController
        mController.setMediaPlayer(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        mController.show(0); // Update controls
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

    public MediaPlayer getMediaPlayer() {
        return PlaybackService.getInstance().getMediaPlayer();
    }

    // MediaPlayerControls override
    public boolean isPrepared() {
        return PlaybackService.isRunning() && PlaybackService.getInstance().isPrepared();
    }

    @Override
    public void start() {
        getMediaPlayer().start();
    }

    @Override
    public void pause() {
        if (isPrepared())
            getMediaPlayer().pause();
    }

    @Override
    public int getDuration() {
        return isPrepared() ? getMediaPlayer().getDuration() : 0;
    }

    @Override
    public int getCurrentPosition() {
        return isPrepared() ? getMediaPlayer().getCurrentPosition() : 0;
    }

    @Override
    public void seekTo(int i) {
        if (isPrepared())
            getMediaPlayer().seekTo(i);
    }

    @Override
    public boolean isPlaying() {
        return isPrepared() && getMediaPlayer().isPlaying();
    }

    @Override
    public int getBufferPercentage() {
        return 0;
    }

    @Override
    public boolean canPause() {
        return true;
    }

    @Override
    public boolean canSeekBackward() {
        return true;
    }

    @Override
    public boolean canSeekForward() {
        return true;
    }

    @Override
    public int getAudioSessionId() {
        return 0;
    }
}
