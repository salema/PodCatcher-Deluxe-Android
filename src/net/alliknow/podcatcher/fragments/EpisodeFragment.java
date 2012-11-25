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
package net.alliknow.podcatcher.fragments;

import java.util.Timer;
import java.util.TimerTask;

import net.alliknow.podcatcher.R;
import net.alliknow.podcatcher.listeners.PlayServiceListener;
import net.alliknow.podcatcher.services.PlayEpisodeService;
import net.alliknow.podcatcher.services.PlayEpisodeService.PlayServiceBinder;
import net.alliknow.podcatcher.types.Episode;
import net.alliknow.podcatcher.views.Player;
import android.app.Fragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

/**
 * Fragment showing episode details
 * 
 * @author Kevin Hausmann
 */
public class EpisodeFragment extends Fragment implements PlayServiceListener, OnSeekBarChangeListener {

	/** The load episode menu bar item */
	private MenuItem loadMenuItem;
	/** The empty view */
	private View emptyView;
	/** The episode title view */
	private TextView episodeTitleView;
	/** The podcast title view */
	private TextView podcastTitleView;
	/** The divider view between title and description */
	private View dividerView;
	/** The episode description web view */
	private WebView episodeDetailView;
	/** The player view */
	private Player playerView;
		
	/** Current episode */
	private Episode episode;
	/** Play service */
	private PlayEpisodeService service;
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
			// This will only work, if our callback actually exists
			if (EpisodeFragment.this == null || EpisodeFragment.this.getActivity() == null) return;
			
