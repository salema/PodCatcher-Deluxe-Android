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
import net.alliknow.podcatcher.types.Episode;
import net.alliknow.podcatcher.types.Podcast;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

/**
 * Our main activity class. Handles configuration changes.
 * All the heavy lifting is done in fragments, that will be
 * retained on activity restarts.
 * 
 * @author Kevin Hausmann
 */
public class PodcastActivity extends Activity implements 
	OnSelectPodcastListener, OnLoadPodcastListener, OnSelectEpisodeListener {
	
	/** The podcast list fragment */
	private PodcastListFragment podcastListFragment;
	/** The episode list fragment */
	private EpisodeListFragment episodeListFragment;
	/** The episode details fragment */
	private EpisodeFragment episodeFragment;
	
	private View firstDivider;
	
	/** The podcatcher website URL */
	private static final String PODCATCHER_WEBSITE = "http://www.podcatcher-deluxe.com";
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    
	    setContentView(R.layout.main);
	    
	    podcastListFragment = (PodcastListFragment) getFragmentManager().findFragmentById(R.id.podcast_list);
		episodeListFragment = (EpisodeListFragment) getFragmentManager().findFragmentById(R.id.episode_list);
		episodeFragment = (EpisodeFragment) getFragmentManager().findFragmentById(R.id.episode);
		
		if (podcastListFragment.isPodcastSelected()) switchBackground(R.id.first_divider, true);
		if (episodeListFragment.isEpisodeSelected()) switchBackground(R.id.second_divider, true);
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
		getMenuInflater().inflate(R.menu.podcatcher_menu, menu);
		
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
	        case R.id.about_menu:
	        	Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(PODCATCHER_WEBSITE));
	   			startActivity(intent);
	            
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
		switchBackground(R.id.first_divider, true);
		episodeListFragment.clearAndSpin();
	}
	
	@Override
	public void onNoPodcastSelected() {
		switchBackground(R.id.first_divider, false);
	}
	
	@Override
	public void onPodcastLoadProgress(int progress) {
		episodeListFragment.showProgress(progress);
	}
	
	@Override
	public void onPodcastLoaded(Podcast podcast, boolean wasBackground) {
		if (! wasBackground)
			episodeListFragment.setEpisodeList(podcast.getEpisodes());
	}
	
	@Override
	public void onPodcastLoadFailed(Podcast failedPodcast, boolean wasBackground) {
		if (! wasBackground) {
			// Reset the episode list so the old one would not reappear of config changes
			episodeListFragment.setEpisodeList(new ArrayList<Episode>());
			episodeListFragment.showError(getResources().getString(R.string.error_podcast_load));
		}
	}

	@Override
	public void onEpisodeSelected(Episode selectedEpisode) {
		switchBackground(R.id.second_divider, true);
		episodeFragment.setEpisode(selectedEpisode);
	}
	
	private void switchBackground(int viewId, boolean color) {
		if (getWindow() == null || getWindow().findViewById(viewId) == null) return;
		
		View divider = getWindow().findViewById(viewId);
		divider.setBackgroundResource(color ? android.R.color.holo_orange_dark : android.R.color.darker_gray);
	}
}
