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
 * This class implements a Android service. It can be used to play back
 * podcast episodes and tries to hide away the complexity of the media
 * player support in Android. All methods should fail gracefully.
 * 
 * Connect to the service from your activity/fragment to use it. Also
 * implement the listeners defined here for interaction.
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
    private final IBinder binder = new PlayServiceBinder();
    
    /**
     * The binder to return to client. 
     */
	public class PlayServiceBinder extends Binder {
		public PlayEpisodeService getService() {
            // Return this instance of this service, so clients can call public methods
            return PlayEpisodeService.this;
        }
    }
	
	/**
	 * Listener interface to implement if you are interested to be alerted
	 * when the service loaded an episode and is prepared to play it back.
	 */
	public interface OnReadyToPlayListener {
		
		/**
		 * Called by the service on the listener if an episode is loaded
		 * and ready to play (the service might in fact already have started
		 * playback...)
		 */
		public void onReadyToPlay();
	}
	
	/**
	 * Listener interface to implement if you are interested to be alerted
	 * when the service finished playing an episode.
	 */
	public interface OnPlaybackCompleteListener {
		
		/**
		 * Called by the service on the listener if an episode finished playing.
		 * The service does not free resources on completion automatically,
		 * you might want to call <code>reset()</code>.
		 */
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
			
			// Stop current playback if any
			if (isPlaying()) player.stop();
			// Release the current player and reset variables
			reset();
			
			this.currentEpisode = episode;
						
			// Start playback for new episode
			try {
				initPlayer();
				player.setDataSource(episode.getMediaUrl().toExternalForm());
				player.prepareAsync(); // might take long! (for buffering, etc)
			} catch (Exception e) {
				Log.e(getClass().getSimpleName(), "Prepare/Play failed for episode: " +  episode, e);
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
	 * @return Whether the service is prepared, i.e.
	 * any episode is loaded;
	 */
	public boolean isPrepared() {
		return prepared;
	}
	
	/**
	 * Checks whether the currently loaded episode is equal to the one given
	 * @param episode Episode to check for
	 * @return true iff given episode is loaded, false otherwise
	 */
	public boolean hasPreparedEpisode(Episode episode) {
		return currentEpisode != null && currentEpisode.equals(episode);
	}
	
	/**
	 * @return The title of the currently loaded episode (if any, might be <code>null</code>)
	 */
	public String getCurrentEpisodeName() {
		if (currentEpisode == null) return null;
		else return currentEpisode.getName();
	}
	
	/**
	 * @return The title of the currently loaded episode's podcast (if any, might be <code>null</code>)
	 */
	public String getCurrentEpisodePodcastName() {
		if (currentEpisode == null) return null;
		else return currentEpisode.getPodcast().getName();
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
		else {
			reset();
			Log.d(getClass().getSimpleName(), "Episode playback completed, but no listener attached");
		}
	}
	
	@Override
	public void onDestroy() {
		Log.d(getClass().getSimpleName(), "Service destroyed");
		
		reset();
		
		readyListener = null;
		completeListener = null;
	}
	
	/**
	 * Reset the service to creation state
	 */
	public void reset() {
		this.currentEpisode = null;
		this.prepared = false;
		
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
	}
}
