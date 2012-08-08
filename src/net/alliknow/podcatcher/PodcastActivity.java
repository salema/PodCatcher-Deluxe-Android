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
import net.alliknow.podcatcher.fragments.EpisodeListFragment.OnEpisodeSelectedListener;
import net.alliknow.podcatcher.fragments.PodcastListFragment;
import net.alliknow.podcatcher.fragments.PodcastListFragment.OnPodcastLoadedListener;
import net.alliknow.podcatcher.fragments.PodcastListFragment.OnPodcastSelectedListener;
import net.alliknow.podcatcher.types.Episode;
import net.alliknow.podcatcher.types.Podcast;
import android.app.Activity;
import android.os.Bundle;

public class PodcastActivity extends Activity implements 
	OnPodcastSelectedListener, OnPodcastLoadedListener, OnEpisodeSelectedListener {
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	
	    setContentView(R.layout.main);
	}

	@Override
	public void onPodcastSelected(Podcast podcast) {
		findEpisodeListFragment().clearAndSpin();
	}
	
	/**
	 * Notified by async RSS file loader on completion.
	 * Updates UI to display the podcast's episodes.
	 * @param podcast Podcast RSS feed loaded for
	 */
	public void onPodcastLoaded(Podcast podcast) {
		findEpisodeListFragment().setEpisodeList(podcast.getEpisodes());
	}

	@Override
	public void onEpisodeSelected(Episode selectedEpisode) {
		EpisodeFragment ef = (EpisodeFragment) getFragmentManager().findFragmentById(R.id.episode);
		ef.setEpisode(selectedEpisode);
	}
	
	private PodcastListFragment findPodcastListFragment() {
		return (PodcastListFragment) getFragmentManager().findFragmentById(R.id.podcast_list);
	}
	
	private EpisodeListFragment findEpisodeListFragment() {
		return (EpisodeListFragment) getFragmentManager().findFragmentById(R.id.episode_list);
	}
}
