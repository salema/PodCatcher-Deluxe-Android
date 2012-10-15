/** Copyright 2012 Kevin Hausmann
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

import net.alliknow.podcatcher.types.Episode;
import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

/**
 * Play an episode service, wraps media player.
 * 
 * @author Kevin Hausmann
 */
public class PlayEpisodeService extends Service implements OnPreparedListener, OnCompletionListener {

	/** Current episode */
	private Episode currentEpisode;
	/** Our MediaPlayer handle */
	private MediaPlayer player;
	/** Is the player prepared ? */
	private boolean prepared = false;
	
	/** A listener notified on preparation success */
	private OnReadyToPlayListener readyListener;
	/** A listener notified on playback completion */
	private OnPlaybackCompleteListener completeListener;
	
	/** Binder given to clients */
    private final IBinder binder = new PlayEpisodeBinder();
    
	public class PlayEpisodeBinder extends Binder {
		public PlayEpisodeService getService() {
            // Return this instance of LocalService so clients can call public methods
            return PlayEpisodeService.this;
        }
    }
	
	public interface OnReadyToPlayListener {
		public void onReadyToPlay();
	}
	
	public interface OnPlaybackCompleteListener {
		public void onPlaybackComplete();
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		Log.d(getClass().getSimpleName(), "Service bound");
		return binder;
	}
	
	/**
	 * @return Whether the player is currently playing
	 */
	public boolean isPlaying() {
		return player != null && player.isPlaying();
	}
	
	/**
	 * @param readyListener A listener to be alerted on preparation success
	 */
	public void setReadyToPlayListener(OnReadyToPlayListener readyListener) {
		this.readyListener = readyListener;
	}
	
	/**
	 * @param readyListener A listener to be alerted on playback completion
	 */
	public void setPlaybackCompleteListener(OnPlaybackCompleteListener completeListener) {
		this.completeListener = completeListener;
	}

	/**
	 * Load and start playback for given episode. Will end any current playback.
	 * @param episode Episode to play (not null)
	 */
	public void playEpisode(Episode episode) {
		if (episode != null) {
			Log.d(getClass().getSimpleName(), "Loading episode " +  episode);
			
			this.prepared = false;
			this.currentEpisode = episode;
			
			// Stop current playback if any
			if (isPlaying()) player.stop();
			// Release the current player
			releasePlayer();
						
			// Start playback for new episode
			try {
				initPlayer();
				player.setDataSource(episode.getMediaUrl().toExternalForm());
				player.prepareAsync(); // might take long! (for buffering, etc)
			} catch (Exception e) {
				Log.e(getClass().getSimpleName(), "Play failed for episode: " +  episode, e);
			}		
		}
	}
	
	/**
	 * Pause current playback
	 */
	public void pause() {
		if (currentEpisode == null) Log.d(getClass().getSimpleName(), "Called pause without setting episode");
		else if (prepared && isPlaying()) player.pause();
	}
	
	/**
	 * Resume to play current episode
	 */
	public void resume() {
		if (currentEpisode == null) Log.d(getClass().getSimpleName(), "Called resume without setting episode");
		else if (prepared && !isPlaying()) player.start();
	}
	
	/**
	 * @return The episode currently loaded by the service (may be null)
	 */
	public Episode getCurrentEpisode() {
		return currentEpisode;
	}
	
	/**
	 * @return Whether the service is prepared, i.e.
	 * any episode is loaded;
	 */
	public boolean isPrepared() {
		return prepared;
	}
	
	/**
	 * @return Current position of playback in seconds from media start
	 * Does not throw any exception but returns at least zero 
	 */
	public int getCurrentPosition() {
		if (player == null || !prepared) return 0;
		else return player.getCurrentPosition() / 1000;
	}
	
	/**
	 * @return Duration of media element in seconds
	 * Does not throw any exception but returns at least zero 
	 */
	public int getDuration() {
		if (player == null || !prepared) return 0;
		else return player.getDuration() / 1000;
	}
	
	/**
	 * Reset the service to creation state
	 */
	public void reset() {
		this.currentEpisode = null;
		this.prepared = false;
		
		releasePlayer();
	}
	
	@Override
	public void onPrepared(MediaPlayer mp) {
		prepared = true;
		player.start();
		
		if (readyListener != null) readyListener.onReadyToPlay();
		else Log.d(getClass().getSimpleName(), "Episode prepared, but no listener attached");
	}
	
	@Override
	public void onCompletion(MediaPlayer mp) {
		if (completeListener != null) completeListener.onPlaybackComplete();
		else Log.d(getClass().getSimpleName(), "Episode playback completed, but no listener attached");
	}
	
	@Override
	public void onDestroy() {
		Log.d(getClass().getSimpleName(), "Service destroyed");
		releasePlayer();
		currentEpisode = null;
		
		readyListener = null;
		completeListener = null;
	}
	
	private void initPlayer() {
		player = new MediaPlayer();
		player.setAudioStreamType(AudioManager.STREAM_MUSIC);
		player.setOnPreparedListener(this);
		player.setOnCompletionListener(this);
	}
	
	private void releasePlayer() {
		this.prepared = false;
		
		if (player != null) {
			player.release();
			player = null;
		}
	}
}
