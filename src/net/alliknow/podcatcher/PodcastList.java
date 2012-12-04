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
package net.alliknow.podcatcher;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;

import net.alliknow.podcatcher.types.Podcast;
import android.util.Log;

/**
 * Custom list type for podcasts.
 */
public class PodcastList extends ArrayList<Podcast> {
	
	/** The id */
	private static final long serialVersionUID = 7226640001395545556L;
	
	/** The name of the file we store our saved podcasts in (as OPML) */
	public static final String OPML_FILENAME = "podcasts.opml";
	/** The OPML file encoding */
	public static final String OPML_FILE_ENCODING = "utf8";
		
	@Override
	public boolean add(Podcast podcast) {
		if (podcast.hasNameAndUrl()) super.add(podcast);
		
		return true;
	}
	
	@Override
	public void add(int index, Podcast podcast) {
		if (podcast.hasNameAndUrl()) super.add(index, podcast);
	}
	
	/**
	 * Sort the podcast list using the <code>compareTo</code>
	 * method of the podcast type.
	 */
	public void sort() {
		Collections.sort(this);
	}
	
	/**
	 * Add a small number of sample podcast to the list for testing.
	 */
	public void addSamplePodcasts() {
		try {
			add(new Podcast("This American Life", new URL("http://feeds.thisamericanlife.org/talpodcast")));
			add(new Podcast("Radiolab", new URL("http://feeds.wnyc.org/radiolab")));
			add(new Podcast("Linux' Outlaws", new URL("http://feeds.feedburner.com/linuxoutlaws")));
			add(new Podcast("GEO", new URL("http://www.geo.de/GEOaudio/index.xml")));
			add(new Podcast("Mäuse", new URL("http://podcast.wdr.de/maus.xml")));
			add(new Podcast("D&uuml;de", new URL("http://feeds.feedburner.com/UhhYeahDude")));
			add(new Podcast("neo", new URL("http://www.zdf.de/ZDFmediathek/podcast/1446344?view=podcast")));
			
			sort();
		} catch (MalformedURLException e) {
			Log.e(getClass().getSimpleName(), "Cannot add sample podcasts!", e);
		}
	}
	
}
