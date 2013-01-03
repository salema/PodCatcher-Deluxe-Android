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

import java.util.List;

import net.alliknow.podcatcher.listeners.OnLoadPodcastListListener;
import net.alliknow.podcatcher.listeners.OnLoadPodcastListener;
import net.alliknow.podcatcher.listeners.OnLoadPodcastLogoListener;
import net.alliknow.podcatcher.listeners.OnSelectEpisodeListener;
import net.alliknow.podcatcher.listeners.OnSelectPodcastListener;
import net.alliknow.podcatcher.model.tasks.Progress;
import net.alliknow.podcatcher.model.types.Episode;
import net.alliknow.podcatcher.model.types.Podcast;
import net.alliknow.podcatcher.view.fragments.EpisodeFragment;
import net.alliknow.podcatcher.view.fragments.EpisodeListFragment;
import net.alliknow.podcatcher.view.fragments.PodcastListFragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.View;

/**
 * Our main activity class.
 * Works as the main controller. Depending on the view state,
 * other activities cooperate.
 */
public class PodcastActivity extends PodcatcherBaseActivity 
		implements OnLoadPodcastListListener, OnSelectPodcastListener, OnLoadPodcastListener,
			OnLoadPodcastLogoListener, OnSelectEpisodeListener {
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    
	    // Enable strict mode when on debug
	    if (((Podcatcher) getApplication()).isInDebugMode()) StrictMode.enableDefaults();
	    
	    // Register as listener to the podcast data manager
	    dataManager.addLoadPodcastListListener(this);
	    dataManager.addLoadPodcastListener(this);
	    //dataManager.addLoadPodcastLogoListener(this);
	    
	    // Check if podcast list is already available - if so, set it
	    List<Podcast> podcastList = dataManager.getPodcastList();
		if (podcastList != null) onPodcastListLoaded(podcastList);
	    
	    // Inflate the main content view (depends on view mode)
	    setContentView(R.layout.main);
	    
		// On small screens in landscape mode we need to add the episode list fragment
		if (viewMode == SMALL_LANDSCAPE_VIEW && getFragmentManager().findFragmentByTag(episodeListFragmentTag) == null)
			getFragmentManager().beginTransaction().add(R.id.content, new EpisodeListFragment(), episodeListFragmentTag).commit();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
		// Make sure dividers (if any) reflect selection state
		updateDivider();
	}
	
	@Override
	public void onPodcastListLoaded(List<Podcast> podcastList) {
		findPodcastListFragment().setPodcastList(podcastList);
		
		// If podcast list is empty we show dialog on startup
		//if (podcastList.isEmpty()) showAddPodcastDialog();
	}
	
	@Override
	public void onPodcastSelected(Podcast podcast) {
		// Stop loading previous tasks
		dataManager.cancelAllLoadTasks();
		
		// Load podcast
		dataManager.load(podcast);
		
		switch (viewMode) {
			case SMALL_LANDSCAPE_VIEW:
				// This will go back to the list view in case we are showing episode details
				getFragmentManager().popBackStack();
				// There is no break here on purpose, we need to run the code below as well
			case LARGE_PORTRAIT_VIEW:
			case LARGE_LANDSCAPE_VIEW:
				// Select in podcast list
				findPodcastListFragment().select(podcast);
				// List fragment is visible, make it show progress UI
				EpisodeListFragment episodeListFragment = findEpisodeListFragment();
				episodeListFragment.prepareForPodcast(podcast);
				updateDivider();
				break;
			case SMALL_PORTRAIT_VIEW:
				// Otherwise we need to launch a new activity to display the episode list
	            Intent intent = new Intent();
	            intent.setClass(this, ShowEpisodeListActivity.class);
	            //intent.putExtra("podcast", url); // String
	            startActivity(intent);
		}
	}

	@Override
	public void onAllPodcastsSelected() {
		// Stop loading previous tasks
		dataManager.cancelAllLoadTasks();
		
		// Load all podcasts
//				for (Podcast podcast : data.getPodcastList())
//					if (podcast.needsReload()) {
//						// Otherwise progress will not show
//						podcast.resetEpisodes();
//						
//						data.load(podcast);
//					} else onPodcastLoaded(podcast);
		
		switch (viewMode) {
			case SMALL_LANDSCAPE_VIEW:
				// This will go back to the list view in case we are showing episode details
				getFragmentManager().popBackStack();
				// There is no break here on purpose, we need to run the code below as well
			case LARGE_PORTRAIT_VIEW:
			case LARGE_LANDSCAPE_VIEW:
				findPodcastListFragment().selectAll();
				// List fragment is visible, make it show progress UI
				EpisodeListFragment episodeListFragment = findEpisodeListFragment();
				episodeListFragment.prepareForAllPodcasts();
				updateDivider();
				break;
			case SMALL_PORTRAIT_VIEW:
				// Otherwise we need to launch a new activity to display the episode list
	            Intent intent = new Intent();
	            intent.setClass(this, ShowEpisodeListActivity.class);
	            //intent.putExtra("multiple", true);
	            startActivity(intent);
		}
	}
	
	@Override
	public void onNoPodcastSelected() {
		//multiplePodcastsMode = false;
		findPodcastListFragment().selectNone();
		
		// If there is an episode list visible, reset it
		EpisodeListFragment episodeListFragment = findEpisodeListFragment();
		if (episodeListFragment != null) episodeListFragment.resetUi();
		
		updateDivider();
	}
	
	/**
	 * Removes the podcast selected in context mode.
	 */
