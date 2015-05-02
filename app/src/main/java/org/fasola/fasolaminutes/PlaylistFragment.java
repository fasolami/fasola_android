package org.fasola.fasolaminutes;

import android.support.v4.app.ListFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import com.mobeta.android.dslv.DragSortListView;

import java.util.ArrayList;
import java.util.List;


/**
 * A Fragment with a DragSortListView that shows a playlist
 */
public class PlaylistFragment extends ListFragment implements DragSortListView.DropListener {

    List<String> mItems;
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
        mItems = new ArrayList<>();
        setListAdapter(new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, mItems));
        updateList();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateList();
    }

    private void updateList() {
        PlaybackService service = PlaybackService.getInstance();
        mItems.clear();
        if (service != null) {
            for (PlaybackService.Song song: service.getPlaylist())
                mItems.add(song.name + "\n" + song.singing + "\n" + song.leaders);
        }
        ((ArrayAdapter<String>)getListAdapter()).notifyDataSetChanged();
    }

    @Override
    public void drop(int from, int to) {
        String item = mItems.remove(from);
        mItems.add(to, item);
        ((ArrayAdapter<String>)getListAdapter()).notifyDataSetChanged();
    }
}
