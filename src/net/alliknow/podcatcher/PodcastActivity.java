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

import net.alliknow.podcatcher.listeners.OnSelectEpisodeListener;
import net.alliknow.podcatcher.listeners.OnSelectPodcastListener;
import net.alliknow.podcatcher.model.types.Episode;
import net.alliknow.podcatcher.model.types.Podcast;
import net.alliknow.podcatcher.view.fragments.EpisodeFragment;
import net.alliknow.podcatcher.view.fragments.EpisodeListFragment;
import net.alliknow.podcatcher.view.fragments.PodcastListFragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.os.StrictMode;
import android.view.View;

/**
 * Our main activity class. Handles configuration changes.
 * All the heavy lifting is done in fragments, that will be
 * retained on activity restarts.
 */
public class PodcastActivity extends PodcatcherBaseActivity implements OnSelectPodcastListener, OnSelectEpisodeListener {
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    
	    if (((Podcatcher) getApplication()).isInDebugMode()) StrictMode.enableDefaults();
	    
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
	public void onPodcastSelected(Podcast podcast) {
		switch (viewMode) {
			case SMALL_LANDSCAPE_VIEW:
				// This will go back to the list view in case we are showing episode details
				getFragmentManager().popBackStack();
				// There is no break here on purpose, we need to run the code below as well
			case LARGE_PORTRAIT_VIEW:
			case LARGE_LANDSCAPE_VIEW:
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
		switch (viewMode) {
			case SMALL_LANDSCAPE_VIEW:
				// This will go back to the list view in case we are showing episode details
				getFragmentManager().popBackStack();
				// There is no break here on purpose, we need to run the code below as well
			case LARGE_PORTRAIT_VIEW:
			case LARGE_LANDSCAPE_VIEW:
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
		
		// If there is an episode list visible, reset it
		EpisodeListFragment episodeListFragment = findEpisodeListFragment();
		if (episodeListFragment != null) episodeListFragment.resetUi();
		
		updateDivider();
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
