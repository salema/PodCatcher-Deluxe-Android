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

import javax.xml.parsers.DocumentBuilderFactory;

import net.alliknow.podcatcher.PodcastActivity;
import net.alliknow.podcatcher.types.Podcast;

import org.w3c.dom.Document;

import android.os.AsyncTask;

/**
 * Loads podcast RSS file asynchroniously. Auto-cancelles itself
 * on network or document parsing errors.
 * 
 * @author Kevin Hausmann
 */
public class LoadPodcastTask extends AsyncTask<Podcast, Void, Document> {
	
	/** Owner */
	private final PodcastActivity podcastActivity;

	/** Podcast currently loading */
	private Podcast podcast;
	
	/**
	 * Create new task
	 * @param podcastActivity Owner activity
	 */
	public LoadPodcastTask(PodcastActivity podcastActivity) {
		this.podcastActivity = podcastActivity;
	}
	
	@Override
	protected Document doInBackground(Podcast... podcasts) {
		this.podcast = podcasts[0];

		try {
			return DocumentBuilderFactory.newInstance().newDocumentBuilder()
					.parse(podcast.getUrl().openStream());
		} catch (Exception e) {
			this.cancel(true);
		}
		
		return null;
	}
	
	@Override
	protected void onCancelled(Document result) {
		this.podcastActivity.onPodcastLoadFailed();
	}
	
	
	@Override
	protected void onPostExecute(Document result) {
		this.podcast.setRssFile(result);
		this.podcastActivity.onPodcastLoaded(podcast);
	}
	
}