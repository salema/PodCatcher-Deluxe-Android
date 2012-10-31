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

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URLConnection;

import javax.xml.parsers.DocumentBuilderFactory;

import net.alliknow.podcatcher.listeners.OnLoadPodcastListener;
import net.alliknow.podcatcher.types.Podcast;

import org.w3c.dom.Document;

import android.os.AsyncTask;
import android.util.Log;

/**
 * Loads podcast RSS file asynchroniously.
 * Implement to PodcastLoader interface to be alerted on completion or failure.
 * 
 * @author Kevin Hausmann
 */
public class LoadPodcastTask extends AsyncTask<Podcast, Integer, Void> {
	
	/** Flag given by progress callback for connecting */
	public static final int PROGRESS_CONNECT = -3;
	/** Flag given by progress callback for loading */
	public static final int PROGRESS_LOAD = -2;
	/** Flag given by progress callback for parsing */
	public static final int PROGRESS_PARSE = -1;
	
	/** The connection timeout */
	private static final int PODCAST_LOAD_TIMEOUT = 8000;

	/** Owner */
	private final OnLoadPodcastListener listener;

	/** Podcast currently loading */
	private Podcast podcast;
	/** Document builder to use */
	private DocumentBuilderFactory factory;
	
	/** Whether we run in the background */
	private boolean background = false;
	/** Store whether loading failed */
	private boolean failed = false;
	
	/**
	 * Create new task
	 * @param fragment Owner fragment
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
			
			if (! background) publishProgress(PROGRESS_CONNECT);
			URLConnection connection = podcast.getUrl().openConnection();
			connection.setConnectTimeout(PODCAST_LOAD_TIMEOUT);
			// TODO I might want to set a ReadTimeout here ???
			
			Document podcastRssFile = loadPodcastFile(connection);
			if (! isCancelled()) podcast.setRssFile(podcastRssFile);
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
		if (failed) notifyFailed();
		// Podcast was loaded
		else if (listener != null) listener.onPodcastLoaded(podcast, background);
		else Log.d(getClass().getSimpleName(), "Podcast loaded, but no listener attached");
	}
	
	private Document loadPodcastFile(URLConnection connection) throws Exception {
		InputStream in = null;
		ByteArrayOutputStream result = null;
		
		try {
			// Open stream and check whether we know its length
			in = new BufferedInputStream(connection.getInputStream());
			boolean sendLoadProgress = connection.getContentLength() > 0;
					
			// Create the byte buffer to write to
			result = new ByteArrayOutputStream();
			if (! background) publishProgress(PROGRESS_LOAD);
			
			byte[] buffer = new byte[1024];
			int bytesRead = 0;
			int totalBytes = 0;
			// Read stream and report progress (if possible)
			while((bytesRead = in.read(buffer)) > 0) {
				if (isCancelled()) return null;
				result.write(buffer, 0, bytesRead);
				totalBytes += bytesRead;
			  
				if (sendLoadProgress && !background)
					publishProgress((int)((float)totalBytes / (float)connection.getContentLength() * 100));
			}
			
			// Return result as a document
			if (! background) publishProgress(PROGRESS_PARSE);
			return factory.newDocumentBuilder().parse(new ByteArrayInputStream(result.toByteArray()));
		} catch (Exception e) {
			throw e;
		} finally {
			// Close the streams
			if (in != null) in.close();
			if (result != null) result.close();
		}
	}
	
	private void notifyFailed() {
		if (listener != null) listener.onPodcastLoadFailed(podcast, background);
		else Log.d(getClass().getSimpleName(), "Podcast failed to load, but no listener attached");
	}
}