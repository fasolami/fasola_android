package org.fasola.fasolaminutes;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.mobeta.android.dslv.DragSortListView;

import java.util.ArrayList;
import java.util.List;


/**
 * A Fragment with a DragSortListView that shows a playlist
 */
public class PlaylistFragment extends ListFragment implements DragSortListView.DropListener {
    DragSortListView mList;

    public PlaylistFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mList = (DragSortListView)inflater.inflate(R.layout.fragment_playlist, container, false);
        return mList;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Setup listener
        mList.setDropListener(this);
        // Connect to the playback service and display the playlist
        setListAdapter(new PlaylistListAdapter(getActivity()));
    }

    @Override
    public void onResume() {
        super.onResume();
        ((BaseAdapter)getListAdapter()).notifyDataSetChanged();
    }

    @Override
    public void drop(int from, int to) {
        /*
        String item = mItems.remove(from);
        mItems.add(to, item);
        ((BaseAdapter)getListAdapter()).notifyDataSetChanged();
        */
    }

    /**
     * Custom ListAdapter backed by the PlaybackService's playlist
     */
    static class PlaylistListAdapter extends BaseAdapter {
        List<PlaybackService.Song> mEmptyList = new ArrayList<>();
        Context mContext;
        LayoutInflater mInflater;

        public PlaylistListAdapter(Context context) {
            mContext = context;
            mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        List<PlaybackService.Song> getPlaylist() {
            PlaybackService service = PlaybackService.getInstance();
            return service == null ? mEmptyList : service.getPlaylist();
        }

        @Override
        public int getCount() {
            return getPlaylist().size();
        }

        @Override
        public Object getItem(int i) {
            return getPlaylist().get(i);
        }

        public PlaybackService.Song getSong(int i) {
            return (PlaybackService.Song)getItem(i);
        }

        @Override
        public long getItemId(int i) {
            return getSong(i).leadId;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            if (view == null)
                view = mInflater.inflate(R.layout.playlist_list_item, viewGroup, false);
            PlaybackService.Song song = getSong(i);
            ((TextView) view.findViewById(android.R.id.text1)).setText(song.name);
            ((TextView) view.findViewById(android.R.id.text2)).setText(song.singing);
            ((TextView) view.findViewById(R.id.text3)).setText(song.leaders);
            return view;
        }
    }
}
