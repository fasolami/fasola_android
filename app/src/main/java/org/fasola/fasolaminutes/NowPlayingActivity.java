package org.fasola.fasolaminutes;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.widget.MediaController;

import java.util.List;


public class NowPlayingActivity extends SimpleTabActivity {
    long songId = -1;
    MediaController mController;
    PlaybackService.Control mPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nowplaying);
        // Setup MediaController
        mController = (MediaController)findViewById(R.id.media_controller);
        mPlayer = new PlaybackService.Control(this);
        mController.setMediaPlayer(mPlayer);
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
                if (song.songId != songId) {
                    songId = song.songId;
                    List<Fragment> fragments = getSupportFragmentManager().getFragments();
                    if (fragments != null)
                        for (Fragment fragment : fragments)
                            if (fragment instanceof SongActivity.SongFragment)
                                ((SongActivity.SongFragment) fragment).setSongId(songId);
                }
                setProgressBarIndeterminateVisibility(service != null && service.isLoading());
            }
            else {
                songId = -1;
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
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_nowplaying_activity, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onNewFragment(Fragment fragment) {
        Bundle args = new Bundle();
        args.putLong(CursorListFragment.EXTRA_ID, songId);
        fragment.setArguments(args);
    }
}
