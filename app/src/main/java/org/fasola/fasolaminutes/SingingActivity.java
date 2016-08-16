package org.fasola.fasolaminutes;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.text.SpannableString;
import android.text.style.BackgroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SearchView;
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
                        if (singing.isDenson.getString().equals("0")) {
                            ((TextView) view.findViewById(android.R.id.empty)).setText(R.string.empty_singing);
                            view.findViewById(R.id.songs).setVisibility(View.GONE);
                            view.findViewById(R.id.leaders).setVisibility(View.GONE);
                        } else {
                            view.findViewById(R.id.songs).setVisibility(View.VISIBLE);
                            view.findViewById(R.id.leaders).setVisibility(View.VISIBLE);
                        }
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
                String[] names = cursor.getString(leaderColumn).split(" \\(")[0].split(", ");
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
        protected String mSearchTerm = "";
        protected int mResultPosition = -1;
        private static final int mPrimaryHighlight = Color.CYAN;
        private static final int mSecondaryHighlight = Color.MAGENTA;
        private static final String BUNDLE_SEARCH = "SEARCH_TERM";
        private static final String BUNDLE_SEARCH_POSITION = "SEARCH_POSITION";

        protected ListView mList = null;
        protected ArrayAdapter<SpannableString> mAdapter = null;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            if (savedInstanceState != null) {
                mSearchTerm = savedInstanceState.getString(BUNDLE_SEARCH, mSearchTerm);
                mResultPosition = savedInstanceState.getInt(BUNDLE_SEARCH_POSITION, mResultPosition);
            }
            setHasOptionsMenu(true);
        }

        @Override
        public void onSaveInstanceState(final Bundle saveInstanceState) {
            super.onSaveInstanceState(saveInstanceState);
            saveInstanceState.putSerializable(BUNDLE_SEARCH, mSearchTerm);
            saveInstanceState.putSerializable(BUNDLE_SEARCH_POSITION, mResultPosition);
        }


        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            return inflater.inflate(R.layout.fragment_singing_text, container, false);
        }

        @Override
        public void onViewCreated(final View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            mList = (ListView) view.findViewById(R.id.full_text);
            // Hide keyboard when touched
            final InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            mList.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                    return false;
                }
            });
            // Get the text
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
                        // Create SpannableString paragraphs for the adapter
                        String[] paragraphs = text.split("\n");
                        SpannableString[] spannableParagraphs = new SpannableString[paragraphs.length];
                        for (int i = 0; i < paragraphs.length; ++i)
                            spannableParagraphs[i] = new SpannableString(paragraphs[i]);
                        // TextView chokes on very long text (e.g. full text for camp)
                        // Fake it with a ListView of smaller TextViews for better performance
                        mAdapter = new ArrayAdapter<>(getActivity(),
                                R.layout.list_item_long_text,
                                spannableParagraphs);
                        mList.setAdapter(mAdapter);
                        setSearch(mSearchTerm);
                    }
                }
            });
        }

        public void setSearch(String searchTerm) {
            mSearchTerm = searchTerm;
            if (mAdapter == null)
                return;
            boolean keepMatch = false; // Does the existing match still match?
            int currentPosition = 0;   // The first character of the current list item
            int firstMatch = -1;       // The first position with a match
            String lcSearch = mSearchTerm.toLowerCase();
            for (int i = 0; i < mAdapter.getCount(); ++i) {
                SpannableString styledText = mAdapter.getItem(i);
                // Clear highlights
                for (BackgroundColorSpan span : styledText.getSpans(0, styledText.length(), BackgroundColorSpan.class))
                    styledText.removeSpan(span);
                // Add highlights
                if (mSearchTerm.isEmpty())
                    continue;
                String text = styledText.toString().toLowerCase();
                int pos = -1;
                while (true) {
                    pos = text.indexOf(lcSearch, pos + 1);
                    if (pos == -1)
                        break;
                    styledText.setSpan(new BackgroundColorSpan(mSecondaryHighlight), pos, pos + searchTerm.length(), 0);
                    // Track match variables
                    if (firstMatch == -1)
                        firstMatch = currentPosition + pos;
                    if (! keepMatch)
                        keepMatch = (mResultPosition == currentPosition + pos);
                }
                currentPosition += text.length();
            }
            // Update the match
            if (! keepMatch)
                mResultPosition = firstMatch;
            updateHighlight();
            mAdapter.notifyDataSetChanged();
        }

        /** Update highlight colors */
        void updateHighlight() {
            if (mResultPosition == -1)
                return;
            int position = 0;
            // Update background color of all spans
            for (int i = 0; i < mAdapter.getCount(); ++i) {
                SpannableString styledText = mAdapter.getItem(i);
                BackgroundColorSpan[] spans = styledText.getSpans(0, styledText.length(), BackgroundColorSpan.class);
                for (int j = spans.length - 1; j >= 0; --j) {
                    BackgroundColorSpan span = spans[j];
                    int start = styledText.getSpanStart(span);
                    int bgColor = (mResultPosition == position + start) ? mPrimaryHighlight : mSecondaryHighlight;
                    if (span.getBackgroundColor() != bgColor) {
                        styledText.removeSpan(span);
                        styledText.setSpan(new BackgroundColorSpan(bgColor), start, start + mSearchTerm.length(), 0);
                    }
                }
                position += styledText.length();
            }
        }

        /** Change the highlight position */
        void moveHighlight(int direction) {
            if (mResultPosition == -1)
                return;
            int previousPosition = -1;
            int firstMatch = -1;
            int position = 0;
            for (int i = 0; i < mAdapter.getCount(); ++i) {
                SpannableString styledText = mAdapter.getItem(i);
                for (BackgroundColorSpan span : styledText.getSpans(0, styledText.length(), BackgroundColorSpan.class)) {
                    int pos = position + styledText.getSpanStart(span);
                    if (firstMatch == -1)
                        firstMatch = pos;
                    // Check for position match
                    if (pos >= mResultPosition) {
                        if (direction > 0) {
                            // Wait til the next iteration
                            direction = 0;
                        } else if (direction == 0) {
                            mResultPosition = pos;
                            return;
                        } else { // direction == -1
                            mResultPosition = previousPosition;
                            return;
                        }
                    }
                    previousPosition = pos;
                }
                position += styledText.length();
            }
            // Check for wrap-around
            if (direction == -1)
                mResultPosition = previousPosition; // This is the last match
            else
                mResultPosition = firstMatch;
        }

        @Override
        public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
            inflater.inflate(R.menu.menu_singing_text_fragment, menu);
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            if (item.getItemId() == R.id.menu_next_result) {
                moveHighlight(1);
                updateHighlight();
                mAdapter.notifyDataSetChanged();
                return true;
            } else if (item.getItemId() == R.id.menu_prev_result) {
                moveHighlight(-1);
                updateHighlight();
                mAdapter.notifyDataSetChanged();
                return true;
            }
            return super.onOptionsItemSelected(item);
        }

        // Search boilerplate
        @Override
        public void onPrepareOptionsMenu(final Menu menu) {
            super.onPrepareOptionsMenu(menu);
            // Get the SearchView
            final MenuItem searchItem = menu.findItem(R.id.menu_search);
            if (searchItem == null)
                return;
            // Show/hide arrows with search
            searchItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
                @Override
                public boolean onMenuItemActionExpand(MenuItem item) {
                    menu.findItem(R.id.menu_next_result).setVisible(true);
                    menu.findItem(R.id.menu_prev_result).setVisible(true);
                    return true;
                }

                @Override
                public boolean onMenuItemActionCollapse(MenuItem item) {
                    menu.findItem(R.id.menu_next_result).setVisible(false);
                    menu.findItem(R.id.menu_prev_result).setVisible(false);
                    return true;
                }
            });
            // Expand/collapse before setting callbacks, because both expanding and collapsing
            // reset search text to blank
            final SearchView searchView = (SearchView) searchItem.getActionView();
            searchView.setIconifiedByDefault(true);
            if (mSearchTerm != null && ! mSearchTerm.isEmpty())
                searchItem.expandActionView();
            else
                searchItem.collapseActionView();
            // Update search results as you type
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    searchView.clearFocus(); // Hide the keyboard
                    onQueryTextChange(query);
                    return true;
                }

                @Override
                public boolean onQueryTextChange(String query) {
                    // db uses curly apostrophes
                    setSearch(query.replace('\'', '\u2019'));
                    return true;
                }
            });
            // Set cached search text
            searchView.setQuery(mSearchTerm, false);
        }
    }
}
