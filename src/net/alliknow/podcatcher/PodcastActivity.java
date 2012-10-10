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
import net.alliknow.podcatcher.fragments.PodcastListFragment.OnPodcastLoadedListener;
import net.alliknow.podcatcher.fragments.PodcastListFragment.OnPodcastSelectedListener;
import net.alliknow.podcatcher.types.Episode;
import net.alliknow.podcatcher.types.Podcast;
import android.app.Activity;
import android.os.Bundle;

/**
 * Our main activity class. Handles configuration changes.
 * All the heavy lifting is done in fragments, that will be
 * retaining on activity restarts.
 * 
 * @author Kevin Hausmann
 */
public class PodcastActivity extends Activity implements 
	OnPodcastSelectedListener, OnPodcastLoadedListener, OnEpisodeSelectedListener {
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	
	    setContentView(R.layout.main);
	}

	@Override
	public void onPodcastSelected(Podcast podcast) {
		findEpisodeListFragment().clearAndSpin();
	}
	
	@Override
	public void onPodcastLoaded(Podcast podcast) {
		findEpisodeListFragment().setEpisodeList(podcast.getEpisodes());
	}
	
	@Override
	public void onPodcastLoadFailed(Podcast failedPodcast) {
		findEpisodeListFragment().showError(getResources().getString(R.string.error_podcast_load));
	}

	@Override
	public void onEpisodeSelected(Episode selectedEpisode) {
		EpisodeFragment ef = (EpisodeFragment) getFragmentManager().findFragmentById(R.id.episode);
		ef.setEpisode(selectedEpisode);
	}
	
	private EpisodeListFragment findEpisodeListFragment() {
		return (EpisodeListFragment) getFragmentManager().findFragmentById(R.id.episode_list);
	}
}
