/** Copyright 2012, 2013 Kevin Hausmann
 *
 * This file is part of PodCatcher Deluxe.
 *
 * PodCatcher Deluxe is free software: you can redistribute it 
 * and/or modify it under the terms of the GNU General Public License as 
 * published by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * PodCatcher Deluxe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PodCatcher Deluxe. If not, see <http://www.gnu.org/licenses/>.
 */

package net.alliknow.podcatcher;

import android.app.FragmentTransaction;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.SeekBar;

import net.alliknow.podcatcher.listeners.OnSelectEpisodeListener;
import net.alliknow.podcatcher.listeners.PlayServiceListener;
import net.alliknow.podcatcher.listeners.PlayerListener;
import net.alliknow.podcatcher.model.types.Episode;
import net.alliknow.podcatcher.services.PlayEpisodeService;
import net.alliknow.podcatcher.services.PlayEpisodeService.PlayServiceBinder;
import net.alliknow.podcatcher.view.fragments.EpisodeFragment;
import net.alliknow.podcatcher.view.fragments.PlayerFragment;

import java.lang.ref.WeakReference;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Show episode activity. This is thought of an abstract activity for an app
 * only consisting of an episode view and the player. Sub-classes could extends
 * or simply show this layout.
 */
public abstract class EpisodeActivity extends BaseActivity implements
        PlayerListener, PlayServiceListener, OnSelectEpisodeListener {

    /** Key used to store episode URL in intent or bundle */
    public static final String EPISODE_URL_KEY = "episode_url";

    /** The current episode fragment */
    protected EpisodeFragment episodeFragment;
    /** The current player fragment */
    protected PlayerFragment playerFragment;

    /** Play service */
    protected PlayEpisodeService service;

    /** Play update timer */
    private Timer playUpdateTimer = new Timer();
    /** Play update timer task */
    private TimerTask playUpdateTimerTask;
    /** Flag for visibility, coordinating timer */
    private boolean visible = false;

    /** The actual task to regularly update the UI on playback */
    private static class PlayProgressTask extends TimerTask {

        /** Use weak reference to avoid any leaking of activities */
        private final WeakReference<EpisodeActivity> activityReference;

        /**
         * Create a new update task.
         * 
         * @param episodeActivity Activity to call update player for.
         */
        public PlayProgressTask(EpisodeActivity episodeActivity) {
            activityReference = new WeakReference<EpisodeActivity>(episodeActivity);
        }

        @Override
        public void run() {
            final EpisodeActivity episodeActivity = activityReference.get();
            if (episodeActivity != null) {
                // Need to run on UI thread, since we want to update the play
                // button
                episodeActivity.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        episodeActivity.updatePlayer();
                    }
                });
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Make sure play service is started
        startService(new Intent(this, PlayEpisodeService.class));
        // Attach to play service
        Intent intent = new Intent(this, PlayEpisodeService.class);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }

    /**
     * Get the fragments needed by this activity from the fragment manager and
     * set member fields. Sub-classes should call this after setting their
     * content view or plugging in fragments. Sub-classes that use their own
     * fragments should also extend this. Members will only be set if
     * <code>null</code>.
     */
    protected void findFragments() {
        // The player fragment to use
        if (playerFragment == null)
            playerFragment = (PlayerFragment) findByTagId(R.string.player_fragment_tag);

        // The episode fragment to use
        if (episodeFragment == null)
            episodeFragment = (EpisodeFragment) findByTagId(R.string.episode_fragment_tag);
    }

    @Override
    protected void onStart() {
        super.onStart();
        this.visible = true;

        updateActionBar();
        updatePlayer();
        startPlayProgressTimer();
    }

    @Override
    protected void onStop() {
        super.onStop();
        this.visible = false;

        stopPlayProgressTimer();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        playUpdateTimer.cancel();

        // Detach from play service (prevents leaking)
        if (service != null) {
            service.removePlayServiceListener(this);
            unbindService(connection);
        }
    }

    @Override
    public void onEpisodeSelected(Episode selectedEpisode) {
        selection.setEpisode(selectedEpisode);

        switch (view) {
            case LARGE_PORTRAIT:
            case LARGE_LANDSCAPE:
                // Set episode in episode fragment
                episodeFragment.setEpisode(selectedEpisode);

                break;
            case SMALL_LANDSCAPE:
                // Find, and if not already done create, episode fragment
                if (episodeFragment == null)
                    episodeFragment = new EpisodeFragment();

                // Add the fragment to the UI, replacing the list fragment if it
                // is not already there
                if (getFragmentManager().getBackStackEntryCount() == 0) {
                    FragmentTransaction transaction = getFragmentManager().beginTransaction();
                    transaction.replace(R.id.right, episodeFragment,
                            getString(R.string.episode_fragment_tag));
                    transaction.addToBackStack(null);
                    transaction.commit();
                }

                // Set the episode
                episodeFragment.setEpisode(selectedEpisode);
                episodeFragment.setShowEpisodeDate(true);

                break;
            case SMALL_PORTRAIT:
                // This should be handled by sub-class
                break;
        }

        updatePlayer();
    }

    @Override
    public void onReturnToPlayingEpisode() {
        if (service != null && service.getCurrentEpisode() != null)
            onEpisodeSelected(service.getCurrentEpisode());
    }

    @Override
    public void onNoEpisodeSelected() {
        selection.resetEpisode();

        updatePlayer();
    }

    @Override
    public void onToggleLoad() {
        if (service.isLoadedEpisode(selection.getEpisode()))
            onPlaybackComplete();
        else if (selection.isEpisodeSet()) {
            stopPlayProgressTimer();

            service.playEpisode(selection.getEpisode());

            // Update UI
            updatePlayer();
            if (playerFragment != null)
                playerFragment.updateSeekBarSecondaryProgress(0);
        } else
            Log.w(getClass().getSimpleName(), "Cannot load episode (episode or service are null)");
    }

    @Override
    public void onTogglePlay() {
        if (service != null && service.isPrepared()) {
            // Player is playing
            if (service.isPlaying()) {
                service.pause();
                stopPlayProgressTimer();
            } // Player in pause
            else {
                service.resume();
                startPlayProgressTimer();
            }

            updatePlayer();
        } else
            Log.w(getClass().getSimpleName(),
                    "Cannot play/pause episode (service null or unprepared)");
    }

    @Override
    public void onPlaybackStarted() {
        updatePlayer();
        startPlayProgressTimer();
    }

    @Override
    public void onPlaybackStateChanged() {
        updatePlayer();

        if (service != null && service.isPlaying())
            startPlayProgressTimer();
        else
            stopPlayProgressTimer();
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser) {
            service.seekTo(progress);
            updatePlayer();
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        stopPlayProgressTimer();
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        startPlayProgressTimer();
    }

    @Override
    public void onStopForBuffering() {
        stopPlayProgressTimer();
        updatePlayer();
    }

    @Override
    public void onResumeFromBuffering() {
        onPlaybackStarted();
    }

    @Override
    public void onBufferUpdate(int seconds) {
        if (playerFragment != null)
            playerFragment.updateSeekBarSecondaryProgress(seconds);
    }

    @Override
    public void onPlaybackComplete() {
        stopPlayProgressTimer();
        service.reset();

        updatePlayer();
    }

    @Override
    public void onError() {
        stopPlayProgressTimer();
        service.reset();

        updatePlayer();
        playerFragment.setPlayerVisibilility(true);
        playerFragment.setErrorViewVisibility(true);
    }

    /**
     * Update the action bar to reflect current selection and loading state.
     * Sub-classes need to overwrite.
     */
    protected abstract void updateActionBar();

    /**
     * Update the player fragment UI to reflect current state of play.
     */
    protected void updatePlayer() {
        if (playerFragment != null && service != null) {
            final boolean currentEpisodeIsShowing = service.isLoadedEpisode(selection.getEpisode());

            // Show/hide menu item
            playerFragment.setLoadMenuItemVisibility(selection.isEpisodeSet(),
                    !currentEpisodeIsShowing);

            final boolean showPlayer = service.isPreparing() || service.isPrepared();
            playerFragment.setPlayerVisibilility(showPlayer);
            if (showPlayer) {
                // Make sure error view is hidden
                playerFragment.setErrorViewVisibility(false);
                // Show/hide title and seek bar
                playerFragment.setPlayerTitleVisibility(!view.isSmallLandscape()
                        && !currentEpisodeIsShowing);
                playerFragment.setPlayerSeekbarVisibility(!view.isSmallLandscape());
                // Set player button label format
                playerFragment.setShowShortPosition(
                        view.isSmall() && service.getDuration() >= 60 * 60);
                // Update UI to reflect service status
                playerFragment.updatePlayerTitle(service.getCurrentEpisode());
                playerFragment.updateSeekBar(!service.isPreparing(), service.getDuration(),
                        service.getCurrentPosition());
                playerFragment.updateButton(service.isBuffering(), service.isPlaying(),
                        service.getDuration(), service.getCurrentPosition());
            }
        }
    }

    private void startPlayProgressTimer() {
        // Do not start the task if there is no progress to monitor and we are
        // visible (this fixes the case of stacked activities running the timer)
        if (visible && service != null && service.isPlaying()) {
            // Only start task if it isn't already running and
            // there is actually some progress to monitor
            if (playUpdateTimerTask == null) {
                PlayProgressTask task = new PlayProgressTask(this);

                try {
                    playUpdateTimer.schedule(task, 0, 1000);
                    playUpdateTimerTask = task;
                } catch (IllegalStateException e) {
                    // In rare cases, the timer might be canceled (the activity
                    // is going down) while schedule() is called, skip...
                }
            }
        }
    }

    private void stopPlayProgressTimer() {
        if (playUpdateTimerTask != null) {
            playUpdateTimerTask.cancel();
            playUpdateTimerTask = null;
        }
    }

    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection connection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder serviceBinder) {
            service = ((PlayServiceBinder) serviceBinder).getService();

            // Register listener
            service.addPlayServiceListener(EpisodeActivity.this);

            // Update player UI
            updatePlayer();

            // Restart play progress timer task if service is playing
            if (service.isPlaying())
                startPlayProgressTimer();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            // Nothing to do here
        }
    };
}
