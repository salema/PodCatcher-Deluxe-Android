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
import net.alliknow.podcatcher.services.PlayEpisodeService.PlayEpisodeBinder;
import net.alliknow.podcatcher.types.Episode;
import android.app.Fragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
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
public class EpisodeFragment extends Fragment {

	/** The play button */
	private MenuItem playButton;
	private boolean plays = false;
	/** Current episode */
	private Episode episode;
	/** Play service */
	private PlayEpisodeService service;
	/** Whether we are currently bound to the service */
	private boolean bound;
		
	@Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	
    	this.setRetainInstance(true);
    	this.setHasOptionsMenu(true);
    }
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		
		return inflater.inflate(R.layout.episode, container, false);
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.episode_menu, menu);
		
		this.playButton = menu.findItem(R.id.play);
	}
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		
		// Restore from configuration change 
		if (this.episode != null) setEpisode(this.episode);
	}
	
	@Override
	public void onStart() {
		super.onStart();
		
		// Bind to service
        Intent intent = new Intent(this.getActivity(), PlayEpisodeService.class);
        getActivity().bindService(intent, connection, Context.BIND_AUTO_CREATE);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.play) togglePlay();
		
		return item.getItemId() == R.id.play;
	}
	
	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		
		menu.findItem(R.id.play).setShowAsAction(
				this.episode == null ? MenuItem.SHOW_AS_ACTION_NEVER : MenuItem.SHOW_AS_ACTION_ALWAYS);
	}
	
	@Override
	public void onStop() {
		super.onStop();
		
		// Unbind from the service
        if (bound) {
            getActivity().unbindService(connection);
            bound = false;
        }
	}
	
	/**
	 * Set the displayed episode, all UI will be updated
	 * @param selectedEpisode Episode to show
	 */
	public void setEpisode(Episode selectedEpisode) {
		this.episode = selectedEpisode;
		
		getView().findViewById(R.id.episode_divider).setVisibility(View.VISIBLE);
		((TextView) getView().findViewById(R.id.podcast_title)).setText(episode.getPodcast().getName());
		((TextView) getView().findViewById(R.id.episode_title)).setText(episode.getName());
				
		WebView view = (WebView) getView().findViewById(R.id.episode_description);
		view.getSettings().setDefaultFontSize(12);
		view.loadDataWithBaseURL(null, this.episode.getDescription(), "text/html", "utf-8", null);
		
		getActivity().invalidateOptionsMenu();
	}
	
	public void togglePlay() {
		if (this.episode == null) return;
		
		this.plays = !this.plays;
		
		// Episode not played before
		if (! this.episode.equals(service.getCurrentEpisode())) {
			this.playButton.setTitle(R.string.pause);
			service.playEpisode(this.episode); 
		}
		// Player in pause
		else if (plays) {
			this.playButton.setTitle(R.string.pause);
			service.resume();
		} 
		// Player playing
		else {
			this.playButton.setTitle(R.string.play);
			service.pause();
		}		
	}
	
	/** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection connection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder serviceBinder) {
        	PlayEpisodeBinder binder = (PlayEpisodeBinder) serviceBinder;
            service = binder.getService();
            bound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            bound = false;
        }
    };
}
