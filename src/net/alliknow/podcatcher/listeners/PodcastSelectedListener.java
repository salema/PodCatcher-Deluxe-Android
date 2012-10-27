package net.alliknow.podcatcher.listeners;

import net.alliknow.podcatcher.types.Podcast;

/** Container Activity must implement this interface */
public interface PodcastSelectedListener {
	/**
	 * Updates the UI to reflect that a podcast has been selected.
	 * @param selectedPodcast Podcast selected by the user
	 */
	public void onPodcastSelected(Podcast selectedPodcast);
}