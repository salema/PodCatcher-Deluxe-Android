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
package net.alliknow.podcatcher.model.tasks.remote;

import java.io.ByteArrayInputStream;

import net.alliknow.podcatcher.listeners.OnLoadPodcastListener;
import net.alliknow.podcatcher.model.tasks.Progress;
import net.alliknow.podcatcher.model.types.Podcast;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.util.Log;

/**
 * Loads podcast RSS file asynchronously.
 * Implement the PodcastLoader interface to be alerted on completion or failure.
 * The downloaded file will be used as the podcast's content via <code>parse()</code>,
 * use the podcast object given (and returned via callbacks) to access it.
 */
public class LoadPodcastTask extends LoadRemoteFileTask<Podcast, Void> {
	
	/** Maximum byte size for the RSS file to load */
	public static final int MAX_RSS_FILE_SIZE = 2000000;
	
	/** Owner */
	private final OnLoadPodcastListener listener;

	/** Podcast currently loading */
	private Podcast podcast;
	/** XML pull parser factory to use */
	private XmlPullParserFactory factory;
	
	/**
	 * Create new task.
	 * @param listener Owner fragment, receives call-backs.
	 */
	public LoadPodcastTask(OnLoadPodcastListener listener) {
		this.listener = listener;
		this.loadLimit = MAX_RSS_FILE_SIZE;
		
		try {
			factory = XmlPullParserFactory.newInstance();
		} catch (XmlPullParserException e) {
			Log.e(getClass().getSimpleName(), "Cannot get parser factory!", e);
		}
        factory.setNamespaceAware(true);
	}
	
	@Override
	protected Void doInBackground(Podcast... podcasts) {
		this.podcast = podcasts[0];

		try {
			if (podcast == null || podcast.getUrl() == null) throw new Exception("Podcast and/or URL cannot be null!");
			podcast.setLoading(true);
			
			// Load the file from the internets
			publishProgress(Progress.CONNECT);
			byte[] podcastRssFile = loadFile(podcast.getUrl());
			
			// Get result as a document
			if (isCancelled()) return null;
			else publishProgress(Progress.PARSE);
			
			// Create the parser to use
			XmlPullParser parser = factory.newPullParser();
			parser.setInput(new ByteArrayInputStream(podcastRssFile), null);
			
			// Set as podcast content
			if (! isCancelled()) podcast.parse(parser);
		} catch (Exception e) {
			failed = true;
			Log.w(getClass().getSimpleName(), "Load failed for podcast \"" + podcasts[0] + "\"", e);
		}
		
		return null;
	}
	
	@Override
	protected void onProgressUpdate(Progress... progress) {
		if (listener != null) listener.onPodcastLoadProgress(podcast, progress[0]);
		else if (listener == null) Log.d(getClass().getSimpleName(), "Podcast progress update, but no listener attached");
	}
	
	@Override
	protected void onPostExecute(Void nothing) {
		if (podcast != null) podcast.setLoading(false);
		
		// Background task failed to complete
		if (failed) {
			if (listener != null) listener.onPodcastLoadFailed(podcast);
			else Log.d(getClass().getSimpleName(), "Podcast failed to load, but no listener attached");
		} // Podcast was loaded
		else if (listener != null) listener.onPodcastLoaded(podcast);
		else Log.d(getClass().getSimpleName(), "Podcast loaded, but no listener attached");
	}
	
	@Override
	protected void onCancelled(Void result) {
		if (podcast != null) podcast.setLoading(false);
	}
}