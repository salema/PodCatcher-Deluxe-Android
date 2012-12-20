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
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.alliknow.podcatcher.PodcastData;
import net.alliknow.podcatcher.listeners.OnLoadPodcastListListener;
import net.alliknow.podcatcher.tags.OPML;
import net.alliknow.podcatcher.types.Podcast;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.content.Context;
import android.os.AsyncTask;
import android.text.Html;
import android.util.Log;

/**
 * Loads the default podcast list from the filesystem asynchronously.
 */
public class LoadPodcastListTask extends AsyncTask<Void, Progress, List<Podcast>> {

	/** Our context */
	private Context context;
	/** The listener callback */
	private OnLoadPodcastListListener listener;
		
	/**
	 * Create new task.
	 * @param context Context to read file from. This will not be
	 * leaked if you keep a handle on this task, but set to <code>null</code>
	 * after execution.
	 * @param listener Callback to be alerted on completion.
	 */
	public LoadPodcastListTask(Context context, OnLoadPodcastListListener listener) {
		this.listener = listener;
		this.context = context;
	}
	
	@Override
	protected List<Podcast> doInBackground(Void... params) {
		InputStream fileStream = null;
		
		try {
			// Build parser
			XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
			factory.setNamespaceAware(true);
			// Create the parser to use
			XmlPullParser parser = factory.newPullParser();
			// Open default podcast file
			fileStream = context.openFileInput(PodcastData.OPML_FILENAME);
			parser.setInput(fileStream, PodcastData.OPML_FILE_ENCODING);
			// Create list
			List<Podcast> result = new ArrayList<Podcast>();
						
			// Start parsing
			int eventType = parser.next();
					
			// Read complete document
			while (eventType != XmlPullParser.END_DOCUMENT) {
				// We only need start tags here
				if (eventType == XmlPullParser.START_TAG) {
					String tagName = parser.getName();
					
					// Podcast found
					if (tagName.equalsIgnoreCase(OPML.OUTLINE)) 
						result.add(createPodcast(parser));
				}
				
				// Done, get next parsing event
				eventType = parser.next();
			}
					
			// Sort and tidy up!
			while (result.remove(null));
			Collections.sort(result);
						
			return result;
		} catch (Exception e) {
			Log.e(getClass().getSimpleName(), "Load failed for podcast list!", e);
			
			// Return empty list as a fall-back
			return new ArrayList<Podcast>();
		} finally {
			// Make sure we do not leak the context
			this.context = null;
			// Make sure we close the file stream
			if (fileStream != null)
				try {
					fileStream.close();
				} catch (IOException e) { /* pass... */ }
		}
	}

	private Podcast createPodcast(XmlPullParser parser) {
		try {
			// Make sure we start at item tag
			parser.require(XmlPullParser.START_TAG, "", OPML.OUTLINE);
			// Get the podcast name
			String name = parser.getAttributeValue("", OPML.TEXT);
			// Make sure podcast name looks good
			if (name.equals("null")) name = null;
			else name = Html.fromHtml(name).toString();
			// Get and parse podcast url
			URL url = new URL(parser.getAttributeValue("", OPML.XMLURL));
			// Create the podcast
			return new Podcast(name, url);
		} catch (MalformedURLException e) {
			Log.d(getClass().getSimpleName(), "OPML outline has bad URL!", e);
		} catch (XmlPullParserException e) {
			Log.d(getClass().getSimpleName(), "OPML outline not parsable!", e);
		} catch (IOException e) { /* pass */ }
		
		return null;
	}

	@Override
	protected void onPostExecute(List<Podcast> result) {
		if (listener != null) listener.onPodcastListLoaded(result);
		else Log.d(getClass().getSimpleName(), "Podcast list loaded, but no listener attached");
	}
}
