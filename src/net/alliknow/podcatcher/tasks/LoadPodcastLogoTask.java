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

import net.alliknow.podcatcher.listeners.OnLoadPodcastLogoListener;
import net.alliknow.podcatcher.types.Podcast;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

/**
 * An async task to load a podcast logo.
 * Implement PodcastLogoLoader to be alerted on completion or failure
 * 
 * @author Kevin Hausmann
 */
public class LoadPodcastLogoTask extends LoadRemoteFileTask<Podcast, Bitmap> {

	/** Owner */
	private final OnLoadPodcastLogoListener loader;
	
	/** Dimensions we decode the logo image file to (saves memory in places) */
	protected final int requestedWidth;
	protected final int requestedHeight;
	
	/**
	 * Create new task
	 * @param fragment Owner fragment
	 */
	public LoadPodcastLogoTask(OnLoadPodcastLogoListener fragment, int requestedWidth, int requestedHeight) {
		this.loader = fragment;
		this.background = true;
		
		this.requestedWidth = requestedWidth;
		this.requestedHeight = requestedHeight;
	}
	
	@Override
	protected Bitmap doInBackground(Podcast... podcasts) {
		try {
			if (podcasts[0] == null || podcasts[0].getLogoUrl() == null) throw new Exception("Podcast and/or logo URL cannot be null!");
			
			byte[] logo = loadFile(podcasts[0].getLogoUrl());
			
			if (! isCancelled()) return decodeAndSampleBitmap(logo);
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
		if (failed || result == null) {
			if (loader != null) loader.onPodcastLogoLoadFailed();
			else Log.d(getClass().getSimpleName(), "Podcast logo loading failed, but no listener attached");
		} // Podcast logo was loaded
		else {
			if (loader != null) loader.onPodcastLogoLoaded(result);
			else Log.d(getClass().getSimpleName(), "Podcast logo loaded, but no listener attached");
		}
	}
	
	protected Bitmap decodeAndSampleBitmap(byte[] data) {
	    // First decode with inJustDecodeBounds=true to check dimensions
	    final BitmapFactory.Options options = new BitmapFactory.Options();
	    options.inJustDecodeBounds = true;
	    BitmapFactory.decodeByteArray(data, 0, data.length, options);

	    // Calculate inSampleSize
	    options.inSampleSize = calculateInSampleSize(options);

	    // Decode bitmap with inSampleSize set
	    options.inJustDecodeBounds = false;
	    return BitmapFactory.decodeByteArray(data, 0, data.length, options);
	}
	
	protected int calculateInSampleSize(BitmapFactory.Options options) {
	    // Raw height and width of image
	    final int height = options.outHeight;
	    final int width = options.outWidth;
	    int inSampleSize = 1;
	
	    if (height > requestedHeight || width > requestedWidth) {
	        if (width > height) {
	            inSampleSize = Math.round((float)height / (float)requestedHeight);
	        } else {
	            inSampleSize = Math.round((float)width / (float)requestedWidth);
	        }
	    }
	    return inSampleSize;
	}
}
