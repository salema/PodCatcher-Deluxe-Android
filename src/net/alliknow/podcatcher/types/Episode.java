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

/**
 * The episode type.
 * 
 * @author Kevin Hausmann
 */
public class Episode implements Comparable<Episode> {

	/** This episode title */
	private String name;
	/** The episode's online location */
	private URL mediaUrl;
	/** The podcast this episode is part of */
	private Podcast podcast;
	/** The episode's release date */
	private Date pubDate;
	/** The episode's description */
	private String description;
	
	/**
	 * Create a new episode
	 * @param episodeNodes XML document nodes representing this episode
	 */
	public Episode(Podcast podcast, NodeList episodeNodes) {
		this.podcast = podcast;
		
		readData(episodeNodes);
	}

	/**
	 * @return The episode's title
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * @return The media content online location
	 */
	public URL getMediaUrl() {
		return mediaUrl;
	}
	
	/**
	 * @return The owning podcast
	 */
	public Podcast getPodcast() {
		return podcast;
	}
	
	/**
	 * @return The publication date for this episode
	 */
	public Date getPubDate() {
		return pubDate;
	}
	
	/**
	 * @return The description for this episode (if any). Might be null.
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
		if (!(o instanceof Episode)) return false;
		else return mediaUrl.equals(((Episode) o).getMediaUrl());
	}
	
	@Override
	public int hashCode() {
		return mediaUrl.hashCode();
	}

	@Override
	public int compareTo(Episode another) {
		return pubDate.compareTo(another.getPubDate());
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
	
	private URL createMediaUrl(String attributeValue) {
		try {
			return new URL(attributeValue);
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return null;
	}
	
	private Date parsePubDate(String attributeValue) {
		try {
			DateFormat formatter = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.ENGLISH);
			return formatter.parse(attributeValue);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return null;
	}
}
