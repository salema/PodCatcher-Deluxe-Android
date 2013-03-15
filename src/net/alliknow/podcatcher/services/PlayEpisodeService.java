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

package net.alliknow.podcatcher.services;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnInfoListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import net.alliknow.podcatcher.EpisodeActivity;
import net.alliknow.podcatcher.EpisodeListActivity;
import net.alliknow.podcatcher.EpisodeListActivity.ContentMode;
import net.alliknow.podcatcher.PodcastActivity;
import net.alliknow.podcatcher.R;
import net.alliknow.podcatcher.listeners.PlayServiceListener;
import net.alliknow.podcatcher.model.EpisodeManager;
import net.alliknow.podcatcher.model.types.Episode;

import java.util.HashSet;
import java.util.Set;

/**
 * Play an episode service, wraps media player. This class implements an Android
 * service. It can be used to play back podcast episodes and tries to hide away
 * the complexity of the media player support in Android. All methods should
 * fail gracefully. Connect to the service from your activity/fragment to use
 * it. Also implement <code>PlayServiceListener</code> for interaction.
 */
public class PlayEpisodeService extends Service implements OnPreparedListener,
        OnCompletionListener, OnErrorListener, OnBufferingUpdateListener,
        OnInfoListener, OnAudioFocusChangeListener {

    /** The episode manager handle */
    private EpisodeManager episodeManager;
    /** Current episode */
    private Episode currentEpisode;
    /** Our MediaPlayer handle */
    private MediaPlayer player;
    /** Is the player prepared ? */
    private boolean prepared = false;
    /** Is the player currently buffering ? */
    private boolean buffering = false;
    /** Do we have audio focus ? */
    private boolean hasFocus = false;

    /** Binder given to clients */
    private final IBinder binder = new PlayServiceBinder();
    /** The call-back set for the play service listeners */
    private Set<PlayServiceListener> listeners = new HashSet<PlayServiceListener>();

    /** Our wifi lock */
    private WifiLock wifiLock;
    /** Our notification id */
    private static final int NOTIFICATION_ID = 1;

    /**
     * The binder to return to client.
     */
    public class PlayServiceBinder extends Binder {

        /**
         * @return The service binder.
         */
        public PlayEpisodeService getService() {
            // Return this instance of this service, so clients can call public
            // methods
            return PlayEpisodeService.this;
        }
    }

    /** Receiver for unplugging headphones */
    private final BroadcastReceiver receiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            pause();
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();

        wifiLock = ((WifiManager) getSystemService(Context.WIFI_SERVICE))
                .createWifiLock(WifiManager.WIFI_MODE_FULL, "mylock");

        IntentFilter filter = new IntentFilter();
        filter.addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        registerReceiver(receiver, filter);

        episodeManager = EpisodeManager.getInstance();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        if (currentEpisode == null) {
            stopSelf();

            Log.i(getClass().getSimpleName(),
                    "Service stopped since no clients are bound anymore and no episode is loaded");
        }

        return false;
    }

    @Override
    public void onDestroy() {
        reset();

        listeners = null;
        unregisterReceiver(receiver);
    }

    /**
     * @return Whether the player is currently playing.
     */
    public boolean isPlaying() {
        return player != null && player.isPlaying();
    }

    /**
     * Register a play service listener.
     * 
     * @param listener Listener to add.
     */
    public void addPlayServiceListener(PlayServiceListener listener) {
        listeners.add(listener);
    }

    /**
     * Unregister a play service listener.
     * 
     * @param listener Listener to remove.
     */
    public void removePlayServiceListener(PlayServiceListener listener) {
        listeners.remove(listener);
    }

    /**
     * Load and start playback for given episode. Will end any current playback.
     * 
     * @param episode Episode to play (not <code>null</code>).
     */
    public void playEpisode(Episode episode) {
        if (episode != null) {
            // Stop and release the current player and reset variables
            reset();

            this.currentEpisode = episode;

            // Start playback for new episode
            try {
                initPlayer();

                Log.i(getClass().getSimpleName(),
                        "Downloaded: " + episodeManager.isDownloaded(episode));

                // Play local file
                if (episodeManager.isDownloaded(episode))
                    player.setDataSource(episodeManager.getLocalPath(episode));
                // Need to resort to remote file
                else {
                    player.setDataSource(episode.getMediaUrl().toString());
                    wifiLock.acquire();
                }

                player.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
                player.prepareAsync(); // might take long! (for buffering, etc)
            } catch (Exception e) {
                Log.e(getClass().getSimpleName(), "Prepare/Play failed for episode: " + episode, e);
            }
        }
    }

    /**
     * Pause current playback.
     */
    public void pause() {
        if (currentEpisode == null)
            Log.w(getClass().getSimpleName(), "Called pause without setting episode");
        else if (prepared && isPlaying())
            player.pause();
    }

    /**
     * Resume to play current episode.
     */
    public void resume() {
        if (currentEpisode == null)
            Log.w(getClass().getSimpleName(), "Called resume without setting episode");
        else if (!hasFocus)
            Log.w(getClass().getSimpleName(), "Called resume without having audio focus");
        else if (prepared && !isPlaying())
            player.start();
    }

    /**
     * Seek player to given location in media file.
     * 
     * @param seconds Seconds from the start to seek to.
     */
    public void seekTo(int seconds) {
        if (isPrepared() && seconds >= 0 && seconds <= getDuration())
            player.seekTo(seconds * 1000); // multiply to get millis
    }

    /**
     * @return Whether the service is currently preparing, i.e. buffering data
     *         and will start playing asap.
     */
    public boolean isPreparing() {
        return currentEpisode != null && !prepared;
    }

    /**
     * @return Whether the service is prepared, i.e. any episode is loaded.
     */
    public boolean isPrepared() {
        return prepared;
    }

    /**
     * @return Whether the service is currently buffering data.
     */
    public boolean isBuffering() {
        return buffering || isPreparing();
    }

    /**
     * Checks whether the currently loaded episode is equal to the one given.
     * The check we be true regardless of whether the episode has been actually
     * prepared or not.
     * 
     * @param episode Episode to check for.
     * @return true iff given episode is loaded (or loading), false otherwise.
     */
    public boolean loadedEpisode(Episode episode) {
        return currentEpisode != null && currentEpisode.equals(episode);
    }

    /**
     * @return The episode currently loaded.
     */
    public Episode getCurrentEpisode() {
        return currentEpisode;
    }

    /**
     * @return The title of the currently loaded episode (if any, might be
     *         <code>null</code>).
     */
    public String getCurrentEpisodeName() {
        if (currentEpisode == null)
            return null;
        else
            return currentEpisode.getName();
    }

    /**
     * @return The title of the currently loaded episode's podcast (if any,
     *         might be <code>null</code>).
     */
    public String getCurrentEpisodePodcastName() {
        if (currentEpisode != null)
            return currentEpisode.getPodcast().getName();
        else
            return null;
    }

    /**
     * @return Current position of playback in seconds from media start. Does
     *         not throw any exception but returns at least zero.
     */
    public int getCurrentPosition() {
        if (player == null || !prepared)
            return 0;
        else
            return player.getCurrentPosition() / 1000;
    }

    /**
     * @return Duration of media element in seconds. Does not throw any
     *         exception but returns at least zero.
     */
    public int getDuration() {
        if (player == null || !prepared)
            return 0;
        else
            return player.getDuration() / 1000;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        prepared = true;

        // Try to get audio focus
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int result = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN);

        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            hasFocus = true;
            player.start();

            putForeground();

            if (listeners.size() > 0)
                for (PlayServiceListener listener : listeners)
                    listener.onReadyToPlay();
            else
                Log.w(getClass().getSimpleName(), "Episode prepared, but no listener attached");
        }
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {
        if (listeners.size() > 0)
            for (PlayServiceListener listener : listeners)
                listener.onBufferUpdate(getDuration() * percent / 100);
        else
            Log.w(getClass().getSimpleName(), "Buffer state changed, but no listener attached");
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        if (listeners.size() > 0)
            for (PlayServiceListener listener : listeners)
                listener.onPlaybackComplete();
        else {
            reset();
            Log.w(getClass().getSimpleName(),
                    "Episode playback completed, but no listener attached");
        }
    }

    @Override
    public boolean onInfo(MediaPlayer mp, int what, int extra) {
        switch (what) {
            case MediaPlayer.MEDIA_INFO_BUFFERING_START:
                buffering = true;

                for (PlayServiceListener listener : listeners)
                    listener.onStopForBuffering();

                break;

            case MediaPlayer.MEDIA_INFO_BUFFERING_END:
                buffering = false;

                for (PlayServiceListener listener : listeners)
                    listener.onResumeFromBuffering();

                break;
        }

        if (listeners.size() == 0)
            Log.w(getClass().getSimpleName(), "Media player send info, but no listener attached");

        return listeners.size() > 0
                && (what == MediaPlayer.MEDIA_INFO_BUFFERING_START || what == MediaPlayer.MEDIA_INFO_BUFFERING_END);
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        if (listeners.size() > 0)
            for (PlayServiceListener listener : listeners)
                listener.onError();
        else {
            reset();
            Log.w(getClass().getSimpleName(), "Media player send error, but no listener attached");
        }

        return true;
    }

    /**
     * Reset the service to creation state.
     */
    public void reset() {
        // Stop current playback if any
        if (isPlaying())
            player.stop();
        // Reset variables
        this.currentEpisode = null;
        this.prepared = false;
        this.buffering = false;
        // Release resources
        ((AudioManager) getSystemService(Context.AUDIO_SERVICE)).abandonAudioFocus(this);
        hasFocus = false;

        if (wifiLock.isHeld())
            wifiLock.release();
        stopForeground(true);

        if (player != null) {
            player.release();
            player = null;
        }
    }

    private void initPlayer() {
        player = new MediaPlayer();

        player.setAudioStreamType(AudioManager.STREAM_MUSIC);
        player.setOnPreparedListener(this);
        player.setOnCompletionListener(this);
        player.setOnErrorListener(this);
        player.setOnInfoListener(this);
        player.setOnBufferingUpdateListener(this);
    }

    private void putForeground() {
        // This will bring back to app
        PendingIntent pendingIntent = PendingIntent.getActivity(
                getApplicationContext(),
                0,
                new Intent(getApplicationContext(), PodcastActivity.class)
                        .putExtra(EpisodeListActivity.MODE_KEY, ContentMode.SINGLE_PODCAST)
                        .putExtra(EpisodeListActivity.PODCAST_URL_KEY,
                                currentEpisode.getPodcast().getUrl().toString())
                        .putExtra(EpisodeActivity.EPISODE_URL_KEY,
                                currentEpisode.getMediaUrl().toString())
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
                                Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_UPDATE_CURRENT);

        // Prepare the notification
        Notification notification = new Notification.Builder(getApplicationContext())
                .setContentIntent(pendingIntent)
                .setTicker(currentEpisode.getName())
                .setSmallIcon(R.drawable.ic_stat)
                .setContentTitle(currentEpisode.getName())
                .setContentText(currentEpisode.getPodcast().getName())
                .setContentInfo(getString(R.string.app_name))
                .setWhen(0)
                .setOngoing(true).getNotification();

        startForeground(NOTIFICATION_ID, notification);
    }

    @Override
    public void onAudioFocusChange(int focusChange) {

        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_GAIN:
                hasFocus = true;

                player.setVolume(1.0f, 1.0f);
                break;

            case AudioManager.AUDIOFOCUS_LOSS:
                // Lost focus for an unbounded amount of time: stop playback and
                // release media player
                hasFocus = false;

                if (isPlaying())
                    player.stop();
                onCompletion(player);
                break;

            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                // Lost focus for a short time, but we have to stop
                // playback. We don't release the media player because playback
                // is likely to resume
                hasFocus = false;

                if (isPlaying())
                    pause();
                break;

            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                // Lost focus for a short time, but it's ok to keep playing
                // at an attenuated level
                hasFocus = false;

                if (isPlaying())
                    player.setVolume(0.1f, 0.1f);
                break;
        }
    }
}
