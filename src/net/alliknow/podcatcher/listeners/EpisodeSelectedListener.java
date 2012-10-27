package net.alliknow.podcatcher.listeners;

import net.alliknow.podcatcher.types.Episode;

/** Container Activity must implement this interface */
public interface EpisodeSelectedListener {
	/**
	 * Updates the UI to reflect that a podcast has been selected.
	 * @param selectedPodcast Podcast selected by the user
	 */
	public void onEpisodeSelected(Episode selectedEpisode);
}