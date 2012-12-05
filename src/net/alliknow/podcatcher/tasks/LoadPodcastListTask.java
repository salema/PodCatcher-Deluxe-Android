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

import static net.alliknow.podcatcher.Podcatcher.OPML_FILENAME;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;

import net.alliknow.podcatcher.listeners.OnLoadPodcastListListener;
import net.alliknow.podcatcher.tags.OPML;
import net.alliknow.podcatcher.types.Podcast;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

/**
 * Loads the default podcast list from the filesystem asynchronously.
 */
public class LoadPodcastListTask extends AsyncTask<Void, Progress, List<Podcast>> {

	/** The listener callback */
	private final OnLoadPodcastListListener listener;
	/** Our context */
	private final Context context;
	
	/**
	 * Create new task.
	 * @param context Context to read file from.
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
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware(true);
			// Open default podcast file
			fileStream = context.openFileInput(OPML_FILENAME);
			Document podcastFile = factory.newDocumentBuilder().parse(fileStream);
			NodeList podcasts = podcastFile.getElementsByTagName(OPML.OUTLINE);
			
			// Create and fill list
			List<Podcast> result = new ArrayList<Podcast>();
			for (int index = 0; index < podcasts.getLength(); index++) 
				result.add(new Podcast(podcasts.item(index)));
			
			// Sort and tidy up!
			Collections.sort(result);
			fileStream.close();
			
			return result;
		} catch (Exception e) {
			Log.e(getClass().getSimpleName(), "Load failed for podcast list!", e);
			return null;
		} 		
	}

	@Override
	protected void onPostExecute(List<Podcast> result) {
		if (listener != null) listener.onPodcastListLoaded(result);
		else Log.d(getClass().getSimpleName(), "Podcast list loaded, but no listener attached");
	}
}
