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
import android.widget.SeekBar;

import net.alliknow.podcatcher.listeners.PlayServiceListener;
import net.alliknow.podcatcher.listeners.PlayerListener;
import net.alliknow.podcatcher.model.types.Episode;
import net.alliknow.podcatcher.services.PlayEpisodeService;
import net.alliknow.podcatcher.services.PlayEpisodeService.PlayServiceBinder;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Show episode activity.
 */
public class EpisodeActivity extends BaseActivity implements
        PlayerListener, PlayServiceListener {

    /** Key used to store podcast URL in intent or bundle */
    public static final String PODCAST_URL_KEY = "podcast_url";
    /** Key used to store episode URL in intent or bundle */
    public static final String EPISODE_URL_KEY = "episode_url";

    /** The episode currently selected and displayed */
    protected Episode currentEpisode;

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
                    if (findPlayerFragment() != null)
                        findPlayerFragment().update(service, currentEpisode);
                }
            });
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        // Make sure play service is started
        startService(new Intent(this, PlayEpisodeService.class));
        // Attach to play service
        Intent intent = new Intent(this, PlayEpisodeService.class);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onResume() {
        super.onResume();

        // Restore from configuration change
        if (currentEpisode != null && findEpisodeFragment() != null)
            findEpisodeFragment().setEpisode(currentEpisode);
    }

    @Override
    protected void onPause() {
        super.onPause();

        stopPlayProgressTimer();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        playUpdateTimer.cancel();

        // Detach from play service (prevents leaking)
        if (service != null)
            unbindService(connection);

        // This would prevent strange service behavior
        if (service != null && !service.isPlaying())
            service.stopSelf();

    }

    public void onLoadEpisode() {
        // if (service.isWorkingWith(currentEpisode))
        // onPlaybackComplete();
        // else
        if (currentEpisode != null && service != null) {
            // Episode should not be loaded
            if (!service.isWorkingWith(currentEpisode)) {
                stopPlayProgressTimer();

                service.playEpisode(currentEpisode);

                findPlayerFragment().update(service, currentEpisode);
            }
        } else
            Log.d(getClass().getSimpleName(), "Cannot load episode (episode or service are null)");
    }

    @Override
    public void onUnloadEpisode() {
        // TODO Auto-generated method stub
    }

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

            findPlayerFragment().update(service, currentEpisode);
        } else
            Log.d(getClass().getSimpleName(),
                    "Cannot play/pause episode (service null or unprepared)");
    }

    @Override
    public void onReturnToPlayingEpisode() {
        // if (service != null && service.getCurrentEpisode() != null &&
        // selectedListener != null)
        // selectedListener.onEpisodeSelected(service.getCurrentEpisode());
    }

    @Override
    public void onReadyToPlay() {
        findPlayerFragment().update(service, currentEpisode);
        startPlayProgressTimer();
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser) {
            service.seekTo(progress);
            findPlayerFragment().update(service, currentEpisode);
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
        findPlayerFragment().update(service, currentEpisode);
    }

    @Override
    public void onResumeFromBuffering() {
        onReadyToPlay();
    }

    @Override
    public void onBufferUpdate(int seconds) {
        if (findPlayerFragment() != null)
            findPlayerFragment().setSecondaryProgress(seconds);
    }

    @Override
    public void onPlaybackComplete() {
        stopPlayProgressTimer();

        service.reset();

        findPlayerFragment().update(service, currentEpisode);
    }

    @Override
    public void onError() {
        service.reset();

        findPlayerFragment().update(service, currentEpisode);

        findPlayerFragment().showError();

        Log.w(getClass().getSimpleName(), "Play service send an error");
    }

    private void startPlayProgressTimer() {
        // Only start task if it isn't already running and
        // there is actually some progress to monitor
        if (playUpdateTimerTask == null && !seeking) {
            playUpdateTimerTask = new PlayProgressTask();
            playUpdateTimer.schedule(playUpdateTimerTask, 0, 1000);
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
            Log.d(EpisodeActivity.this.getClass().getSimpleName(), "Bound to playback service");

            // Register listener and notification
            service.setPlayServiceListener(EpisodeActivity.this);

            // Update UI to reflect service status
            if (currentEpisode == null && service.getCurrentEpisode() != null) {
                findEpisodeFragment().setEpisode(service.getCurrentEpisode());
                findPlayerFragment().update(service, currentEpisode);
            }

            // Restart play progress timer task if service is playing
            startPlayProgressTimer();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            Log.d(EpisodeActivity.this.getClass().getSimpleName(), "Unbound from playback service");
        }
    };
}
