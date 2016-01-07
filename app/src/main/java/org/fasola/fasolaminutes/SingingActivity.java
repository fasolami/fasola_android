package org.fasola.fasolaminutes;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;


public class SingingActivity extends SimpleTabActivity {
    // Underscores so we can use this as an alias in SQL statements (Android doesn't like dots)
    public final static String EXTRA_LEAD_ID = "__SINGING_LEAD_ID";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_singing);
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

    public static class SingingSongListFragment extends CursorListFragment
                                                implements ListDialogFragment.Listener {
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View root = inflater.inflate(R.layout.fragment_singing_songlist, container, false);
            inflateList(inflater, (ViewGroup) root, savedInstanceState);
            return root;
        }

        @Override
        public void onViewCreated(final View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            setItemLayout(R.layout.singing_song_list_item);
            setIntentActivity(LeaderActivity.class);
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
                                .select(C.SongLeader.leadId).as(EXTRA_LEAD_ID)
                                .select(C.Leader.id.func("group_concat")).as("__leaderIds")
                                .select(C.SongLeader.audioUrl).as(CursorListFragment.AUDIO_COLUMN)
                                .whereEq(C.SongLeader.singingId)
                                .group(C.SongLeader.leadId)
                                .order(C.SongLeader.singingOrder, "ASC");
            setQuery(query, String.valueOf(id));
        }

        @Override
        public void onLoadFinished(Cursor cursor) {
            // Highlight the Intent's lead id
            long leadId = getActivity().getIntent().getLongExtra(EXTRA_LEAD_ID, -1);
            if (leadId > -1)
                setHighlight(cursor, EXTRA_LEAD_ID, leadId);
            super.onLoadFinished(cursor);
        }

        @Override
        public void onListItemClick(ListView l, View v, int position, long id) {
            Cursor cursor = getListAdapter().getCursor();
            if (cursor.moveToPosition(position)) {
                long leadId = cursor.getLong(cursor.getColumnIndex(EXTRA_LEAD_ID));
                // Check for multiple leaders
                int idColumn = cursor.getColumnIndex("__leaderIds");
                String[] leaderIds = cursor.getString(idColumn).split(",");
                if (leaderIds.length == 1) {
                    // Single leader: start the activity
                    sendIntent(Long.parseLong(leaderIds[0]), leadId);
                }
                else {
                    // Multiple leaders: prompt
                    int leaderColumn = 2;
                    String[] names = cursor.getString(leaderColumn).split(", ");
                    // Add data
                    Bundle data = new Bundle();
                    data.putLong("leadId", leadId);
                    data.putStringArray("leaderIds", leaderIds);
                    // show the dialog
                    ListDialogFragment dialog = ListDialogFragment.newInstance(
                        R.string.select_leader, names, data);
                    dialog.setTargetFragment(this, 1);
                    dialog.show(getFragmentManager(), "select_leader");
                }
            }
        }

        @Override
        public void onListDialogClick(DialogFragment dialog, int which, Bundle data) {
            String[] leaderIds = data.getStringArray("leaderIds");
            long leadId = data.getLong("leadId");
            sendIntent(Long.parseLong(leaderIds[which]), leadId);
        }

        // Start a LeaderActivity with id and leadId
        protected void sendIntent(long leaderId, long leadId) {
            Log.v("CursorListFragment", "Starting " + mIntentClass.getSimpleName() +
                    " with id=" + String.valueOf(leaderId));
            Intent intent = new Intent(getActivity(), mIntentClass);
            intent.putExtra(CursorListFragment.EXTRA_ID, leaderId);
            intent.putExtra(EXTRA_LEAD_ID, leadId);
            startActivity(intent);
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
            super.onViewCreated(view, savedInstanceState);
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
        }
    }
}
