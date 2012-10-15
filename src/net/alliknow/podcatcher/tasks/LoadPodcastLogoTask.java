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
package net.alliknow.podcatcher.tasks;

import net.alliknow.podcatcher.types.Podcast;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;

/**
 * An async task to load a podcast logo.
 * Implement PodcastLogoLoader to be alerted on completion or failure
 * 
 * @author Kevin Hausmann
 */
public class LoadPodcastLogoTask extends AsyncTask<Podcast, Void, Bitmap> {

	/**
     * Interface definition for a callback to be invoked when a podcast logo is loaded.
     */
	public interface PodcastLogoLoader {
		
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
	
	/** Owner */
	private final PodcastLogoLoader loader;
	
	/** Store whether loading failed */
	private boolean failed = false;
	
	/**
	 * Create new task
	 * @param fragment Owner fragment
	 */
	public LoadPodcastLogoTask(PodcastLogoLoader fragment) {
		this.loader = fragment;
	}
	
	@Override
	protected Bitmap doInBackground(Podcast... podcasts) {
		try {
			if (podcasts[0] == null || podcasts[0].getLogoUrl() == null) throw new Exception("Podcast and/or logo URL cannot be null!");
			
			return BitmapFactory.decodeStream(podcasts[0].getLogoUrl().openStream());
		} catch (Exception e) {
			failed = true;
			Log.w(getClass().getSimpleName(), "Logo failed to load for podcast \"" + podcasts[0] + "\" with " +
					"logo URL " + podcasts[0].getLogoUrl(), e);
		}
		
		return null;
	}
	
	@Override
	protected void onPostExecute(Bitmap result) {
		// Background task failed to complete
		if (failed || result == null) notifyFailed();
		// Podcast logo was loaded
		else {
			if (loader != null) loader.onPodcastLogoLoaded(result);
			else Log.d(getClass().getSimpleName(), "Podcast logo loaded, but no listener attached");
		}
	}

	private void notifyFailed() {
		if (loader != null) loader.onPodcastLogoLoadFailed();
		else Log.d(getClass().getSimpleName(), "Podcast logo loading failed, but no listener attached");
	}
}
