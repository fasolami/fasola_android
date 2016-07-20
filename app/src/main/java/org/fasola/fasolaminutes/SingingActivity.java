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
import android.widget.AdapterView;
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

    public static class SingingSongListFragment extends CursorStickyListFragment
                                                implements ListDialogFragment.Listener {

        long mId;

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View root = inflater.inflate(R.layout.fragment_singing_songlist, container, false);
            inflateList(inflater, (ViewGroup) root, savedInstanceState);
            return root;
        }

        @Override
        public void onViewCreated(final View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            getListView().setOnItemLongClickListener(mOnLongClickListener);
            setMenuResource(R.menu.menu_singing_songs_list_fragment);
            setDefaultSortId(R.id.menu_singing_song_sort_order);
            setFastScrollEnabled(true);
            setItemLayout(R.layout.list_item_singing_song);
            setIntentActivity(LeaderActivity.class);
            ((TextView) view.findViewById(android.R.id.empty)).setText("");
            mId = getActivity().getIntent().getLongExtra(EXTRA_ID, -1);
            // Singing info query
            SQL.Query query = C.Singing.select(C.Singing.name, C.Singing.location, C.Singing.startDate,
                                               C.Singing.songCount, C.Singing.leaderCount, C.Singing.isDenson)
                                        .whereEq(C.Singing.id);
            getLoaderManager().initLoader(1, null, new MinutesLoader(query, String.valueOf(mId)) {
                @Override
                public void onLoadFinished(Cursor cursor) {
                    C.SingingDAO singing = C.Singing.fromCursor(cursor);
                    if (singing != null) {
                        String name = singing.name.getString();
                        String location = singing.location.getString();
                        String date = singing.startDate.getString();
                        int nSongs = singing.songCount.getInt();
                        int nLeaders = singing.leaderCount.getInt();
                        String songs = getResources().getQuantityString(R.plurals.songsLed, nSongs, nSongs);
                        String leaders = getResources().getQuantityString(R.plurals.leaders, nLeaders, nLeaders);
                        ((TextView) view.findViewById(R.id.title)).setText(name);
                        ((TextView) view.findViewById(R.id.location)).setText(location);
                        ((TextView) view.findViewById(R.id.date)).setText(date);
                        ((TextView) view.findViewById(R.id.songs)).setText(songs);
                        ((TextView) view.findViewById(R.id.leaders)).setText(leaders);
                        // Show a message if this is not a Denson book singing
                        if (singing.isDenson.getString().equals("0"))
                            ((TextView) view.findViewById(android.R.id.empty)).setText(R.string.empty_singing);
                    }
                }
            });
            updateQuery();
        }

        /** The normal query */
        private SQL.Query standardQuery() {
            return SQL.select(
                    C.Song.id,
                    C.Song.fullName,
                    C.Leader.allNames)
                .select(C.SongLeader.leadId).as(EXTRA_LEAD_ID)
                .select(C.Leader.id.func("group_concat")).as("__leaderIds")
                .select(C.SongLeader.audioUrl).as(CursorListFragment.AUDIO_COLUMN)
                .from(C.SongLeader)
                .where(C.SongLeader.singingId, "=", mId)
                .group(C.SongLeader.leadId);
        }

        /** A query that separates leaders into parts */
        private SQL.Query leaderQuery() {
            return SQL.select(
                    C.Song.id,
                    C.Song.fullName,
                    C.Leader.fullName +
                        " || ' ' || " +
                        C.SongLeader.coleaders.format(
                            "CASE WHEN {column} IS NOT NULL" +
                                " THEN '(with ' || {column} || ')'" +
                                " ELSE '' " +
                            " END"))
                .select(C.SongLeader.leadId).as(EXTRA_LEAD_ID)
                    .select(C.Leader.id).as("__leaderIds")
                .select(C.SongLeader.audioUrl).as(CursorListFragment.AUDIO_COLUMN)
                .from(C.SongLeader)
                .where(C.SongLeader.singingId, "=", mId);
        }

        @Override
        public SQL.Query onUpdateQuery() {
            // Order
            switch (getSortId()) {
                case R.id.menu_singing_song_sort_leader:
                    showHeaders(true);
                    setAlphabetIndexer();
                    return leaderQuery().sectionIndex(C.Leader.lastName)
                                .order(C.Leader.lastName, "ASC", C.Leader.fullName, "ASC");
                case R.id.menu_singing_song_sort_page:
                    showHeaders(false);
                    return standardQuery().order(C.Song.pageSort, "ASC");
                case R.id.menu_singing_song_sort_order:
                default:
                    showHeaders(false);
                    return standardQuery().order(C.SongLeader.singingOrder, "ASC");
            }
        }

        @Override
        public SQL.Query onUpdateSearch(SQL.Query query, String searchTerm) {
            switch (getSortId()) {
                case R.id.menu_singing_song_sort_leader:
                    return query.where(C.Leader.fullName, "LIKE", "%" + searchTerm + "%")
                                .or(C.Song.fullName, "LIKE", "%" + searchTerm + "%");
                default:
                    // having since Leader.allNames is a group_concat
                    return query.having(C.Leader.allNames, "LIKE", "%" + searchTerm + "%")
                                .or(C.Song.fullName, "LIKE", "%" + searchTerm + "%");
            }
        }

        @Override
        public void onLoadFinished(Cursor cursor) {
            // Highlight the Intent's lead id
            long leadId = getActivity().getIntent().getLongExtra(EXTRA_LEAD_ID, -1);
            if (leadId > -1)
                setHighlight(cursor, EXTRA_LEAD_ID, leadId);
            super.onLoadFinished(cursor);
        }

        // Click: prompt if multiple leaders
        @Override
        public void onListItemClick(ListView l, View v, int position, long id) {
            navigationPrompt(position, PROMPT_LEADERS);
        }

        // Long click: prompt song and leaders
        final private AdapterView.OnItemLongClickListener mOnLongClickListener
                = new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View v, int position, long id) {
                navigationPrompt(position, PROMPT_SONG|PROMPT_LEADERS|PROMPT_FORCE);
                return true;
            }
        };


        static final int PROMPT_SONG = 1;
        static final int PROMPT_LEADERS = 2;
        static final int PROMPT_FORCE = 4;

        private void navigationPrompt(int position, int flags) {
            Cursor cursor = getListAdapter().getCursor();
            if (!cursor.moveToPosition(position))
                return;
            long songId = cursor.getLong(0);
            long leadId = cursor.getLong(cursor.getColumnIndex(EXTRA_LEAD_ID));
            String[] leaderIds = cursor.getString(cursor.getColumnIndex("__leaderIds")).split(",");
            // Check for multiple leaders
            if (leaderIds.length == 1 && (flags & PROMPT_FORCE) == 0) {
                // Single leader: start the activity
                sendLeaderIntent(Long.parseLong(leaderIds[0]), leadId);
            } else {
                // Multiple leaders (or PROMPT_FORCE): prompt
                // Set data
                Bundle data = new Bundle();
                data.putLong("leadId", leadId);
                data.putStringArray("leaderIds", leaderIds);
                int title = R.string.select_leader;
                int leaderColumn = 2;
                String[] names = cursor.getString(leaderColumn).split(", ");
                if ((flags & PROMPT_SONG) != 0) {
                    // Add song data
                    data.putLong("songId", songId);
                    title = R.string.select_song_or_leader;
                    // Prepend song title to the list of names
                    String[] newNames = new String[names.length + 1];
                    int songColumn = 1;
                    newNames[0] = cursor.getString(songColumn);
                    System.arraycopy(names, 0, newNames, 1, names.length);
                    names = newNames;
                }
                // Show the dialog
                ListDialogFragment dialog = ListDialogFragment.newInstance(title, names, data);
                dialog.setTargetFragment(this, 1);
                dialog.show(getFragmentManager(), "select_leader");
            }
        }

        @Override
        public void onListDialogClick(DialogFragment dialog, int which, Bundle data) {
            String[] leaderIds = data.getStringArray("leaderIds");
            long songId = data.getLong("songId", -1);
            long leadId = data.getLong("leadId");
            if (songId > -1) {
                if (which == 0) {
                    // If we sent a song, 0 means the song id
                    sendSongIntent(songId);
                    return;
                } else {
                    // searching leaderIds array -- which is 1-based if we sent a songId
                    --which;
                }
            }
            sendLeaderIntent(Long.parseLong(leaderIds[which]), leadId);
        }

        // Start a LeaderActivity with id and leadId
        protected void sendLeaderIntent(long leaderId, long leadId) {
            Log.v("CursorListFragment", "Starting " + mIntentClass.getSimpleName() +
                    " with id=" + String.valueOf(leaderId));
            Intent intent = new Intent(getActivity(), mIntentClass);
            intent.putExtra(CursorListFragment.EXTRA_ID, leaderId);
            intent.putExtra(EXTRA_LEAD_ID, leadId);
            startActivity(intent);
        }

        // Start a SongActivity with id
        protected void sendSongIntent(long songId) {
            Log.v("CursorListFragment", "Starting " + SongActivity.class.getSimpleName() +
                    " with id=" + String.valueOf(songId));
            Intent intent = new Intent(getActivity(), SongActivity.class);
            intent.putExtra(CursorListFragment.EXTRA_ID, songId);
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
                        String text = singing.fullText.getString()
                                .replaceAll(
                                        "[\\[{<]" +
                                            "(?:\\d+[tb]?//)?(\\d+[tb]?)" +
                                        "[\\]}>]",
                                    "$1");
                        // TextView chokes on very long text (e.g. full text for camp)
                        // Fake it with a ListView of smaller TextViews for better performance
                        ListView list = (ListView) view.findViewById(R.id.full_text);
                        list.setAdapter(new ArrayAdapter<>(getActivity(),
                                R.layout.list_item_long_text,
                                text.split("\n")));
                    }
                }
            });
        }
    }
}
