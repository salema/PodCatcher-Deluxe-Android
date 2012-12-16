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

import java.util.ArrayList;

import net.alliknow.podcatcher.fragments.EpisodeFragment;
import net.alliknow.podcatcher.fragments.EpisodeListFragment;
import net.alliknow.podcatcher.fragments.PodcastListFragment;
import net.alliknow.podcatcher.listeners.OnLoadPodcastListener;
import net.alliknow.podcatcher.listeners.OnSelectEpisodeListener;
import net.alliknow.podcatcher.listeners.OnSelectPodcastListener;
import net.alliknow.podcatcher.tasks.Progress;
import net.alliknow.podcatcher.types.Episode;
import net.alliknow.podcatcher.types.Podcast;
import android.app.Activity;
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
public class PodcastActivity extends Activity implements 
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
	
	/** The (current) episode list fragment, may not be available (i.e. <code>null</code>) */
	private EpisodeListFragment episodeListFragment;
	/** The (current) episode  fragment, may not be available (i.e. <code>null</code>) */
	private EpisodeFragment episodeFragment;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    
	    if (Podcatcher.isInDebugMode(this)) StrictMode.enableDefaults();
	    
	    setContentView(R.layout.main);
		updateDivider();
	}
	
	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		
		if (savedInstanceState != null)
			multiplePodcastsMode = savedInstanceState.getBoolean(MODE_KEY);
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
		
		episodeListFragment = (EpisodeListFragment) getFragmentManager()
				.findFragmentByTag(getResources().getString(R.string.episode_list_fragment_tag));
		
		if (episodeListFragment == null) episodeListFragment = new EpisodeListFragment();
			
		if (! episodeListFragment.isVisible()) {
			FragmentTransaction transaction = getFragmentManager().beginTransaction();
			transaction.replace(R.id.content, episodeListFragment,
					getResources().getString(R.string.episode_list_fragment_tag));
			transaction.addToBackStack(null);
			transaction.commit();
		}
		
		episodeListFragment.resetAndSpin();
		
		updateDivider();
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
		
		if (episodeListFragment != null) episodeListFragment.resetUi();
		updateDivider();
	}
	
	@Override
	public void onPodcastLoadProgress(Podcast podcast, Progress progress) {
		if (episodeListFragment != null) episodeListFragment.showProgress(progress);
	}
	
	@Override
	public void onPodcastLoaded(Podcast podcast) {
		if (multiplePodcastsMode && episodeListFragment != null) episodeListFragment.addEpisodeList(podcast.getEpisodes());
		else if (episodeListFragment != null) episodeListFragment.setEpisodeList(podcast.getEpisodes());

		if (episodeListFragment != null && episodeFragment != null && 
				episodeListFragment.containsEpisode(episodeFragment.getEpisode()))
			episodeListFragment.selectEpisode(episodeFragment.getEpisode());
		
		updateDivider();
	}
	
	@Override
	public void onPodcastLoadFailed(Podcast failedPodcast) {
		EpisodeListFragment episodeListFragment = (EpisodeListFragment) getFragmentManager()
				.findFragmentByTag(getResources().getString(R.string.episode_list_fragment_tag));
		
		// Reset the episode list so the old one would not reappear of config changes
		episodeListFragment.setEpisodeList(new ArrayList<Episode>());
		episodeListFragment.showLoadFailed();
	}

	@Override
	public void onEpisodeSelected(Episode selectedEpisode) {
		// Make sure selection matches in list fragment		
		if (episodeListFragment != null) episodeListFragment.selectEpisode(selectedEpisode);
		
		episodeFragment = (EpisodeFragment) getFragmentManager()
				.findFragmentByTag(getResources().getString(R.string.episode_fragment_tag));
		
		if (episodeFragment == null){
			episodeFragment = new EpisodeFragment();
			
			FragmentTransaction transaction = getFragmentManager().beginTransaction();
			transaction.replace(R.id.content, episodeFragment,
					getResources().getString(R.string.episode_fragment_tag));
			transaction.addToBackStack(null);
			transaction.commit();
		}
		
		episodeFragment.setEpisode(selectedEpisode);
		
		updateDivider();
	}
	
	@Override
	public void onNoEpisodeSelected() {
		if (episodeListFragment != null) episodeListFragment.selectNone();
		
		updateDivider();
	}
	
	private void updateDivider() {
		PodcastListFragment podcastListFragment = (PodcastListFragment) getFragmentManager()
				.findFragmentByTag(getResources().getString(R.string.podcast_list_fragment_tag));
		EpisodeListFragment episodeListFragment = (EpisodeListFragment) getFragmentManager()
				.findFragmentByTag(getResources().getString(R.string.episode_list_fragment_tag));
		
		if (podcastListFragment != null) 
			colorDivider(R.id.divider_first, podcastListFragment.isPodcastSelected());
		if (episodeListFragment != null)
			colorDivider(R.id.divider_second, episodeListFragment.isEpisodeSelected());
	}
	
	private void colorDivider(int dividerViewId, boolean color) {
		if (getWindow() != null && getWindow().findViewById(dividerViewId) != null) {
			View divider = getWindow().findViewById(dividerViewId);
			divider.setBackgroundResource(color ? R.color.divider_on : R.color.divider_off);
		}
	}
}
