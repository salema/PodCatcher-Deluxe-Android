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
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import net.alliknow.podcatcher.listeners.PodcastLoadListener;
import net.alliknow.podcatcher.types.Podcast;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import android.os.AsyncTask;
import android.util.Log;

/**
 * Loads podcast RSS file asynchroniously.
 * Implement to PodcastLoader interface to be alerted on completion or failure
 * 
 * @author Kevin Hausmann
 */
public class LoadPodcastTask extends AsyncTask<Podcast, Integer, Document> {
	
	private static final int PODCAST_LOAD_TIMEOUT = 8000;

	/** Owner */
	private final PodcastLoadListener listener;

	/** Podcast currently loading */
	private Podcast podcast;
	
	/** Document builder to use */
	private DocumentBuilderFactory factory;
	
	/** Store whether loading failed */
	private boolean failed = false;
	
	/**
	 * Create new task
	 * @param fragment Owner fragment
	 */
	public LoadPodcastTask(PodcastLoadListener listener) {
		this.listener = listener;
		
		factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
	}
	
	@Override
	protected Document doInBackground(Podcast... podcasts) {
		this.podcast = podcasts[0];

		try {
			if (podcast == null || podcast.getUrl() == null) throw new Exception("Podcast and/or URL cannot be null!");
			
			URLConnection connection = podcast.getUrl().openConnection();
			connection.setConnectTimeout(PODCAST_LOAD_TIMEOUT);
			// TODO I might want to set a ReadTimeout here ???
			
			return loadPodcastFile(connection);
		} catch (Exception e) {
			failed = true;
			Log.w(getClass().getSimpleName(), "Load failed for podcast \"" + podcasts[0] + "\"", e);
		}
		
		return null;
	}
	
	@Override
	protected void onProgressUpdate(Integer... values) {
		if (listener != null) listener.onPodcastLoadProgress(values[0]);
		else Log.d(getClass().getSimpleName(), "Podcast progress update, but no listener attached");
	}
	
	@Override
	protected void onPostExecute(Document result) {
		// Background task failed to complete
		if (failed || result == null) notifyFailed();
		// Podcast was loaded
		else {
			podcast.setRssFile(result);
			
			if (listener != null) listener.onPodcastLoaded(podcast);
			else Log.d(getClass().getSimpleName(), "Podcast loaded, but no listener attached");
		}
	}
	
	private Document loadPodcastFile(URLConnection connection) throws IOException, SAXException, ParserConfigurationException {
		InputStream in = new BufferedInputStream(connection.getInputStream());
		boolean sendProgressUpdates = connection.getContentLength() > 0;
		
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		
		int currentByte = 0;
		int bytesRead = 0;
		while((currentByte = in.read()) >= 0) {
		  buffer.write(currentByte);
		  bytesRead++;
		  
		  if (sendProgressUpdates && bytesRead % 1000 == 0) {
			  publishProgress((int)((float)bytesRead / (float)connection.getContentLength() * 100));
		  }
		}
		
		return factory.newDocumentBuilder().parse(new ByteArrayInputStream(buffer.toByteArray()));
	}
	
	private void notifyFailed() {
		if (listener != null) listener.onPodcastLoadFailed(podcast);
		else Log.d(getClass().getSimpleName(), "Podcast failed to load, but no listener attached");
	}
}