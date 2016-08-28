package org.fasola.fasolaminutes;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.MediaController;
import android.widget.TextView;

import java.util.List;


public class NowPlayingActivity extends SimpleTabActivity {
    Playlist.Song mSong;
    MediaController mController;
    PlaybackService.Control mPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nowplaying);
        // Save offscreen pages
        mViewPager.setOffscreenPageLimit(mPagerAdapter.getCount());
        // Setup MediaController
        mController = (MediaController)findViewById(R.id.media_controller);
        mPlayer = new PlaybackService.Control(this);
        mController.setMediaPlayer(mPlayer);
        setHelpResource(R.string.help_now_playing_activity);
    }

    @Override
    protected void onResume() {
        mObserver.registerAll(this, PlaybackService.BROADCAST_ALL);
        super.onResume();
    }

    @Override
    protected void onResumeFragments() {
        super.onResumeFragments();
        mObserver.onChanged();
        mObserver.onCursorChanged();
    }

    @Override
    protected void onPause() {
        mObserver.unregister();
        super.onPause();
    }

    PlaylistObserver mObserver = new PlaylistObserver() {
        @Override
        public void onChanged() {
            mController.show(0); // Update seekbar
            PlaybackService service = PlaybackService.getInstance();
            Playlist.Song song;
            if (service != null)
                song = service.getSong();
            else
                song = Playlist.getInstance().getCurrent();
            if (song != null) {
                setTitle(song.name);
                // Update fragments
                if (song != mSong) {
                    mSong = song;
                    List<Fragment> fragments = getSupportFragmentManager().getFragments();
                    if (fragments != null)
                        for (Fragment fragment : fragments) {
                            if (fragment instanceof SongActivity.SongFragment)
                                ((SongActivity.SongFragment) fragment).setSongId(mSong.songId);
                            else if (fragment instanceof NowPlayingInfoFragment)
                                ((NowPlayingInfoFragment) fragment).setLeadId(mSong.leadId);
                        }
                }
                setProgressBarIndeterminateVisibility(service != null && service.isLoading());
            }
            else {
                mSong = null;
                setProgressBarIndeterminateVisibility(false);
            }
        }

        @Override
        public void onCursorChanged() {
            // Attach or remove prev/next listeners
            Playlist playlist = Playlist.getInstance();
            int pos = playlist.getPosition();
            mController.setPrevNextListeners(
                    playlist.hasNext() ? mPlayer.nextListener : null,
                    playlist.hasPrevious() ? mPlayer.prevListener : null
            );
        }
    };

    @Override
    public void onNewFragment(Fragment fragment) {
        Bundle args = new Bundle();
        if (mSong != null) {
            if (fragment instanceof SongActivity.SongFragment)
                args.putLong(CursorListFragment.EXTRA_ID, mSong.songId);
            else
                args.putLong(CursorListFragment.EXTRA_ID, mSong.leadId);
        }
        fragment.setArguments(args);
    }

    static public class NowPlayingInfoFragment extends CursorListFragment {
        // For click handlers
        long mSingingId = -1;
        long mSongId = -1;
        long mLeadId = -1;

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View root = inflater.inflate(R.layout.fragment_now_playing_info, container, false);
            inflateList(inflater, (ViewGroup) root, savedInstanceState);
            root.findViewById(R.id.song_name).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mSongId == -1)
                        return;
                    startActivity(new Intent(getActivity(), SongActivity.class)
                            .putExtra(EXTRA_ID, mSongId));
                }
            });
            root.findViewById(R.id.singing).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mSingingId == -1)
                        return;
                    startActivity(new Intent(getActivity(), SingingActivity.class)
                            .putExtra(EXTRA_ID, mSingingId)
                            .putExtra(SingingActivity.EXTRA_LEAD_ID, mLeadId));
                }
            });
            return root;
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            setItemLayout(android.R.layout.simple_list_item_1);
            setIntentActivity(LeaderActivity.class);
            setLeadId(getArguments().getLong(EXTRA_ID, -1));
        }

        public void setLeadId(long leadId) {
            mLeadId = leadId;
            getArguments().putLong(EXTRA_ID, leadId);
            // List query
            setQuery(SQL.select(C.Leader.id, C.Leader.fullName)
                    .where(C.SongLeader.leadId, "=", leadId));
            // Song/Singing query
            SQL.Query query =
                    SQL.select(C.Song.id, C.Song.fullName)
                    .select(C.Singing.id, C.Singing.name, C.Singing.location, C.Singing.startDate)
                    .from(C.SongLeader)
                    .where(C.SongLeader.leadId, "=", leadId);
            getLoaderManager().initLoader((int) leadId, null, new MinutesLoader(query) {
                @Override
                public void onLoadFinished(Cursor cursor) {
                    if (!cursor.moveToFirst())
                        return;
                    View view = getView();
                    mSongId = cursor.getLong(0);
                    String songName = cursor.getString(1);
                    mSingingId = cursor.getLong(2);
                    String singingName = cursor.getString(3);
                    String singingLocation = cursor.getString(4);
                    String singingDate = cursor.getString(5);
                    ((TextView) view.findViewById(R.id.song_name)).setText(songName);
                    ((TextView) view.findViewById(R.id.singing_name)).setText(singingName);
                    ((TextView) view.findViewById(R.id.singing_location)).setText(singingLocation);
                    ((TextView) view.findViewById(R.id.singing_date)).setText(singingDate);
                }
            });
        }

        @Override
        protected void setIntentData(Intent intent, int position, long id) {
            super.setIntentData(intent, position, id);
            // Set leadId for leader activity
            intent.putExtra(SingingActivity.EXTRA_LEAD_ID, mLeadId);
        }
    }
}
