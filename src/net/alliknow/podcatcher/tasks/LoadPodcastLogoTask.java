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

import java.io.IOException;

import net.alliknow.podcatcher.fragments.PodcastListFragment;
import net.alliknow.podcatcher.types.Podcast;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;

/**
 * An async task to load a podcast logo
 * 
 * @author Kevin Hausmann
 */
public class LoadPodcastLogoTask extends AsyncTask<Podcast, Void, Bitmap> {

	/** Owner */
	private final PodcastListFragment owner;
	
	/**
	 * Create new task
	 * @param podcastActivity Owner activity
	 */
	public LoadPodcastLogoTask(PodcastListFragment fragment) {
		this.owner = fragment;
	}
	
	@Override
	protected Bitmap doInBackground(Podcast... podcasts) {
		try {
			return BitmapFactory.decodeStream(podcasts[0].getLogoUrl().openStream());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return null;
	}
	
	@Override
	protected void onPostExecute(Bitmap result) {
		this.owner.onPodcastLogoLoaded(result);
	}
}
