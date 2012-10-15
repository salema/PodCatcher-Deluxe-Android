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

import net.alliknow.podcatcher.Podcatcher;
import net.alliknow.podcatcher.R;
import net.alliknow.podcatcher.services.PlayEpisodeService;
import net.alliknow.podcatcher.services.PlayEpisodeService.OnPlaybackCompleteListener;
import net.alliknow.podcatcher.services.PlayEpisodeService.OnReadyToPlayListener;
import net.alliknow.podcatcher.services.PlayEpisodeService.PlayEpisodeBinder;
import net.alliknow.podcatcher.types.Episode;
import android.app.Activity;
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
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.TextView;

/**
 * Fragment showing episode details
 * 
 * @author Kevin Hausmann
 */
public class EpisodeFragment extends Fragment implements OnReadyToPlayListener, OnPlaybackCompleteListener {

	/** The load episode menu bar item */
	private MenuItem loadMenuItem;
	/** The episode title view */
	private TextView episodeTitleView;
	/** The podcast title view */
	private TextView podcastTitleView;
	/** The episode description web view */
	private WebView episodeDetailView;
	/** The player title view */
	private View playerDividerView;
	/** The player title view */
	private TextView playerTitleView;
	/** The player progress view */
	private View playerProgress;
	/** The player view */
	private View playerView;
	/** The play/pause button */
	private Button playPauseButton;
		
	/** Current episode */
	private Episode episode;
	
	/** Play service */
	private PlayEpisodeService service;
	/** Whether we are currently playing an episode */
	private boolean plays = false;
	
	/** Play update timer task */
	private Timer playUpdateTimer = new Timer();
	/** Play update timer task */
	private TimerTask playUpdateTimerTask;
		
	private class PlayProgressTask extends TimerTask {

		@Override
		public void run() {
			if (EpisodeFragment.this == null || EpisodeFragment.this.getActivity() == null) return;
			
			EpisodeFragment.this.getActivity().runOnUiThread(new Runnable() {
				
				@Override
				public void run() {
					updatePlayButton();
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
		
		episodeTitleView = (TextView) getView().findViewById(R.id.episode_title);
		podcastTitleView = (TextView) getView().findViewById(R.id.podcast_title);
		episodeDetailView = (WebView) getView().findViewById(R.id.episode_description);
		episodeDetailView.getSettings().setDefaultFontSize(getResources().getDimensionPixelSize(R.dimen.default_font_size));
		
		playerDividerView = getView().findViewById(R.id.player_divider);
		playerTitleView = (TextView) getView().findViewById(R.id.player_title);
		playerProgress = getView().findViewById(R.id.player_progress);
		playerView = view.findViewById(R.id.player);
		playPauseButton = (Button) view.findViewById(R.id.playPause);
		playPauseButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				togglePlay();
			}
		});
		
		// Restore from configuration change 
		if (episode != null) setEpisode(episode);
		
		playerView.setVisibility(plays ? View.VISIBLE : View.GONE);
		updatePlayButton();
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.episode_menu, menu);
		
		loadMenuItem = menu.findItem(R.id.load);
		loadMenuItem.setVisible(episode != null);
		loadMenuItem.setEnabled(episode != null && service != null && ! episode.equals(service.getCurrentEpisode()));
		loadMenuItem.setShowAsAction(episode == null ? MenuItem.SHOW_AS_ACTION_NEVER : MenuItem.SHOW_AS_ACTION_ALWAYS);
	}
	
