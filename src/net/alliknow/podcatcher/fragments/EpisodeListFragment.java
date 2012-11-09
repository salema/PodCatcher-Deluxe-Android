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

import java.util.List;

import net.alliknow.podcatcher.R;
import net.alliknow.podcatcher.adapters.EpisodeListAdapter;
import net.alliknow.podcatcher.listeners.OnSelectEpisodeListener;
import net.alliknow.podcatcher.tasks.LoadPodcastTask;
import net.alliknow.podcatcher.types.Episode;
import android.app.ListFragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

/**
 * List fragment to display the list of episodes as part of the
 * podcast activity.
 * 
 * @author Kevin Hausmann
 */
public class EpisodeListFragment extends ListFragment {

	/** The list of episode showing */
	private List<Episode> episodeList;
	
	/** The list view */
	private ListView listView;
	/** The empty view */
	private TextView emptyView;
	/** The progress bar */
	private View progressView;
	/** The progress bar text */
	private TextView progressTextView;
	
	private boolean showProgress = false;
	
	/** The activity we are in (listens to user selection) */ 
    private OnSelectEpisodeListener selectedListener;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	setRetainInstance(true);
    }
    
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		
		return inflater.inflate(R.layout.episode_list, container, false);
	}
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		
		listView = getListView();
		emptyView = (TextView) getView().findViewById(android.R.id.empty);
		progressView = getView().findViewById(R.id.episode_list_progress);
		progressTextView = (TextView) getView().findViewById(R.id.episode_list_progress_text);
		
		if (showProgress) clearAndSpin();
	}
	
	@Override
	public void onListItemClick(ListView list, View view, int position, long id) {
		Episode selectedEpisode = episodeList.get(position);
		((EpisodeListAdapter) getListAdapter()).setSelectedPosition(position);
		
		if (selectedListener != null) selectedListener.onEpisodeSelected(selectedEpisode);
		else Log.d(getClass().getSimpleName(), "Episode selected, but no listener attached");
	}
	
	/**
	 * @param listener Listener to be alerted on episode selection
	 */
	public void setEpisodeSelectedListener(OnSelectEpisodeListener listener) {
		this.selectedListener = listener;
	}
	
	/**
	 * Set the episode list to display and update the UI accordingly
	 * @param list List of episodes to display
	 */
	public void setEpisodeList(List<Episode> list) {
		progressView.setVisibility(View.GONE);
		showProgress = false;
		
		this.episodeList = list;
		
		setListAdapter(new EpisodeListAdapter(getActivity(), episodeList));
		// TODO handle empty episode list
		if (list.isEmpty()) emptyView.setText(R.string.no_episodes);
		else listView.setVisibility(View.VISIBLE);
	}

	/**
	 * Show the UI to be working
	 */
	public void clearAndSpin() {
		showProgress = true;
		progressTextView.setText(null);
		listView.setVisibility(View.GONE);
		emptyView.setVisibility(View.GONE);
				
		//if (! Podcatcher.isInDebugMode(getActivity()))
			progressView.setVisibility(View.VISIBLE);
	}
	
	/**
	 * Update UI with load progress
	 * @param progress Amount loaded or flag from load task
	 */
	public void showProgress(int progress) {
		if (progress == LoadPodcastTask.PROGRESS_CONNECT) 
			progressTextView.setText(getResources().getString(R.string.connect));
		else if (progress == LoadPodcastTask.PROGRESS_LOAD)
			progressTextView.setText(getResources().getString(R.string.load));
		else if (progress >= 0 && progress <= 100) progressTextView.setText(progress + "%");
		else if (progress == LoadPodcastTask.PROGRESS_PARSE)
			progressTextView.setText(getResources().getString(R.string.parse));
		else progressTextView.setText(getResources().getString(R.string.load));
	}

	/**
	 * Show error view
	 * @param string Error message to show
	 */
	public void showError(String message) {
		showProgress = false;
		progressView.setVisibility(View.GONE);
		listView.setVisibility(View.GONE);
		
		emptyView.setText(message);
		emptyView.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
		emptyView.setVisibility(View.VISIBLE);	
	}
}
