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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import net.alliknow.podcatcher.listeners.OnChangeEpisodeStateListener;
import net.alliknow.podcatcher.listeners.OnChangePlaylistListener;
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
 * Show episode activity. This is thought of as an abstract activity for an app
 * only consisting of an episode view and the player. Sub-classes could extend
 * or simply show this layout.
 */
public abstract class EpisodeActivity extends BaseActivity implements
        OnDownloadEpisodeListener, OnChangePlaylistListener, OnChangeEpisodeStateListener,
        PlayerListener, PlayServiceListener {

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
                    updatePlayerUi();
                }
            });
        }
    }

    /**
     * Get the fragments needed by this activity from the fragment manager and
     * set member fields. Sub-classes should call this after setting their
     * content view or plugging in fragments. Sub-classes that use their own
     * fragments should also extend this. Members will only be set if
     * <code>null</code>. It is safe to assume the fragment members to be
     * non-null once this method completed.
     */
    protected void findFragments() {
        // The player fragment to use
        if (playerFragment == null)
            playerFragment = (PlayerFragment) findByTagId(R.string.player_fragment_tag);

        // The episode fragment to use
        if (episodeFragment == null)
            episodeFragment = (EpisodeFragment) findByTagId(R.string.episode_fragment_tag);
    }

    /**
     * Register the various listeners, that will alert our activities on model
     * or UI changes. This runs after the fragments have been established to
     * avoid the case where we are hit by a call-back and could not react to it.
     */
    protected void registerListeners() {
        // Make sure play service is started
        startService(new Intent(this, PlayEpisodeService.class));
        // Attach to play service, this will register the play service listener
        // once the service is up
        Intent intent = new Intent(this, PlayEpisodeService.class);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);

        // We have to do this here instead of onCreate since we can only react
        // on the call-backs properly once we have our fragment
        episodeManager.addDownloadListener(this);
        episodeManager.addPlaylistListener(this);
        episodeManager.addStateChangedListener(this);
    }

    @Override
    protected void onStart() {
        super.onStart();

        // This is safe since it actually only starts the timer if it is
        // actually needed
        startPlayProgressTimer();
    }

    @Override
    protected void onResume() {
        super.onResume();

        updateActionBar();
        updatePlayerUi();
    }

    @Override
    protected void onStop() {
        super.onStop();

        stopPlayProgressTimer();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Stop the timer
        playUpdateTimer.cancel();

        // Disconnect from episode manager
        episodeManager.removeDownloadListener(this);
        episodeManager.removePlaylistListener(this);
        episodeManager.removeStateChangedListener(this);

        // Detach from play service (prevents leaking)
        if (service != null) {
            service.removePlayServiceListener(this);
            unbindService(connection);
        }
    }

    @Override
    public final void onDownloadSuccess() {
        updateDownloadUi();
    }

    @Override
    public final void onDownloadFailed() {
        updateDownloadUi();

        showToast(getString(R.string.download_failed));
    }

    @Override
    public final void onPlaylistChanged() {
        updatePlaylistUi();
        updatePlayerUi();
    }

    @Override
    public final void onStateChanged(Episode episode) {
        updateStateUi();
    }

    @Override
    public void onToggleDownload() {
        // Check for action to perform
        boolean download = !episodeManager.isDownloadingOrDownloaded(currentEpisode);

        // Kick off the appropriate action
        if (download) {
            episodeManager.download(currentEpisode);

            showToast(getString(R.string.started_download) + "\n\""
                    + currentEpisode.getName() + "\"");
        }
        else
            episodeManager.deleteDownload(currentEpisode);

        // Update the UI
        updateDownloadUi();
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
        updatePlayerUi();
        playerFragment.updateSeekBarSecondaryProgress(0);
    }

    @Override
    public void onTogglePlay() {
        // Player is playing
        if (service.isPlaying()) {
            service.pause();
            stopPlayProgressTimer();
        } // Player in pause
        else {
            service.resume();
            startPlayProgressTimer();
        }

        updatePlayerUi();
    }

    @Override
    public void onNext() {
        service.playNext();
    }

    @Override
    public void onPlaybackStarted() {
        startPlayProgressTimer();
    }

    @Override
    public void onPlaybackStateChanged() {
        updatePlayerUi();

        if (service != null && service.isPlaying())
            startPlayProgressTimer();
        else
            stopPlayProgressTimer();
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser) {
            service.seekTo(progress);
            updatePlayerUi();
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
        updatePlayerUi();
    }

    @Override
    public void onResumeFromBuffering() {
        startPlayProgressTimer();
    }

    @Override
    public void onBufferUpdate(int seconds) {
        playerFragment.updateSeekBarSecondaryProgress(seconds);
    }

    @Override
    public void onPlaybackComplete() {
        if (episodeManager.isPlaylistEmpty()) {
            stopPlayProgressTimer();
            updatePlayerUi();
        }
    }

    @Override
    public void onError() {
        stopPlayProgressTimer();
        service.reset();

        updatePlayerUi();
        playerFragment.setPlayerVisibilility(true);
        playerFragment.setErrorViewVisibility(true);
    }

    /**
     * Update the action bar to reflect current selection and loading state.
     * Sub-classes need to overwrite.
     */
    protected abstract void updateActionBar();

    /**
     * Update all UI related to the download state of the current selection.
     * Sub-classes might want to extend this.
     */
    protected void updateDownloadUi() {
        // The episode fragment might be popped out if we are in small landscape
        // view mode and the episode list is currently visible
        if (episodeFragment != null) {
            final boolean downloading = episodeManager.isDownloading(currentEpisode);
            final boolean downloaded = episodeManager.isDownloaded(currentEpisode);

            episodeFragment.setDownloadMenuItemVisibility(currentEpisode != null,
                    !(downloading || downloaded));
            episodeFragment.setDownloadIconVisibility(downloading || downloaded, downloaded);
        }
    }

    /**
     * Update all UI related to the playlist. Sub-classes might want to extend
     * this.
     */
    protected void updatePlaylistUi() {
        // Nothing to do here
    }

    /**
     * Update all UI related to the old/new state of the current selection.
     * Sub-classes might want to extend this.
     */
    protected void updateStateUi() {
        // The episode fragment might be popped out if we are in small landscape
        // view mode and the episode list is currently visible
        if (episodeFragment != null)
            episodeFragment.setNewIconVisibility(!episodeManager.getState(currentEpisode));
    }

    /**
     * Update the player fragment UI to reflect current state of play.
     */
    protected void updatePlayerUi() {
        // Even though all the fragments should be present, the service might
        // not have been connected to yet. Also, the episode manager might not
        // be ready
        try {
            final boolean currentEpisodeIsShowing = service.isLoadedEpisode(currentEpisode);

            // Show/hide menu item
            playerFragment.setLoadMenuItemVisibility(currentEpisode != null,
                    !currentEpisodeIsShowing);

            // Make sure error view is hidden
            playerFragment.setErrorViewVisibility(false);
            // Make sure player is shown if and as needed
            playerFragment.setPlayerVisibilility(service.isPreparing() || service.isPrepared());
            playerFragment.setPlayerTitleVisibility(
                    !viewMode.isSmallLandscape() && !currentEpisodeIsShowing);
            playerFragment.setPlayerSeekbarVisibility(!viewMode.isSmallLandscape());

            // Enable/disable next button
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
            Log.d(getClass().getSimpleName(), "Update player failed!", nex);
        }
    }

    private void startPlayProgressTimer() {
        // Do not start the task if there is no progress to monitor
        if (service != null && service.isPlaying()) {
            // Only start task if it isn't already running and
            // there is actually some progress to monitor
            if (playUpdateTimerTask == null) {
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
            updatePlayerUi();

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