	@Override
	public void onStart() {
		super.onStart();
		
		// Make sure the service runs as long as this fragment exists
    	getActivity().startService(new Intent(getActivity(), PlayEpisodeService.class));
	}
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		
		// Attach to play service via this fragment's activity
		Intent intent = new Intent(getActivity(), PlayEpisodeService.class);
    	getActivity().bindService(intent, connection, Context.BIND_AUTO_CREATE);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.load) loadEpisode();
		
		return item.getItemId() == R.id.load;
	}
	
	@Override
	public void onDetach() {
		super.onDetach();
		
		// Detach from play service via this fragment's activity
		getActivity().unbindService(connection);
		
		// Stop progress update task if existing
		if (playUpdateTimerTask != null) playUpdateTimerTask.cancel();
	}
		
	@Override
	public void onDestroy() {
		super.onDestroy();
		        
        // Make sure the service is stopped on destroy of this fragment
		// TODO Do we actually want this??? (playback will stop on back button press
        //getActivity().stopService(new Intent(getActivity(), PlayEpisodeService.class));
		
		playUpdateTimer.cancel();
	}
	
	/**
	 * Set the displayed episode, all UI will be updated
	 * @param selectedEpisode Episode to show (cannot be null)
	 */
	public void setEpisode(Episode selectedEpisode) {
		this.episode = selectedEpisode;
		
		getView().findViewById(android.R.id.empty).setVisibility(View.GONE);
		
		episodeTitleView.setVisibility(View.VISIBLE);
		episodeTitleView.setText(episode.getName());
		podcastTitleView.setText(episode.getPodcast().getName());
		podcastTitleView.setVisibility(View.VISIBLE);
		getView().findViewById(R.id.episode_divider).setVisibility(View.VISIBLE);
						
		episodeDetailView.loadDataWithBaseURL(null, episode.getDescription(), "text/html", "utf-8", null);
		episodeDetailView.setVisibility(View.VISIBLE);
		
		loadMenuItem.setVisible(true);
		loadMenuItem.setEnabled(! episode.equals(service.getCurrentEpisode()));
		loadMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		
		if (service.getCurrentEpisode() != null && !episode.equals(service.getCurrentEpisode())) {
			playerDividerView.setVisibility(View.VISIBLE);
			playerTitleView.setVisibility(View.VISIBLE);
		} else {
			playerDividerView.setVisibility(View.GONE);
			playerTitleView.setVisibility(View.GONE);
		}
	}
		
	@Override
	public void onReadyToPlay() {
		plays = true;
		
		playerProgress.setVisibility(View.GONE);
		updatePlayButton();
		playerView.setVisibility(View.VISIBLE);
		
		playUpdateTimerTask = new PlayProgressTask();
		playUpdateTimer.schedule(playUpdateTimerTask, 0, 1000);
	}
	
	@Override
	public void onPlaybackComplete() {
		plays = false;
		playUpdateTimerTask.cancel();
		
		service.reset();
	}
	
	private void loadEpisode() {
		if (episode != null && service != null) {		
			// Episode should not be loaded
			if (! episode.equals(service.getCurrentEpisode())) {
				if (playUpdateTimerTask != null) playUpdateTimerTask.cancel();
				playerView.setVisibility(View.GONE);
				playerDividerView.setVisibility(View.GONE);
				playerTitleView.setVisibility(View.GONE);
				playerTitleView.setText(episode.getName() + " - " + episode.getPodcast().getName());
				playerProgress.setVisibility(View.VISIBLE);
				
				service.playEpisode(episode);
				loadMenuItem.setEnabled(false);
			}
		} else Log.d(getClass().getSimpleName(), "Cannot load episode (episode or service are null)");
	}
	
	private void togglePlay() {
		if (service != null && service.getCurrentEpisode() != null) {		
			// Player is playing
			if (plays) {
				service.pause();
				playUpdateTimerTask.cancel();
			} // Player in pause
			else {
				service.resume();
				
				playUpdateTimerTask = new PlayProgressTask();
				playUpdateTimer.schedule(playUpdateTimerTask, 0, 1000);
			}
			
			plays = !plays;
			updatePlayButton();
		} else Log.d(getClass().getSimpleName(), "Cannot play episode (episode or service are null)");
	}
	
	private void updatePlayButton() {
		playPauseButton.setText(plays ? R.string.pause : R.string.resume);
		playPauseButton.setBackgroundResource(plays ? R.drawable.button_red : R.drawable.button_green);
		
		if (isAdded() && service != null && service.getDuration() > 0) {
			final String position = Podcatcher.formatTime(service.getCurrentPosition());
			final String duration = Podcatcher.formatTime(service.getDuration());
			
			playPauseButton.setText(playPauseButton.getText() + " " + getResources().getString(R.string.at) +
					" " + position + " " + getResources().getString(R.string.of) + " " + duration);
		}
	}
	
	/** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection connection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder serviceBinder) {
        	PlayEpisodeBinder binder = (PlayEpisodeBinder) serviceBinder;
            service = binder.getService();
            service.setReadyToPlayListener(EpisodeFragment.this);
            service.setPlaybackCompleteListener(EpisodeFragment.this);
            Log.d(EpisodeFragment.this.getClass().getSimpleName(), "Bound to playback service");
        
            Episode serviceEpisode = service.getCurrentEpisode();
            if (serviceEpisode != null && !serviceEpisode.equals(EpisodeFragment.this.episode)) {
            	playerDividerView.setVisibility(View.VISIBLE);
    			playerTitleView.setVisibility(View.VISIBLE);
    			playerTitleView.setText(serviceEpisode.getName() + " - " + serviceEpisode.getPodcast().getName());
            }
            if (service.getDuration() > 0) {
            	updatePlayButton();
        		playerView.setVisibility(View.VISIBLE);
            }
            if (service.isPlaying()) {
            	plays = true;
            	
        		playUpdateTimerTask = new PlayProgressTask();
        		playUpdateTimer.schedule(playUpdateTimerTask, 0, 1000);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            Log.d(EpisodeFragment.this.getClass().getSimpleName(), "Unbound from playback service");
        }
    };
}
