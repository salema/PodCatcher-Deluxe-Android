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
package net.alliknow.podcatcher.view.fragments;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.alliknow.podcatcher.Podcatcher;
import net.alliknow.podcatcher.R;
import net.alliknow.podcatcher.listeners.OnLoadPodcastListener;
import net.alliknow.podcatcher.listeners.OnSelectEpisodeListener;
import net.alliknow.podcatcher.model.PodcastManager;
import net.alliknow.podcatcher.model.tasks.Progress;
import net.alliknow.podcatcher.model.types.Episode;
import net.alliknow.podcatcher.model.types.Podcast;
import net.alliknow.podcatcher.view.adapters.EpisodeListAdapter;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

/**
 * List fragment to display the list of episodes as part of the
 * podcast activity.
 */
public class EpisodeListFragment extends PodcatcherListFragment 
	implements OnLoadPodcastListener {

	/** Flag to indicate whether we are in multiple podcast mode */ 
	private boolean multiplePodcastsMode = false;
	/** Key used to save the current setting for 
	 * <code>multiplePodcastsMode</code> in bundle */
	private static final String MODE_KEY = "MODE_KEY";
	
	/** The podcast data container */
	private PodcastManager data;
	/** The podcast showing */
	private Podcast currentPodcast;
	/** The current list of episodes */
	private List<Episode> episodeList;
	/** The selected episode */
	private Episode selectedEpisode;
	
	/** The activity we are in (listens to user selection) */ 
    private OnSelectEpisodeListener selectedListener;
    
    @Override
    public void onAttach(Activity activity) {
    	super.onAttach(activity);
    	
    	// Link to podcast data singleton
    	data = ((Podcatcher) activity.getApplication()).getModel();
    	
    	// Make sure we can react on data changes
    	data.addLoadPodcastListener(this);
    	
    	// This has to work
    	selectedListener = (OnSelectEpisodeListener) activity;
    }
    
   	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
   		
   		if (savedInstanceState != null)
			multiplePodcastsMode = savedInstanceState.getBoolean(MODE_KEY);
		
		return inflater.inflate(R.layout.episode_list, container, false);
	}
   	
   	@Override
	public void onListItemClick(ListView list, View view, int position, long id) {
		if (selectedListener != null) selectedListener.onEpisodeSelected(currentPodcast.getEpisodes().get(position));
		else Log.d(getClass().getSimpleName(), "Episode selected, but no listener attached");
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		
		outState.putBoolean(MODE_KEY, multiplePodcastsMode);
	}
	
	@Override
	public void onDetach() {
		super.onDetach();
		
		// Do not leak this fragment
		data.removeLoadPodcastListener(this);
		
		// Do not leak activity
		selectedListener = null;
	}
	
	/**
	 * Select given episode, iff available in current episode list.
	 * If the episode is not available the list will go to "no-selection-state".
	 * @param selectedEpisode Episode to select.
	 */
	public void selectEpisode(Episode selectedEpisode) {
		this.selectedEpisode = selectedEpisode;
		
		// Episode is available
		if (currentPodcast != null && currentPodcast.getEpisodes().contains(selectedEpisode)) 
			selectItem(currentPodcast.getEpisodes().indexOf(selectedEpisode));
		// Episode is not in the current episode list
		else selectNone();
	}
	
	/**
	 * Check whether there is an episode currently selected in the list.
	 * @return <code>true</code> if so, <code>false</code> otherwise. 
	 */
	public boolean isEpisodeSelected() {
		return selectedEpisode != null;
	}
	
	/**
	 * @param listener Listener to be alerted on episode selection.
	 */
	public void setEpisodeSelectedListener(OnSelectEpisodeListener listener) {
		this.selectedListener = listener;
	}
	
	/**
	 * Check whether the fragments list contains given episode.
	 * @param episode Episode to check for.
	 * @return <code>true</code> iff the current episode list contains given episode.
	 */
	public boolean containsEpisode(Episode episode) {
		return currentPodcast != null && currentPodcast.getEpisodes().contains(episode);
	}
	
	/**
	 * Set the episode list to display and update the UI accordingly.
	 * @param list List of episodes to display.
	 */
	public void prepareForPodcast(Podcast podcast) {
		multiplePodcastsMode = false;
		resetAndSpin();
		
		// Reset internal variables
		if (selectedListener != null) selectedListener.onNoEpisodeSelected();
		
		currentPodcast = podcast;
		
		if (! podcast.needsReload()) {
			episodeList = currentPodcast.getEpisodes();
			processNewEpisodes();
		}
	}
	
	public void prepareForAllPodcasts() {
		multiplePodcastsMode = true;
		resetAndSpin();
		
		// Reset internal variables
		if (selectedListener != null) selectedListener.onNoEpisodeSelected();
		
		currentPodcast = null;
		episodeList = new ArrayList<Episode>();
		
		for (Podcast podcast : data.getPodcastList())
			if (! podcast.needsReload()) {
				episodeList.addAll(podcast.getEpisodes());
			}
		
		Collections.sort(episodeList);
	}
	
	@Override
	public void onPodcastLoadProgress(Podcast podcast, Progress progress) {
		if (podcast.equals(currentPodcast)) showProgress(progress);
	}
	
	@Override
	public void onPodcastLoaded(Podcast podcast) {
		if (multiplePodcastsMode) {
			// TODO decide on this: episodeList.addAll(list.subList(0, list.size() > 100 ? 100 : list.size() - 1));
			if (podcast.getEpisodes().size() > 0) { 
				episodeList.addAll(podcast.getEpisodes());
				Collections.sort(episodeList);
			}
		}
		else if (podcast.equals(currentPodcast)) episodeList = podcast.getEpisodes();
		
		processNewEpisodes();
		
//		// Additionally, if on large device, process clever selection update
//		if (viewMode == LARGE_LANDSCAPE_VIEW || viewMode == LARGE_PORTRAIT_VIEW) {
//			EpisodeListFragment episodeListFragment = findEpisodeListFragment();
//			EpisodeFragment episodeFragment = findEpisodeFragment();
//			
//			if (episodeListFragment.containsEpisode(episodeFragment.getEpisode()))
//				episodeListFragment.selectEpisode(episodeFragment.getEpisode());
//		}
	}
	
	@Override
	public void onPodcastLoadFailed(Podcast failedPodcast) {
		if (failedPodcast.equals(currentPodcast)) showLoadFailed();
	}
	
	private void processNewEpisodes() {
		showProgress = false;
		showLoadFailed = false;
		
		if (isResumed()) {		
			setListAdapter(new EpisodeListAdapter(getActivity(), 
				new ArrayList<Episode>(episodeList), selectAll));
			
			// Update UI
			if (currentPodcast.getEpisodes().isEmpty()) emptyView.setText(R.string.no_episodes);
			updateUiElementVisibility();
		}
	}
	
	/**
	 * Unselect selected episode (if any).
	 */
	public void selectNone() {
		selectedEpisode = null;
		super.selectNone();
	}
	
	@Override
	protected void reset() {
		currentPodcast = null;
		episodeList = null;
		selectedEpisode = null;
				
		if (emptyView != null) 
			emptyView.setText(R.string.no_podcast_selected);
		
		super.reset();
	}

	/**
	 * Show error view.
	 */
	@Override
	public void showLoadFailed() {
		progressView.showError(R.string.error_podcast_load);
		
		super.showLoadFailed();
	}

	/**
	 * @param b
	 */
	public void setMultiplePodcastMode(boolean b) {
		this.multiplePodcastsMode = b;
	}
}
