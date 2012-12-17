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
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
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
	
	private int viewMode;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    
	    if (Podcatcher.isInDebugMode(this)) StrictMode.enableDefaults();
	    
	    setContentView(R.layout.main);
	    		
		figureOutViewMode();
		Log.d(getClass().getSimpleName(), "View mode detected to be: " + viewMode);
		
		if (viewMode == 1 && getFragmentManager().findFragmentByTag("TestA") == null)
			getFragmentManager().beginTransaction().add(R.id.content, new EpisodeListFragment(), "TestA").commit();
		
		updateDivider();
	}
	
	private void figureOutViewMode() {
		if (getResources().getConfiguration().smallestScreenWidthDp >= 600) viewMode = 2;
		else if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) viewMode = 0;
		else viewMode = 1;
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
		
		if (viewMode > 1) {		
			episodeListFragment = (EpisodeListFragment) getFragmentManager().findFragmentById(R.id.episode_list);
			episodeListFragment.resetAndSpin();
			updateDivider();
		} else if (viewMode > 0) {		
			episodeListFragment = (EpisodeListFragment) getFragmentManager().findFragmentById(R.id.content);
			episodeListFragment.resetAndSpin();
			updateDivider();
		} else {
			// Otherwise we need to launch a new activity to display the episode list
            Intent intent = new Intent();
            intent.setClass(this, ShowEpisodeListActivity.class);
            //intent.putExtra("index", index);
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
		
		if (episodeListFragment != null) episodeListFragment.resetUi();
		updateDivider();
	}
	
	@Override
	public void onPodcastLoadProgress(Podcast podcast, Progress progress) {
		if (viewMode > 1) {		
			episodeListFragment = (EpisodeListFragment) getFragmentManager().findFragmentById(R.id.episode_list);
			episodeListFragment.showProgress(progress);
		} else if (viewMode > 0) {		
			episodeListFragment = (EpisodeListFragment) getFragmentManager().findFragmentById(R.id.content);
			episodeListFragment.showProgress(progress);
		} else {
			// Otherwise we need to launch a new activity to display the episode list
            Intent intent = new Intent();
            intent.setClass(this, ShowEpisodeListActivity.class);
            intent.putExtra("progress", true);
            startActivity(intent);
		}
	}
	
	@Override
	public void onPodcastLoaded(Podcast podcast) {
		if (viewMode > 1) {		
			episodeListFragment = (EpisodeListFragment) getFragmentManager().findFragmentById(R.id.episode_list);

			if (multiplePodcastsMode) episodeListFragment.addEpisodeList(podcast.getEpisodes());
			else episodeListFragment.setEpisodeList(podcast.getEpisodes());
			
			if (viewMode > 1 && episodeListFragment.containsEpisode(episodeFragment.getEpisode()))
				//episodeListFragment.selectEpisode(episodeFragment.getEpisode());
			
			updateDivider();
		} else if (viewMode > 0) {
			episodeListFragment = (EpisodeListFragment) getFragmentManager().findFragmentById(R.id.content);
			
			if (multiplePodcastsMode) episodeListFragment.addEpisodeList(podcast.getEpisodes());
			else episodeListFragment.setEpisodeList(podcast.getEpisodes());
		} else {
			Intent intent = new Intent();
            intent.setClass(this, ShowEpisodeListActivity.class);
            intent.putExtra("select", true);
            startActivity(intent);
		}
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
		
		if (viewMode > 1) {
			episodeFragment = (EpisodeFragment) getFragmentManager().findFragmentById(R.id.episode_list);
			episodeFragment.setEpisode(selectedEpisode);
		} else if (viewMode > 0) {
			episodeFragment = (EpisodeFragment) getFragmentManager().findFragmentByTag("Test");
			if (episodeFragment == null) episodeFragment = new EpisodeFragment();
			FragmentTransaction transaction = getFragmentManager().beginTransaction();
			transaction.replace(R.id.content, episodeFragment, "Test");
			transaction.addToBackStack(null);
			transaction.commit();
			episodeFragment.setEpisode(selectedEpisode);
		} else {
			Intent intent = new Intent();
            intent.setClass(this, ShowEpisodeActivity.class);
            //intent.putExtra("index", index);
            startActivity(intent);
		}

		if (viewMode > 0) updateDivider();
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
