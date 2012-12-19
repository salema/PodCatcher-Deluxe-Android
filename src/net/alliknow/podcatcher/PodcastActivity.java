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

import net.alliknow.podcatcher.fragments.EpisodeFragment;
import net.alliknow.podcatcher.fragments.EpisodeListFragment;
import net.alliknow.podcatcher.fragments.PodcastListFragment;
import net.alliknow.podcatcher.listeners.OnLoadPodcastListener;
import net.alliknow.podcatcher.listeners.OnSelectEpisodeListener;
import net.alliknow.podcatcher.listeners.OnSelectPodcastListener;
import net.alliknow.podcatcher.tasks.Progress;
import net.alliknow.podcatcher.types.Episode;
import net.alliknow.podcatcher.types.Podcast;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

/**
 * Our main activity class. Handles configuration changes.
 * All the heavy lifting is done in fragments, that will be
 * retained on activity restarts.
 */
public class PodcastActivity extends PodcatcherBaseActivity implements 
	OnSelectPodcastListener, OnLoadPodcastListener, OnSelectEpisodeListener {
	
	/** Flag to indicate whether we are in multiple podcast mode */ 
	private boolean multiplePodcastsMode = false;
	/** Key used to save the current setting for 
	 * <code>multiplePodcastsMode</code> in bundle */
	private static final String MODE_KEY = "MODE_KEY";
	
	/** The podcatcher website URL */
	private static final String PODCATCHER_WEBSITE = "http://www.podcatcher-deluxe.com";
	/** The podcatcher help website URL */
	private static final String PODCATCHER_HELPSITE = "http://www.podcatcher-deluxe.com/help";
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    
	    if (Podcatcher.isInDebugMode(this)) StrictMode.enableDefaults();
	    
	    // Inflate the main content view (depends on view mode)
	    setContentView(R.layout.main);
	    
	    // On small screens in landscape mode we need to add the episode list fragment
		if (viewMode == SMALL_LANDSCAPE_VIEW && getFragmentManager().findFragmentByTag(episodeListFragmentTag) == null)
			getFragmentManager().beginTransaction().add(R.id.content, new EpisodeListFragment(), episodeListFragmentTag).commit();
	}
	
	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		
		if (savedInstanceState != null)
			multiplePodcastsMode = savedInstanceState.getBoolean(MODE_KEY);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
		// Make sure dividers (if any) reflect selection state
		updateDivider();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.podcatcher, menu);
		
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
	        case R.id.about_menuitem:
	        	startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(PODCATCHER_WEBSITE)));
	            
	   			return true;
	        case R.id.help_menuitem:
	        	startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(PODCATCHER_HELPSITE)));
	        	
	   			return true;
	        default:
	            return super.onOptionsItemSelected(item);
		}
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		
		outState.putBoolean(MODE_KEY, multiplePodcastsMode);
	}
	
	@Override
	public void onPodcastSelected(Podcast podcast) {
		multiplePodcastsMode = false;
		
		switch (viewMode) {
			case SMALL_LANDSCAPE_VIEW:
				// This will go back to the list view in case we are showing episode details
				getFragmentManager().popBackStack();
			case LARGE_PORTRAIT_VIEW:
			case LARGE_LANDSCAPE_VIEW:
				// List fragment is visible, make it show progress UI
				EpisodeListFragment episodeListFragment = findEpisodeListFragment();
				episodeListFragment.resetAndSpin();
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
		onPodcastSelected(null);
		
		multiplePodcastsMode = true;
		updateDivider();
	}
	
	@Override
	public void onNoPodcastSelected() {
		multiplePodcastsMode = false;
		
		// If there is an episode list visible, reset it
		EpisodeListFragment episodeListFragment = findEpisodeListFragment();
		if (episodeListFragment != null) episodeListFragment.resetUi();
		
		updateDivider();
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
				
				if (multiplePodcastsMode) episodeListFragment.addEpisodeList(podcast.getEpisodes());
				else episodeListFragment.setEpisodeList(podcast.getEpisodes());
				
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
			case SMALL_LANDSCAPE_VIEW:
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
