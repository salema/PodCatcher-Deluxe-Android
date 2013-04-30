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

import android.app.Fragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import net.alliknow.podcatcher.listeners.OnChangeEpisodeStateListener;
import net.alliknow.podcatcher.listeners.OnDownloadEpisodeListener;
import net.alliknow.podcatcher.listeners.PlayServiceListener;
import net.alliknow.podcatcher.listeners.PlayerListener;
import net.alliknow.podcatcher.model.types.Episode;
import net.alliknow.podcatcher.services.PlayEpisodeService;
import net.alliknow.podcatcher.services.PlayEpisodeService.PlayServiceBinder;
import net.alliknow.podcatcher.view.fragments.EpisodeFragment;
import net.alliknow.podcatcher.view.fragments.PlayerFragment;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Show episode activity. This is thought of an abstract activity for an app
 * only consisting of an episode view and the player. Sub-classes could extends
 * or simply show this layout.
 */
public abstract class EpisodeActivity extends BaseActivity implements
        OnDownloadEpisodeListener, OnChangeEpisodeStateListener, PlayerListener,
        PlayServiceListener {

    /** The current episode fragment */
    protected EpisodeFragment episodeFragment;
    /** The current player fragment */
    protected PlayerFragment playerFragment;

    /** The episode currently selected and displayed */
    protected Episode currentEpisode;
    /** Key used to store episode URL in intent or bundle */
    public static final String EPISODE_URL_KEY = "episode_url";

    /** Play service */
    protected PlayEpisodeService service;

    /** Whether a seek is currently active */
    private boolean seeking = false;

    /** Play update timer task */
    private Timer playUpdateTimer = new Timer();
    /** Play update timer task */
    private TimerTask playUpdateTimerTask;

    /** The actual task to regularly update the UI on playback */
    private class PlayProgressTask extends TimerTask {

        @Override
        public void run() {
            // Need to run on UI thread, since we want to update the play button
            runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    updatePlayer();
                }
            });
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

        // We have to do this here instead of onCreate since we can only react
        // on the call-backs properly once we have our fragment
        episodeManager.addDownloadListener(this);
        episodeManager.addStateChangedListener(this);
    }

    @Override
    protected void onStart() {
        super.onStart();

        updateActionBar();
        updatePlayer();
        startPlayProgressTimer();
    }

    @Override
    protected void onStop() {
        super.onStop();

        stopPlayProgressTimer();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Disconnect from episode manager
        episodeManager.removeDownloadListener(this);
        episodeManager.removeStateChangedListener(this);

        // Stop the timer
        playUpdateTimer.cancel();

        // Detach from play service (prevents leaking)
        if (service != null) {
            service.removePlayServiceListener(this);
            unbindService(connection);
        }
    }

    @Override
    public void onToggleDownload() {
        // Check for action to perform
        boolean download = !(episodeManager.isDownloading(currentEpisode) ||
                episodeManager.isDownloaded(currentEpisode));

        // Kick off the appropriate action
        if (download) {
            episodeManager.download(currentEpisode);

            showToast(getString(R.string.started_download) + "\n\""
                    + currentEpisode.getName() + "\"");
        }
        else
            episodeManager.deleteDownload(currentEpisode);

        // Update the UI
        updateDownloadStatus();
    }

    @Override
    public void onDownloadSuccess() {
        updateDownloadStatus();
    }

    @Override
    public void onDownloadFailed() {
        updateDownloadStatus();

        showToast(getString(R.string.download_failed));
    }

    @Override
    public void onStateChanged(Episode episode) {
        updateNewStatus();
    }

    @Override
    public void onToggleLoad() {
        // Stop timer task
        stopPlayProgressTimer();

        // Stop called: unload episode
        if (service.isLoadedEpisode(currentEpisode))
            service.reset();
        // Play called on unloaded episode
        else if (currentEpisode != null)
            service.playEpisode(currentEpisode);

        // Update UI
        updatePlayer();
        if (playerFragment != null)
            playerFragment.updateSeekBarSecondaryProgress(0);
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
    public void onNext() {
        service.playNext();

        updatePlayer();
        updatePlaylistStatus();
    }

    @Override
    public void onPlaybackStarted() {
        updatePlayer();
        startPlayProgressTimer();
    }

    @Override
    public void onPlaybackStateChanged() {
        updatePlayer();
        updatePlaylistStatus();

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
        seeking = true;
        stopPlayProgressTimer();
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        seeking = false;
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
        // Mark the episode old (needs to be done before resetting the service!)
        episodeManager.setState(service.getCurrentEpisode(), true);
        // Remove the finished episode from the playlist
        episodeManager.removeFromPlaylist(service.getCurrentEpisode());

        if (episodeManager.isPlaylistEmpty()) {
            stopPlayProgressTimer();
            service.reset();
            updatePlayer();
        } else
            onNext();
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
     * Update the episode state icon.
     */
    protected void updateNewStatus() {
        if (episodeFragment != null) {
            episodeFragment.setNewIconVisibility(!episodeManager.getState(currentEpisode));
        }
    }

    /**
     * Update the download menu item state and visibility.
     */
    protected void updateDownloadStatus() {
        if (episodeFragment != null) {
            final boolean downloading = episodeManager.isDownloading(currentEpisode);
            final boolean downloaded = episodeManager.isDownloaded(currentEpisode);

            episodeFragment.setDownloadMenuItemVisibility(currentEpisode != null,
                    !(downloading || downloaded));

            episodeFragment.setDownloadIconVisibility(downloading || downloaded, downloaded);
        }
    }

    /**
     * Update the UI to reflect current playlist state state. Sub-classes need
     * to overwrite.
     */
    protected abstract void updatePlaylistStatus();

    /**
     * Update the player fragment UI to reflect current state of play.
     */
    protected void updatePlayer() {
        try {
            final boolean currentEpisodeIsShowing = service.isLoadedEpisode(currentEpisode);

            // Show/hide menu item
            playerFragment.setLoadMenuItemVisibility(currentEpisode != null,
                    !currentEpisodeIsShowing);

            // Make sure error view is hidden
            playerFragment.setErrorViewVisibility(false);
            // Make sure player is shown if and as needed
            playerFragment.setPlayerVisibilility(service.isPreparing() || service.isPrepared());
            playerFragment.setPlayerTitleVisibility(!viewMode.isSmallLandscape()
                    && !currentEpisodeIsShowing);
            playerFragment.setPlayerSeekbarVisibility(!viewMode.isSmallLandscape());

            final boolean playlistHasEntries = !episodeManager.isPlaylistEmpty();
            playerFragment.setShowShortPosition(playlistHasEntries && viewMode.isSmall());
            playerFragment.setNextButtonVisibility(playlistHasEntries);

            // Update UI to reflect service status
            playerFragment.updatePlayerTitle(service.getCurrentEpisode());
            playerFragment.updateSeekBar(!service.isPreparing(), service.getDuration(),
                    service.getCurrentPosition());
            playerFragment.updateButton(service.isBuffering(), service.isPlaying(),
                    service.getDuration(), service.getCurrentPosition());
        } catch (NullPointerException nex) {
            // pass
        }
    }

    /**
     * Gets the fragment for a given tag string id (resolved via app's
     * resources) from the fragment manager.
     * 
     * @param tagId Id of the tag string in resources.
     * @return The fragment stored under the given tag or <code>null</code> if
     *         not added to the fragment manager.
     */
    protected Fragment findByTagId(int tagId) {
        return getFragmentManager().findFragmentByTag(getString(tagId));
    }

    private void startPlayProgressTimer() {
        // Do not start the task if there is no progress to monitor
        if (service != null && service.isPlaying()) {
            // Only start task if it isn't already running and
            // there is actually some progress to monitor
            if (playUpdateTimerTask == null && !seeking) {
                PlayProgressTask task = new PlayProgressTask();

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

    private void showToast(String text) {
        Toast toast = Toast.makeText(this, text, Toast.LENGTH_SHORT);

        TextView textView = (TextView) toast.getView().findViewById(android.R.id.message);
        textView.setGravity(Gravity.CENTER);

        toast.show();
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
