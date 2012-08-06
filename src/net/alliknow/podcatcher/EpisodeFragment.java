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
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.Button;

/**
 * Fragment showing episode details
 * 
 * @author Kevin Hausmann
 */
public class EpisodeFragment extends Fragment {

	/** The play button */
	private Button playButton;
	private boolean plays = false;
	/** Current episode */
	private Episode episode;
	/** Play service */
	private PlayEpisodeService service;
	private boolean bound;
		
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		
		View view = inflater.inflate(R.layout.episode, container, false);
		
		this.playButton = (Button) view.findViewById(R.id.play_button);
		this.playButton.setOnClickListener(new View.OnClickListener() {
		    public void onClick(View v) {
		        playEpisode();
		    }
		});
		
		return view;
	}
	
	@Override
	public void onStart() {
		super.onStart();
		
		// Bind to service
        Intent intent = new Intent(this.getActivity(), PlayEpisodeService.class);
        getActivity().bindService(intent, connection, Context.BIND_AUTO_CREATE);
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
		
		WebView view = (WebView) getView().findViewById(R.id.episode_description);
		view.getSettings().setDefaultFontSize(12);
		view.loadData(this.episode.getDescription(), "text/html", null);
	}
	
	public void playEpisode() {
		service.playEpisode(this.episode);
		this.plays = !this.plays;
		
		if (this.plays) this.playButton.setText(R.string.pause);
		else this.playButton.setText(R.string.play);
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
