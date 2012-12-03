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
package net.alliknow.podcatcher.types;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import net.alliknow.podcatcher.tags.RSS;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.graphics.Bitmap;
import android.util.Log;

/**
 * The episode type.
 */
public class Episode implements Comparable<Episode> {

	/** The podcast this episode is part of */
	private Podcast podcast;
	
	/** This episode title */
	private String name;
	/** The episode's online location */
	private URL mediaUrl;
	/** The episode's release date */
	private Date pubDate;
	/** The episode's description */
	private String description;
	
	/**
	 * Create a new episode.
	 * @param podcast Podcast this episode belongs to.
	 * @param episodeNodes XML document nodes representing this episode.
	 */
	public Episode(Podcast podcast, NodeList episodeNodes) {
		this.podcast = podcast;
		
		if (episodeNodes != null && episodeNodes.getLength() > 0)
			readData(episodeNodes);
	}

	/**
	 * @return The episode's title.
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * @return The media content online location.
	 */
	public URL getMediaUrl() {
		return mediaUrl;
	}
	
	/**
	 * @return The owning podcast's name.
	 */
	public String getPodcastName() {
		if (podcast == null) return null;
		else return podcast.getName();
	}
	
	/**
	 * Get the podcast logo if available.
	 * @return The logo bitmap or <code>null</code> if unavailable.
	 */
	public Bitmap getPodcastLogo() {
		if (podcast == null) return null;
		else return podcast.getLogo();
	}
	
	/**
	 * @return The publication date for this episode.
	 */
	public Date getPubDate() {
		if (pubDate == null) return null;
		else return new Date(pubDate.getTime());
	}
	
	/**
	 * @return The description for this episode (if any). Might be <code>null</code>.
	 */
	public String getDescription() {
		return description;
	}
	
	@Override
	public String toString() {
		return getName();
	}
	
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		else if (!(o instanceof Episode)) return false;
		
		Episode other = (Episode)o;
		
		if (mediaUrl == null || other.getMediaUrl() == null) return false;
		else return mediaUrl.toExternalForm().equals(((Episode) o).getMediaUrl().toExternalForm());
	}
	
	@Override
	public int hashCode() {
		return mediaUrl == null ? 0 : mediaUrl.toExternalForm().hashCode();
	}

	@Override
	public int compareTo(Episode another) {
		if (pubDate == null || another == null || another.getPubDate() == null) return 0;
		else return -1 * pubDate.compareTo(another.getPubDate());
	}
	
	void parse(XmlPullParser xpp) throws XmlPullParserException, IOException {
		int eventType = xpp.next();
		
		while (!(eventType == XmlPullParser.END_TAG && xpp.getName() != null && xpp.getName().equalsIgnoreCase(RSS.ITEM))) {
			if (eventType != XmlPullParser.START_TAG) {
				eventType = xpp.next();
				continue;
			}
			
			// Episode title
			if (xpp.getName().equalsIgnoreCase(RSS.TITLE)) name = getNextText(xpp);
			// Episode media URL
			else if (xpp.getName().equalsIgnoreCase(RSS.ENCLOSURE))
				mediaUrl = createMediaUrl(xpp.getAttributeValue("", RSS.URL));
			// Episode publication date (2 options)
			else if (xpp.getName().equalsIgnoreCase(RSS.DATE))
				pubDate = parsePubDate(getNextText(xpp));
			else if (xpp.getName().equalsIgnoreCase(RSS.PUBDATE))
				pubDate = parsePubDate(getNextText(xpp));
			// Episode description
			else if (xpp.getName().equalsIgnoreCase(RSS.DESCRIPTION))
				description = getNextText(xpp);
			
			eventType = xpp.next();
		}
	}
	
	private String getNextText(XmlPullParser xpp) throws XmlPullParserException, IOException {
		String text;
		int eventType;
		
		do {
			eventType = xpp.next();
			text = xpp.getText();
		} while (eventType != XmlPullParser.TEXT);
		
		return text;
	}

	private void readData(NodeList episodeNodes) {
		// Go through all nodes and find the relevant information
		for (int index = 0; index < episodeNodes.getLength(); index++) {
			Node currentNode = episodeNodes.item(index);
			
			// Episode title
			if (currentNode.getNodeName().equals(RSS.TITLE)) 
				name = currentNode.getTextContent().trim();
			// Episode media URL
			else if (currentNode.getNodeName().equals(RSS.ENCLOSURE))
				mediaUrl = createMediaUrl(currentNode.getAttributes().getNamedItem(RSS.URL).getNodeValue());
			// Episode publication date (2 options)
			else if (currentNode.getNodeName().equals(RSS.DATE))
				pubDate = parsePubDate(currentNode.getTextContent());
			else if (currentNode.getNodeName().equals(RSS.PUBDATE))
				pubDate = parsePubDate(currentNode.getTextContent());
			// Episode description
			else if (currentNode.getNodeName().equals(RSS.DESCRIPTION))
				description = currentNode.getTextContent();
		}
	}
	
	private URL createMediaUrl(String url) {
		try {
			return new URL(url);
		} catch (MalformedURLException e) {
			Log.e(getClass().getSimpleName(), "Episode has invalid URL", e);
		}
		
		return null;
	}
	
	private Date parsePubDate(String attributeValue) {
		try {
			DateFormat formatter = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.ENGLISH);
			return formatter.parse(attributeValue);
		} catch (ParseException e) {
			Log.d(getClass().getSimpleName(), "Episode has invalid publication date", e);
		}
		
		return null;
	}
}