			// Need to run on UI thread, since we want to update the play button
			EpisodeFragment.this.getActivity().runOnUiThread(new Runnable() {
				
				@Override
				public void run() {
					playerView.update(service, episode);
				}
			});
		}
	}
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	
    	setRetainInstance(true);
    	setHasOptionsMenu(true);
    }
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		
		return inflater.inflate(R.layout.episode, container, false);
	}
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		
		emptyView = getView().findViewById(android.R.id.empty);
		episodeTitleView = (TextView) getView().findViewById(R.id.episode_title);
		podcastTitleView = (TextView) getView().findViewById(R.id.podcast_title);
		episodeDetailView = (WebView) getView().findViewById(R.id.episode_description);
		dividerView = getView().findViewById(R.id.episode_divider);
		
		playerView = (Player) view.findViewById(R.id.player);
		playerView.setOnSeekBarChangeListener(this);
		playerView.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				togglePlay();
			}
		});
		playerView.setOnLongClickListener(new OnLongClickListener() {
			
			@Override
			public boolean onLongClick(View v) {
				onPlaybackComplete();
				
				return true;
			}
		});
		
		// Restore from configuration change 
		if (episode != null) setEpisode(episode);
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.episode, menu);
		
		loadMenuItem = menu.findItem(R.id.episode_load_menuitem);
		updateLoadMenuItem();
	}
	
	@Override
	public void onStart() {
		super.onStart();
		
		// Make sure the service runs as long as this fragment exists
    	getActivity().startService(new Intent(getActivity(), PlayEpisodeService.class));
    	// Attach to play service via this fragment's activity
    	Intent intent = new Intent(getActivity(), PlayEpisodeService.class);
    	getActivity().bindService(intent, connection, Context.BIND_AUTO_CREATE);  
	}
		
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
	    	case R.id.episode_load_menuitem:
	    		if (service.isWorkingWith(episode)) onPlaybackComplete();
				else loadEpisode();
	    		
	    		return true;
	    	default:
	    		return super.onOptionsItemSelected(item);
		}
	}
	
	@Override
	public void onPause() {
		super.onPause();
		
		if (service != null && service.isPrepared()) service.showNotification(true);
	}
	
	@Override
	public void onResume() {
		super.onResume();
		
		if (service != null) service.showNotification(false);
		
	}
	
	@Override
	public void onDetach() {
		super.onDetach();
		
		// Detach from service callbacks
		if (service != null) service.setPlayServiceListener(null);
		
		// Detach from play service via this fragment's activity
		getActivity().unbindService(connection);
				
		// Stop progress update task if existing
		stopPlayProgressTimer();
	}
		
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		playUpdateTimer.cancel();
	}
	
	/**
	 * Set the displayed episode, all UI will be updated
	 * @param selectedEpisode Episode to show (cannot be null)
	 */
	public void setEpisode(Episode selectedEpisode) {
		if (selectedEpisode != null) {
			this.episode = selectedEpisode;
			
			emptyView.setVisibility(View.GONE);
			
			episodeTitleView.setVisibility(View.VISIBLE);
			episodeTitleView.setText(episode.getName());
			podcastTitleView.setText(episode.getPodcastName());
			podcastTitleView.setVisibility(View.VISIBLE);
			dividerView.setVisibility(View.VISIBLE);
							
			episodeDetailView.loadDataWithBaseURL(null, episode.getDescription(), "text/html", "utf-8", null);
			episodeDetailView.setVisibility(View.VISIBLE);
			
			updateLoadMenuItem();
			playerView.update(service, episode);
		}
	}
		
	@Override
	public void onReadyToPlay() {
		playerView.update(service, episode);
		startPlayProgressTimer();
	}
	
	@Override
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
		if (fromUser) {
			service.seekTo(progress);
			playerView.update(service, episode);
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
		playerView.update(service, episode);
	}

	@Override
	public void onResumeFromBuffering() {
		onReadyToPlay();
	}
	
	@Override
	public void onBufferUpdate(int seconds) {
		playerView.setSecondaryProgress(seconds);
	}

	@Override
	public void onPlaybackComplete() {
		stopPlayProgressTimer();
		
		service.reset();
		
		updateLoadMenuItem();
		playerView.update(service, episode);
	}
	
	@Override
	public void onError() {
		service.reset();
		
		updateLoadMenuItem();
		playerView.update(service, episode);
		
		playerView.showError();
		
		Log.w(getClass().getSimpleName(), "Play service send an error");
	}
	
	private void loadEpisode() {
		if (episode != null && service != null) {		
			// Episode should not be loaded
			if (! service.isWorkingWith(episode)) {
				stopPlayProgressTimer();
				
				service.playEpisode(episode);
								
				updateLoadMenuItem();
				playerView.update(service, episode);
			}
		} else Log.d(getClass().getSimpleName(), "Cannot load episode (episode or service are null)");
	}
	
	private void togglePlay() {
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
			
			playerView.update(service, episode);
		} else Log.d(getClass().getSimpleName(), "Cannot play/pause episode (service null or unprepared)");
	}
	
	private void updateLoadMenuItem() {
		if (loadMenuItem != null) {		
			loadMenuItem.setVisible(episode != null && service != null);
			
			if (loadMenuItem.isVisible()) {
				loadMenuItem.setTitle(service.isWorkingWith(episode) ? R.string.stop : R.string.play );
				loadMenuItem.setIcon(service.isWorkingWith(episode) ? R.drawable.ic_media_stop : R.drawable.ic_media_play);
			}
		}
	}
	
	private void startPlayProgressTimer() {
		// Only start task if it isn't already running and
		// there is actually some progress to monitor 
		if (playUpdateTimerTask == null && !seeking && service.isPlaying()) {
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
            Log.d(EpisodeFragment.this.getClass().getSimpleName(), "Bound to playback service");
            
            // Register listener and notification
            service.setPlayServiceListener(EpisodeFragment.this);
            service.showNotification(false);
            
            // Update UI to reflect service status
            updateLoadMenuItem();
            playerView.update(service, episode);
            // Restart play progress timer task if service is playing
            startPlayProgressTimer();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            Log.d(EpisodeFragment.this.getClass().getSimpleName(), "Unbound from playback service");
        }
    };
}
