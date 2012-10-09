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
 * Play an episode service
 * 
 * @author Kevin Hausmann
 */
public class PlayEpisodeService extends Service implements OnPreparedListener, OnCompletionListener {

	/** Current episode */
	private Episode currentEpisode;
	/** Our MediaPlayer handle */
	private MediaPlayer player;
	/** A listener notified on preparation success */
	private OnReadyToPlayListener readyListener;
	/** Is the player preparing */
	private boolean preparing = false;
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
		Log.d("Play Service", "Service bound");
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

	public void pause() {
		Log.d("Play Service", "Pausing episode " +  currentEpisode);
		if (! preparing) player.pause();
	}
	
	public void resume() {
		Log.d("Play Service", "Resuming episode " +  currentEpisode);
		if (! preparing) player.start();
	}
	
	public void playEpisode(Episode episode) {
		Log.d("Play Service", "Loading episode " +  episode);
		this.currentEpisode = episode;
		
		if (episode != null) {		
			if (isPlaying()) {
				player.stop();
				releasePlayer();
			}
			 
			try {
				initPlayer();
				player.setDataSource(episode.getMediaUrl().toExternalForm());
				player.prepareAsync(); // might take long! (for buffering, etc)
				preparing = true;
			} catch (Exception e) {
				e.printStackTrace();
			}		
		}
	}
	
	@Override
	public void onPrepared(MediaPlayer mp) {
		preparing = false;
		if (readyListener != null) readyListener.onReadyToPlay();
		player.start();
	}
	
	@Override
	public void onCompletion(MediaPlayer mp) {
		if (completeListener != null) completeListener.onPlaybackComplete();
	}
	
	@Override
	public void onDestroy() {
		Log.d("Play Service", "Service destroyed");
		releasePlayer();
		currentEpisode = null;
		
		readyListener = null;
		completeListener = null;
	}
	
	public Episode getCurrentEpisode() {
		return currentEpisode;
	}
	
	private void initPlayer() {
		player = new MediaPlayer();
		player.setAudioStreamType(AudioManager.STREAM_MUSIC);
		player.setOnPreparedListener(this);
		player.setOnCompletionListener(this);
	}
	
	private void releasePlayer() {
		if (player != null) {
			player.release();
			player = null;
		}
	}
}
