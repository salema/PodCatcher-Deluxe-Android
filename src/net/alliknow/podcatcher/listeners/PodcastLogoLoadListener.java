package net.alliknow.podcatcher.listeners;

import android.graphics.Bitmap;

/**
 * Interface definition for a callback to be invoked when a podcast logo is loaded.
 */
public interface PodcastLogoLoadListener {
	
	/**
	 * Called on completion.
	 * @param logo Podcast logo loaded.
	 */
	void onPodcastLogoLoaded(Bitmap logo);
	
	/**
	 * Called when loading the podcast logo failed.
	 */
	void onPodcastLogoLoadFailed();
}