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
	
	/** The podcast list fragment */
	private PodcastListFragment podcastListFragment;
	/** The episode list fragment */
	private EpisodeListFragment episodeListFragment;
	/** The episode details fragment */
	private EpisodeFragment episodeFragment;
	
	/** Flag to indicate whether we are in multiple podcast mode */ 
	private boolean multiplePodcastsMode = false;
	
	/** The podcatcher website URL */
	private static final String PODCATCHER_WEBSITE = "http://www.podcatcher-deluxe.com";
	/** The podcatcher help website URL */
	private static final String PODCATCHER_HELPSITE = "http://www.podcatcher-deluxe.com/help";
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    
	    if (Podcatcher.isInDebugMode(this)) StrictMode.enableDefaults();
	    
	    setContentView(R.layout.main);
	    
	    podcastListFragment = (PodcastListFragment) getFragmentManager().findFragmentById(R.id.podcast_list);
		episodeListFragment = (EpisodeListFragment) getFragmentManager().findFragmentById(R.id.episode_list);
		episodeFragment = (EpisodeFragment) getFragmentManager().findFragmentById(R.id.episode);
		
		colorDivider(R.id.divider_first, podcastListFragment.isPodcastSelected());
		colorDivider(R.id.divider_second, episodeListFragment.isEpisodeSelected());
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		
		podcastListFragment.setPodcastSelectedListener(this);
		podcastListFragment.setPodcastLoadedListener(this);
		episodeListFragment.setEpisodeSelectedListener(this);
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
	protected void onStop() {
		super.onStop();
		
		podcastListFragment.setPodcastSelectedListener(null);
		podcastListFragment.setPodcastLoadedListener(null);
		episodeListFragment.setEpisodeSelectedListener(null);
	}

	@Override
	public void onPodcastSelected(Podcast podcast) {
		multiplePodcastsMode = false;
		
		colorDivider(R.id.divider_first, true);
		colorDivider(R.id.divider_second, false);
		episodeListFragment.clearAndSpin();
	}
	
	@Override
	public void onAllPodcastsSelected() {
		multiplePodcastsMode = true;
		
		colorDivider(R.id.divider_first, true);
		colorDivider(R.id.divider_second, false);
		episodeListFragment.reset();
		episodeListFragment.clearAndSpin();
	}
	
	@Override
	public void onNoPodcastSelected() {
		multiplePodcastsMode = false;
		
		colorDivider(R.id.divider_first, false);
		colorDivider(R.id.divider_second, false);
		episodeListFragment.reset();
	}
	
	@Override
	public void onPodcastLoadProgress(Podcast podcast, Progress progress) {
		episodeListFragment.showProgress(progress);
	}
	
	@Override
	public void onPodcastLoaded(Podcast podcast) {
		if (multiplePodcastsMode) episodeListFragment.addEpisodeList(podcast.getEpisodes());
		else episodeListFragment.setEpisodeList(podcast.getEpisodes());
	}
	
	@Override
	public void onPodcastLoadFailed(Podcast failedPodcast) {
		// Reset the episode list so the old one would not reappear of config changes
		episodeListFragment.setEpisodeList(new ArrayList<Episode>());
		episodeListFragment.showLoadFailed();
	}

	@Override
	public void onEpisodeSelected(Episode selectedEpisode) {
		colorDivider(R.id.divider_second, true);
		episodeFragment.setEpisode(selectedEpisode);
	}
	
	@Override
	public void onNoEpisodeSelected() {
		colorDivider(R.id.divider_second, false);
	}
	
	private void colorDivider(int dividerViewId, boolean color) {
		if (getWindow() != null && getWindow().findViewById(dividerViewId) != null) {
			View divider = getWindow().findViewById(dividerViewId);
			divider.setBackgroundResource(color ? R.color.divider_on : R.color.divider_off);
		}
	}
}
