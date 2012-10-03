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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import net.alliknow.podcatcher.tags.RSS;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * The podcast type.
 * 
 * @author Kevin Hausmann
 */
public class Podcast implements Comparable<Podcast> {

	/** The minimum time podcast content is buffered (in milliseconds). 
	 * If older, we need to reload. */
	public static int TIME_TO_LIFE = 15 * 60 * 1000;
	
	/** Name of the podcast */
	private String name;
	/** Location of the podcast's RSS file */
	private URL url;
	/** XML document representing the RSS file */
	private Document podcastRssFile;
	/** The podcasts list of episodes */
	private List<Episode> episodes;
	/** The podcast's image (logo) location */
	private URL logoUrl;
	/** The point in time when the RSS file as last been set */
	private Date updated;
	/** The encoding of the loaded file */
	private String encoding;
	
	/**
	 * Create new podcast by name and RSS file location.
	 * The name will not be read from the file, but remain as given. 
	 * All other data on the podcast will only be available after
	 * <code>setRssFile</code> was called.
	 * @param name The podcast's name
	 * @param url The location of the podcast's RSS file
	 * @see setRssFile
	 */
	public Podcast(String name, URL url) {
		this.name = name;
		this.url = url;
		
		this.episodes = new ArrayList<Episode>();
	}

	/**
	 * @return the podcast's name
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * @return the podcast's online location
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
	
	/**
	 * Find and return the podcast's image location (logo).
	 * Only works after RSS file is set.
	 * @return URL pointing at the logo location
	 * @see setRssFile
	 */
	public URL getLogoUrl() {
		return this.logoUrl;
	}
	
	/**
	 * The podcast's encoding.
	 * @return Get the input encoding for the podcast file loaded. 
	 * This may be <code>null</code>, if unknown.
	 */
	public String getEncoding() {
		return this.encoding;
	}
	
	/**
	 * Set the RSS file representing this podcast. This is were the object
	 * gets its information from. Many of its methods will not return valid results
	 * unless this method was called. Calling this method also resets all
	 * information read earlier.
	 * @param rssFile XML document representing the podcast
	 */
	public void setRssFile(Document rssFile) {
		this.episodes.clear();
		
		this.podcastRssFile = rssFile;
		this.updated = new Date();
		this.encoding = rssFile.getInputEncoding();
		
		if (name == null) loadName();
		loadMetadata();
		loadEpisodes();
	}
	
	/**
	 * Whether the podcast content is old enough to need reloading. This relates
	 * to the time that <code>setRssFile</code> has last been
	 * called on this object and has nothing to do with the updating
	 * of the podcast RSS file on the provider's server.
	 * 
	 * @return True if time to life expired or the podcast has never been loaded.
	 */
	public boolean needsReload() {
		// Has never been loaded
		if (this.updated == null) return true;
		// Check age
		else return new Date().getTime() - this.updated.getTime() > TIME_TO_LIFE;
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
	
	@Override
	public int hashCode() {
		return this.url.hashCode();
	}

	@Override
	public int compareTo(Podcast another) {
		return this.getName().compareTo(another.getName());
	}
	
	private void loadName() {
		NodeList titleNodes = this.podcastRssFile.getElementsByTagName(RSS.TITLE);
		
		if (titleNodes.getLength() > 0) this.name = titleNodes.item(0).getTextContent();
	}
	
	private void loadMetadata() {
		NodeList imageNodes = this.podcastRssFile.getElementsByTagNameNS("*", RSS.IMAGE);
		
		// image tag used?
		if (imageNodes.getLength() > 0) {
			Node imageNode = imageNodes.item(0);
			
			if (imageNode.getChildNodes().getLength() > 0) {
				this.logoUrl = createLogoUrl(((Element) imageNode).getElementsByTagName(RSS.URL).item(0).getTextContent());
			}
			else this.logoUrl = createLogoUrl(imageNode.getAttributes().getNamedItem(RSS.HREF).getTextContent());
		}
		// image in thumbnail tag
		else {
			NodeList thumbnailNodes = this.podcastRssFile.getElementsByTagName(RSS.THUMBNAIL);
			
			if (thumbnailNodes.getLength() > 0)
				this.logoUrl = createLogoUrl(thumbnailNodes.item(0).getAttributes().getNamedItem(RSS.URL).getTextContent());
		}
	}
	
	private void loadEpisodes() {
		NodeList episodeNodes = this.podcastRssFile.getElementsByTagName(RSS.ITEM);
		
		for (int episodeIndex = 0; episodeIndex < episodeNodes.getLength(); episodeIndex++) {
			Episode newEpisode = new Episode(this, episodeNodes.item(episodeIndex).getChildNodes());
			
			// Only add if there is some actual content to play
			if (newEpisode.getMediaUrl() != null) this.episodes.add(newEpisode);
		}
	}
	
	private URL createLogoUrl(String nodeValue) {
		try {
			return new URL(nodeValue);
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return null;
	}
}
