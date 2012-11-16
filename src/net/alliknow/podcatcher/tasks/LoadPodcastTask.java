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

import java.io.ByteArrayInputStream;

import javax.xml.parsers.DocumentBuilderFactory;

import net.alliknow.podcatcher.listeners.OnLoadPodcastListener;
import net.alliknow.podcatcher.types.Podcast;

import org.w3c.dom.Document;

import android.util.Log;

/**
 * Loads podcast RSS file asynchroniously.
 * Implement to PodcastLoader interface to be alerted on completion or failure.
 * The downloaded file will be used as the podcast's content via <code>setRssFile()</code>,
 * use the podcast object given (and returned via callbacks) to access it.
 * 
 * @author Kevin Hausmann
 */
public class LoadPodcastTask extends LoadRemoteFileTask<Podcast, Void> {
	
	/** Maximum byte size for the logo to load */
	public static final int MAX_RSS_FILE_SIZE = 1000000;
	
	/** Owner */
	private final OnLoadPodcastListener listener;

	/** Podcast currently loading */
	private Podcast podcast;
	/** Document builder to use */
	private DocumentBuilderFactory factory;
	
	/**
	 * Create new task
	 * @param listener Owner fragment
	 */
	public LoadPodcastTask(OnLoadPodcastListener listener) {
		this.listener = listener;
		
		factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
	}
	
	/**
	 * Create new task
	 * @param fragment Owner fragment
	 * @param background Whether this task should run in the background, i.e.
	 * no progress update will be given
	 */
	public LoadPodcastTask(OnLoadPodcastListener listener, boolean background) {
		this(listener);
		
		this.background = background;
	}
	
	@Override
	protected Void doInBackground(Podcast... podcasts) {
		this.podcast = podcasts[0];

		try {
			if (podcast == null || podcast.getUrl() == null) throw new Exception("Podcast and/or URL cannot be null!");
			
			// Load the file from the internets
			if (! background) publishProgress(PROGRESS_CONNECT);
			byte[] podcastRssFile = loadFile(podcast.getUrl(), MAX_RSS_FILE_SIZE);
			
			// Get result as a document
			if (isCancelled()) return null;
			else if (! background) publishProgress(PROGRESS_PARSE);
			Document rssDocument = factory.newDocumentBuilder().parse(new ByteArrayInputStream(podcastRssFile));
			
			// Set as podcast content
			if (! isCancelled()) podcast.setRssFile(rssDocument);
		} catch (Exception e) {
			failed = true;
			Log.w(getClass().getSimpleName(), "Load failed for podcast \"" + podcasts[0] + "\"", e);
		}
		
		return null;
	}
	
	@Override
	protected void onProgressUpdate(Integer... values) {
		if (listener != null) listener.onPodcastLoadProgress(values[0]);
		else if (listener == null) Log.d(getClass().getSimpleName(), "Podcast progress update, but no listener attached");
	}
	
	@Override
	protected void onPostExecute(Void nothing) {
		// Background task failed to complete
		if (failed) {
			if (listener != null) listener.onPodcastLoadFailed(podcast, background);
			else Log.d(getClass().getSimpleName(), "Podcast failed to load, but no listener attached");
		} // Podcast was loaded
		else if (listener != null) listener.onPodcastLoaded(podcast, background);
		else Log.d(getClass().getSimpleName(), "Podcast loaded, but no listener attached");
	}
}