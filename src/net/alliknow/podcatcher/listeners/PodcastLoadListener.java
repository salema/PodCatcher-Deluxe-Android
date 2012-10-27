package net.alliknow.podcatcher.listeners;

import net.alliknow.podcatcher.types.Podcast;

/**
 * Interface definition for a callback to be invoked when a podcast is loaded.
 */
public interface PodcastLoadListener {
	
	/**
	 * Called on progress update.
	 * @param percent Percent of podcast RSS file loaded.
	 * Note that this only works if the http connection
	 * reports its content length correctly. Otherwise 
	 * (and this happens in the wild out there) percent might be >100.
	 */
	public void onPodcastLoadProgress(int percent);
	
	/**
	 * Called on completion.
	 * @param podcast Podcast loaded.
	 */
	public void onPodcastLoaded(Podcast podcast);
	
	/**
	 * Called when loading the podcast failed.
	 * @param podcast Podcast failing to load.
	 */
	public void onPodcastLoadFailed(Podcast podcast);
}