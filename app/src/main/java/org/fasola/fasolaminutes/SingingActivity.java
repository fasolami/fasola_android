package org.fasola.fasolaminutes;

import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;


public class SingingActivity extends SimpleTabActivity {
    public MinutesContract.SingingDAO mSinging;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        long id = getIntent().getLongExtra(MainActivity.EXTRA_ID, -1);
        super.onCreate(savedInstanceState);
        // Query
        mSinging = C.Singing.get(id);
        if (mSinging != null) {
            String name = mSinging.name.getString();
            String location = mSinging.location.getString();
            int nSongs = mSinging.songCount.getInt();
            int nLeaders = mSinging.leaderCount.getInt();
            String songs = getResources().getQuantityString(R.plurals.songsLed, nSongs, nSongs);
            String leaders = getResources().getQuantityString(R.plurals.leaders, nLeaders, nLeaders);
            ((TextView) findViewById(R.id.title)).setText(name);
            ((TextView) findViewById(R.id.location)).setText(location);
            ((TextView) findViewById(R.id.songs)).setText(songs);
            ((TextView) findViewById(R.id.leaders)).setText(leaders);
        }
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_singing;
    }

    @Override
    protected void onCreateTabs() {
        addTab("Songs", SingingSongListFragment.class);
        addTab("Full Text", FullTextFragment.class);
    }

    public static class SingingSongListFragment extends CursorListFragment {
        public SingingSongListFragment() {
            mItemLayoutId = android.R.layout.simple_list_item_2;
            mIntentClass = SongActivity.class;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            long singingId = getActivity().getIntent().getLongExtra(MainActivity.EXTRA_ID, -1);
            SQL.Query query = C.Song.selectList(C.Song.fullName, C.Leader.fullName.func("group_concat", "', '"))
                                .whereEq(C.SongLeader.singingId)
                                .order(C.SongLeader.singingOrder, "ASC");
            setQuery(query, String.valueOf(singingId));
        }
    }

    public static class FullTextFragment extends Fragment {
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            return inflater.inflate(R.layout.fragment_singing_text, container, false);
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            // Query
            String fullText = ((SingingActivity) getActivity()).mSinging.fullText.getString();
            ((TextView) view.findViewById(R.id.full_text)).setText(fullText.replace("\n", "\n\n"));
            // Done
            super.onViewCreated(view, savedInstanceState);
        }
    }
}
