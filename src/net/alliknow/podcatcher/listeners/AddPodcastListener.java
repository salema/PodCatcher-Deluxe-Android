package net.alliknow.podcatcher.listeners;

import net.alliknow.podcatcher.types.Podcast;

/**
 * Interface definition for a callback to be invoked when a podcast is added.
 */
public interface AddPodcastListener {
	
	/**
	 * Called on listener when podcast is added.
	 * @param newPodcast Podcast to add.
	 */
	void addPodcast(Podcast newPodcast);
}