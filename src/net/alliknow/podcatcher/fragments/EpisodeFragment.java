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
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.TextView;

/**
 * Fragment showing episode details
 * 
 * @author Kevin Hausmann
 */
public class EpisodeFragment extends Fragment implements OnReadyToPlayListener, OnPlaybackCompleteListener {

	/** The play button */
	private MenuItem playButton;
	/** Whether we are currently playing an episode */
	private boolean plays = false;
	/** Current episode */
	private Episode episode;
	/** Play service */
	private PlayEpisodeService service;
	
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
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.episode_menu, menu);
		
		playButton = menu.findItem(R.id.play);
	}
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		
		// Restore from configuration change 
		if (episode != null) setEpisode(episode);
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
		if (item.getItemId() == R.id.play) togglePlay();
		
		return item.getItemId() == R.id.play;
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
        getActivity().stopService(new Intent(getActivity(), PlayEpisodeService.class));
	}
	
	/**
	 * Set the displayed episode, all UI will be updated
	 * @param selectedEpisode Episode to show (cannot be null)
	 */
	public void setEpisode(Episode selectedEpisode) {
		if (episode == null || (selectedEpisode != null && !episode.equals(selectedEpisode))) {
			this.episode = selectedEpisode;
			
			getView().findViewById(R.id.episode_divider).setVisibility(View.VISIBLE);
			((TextView) getView().findViewById(R.id.podcast_title)).setText(episode.getPodcast().getName());
			((TextView) getView().findViewById(R.id.episode_title)).setText(episode.getName());
					
			WebView view = (WebView) getView().findViewById(R.id.episode_description);
			view.getSettings().setDefaultFontSize(12);
			view.loadDataWithBaseURL(null, episode.getDescription(), "text/html", "utf-8", null);
			
			playButton.setEnabled(true);
			playButton.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
			
			if (selectedEpisode.equals(service.getCurrentEpisode()))
				playButton.setTitle(plays ? R.string.pause : R.string.play);
			else playButton.setTitle(R.string.play);
		}
	}
	
	@Override
	public void onReadyToPlay() {
		playButton.setEnabled(true);
	}
	
	public void togglePlay() {
		if (episode == null) return;
		
		// Episode not played before
		if (! episode.equals(service.getCurrentEpisode())) {
			plays = false;
			service.playEpisode(episode);
			playButton.setEnabled(false);
		}
		// Player in pause
		else if (! plays) service.resume();
		// Player playing
		else service.pause();
		
		plays = !plays;
		
		playButton.setTitle(plays ? R.string.pause : R.string.play);
	}
	
	@Override
	public void onPlaybackComplete() {
		playButton.setEnabled(false);
		plays = false;
	}
	
	/** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection connection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder serviceBinder) {
        	PlayEpisodeBinder binder = (PlayEpisodeBinder) serviceBinder;
            service = binder.getService();
            service.setReadyToPlayListener(EpisodeFragment.this);
            service.setPlaybackCompleteListener(EpisodeFragment.this);
            Log.d("Play Service", "Bound to service");
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            Log.d("Play Service", "Unbound from service");
        }
    };
}
