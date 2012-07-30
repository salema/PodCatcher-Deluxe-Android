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

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import net.alliknow.podcatcher.tags.RSS;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

/**
 * The podcast type.
 * 
 * @author Kevin Hausmann
 */
public class Podcast {

	private String name;
	private URL url;
	private Document podcastRssFile;
	private List<Episode> episodes;
	
	public Podcast(String name, URL url) {
		this.name = name;
		this.url = url;
		
		this.episodes = new ArrayList<Episode>();
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * @return the url
	 */
	public URL getUrl() {
		return url;
	}
	
	/**
	 * Find and return all episodes for this podcast. Will never return null
	 * but an empty list when encountering problems. Set the RSS file before
	 * expecting any results. 
	 * @return The list of episodes as listed in the feed.
	 * @see setRssFile
	 */
	public List<Episode> getEpisodes() {
		return this.episodes;
	}
		
	public void setRssFile(Document rssFile) {
		this.podcastRssFile = rssFile;
		this.episodes.clear();
		
		NodeList episodeNodes = this.podcastRssFile.getElementsByTagName(RSS.ITEM);
		
		for (int episodeIndex = 0; episodeIndex < episodeNodes.getLength(); episodeIndex++) {
			NodeList itemNodes = episodeNodes.item(episodeIndex).getChildNodes();
			
			for (int index = 0; index < itemNodes.getLength(); index++) {
				if (itemNodes.item(index).getNodeName().equals(RSS.TITLE)) 
					this.episodes.add(new Episode(itemNodes.item(index).getTextContent().trim()));
			}
		}
	}
	
	@Override
	public String toString() {
		if (name == null) return "Unnamed podcast";
		if (url == null) return name;
		else return name + " at " + url.toString();
	}
	
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof Podcast)) return false;
		else return this.url.equals(((Podcast) o).getUrl());
	}
}
