package org.fasola.fasolaminutes;

import android.database.Cursor;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;


public class SingingActivity extends SimpleTabActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Query
        long id = getIntent().getLongExtra(CursorListFragment.EXTRA_ID, -1);
        SQL.Query query = C.Singing.select(C.Singing.name).whereEq(C.Singing.id);
        getSupportLoaderManager().initLoader(1, null, new MinutesLoader(query, String.valueOf(id)) {
            @Override
            public void onLoadFinished(Cursor cursor) {
                C.SingingDAO singing = C.Singing.fromCursor(cursor);
                if (singing != null)
                    setTitle(singing.name.getString());
            }
        });
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
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View root = inflater.inflate(R.layout.fragment_singing_songlist, container, false);
            inflateList(inflater, root, savedInstanceState);
            return root;
        }

        @Override
        public void onViewCreated(final View view, Bundle savedInstanceState) {
            setItemLayout(R.layout.singing_song_list_item);
            setIntentActivity(SongActivity.class);
            long id = getActivity().getIntent().getLongExtra(EXTRA_ID, -1);
            // Singing info query
            SQL.Query query = C.Singing.select(C.Singing.name, C.Singing.location,
                                               C.Singing.songCount, C.Singing.leaderCount)
                                        .whereEq(C.Singing.id);
            getLoaderManager().initLoader(1, null, new MinutesLoader(query, String.valueOf(id)) {
                @Override
                public void onLoadFinished(Cursor cursor) {
                    C.SingingDAO singing = C.Singing.fromCursor(cursor);
                    if (singing != null) {
                        String name = singing.name.getString();
                        String location = singing.location.getString();
                        int nSongs = singing.songCount.getInt();
                        int nLeaders = singing.leaderCount.getInt();
                        String songs = getResources().getQuantityString(R.plurals.songsLed, nSongs, nSongs);
                        String leaders = getResources().getQuantityString(R.plurals.leaders, nLeaders, nLeaders);
                        ((TextView) view.findViewById(R.id.title)).setText(name);
                        ((TextView) view.findViewById(R.id.location)).setText(location);
                        ((TextView) view.findViewById(R.id.songs)).setText(songs);
                        ((TextView) view.findViewById(R.id.leaders)).setText(leaders);
                    }
                }
            });
            // Song list query
            query = C.Song.selectList(C.Song.fullName, C.Leader.fullName.func("group_concat", "', '"))
                                .whereEq(C.SongLeader.singingId)
                                .order(C.SongLeader.singingOrder, "ASC");
            setQuery(query, String.valueOf(id));
            super.onViewCreated(view, savedInstanceState);
        }
    }

    public static class FullTextFragment extends Fragment {
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            return inflater.inflate(R.layout.fragment_singing_text, container, false);
        }

        @Override
        public void onViewCreated(final View view, Bundle savedInstanceState) {
            long id = getActivity().getIntent().getLongExtra(CursorListFragment.EXTRA_ID, -1);
            SQL.Query query = C.Singing.select(C.Singing.fullText).whereEq(C.Singing.id);
            getLoaderManager().initLoader(1, null, new MinutesLoader(query, String.valueOf(id)) {
                @Override
                public void onLoadFinished(Cursor cursor) {
                    C.SingingDAO singing = C.Singing.fromCursor(cursor);
                    if (singing != null) {
                        String text = singing.fullText.getString();
                        // TextView chokes on very long text (e.g. full text for camp)
                        // Fake it with a ListView of smaller TextViews for better performance
                        ListView list = (ListView) view.findViewById(R.id.full_text);
                        list.setAdapter(new ArrayAdapter<>(getActivity(),
                                                           R.layout.long_text_item,
                                                           text.split("\n")));
                    }
                }
            });
            super.onViewCreated(view, savedInstanceState);
        }
    }
}
