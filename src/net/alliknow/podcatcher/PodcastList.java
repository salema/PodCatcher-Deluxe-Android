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

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

import javax.xml.parsers.DocumentBuilderFactory;

import net.alliknow.podcatcher.tags.OPML;
import net.alliknow.podcatcher.types.Podcast;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import android.content.Context;
import android.util.Log;

/**
 * Custom list type for podcasts.
 * 
 * @author Kevin Hausmann
 */
public class PodcastList extends ArrayList<Podcast> {
	
	/** The id */
	private static final long serialVersionUID = 7226640001395545556L;
	/** The name of the file we store our saved podcasts in (as OPML) */
	private static final String OPML_FILENAME = "podcasts.opml";
	/** The OPML file encoding */
	private static final String OPML_FILE_ENCODING = "utf8";
	/** Content of OPML file title tag */
	private static final String OPML_TITLE = "Simple Podcatcher Podcast file";
	
	/**
	 * Load the podcast list from its default location.
	 * @param context Context to use for loading the podcast list.
	 */
	public void load(Context context) {
		//this is just for testing
		//if (Podcatcher.isInDebugMode(context)) writeDummy(context);
		
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware(true);
			
			Document podcastFile = factory.newDocumentBuilder().parse(context.openFileInput(OPML_FILENAME));
			NodeList podcasts = podcastFile.getElementsByTagName(OPML.OUTLINE);
			
			for (int index = 0; index < podcasts.getLength(); index++) 
				add(new Podcast(podcasts.item(index)));
			
			Collections.sort(this); 
		} catch (Exception e) {
			Log.e(getClass().getSimpleName(), "Cannot load OPML file", e);
		}
	}
	
	/**
	 * Store the podcast list to its default location.
	 * @param context Context to use for storing the podcast list.
	 */
	public void store(Context context) {
		try {			
			BufferedWriter writer = getPodcastFileWriter(context);
			
			writer.write("<?xml version=\"1.0\" encoding=\"" + OPML_FILE_ENCODING + "\"?>");
			writer.write("<opml version=\"2.0\">");
			writer.write("<head>");
			writer.write("<title>" + OPML_TITLE + "</title>");
			writer.write("<dateModified>" + new Date().toString() + "</dateModified>");
			writer.write("</head>");
			writer.write("<body>");
			
			for (Podcast podcast : this) writer.write(podcast.toOpmlString());
			
			writer.write("</body></opml>");
			writer.close();
			
			Log.d(getClass().getSimpleName(), "OPML podcast file written");
		} catch (Exception e) {
			Log.e(getClass().getSimpleName(), "Cannot store podcast OPML file", e);
		}
	}

	private void writeDummy(Context context) {
		try {
			BufferedWriter writer = getPodcastFileWriter(context);
			
			writer.write("<?xml version=\"1.0\" encoding=\"" + OPML_FILE_ENCODING + "\"?>");
			writer.write("<opml version=\"2.0\">");
			writer.write("<body>");
			writer.write("<outline text=\"This American Life\" type=\"rss\" xmlUrl=\"http://feeds.thisamericanlife.org/talpodcast\"/>");
			writer.write("<outline text=\"Radiolab\" xmlUrl=\"http://feeds.wnyc.org/radiolab\" type=\"rss\"/>");
			writer.write("<outline text=\"Linux' Outlaws\" xmlUrl=\"http://feeds.feedburner.com/linuxoutlaws\" type=\"rss\"/>");
			writer.write("<outline text=\"GEO\" type=\"rss\" xmlUrl=\"http://www.geo.de/GEOaudio/index.xml\"/>");
			writer.write("<outline text=\"Mäuse\" xmlUrl=\"http://podcast.wdr.de/maus.ml\"/>");
			writer.write("<outline text=\"Dude\" xmlUrl=\"http://feeds.feedburner.com/UhhYeahDude\"/>");
			writer.write("</body></opml>");
			writer.close();
			
			Log.d(getClass().getSimpleName(), "Dummy OPML written");
		} catch (Exception e) {
			Log.e(getClass().getSimpleName(), "Cannot write dummy OPML file", e);
		}
	}
	
	private BufferedWriter getPodcastFileWriter(Context context) throws FileNotFoundException,	UnsupportedEncodingException {
		FileOutputStream fos = context.openFileOutput(OPML_FILENAME, Context.MODE_PRIVATE);
		
		return new BufferedWriter(new OutputStreamWriter(fos, OPML_FILE_ENCODING));
	}
}