//	public void removeCheckedPodcasts() {
//		SparseBooleanArray checkedItems = getListView().getCheckedItemPositions();
//		
//		// Remove checked podcasts
//		for (int index = data.size() - 1; index >= 0; index--)
//			if (checkedItems.get(index)) {
//				// Reset internal variable if necessary
//				if (data.get(index).equals(currentPodcast)) currentPodcast = null;
//				// Remove podcast from list
//				data.remove(index);
//			}
//		
//		// Update UI (current podcast was deleted)
//		if (!selectAll && currentPodcast == null) selectedListener.onNoPodcastSelected();
//		// Current podcast has new position
//		else if (!selectAll) adapter.setSelectedPosition(data.indexOf(currentPodcast));
//		
//		updateUiElementVisibility();
//	}
	
	/**
	 * Notified by async RSS file loader on completion.
	 * Updates UI to display the podcast's episodes.
	 * @param podcast Podcast RSS feed was loaded for.
	 */
	@Override
	public void onPodcastLoaded(Podcast podcast) {
		// This will display the number of episodes
		adapter.notifyDataSetChanged();
		
		// Load logo if this is the podcast we are waiting for and not cached
		if (podcast.equals(currentPodcast) && currentPodcast.getLogo() == null) 
			data.loadLogo(currentPodcast, logoView.getWidth(), logoView.getHeight());
	}
	
	@Override
	public void onPodcastLoadFailed(Podcast podcast) {
		// This will update the list view
		adapter.notifyDataSetChanged();
				
		Log.w(getClass().getSimpleName(), "Podcast failed to load " + podcast);
	}
	
	
	
	@Override
	public void onPodcastLoadProgress(Podcast podcast, Progress progress) {
		switch (viewMode) {
			case LARGE_PORTRAIT_VIEW:
			case LARGE_LANDSCAPE_VIEW:
			case SMALL_LANDSCAPE_VIEW:
				// Simply update the list fragment
				EpisodeListFragment episodeListFragment = findEpisodeListFragment();
				episodeListFragment.showProgress(progress);
				break;
			case SMALL_PORTRAIT_VIEW:
				// Otherwise we send a progress alert to the activity
	            Intent intent = new Intent();
	            intent.setClass(this, ShowEpisodeListActivity.class);
	            intent.putExtra("progress", true);
	            startActivity(intent);
		}
	}
	
	@Override
	public void onPodcastLoaded(Podcast podcast) {
		switch (viewMode) {
			case LARGE_LANDSCAPE_VIEW:
			case LARGE_PORTRAIT_VIEW:
			case SMALL_LANDSCAPE_VIEW:
				// Update list fragment to show episode list
				EpisodeListFragment episodeListFragment = findEpisodeListFragment();
				
				//if (multiplePodcastsMode) episodeListFragment.addEpisodeList(podcast.getEpisodes());
				//else episodeListFragment.setEpisodeList(podcast.getEpisodes());
				
				break;
			case SMALL_PORTRAIT_VIEW:
				// Send intent to activity
				Intent intent = new Intent();
	            intent.setClass(this, ShowEpisodeListActivity.class);
	            intent.putExtra("select", true);
	            startActivity(intent);
		}
		
		// Additionally, if on large device, process clever selection update
		if (viewMode == LARGE_LANDSCAPE_VIEW || viewMode == LARGE_PORTRAIT_VIEW) {
			EpisodeListFragment episodeListFragment = findEpisodeListFragment();
			EpisodeFragment episodeFragment = findEpisodeFragment();
			
			if (episodeListFragment.containsEpisode(episodeFragment.getEpisode()))
				episodeListFragment.selectEpisode(episodeFragment.getEpisode());
		}
	}
	
	@Override
	public void onPodcastLoadFailed(Podcast failedPodcast) {
		switch (viewMode) {
			case LARGE_LANDSCAPE_VIEW:
			case LARGE_PORTRAIT_VIEW:
		rat	case SMALL_LANDSCAPE_VIEW:
				EpisodeListFragment episodeListFragment = findEpisodeListFragment();
				episodeListFragment.showLoadFailed();
				break;
			case SMALL_PORTRAIT_VIEW:
				// Send intent to activity
				Intent intent = new Intent();
	            intent.setClass(this, ShowEpisodeListActivity.class);
	            intent.putExtra("failed", true);
	            startActivity(intent);
		}
	}
	
	@Override
	public void onPodcastLogoLoaded(Podcast podcast, Bitmap logo) {
		// Cache the result in podcast object
		if (podcast.equals(currentPodcast))	currentPodcast.setLogo(logo);
		
		logoView.setImageBitmap(logo);
	}
	
	@Override
	public void onPodcastLogoLoadFailed(Podcast podcast) {
		// pass
	}
	
	@Override
	public void onEpisodeSelected(Episode selectedEpisode) {
		switch (viewMode) {
			case LARGE_PORTRAIT_VIEW:
			case LARGE_LANDSCAPE_VIEW:
				// Set episode in episode fragment
				findEpisodeFragment().setEpisode(selectedEpisode);
				// Make sure selection matches in list fragment
				findEpisodeListFragment().selectEpisode(selectedEpisode);
				break;
			case SMALL_LANDSCAPE_VIEW:
				// Find, and if not already done create, episode fragment
				EpisodeFragment episodeFragment = findEpisodeFragment();
				if (episodeFragment == null) episodeFragment = new EpisodeFragment();
				// Add the fragment to the UI, placing the list fragment 
				FragmentTransaction transaction = getFragmentManager().beginTransaction();
				transaction.replace(R.id.content, episodeFragment, episodeFragmentTag);
				transaction.addToBackStack(null);
				transaction.commit();
				// Set the episode
				episodeFragment.setEpisode(selectedEpisode);
				break;
			case SMALL_PORTRAIT_VIEW:
				// Send intent to open episode as a new activity
				Intent intent = new Intent();
	            intent.setClass(this, ShowEpisodeActivity.class);
	            //intent.putExtra("episode", URL);
	            startActivity(intent);
		}

		if (viewMode != SMALL_PORTRAIT_VIEW) updateDivider();
	}
	
	@Override
	public void onNoEpisodeSelected() {
		// If there is a episode fragment, reset it
		EpisodeListFragment episodeListFragment = findEpisodeListFragment();
		if (episodeListFragment != null) episodeListFragment.selectNone();
		
		updateDivider();
	}
	
	private void updateDivider() {
		if (viewMode != SMALL_PORTRAIT_VIEW) {
			// Try find the fragment
			PodcastListFragment podcastListFragment = findPodcastListFragment();
			EpisodeListFragment episodeListFragment = findEpisodeListFragment();
			
			// Color dividers where possible
			if (podcastListFragment != null) 
				colorDivider(R.id.divider_first, podcastListFragment.isPodcastSelected());
			if (episodeListFragment != null)
				colorDivider(R.id.divider_second, episodeListFragment.isEpisodeSelected());
		}
	}
	
	private void colorDivider(int dividerViewId, boolean color) {
		if (getWindow() != null && getWindow().findViewById(dividerViewId) != null) {
			View divider = getWindow().findViewById(dividerViewId);
			divider.setBackgroundResource(color ? R.color.divider_on : R.color.divider_off);
		}
	}
}
