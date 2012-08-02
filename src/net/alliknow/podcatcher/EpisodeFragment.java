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
package net.alliknow.podcatcher;

import java.io.IOException;

import net.alliknow.podcatcher.types.Episode;
import android.app.Fragment;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.Button;

/**
 * Fragment showing episode details
 * 
 * @author Kevin Hausmann
 */
public class EpisodeFragment extends Fragment implements OnPreparedListener {

	/** Current episode */
	private Episode episode; 
	/** Our MediaPlayer handle */
	private MediaPlayer player;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		
		View view = inflater.inflate(R.layout.episode, container, false);
		
		Button button = (Button) view.findViewById(R.id.play_button);
		button.setOnClickListener(new View.OnClickListener() {
		    public void onClick(View v) {
		        playEpisode();
		    }
		});
		
		return view;
	}
	
	@Override
	public void onPause() {
		super.onPause();
		
		this.releasePlayer();
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		this.releasePlayer();
	}

	/**
	 * Set the displayed episode, all UI will be updated
	 * @param selectedEpisode Episode to show
	 */
	public void setEpisode(Episode selectedEpisode) {
		this.episode = selectedEpisode;
		
		((WebView) getView().findViewById(R.id.episode_description))
			.loadData(this.episode.getDescription(), "text/html", null);
	}
	
	public void playEpisode() {
		if (this.player != null && this.player.isPlaying()) {
			this.player.stop();
			this.releasePlayer();
		}
		else 
			try {
				this.initPlayer();
				this.player.setDataSource(this.episode.getMediaUrl().toExternalForm());
				this.player.prepareAsync(); // might take long! (for buffering, etc)
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (SecurityException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalStateException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}		
	}

	@Override
	public void onPrepared(MediaPlayer mp) {
		this.player.start();
	}
	
	private void initPlayer() {
		this.player = new MediaPlayer();
		this.player.setAudioStreamType(AudioManager.STREAM_MUSIC);
		this.player.setOnPreparedListener(this);
	}
	
	private void releasePlayer() {
		if (this.player != null) {
			this.player.release();
			this.player = null;
		}
	}
}
