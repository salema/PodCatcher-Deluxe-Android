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
	/** The player view */
	private View playerView;
	/** The play/pause button */
	private Button playPauseButton;
	/** The play position text view */
	private TextView playPositionView;
	
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
			final String position = formatTime(service.getCurrentPosition());
			final String duration = formatTime(service.getDuration());
			final String of = getResources().getString(R.string.of);
			
			EpisodeFragment.this.getActivity().runOnUiThread(new Runnable() {
				
				@Override
				public void run() {
					playPositionView.setText(position + " " + of + " " + duration);
				}
			});
		}
		
		private String formatTime(int time) {
			int hours = (int) Math.floor(time / 3600);
			
			int minutes = (int) (Math.floor(time / 60) - 60 * hours);
			int seconds = (int) (Math.floor(time) % 60);
			
			String minutesString = this.formatNumber(minutes, hours > 0);
			String secondsString = this.formatNumber(seconds, true);
			
			if (hours > 0) return hours + ":" + minutesString + ":" + secondsString;
			else return minutesString + ":" + secondsString; 
		}
		
		private String formatNumber(int number, boolean makeTwoDigits) {
			if (number < 10 && makeTwoDigits) return "0" + number;
			else return number + "";
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
		
		playerView = view.findViewById(R.id.player);
		playPauseButton = (Button) view.findViewById(R.id.playPause);
		playPauseButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				togglePlay();
			}
		});
		playPositionView = (TextView) view.findViewById(R.id.playPosition);
		
		// Restore from configuration change 
		if (episode != null) setEpisode(episode);
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.episode_menu, menu);
		
		loadMenuItem = menu.findItem(R.id.load);
		updateLoadMenuItem();
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
		getView().findViewById(R.id.episode_divider).setVisibility(View.VISIBLE);
		((TextView) getView().findViewById(R.id.podcast_title)).setVisibility(View.VISIBLE);
		((TextView) getView().findViewById(R.id.podcast_title)).setText(episode.getPodcast().getName());
		((TextView) getView().findViewById(R.id.episode_title)).setVisibility(View.VISIBLE);
		((TextView) getView().findViewById(R.id.episode_title)).setText(episode.getName());
				
		WebView view = (WebView) getView().findViewById(R.id.episode_description);
		view.getSettings().setDefaultFontSize(getResources().getDimensionPixelSize(R.dimen.default_font_size));
		view.loadDataWithBaseURL(null, episode.getDescription(), "text/html", "utf-8", null);
		view.setVisibility(View.VISIBLE);
		
		loadMenuItem.setVisible(true);
		updateLoadMenuItem();
	}
	
	
	
	@Override
	public void onReadyToPlay() {
		plays = true;
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
				
				service.playEpisode(episode);
				loadMenuItem.setEnabled(false);
			}
		} else Log.d(getClass().getSimpleName(), "Cannot load episode (episode or service are null)");
	}
	
	private void togglePlay() {
		if (episode != null && service != null) {		
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
			playPauseButton.setText(plays ? R.string.pause : R.string.play);
		} else Log.d(getClass().getSimpleName(), "Cannot play episode (episode or service are null)");
	}
	
	private void updateLoadMenuItem() {
		// Enabled
		loadMenuItem.setEnabled(episode != null && service != null && 
				! episode.equals(service.getCurrentEpisode()));
		
		// State
		loadMenuItem.setShowAsAction(episode == null ? 
				MenuItem.SHOW_AS_ACTION_NEVER : MenuItem.SHOW_AS_ACTION_ALWAYS);
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
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            Log.d(EpisodeFragment.this.getClass().getSimpleName(), "Unbound from playback service");
        }
    };
}
