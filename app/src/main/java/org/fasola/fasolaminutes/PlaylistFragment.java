package org.fasola.fasolaminutes;

import android.content.Context;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.mobeta.android.dslv.DragSortListView;

/**
 * A Fragment with a DragSortListView that shows a playlist
 */
public class PlaylistFragment extends ListFragment
        implements DragSortListView.DropListener {

    DragSortListView mList;
    PlaybackService.Control mPlayer;
    Playlist mPlaylist;

    public PlaylistFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_playlist, container, false);
        mList = (DragSortListView)view.findViewById(android.R.id.list);
        mPlaylist = Playlist.getInstance();
        mList.setEmptyView(view.findViewById(android.R.id.empty));
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setListAdapter(new PlaylistListAdapter(getActivity(), mPlaylist));
        // Setup list
        mList.setDropListener(this);
        mList.setFastScrollEnabled(true);
        // Setup MediaController
        mPlayer = new PlaybackService.Control(getActivity());
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        if (isVisibleToUser && mList != null)
            mList.setSelection(mPlaylist.getPosition());
        super.setUserVisibleHint(isVisibleToUser);
    }

    @Override
    public void onResume() {
        super.onResume();
        ((BaseAdapter)getListAdapter()).notifyDataSetChanged();
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        mPlayer.start(position);
    }

    View.OnClickListener mRemoveClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int position = getListView().getPositionForView(v);
            mPlaylist.remove(position);
        }
    };

    @Override
    public void drop(int from, int to) {
        mPlaylist.move(from, to);
        ((BaseAdapter)getListAdapter()).notifyDataSetChanged();
    }

    /**
     * Custom ListAdapter backed by the PlaybackService's playlist
     */
    class PlaylistListAdapter extends BaseAdapter {
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
                view = mInflater.inflate(R.layout.list_item_playlist, viewGroup, false);
            // Text
            Playlist.Song song = getSong(i);
            ((TextView) view.findViewById(android.R.id.text1)).setText(song.name);
            ((TextView) view.findViewById(android.R.id.text2)).setText(song.singing);
            ((TextView) view.findViewById(R.id.text3)).setText(song.leaders);
            // Status indicator
            int iconResource = 0;
            if (mPlaylist.getPosition() == i)
                iconResource = R.drawable.ic_play_indicator;
            else if (mPlaylist.get(i) != null && mPlaylist.get(i).status == Playlist.Song.STATUS_ERROR)
                iconResource = R.drawable.ic_warning_amber_18dp;
            ((TextView) view.findViewById(android.R.id.text1))
                    .setCompoundDrawablesWithIntrinsicBounds(iconResource, 0, 0, 0);
            View image = view.findViewById(R.id.close);
            image.setOnClickListener(mRemoveClickListener);
            return view;
        }

        // Playlist change observers
        @Override
        public void registerDataSetObserver(DataSetObserver observer) {
            mPlaylist.registerObserver(new PlaylistObserver.Wrapper(observer));
            super.registerDataSetObserver(observer);
        }

        @Override
        public void unregisterDataSetObserver(DataSetObserver observer) {
            mPlaylist.unregisterObserver(new PlaylistObserver.Wrapper(observer));
            super.unregisterDataSetObserver(observer);
        }
    }
}
