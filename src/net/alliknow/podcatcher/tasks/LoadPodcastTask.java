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

import java.net.URLConnection;

import javax.xml.parsers.DocumentBuilderFactory;

import net.alliknow.podcatcher.types.Podcast;

import org.w3c.dom.Document;

import android.os.AsyncTask;
import android.util.Log;

/**
 * Loads podcast RSS file asynchroniously. Auto-cancelles itself
 * on network or document parsing errors.
 * 
 * @author Kevin Hausmann
 */
public class LoadPodcastTask extends AsyncTask<Podcast, Void, Document> {
	
	/**
     * Interface definition for a callback to be invoked when a podcast is loaded.
     */
	public interface PodcastLoader {
		
		/**
		 * Called on completion.
		 * @param podcast Podcast loaded.
		 */
		void onPodcastLoaded(Podcast podcast);
		
		/**
		 * Called when loading the podcast failed.
		 * @param podcast Podcast failing to load.
		 */
		void onPodcastLoadFailed(Podcast podcast);
	}
	
	/** Owner */
	private final PodcastLoader loader;

	/** Podcast currently loading */
	private Podcast podcast;
	
	/** Document builder to use */
	private DocumentBuilderFactory factory;
	
	/**
	 * Create new task
	 * @param fragment Owner fragment
	 */
	public LoadPodcastTask(PodcastLoader fragment) {
		this.loader = fragment;
		
		factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
	}
	
	@Override
	protected Document doInBackground(Podcast... podcasts) {
		this.podcast = podcasts[0];

		try {
			if (podcast == null || podcast.getUrl() == null) throw new Exception("Podcast and/or URL cannot be null!");
			
			URLConnection connection = podcast.getUrl().openConnection();
			connection.setConnectTimeout(8000);
			// TODO I might want to set a ReadTimeout here ???
			
			return factory.newDocumentBuilder().parse(connection.getInputStream());
		} catch (Exception e) {
			Log.w("Load Podcast", "Load failed for podcast \"" + podcasts[0] + "\"", e);
			cancel(true);
		}
		
		return null;
	}
	
	@Override
	protected void onPostExecute(Document result) {
		podcast.setRssFile(result);
		loader.onPodcastLoaded(podcast);
	}
	
	@Override
	protected void onCancelled(Document result) {
		loader.onPodcastLoadFailed(podcast);
	}
}