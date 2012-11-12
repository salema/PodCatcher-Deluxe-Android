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

import net.alliknow.podcatcher.tags.OPML;
import net.alliknow.podcatcher.tags.RSS;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import android.text.Html;
import android.text.TextUtils;
import android.util.Log;

/**
 * The podcast type.
 * 
 * @author Kevin Hausmann
 */
public class Podcast implements Comparable<Podcast> {

	/** The minimum time podcast content is buffered (in milliseconds). 
	 * If older, we need to reload. */
	public static final int TIME_TO_LIFE = 15 * 60 * 1000;
	
	/** Name of the podcast */
	private String name;
	/** Location of the podcast's RSS file */
	private URL url;
	/** Podcast's description */
	private String description;
	/** Broadcast language */
	private Language language;
	/** Podcast genre */
	private Genre genre;
	/** Podcast media type */
	private MediaType mediaType;
	/** XML document representing the RSS file */
	private Document podcastRssFile;
	/** The podcasts list of episodes */
	private List<Episode> episodes = new ArrayList<Episode>();
	/** The podcast's image (logo) location */
	private URL logoUrl;
	/** The point in time when the RSS file as last been set */
	private Date updated;
	/** The encoding of the loaded file */
	private String encoding;
	
	/**
	 * Create a new podcast by name and RSS file location.
	 * The name will not be read from the file, but remains as given
	 * (unless you give <code>null</code> as the name). 
	 * All other data on the podcast will only be available after
	 * <code>setRssFile</code> was called.
	 * @param name The podcast's name, if you give <code>null</code> the name
	 * will be read from the RSS file (if available)
	 * @param url The location of the podcast's RSS file
	 * @see setRssFile
	 */
	public Podcast(String name, URL url) {
		this.name = name;
		this.url = url;
	}
	
	/** 
	 * Create a new podcast from an OPML outline node.
	 * The constructor will try to work around a couple of
	 * corner-cases, but there are limits to this...
	 * @param opmlOutline The outline node
	 */
	public Podcast(Node opmlOutline) {
		try {
			this.name = opmlOutline.getAttributes().getNamedItem(OPML.TEXT).getNodeValue();
			
			if (name.equals("null")) this.name = null;
			else this.name = Html.fromHtml(name).toString();
			
			this.url = new URL(opmlOutline.getAttributes().getNamedItem(OPML.XMLURL).getNodeValue());
		} catch (MalformedURLException e) {
			Log.d(getClass().getSimpleName(), "Created podcast with bad URL: " + name);
		} catch (RuntimeException e) {
			// pass
		}
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
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * @param description the description to set
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * @return the language
	 */
	public Language getLanguage() {
		return language;
	}

	/**
	 * @param language the language to set
	 */
	public void setLanguage(Language language) {
		this.language = language;
	}

	/**
	 * @return the genre
	 */
	public Genre getGenre() {
		return genre;
	}

	/**
	 * @param genre the genre to set
	 */
	public void setGenre(Genre genre) {
		this.genre = genre;
	}

	/**
	 * @return the mediaType
	 */
	public MediaType getMediaType() {
		return mediaType;
	}

	/**
	 * @param mediaType the mediaType to set
	 */
	public void setMediaType(MediaType mediaType) {
		this.mediaType = mediaType;
	}

	/**
	 * Find and return all episodes for this podcast. Will never return null
	 * but an empty list when encountering problems. Set the RSS file before
	 * expecting any results. 
	 * @return The list of episodes as listed in the feed.
	 * @see setRssFile
	 */
	public List<Episode> getEpisodes() {
		return episodes;
	}
	
	/**
	 * Find and return the podcast's image location (logo).
	 * Only works after RSS file is set.
	 * @return URL pointing at the logo location
	 * @see setRssFile
	 */
	public URL getLogoUrl() {
		return logoUrl;
	}
	
	/**
	 * The podcast's encoding.
	 * @return Get the input encoding for the podcast file loaded. 
	 * This may be <code>null</code>, if unknown.
	 */
	public String getEncoding() {
		return encoding;
	}
	
	/**
	 * Set the RSS file representing this podcast. This is were the object
	 * gets its information from. Many of its methods will not return valid results
	 * unless this method was called. Calling this method also resets all
	 * information read earlier.
	 * @param rssFile XML document representing the podcast
	 */
	public void setRssFile(Document rssFile) {
		episodes.clear();
		
		podcastRssFile = rssFile;
		updated = new Date();
		encoding = rssFile.getInputEncoding();
		
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
		if (updated == null) return true;
		// Check age
		else return new Date().getTime() - updated.getTime() > TIME_TO_LIFE;
	}
	
	/**
	 * Create an OPML outline from this podcast.
	 * @return The OPML XML outline as a string.
	 * If the podcast does not at least have a valid name
	 * and a non-<code>null</code> URL, this will return
	 * <code>null</code> and you should skip this podcast.
	 */
	public String toOpmlString() {
		if (! hasNameAndUrl()) return null;
		else return "<" + OPML.OUTLINE  + " " + OPML.TEXT + "=\"" + TextUtils.htmlEncode(name) + "\" " +
				OPML.TYPE + "=\"" + OPML.RSS_TYPE + "\" " +
				OPML.XMLURL + "=\"" + url + "\"/>";
	}

	/**
	 * @return Whether this podcast has an non-empty name
	 * and an URL.
	 */
	public boolean hasNameAndUrl() {
		return name != null && name.length() > 0 && url != null;
	}

	@Override
	public String toString() {
		if (name == null) return "Unnamed podcast";
		if (url == null) return name;
		else return name + " at " + url.toString();
	}
	
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		else if (!(o instanceof Podcast)) return false;
		
		Podcast other = (Podcast)o;
		
		if (url == null || other.getUrl() == null) return false; 
		else return url.toExternalForm().equals(((Podcast) o).getUrl().toExternalForm());
	}
	
	@Override
	public int hashCode() {
		return url == null ? 0 : url.toExternalForm().hashCode();
	}

	@Override
	public int compareTo(Podcast another) {
		if (name == null || another == null || another.getName() == null) return 0;
		else return getName().compareToIgnoreCase(another.getName());
	}
	
	private void loadName() {
		NodeList titleNodes = podcastRssFile.getElementsByTagName(RSS.TITLE);
		
		if (titleNodes.getLength() > 0) name = titleNodes.item(0).getTextContent();
	}
	
	private void loadMetadata() {
		NodeList imageNodes = podcastRssFile.getElementsByTagNameNS("*", RSS.IMAGE);
		
		// image tag used?
		if (imageNodes.getLength() > 0) {
			Node imageNode = imageNodes.item(0);
			
			if (imageNode.getChildNodes().getLength() > 0) {
				logoUrl = createLogoUrl(((Element) imageNode).getElementsByTagName(RSS.URL).item(0).getTextContent());
			}
			else logoUrl = createLogoUrl(imageNode.getAttributes().getNamedItem(RSS.HREF).getTextContent());
		}
		// image in thumbnail tag
		else {
			NodeList thumbnailNodes = podcastRssFile.getElementsByTagName(RSS.THUMBNAIL);
			
			if (thumbnailNodes.getLength() > 0)
				logoUrl = createLogoUrl(thumbnailNodes.item(0).getAttributes().getNamedItem(RSS.URL).getTextContent());
		}
	}
	
	private void loadEpisodes() {
		NodeList episodeNodes = podcastRssFile.getElementsByTagName(RSS.ITEM);
		
		for (int episodeIndex = 0; episodeIndex < episodeNodes.getLength(); episodeIndex++) {
			Episode newEpisode = new Episode(this, episodeNodes.item(episodeIndex).getChildNodes());
			
			// Only add if there is some actual content to play
			if (newEpisode.getMediaUrl() != null) episodes.add(newEpisode);
		}
	}
	
	private URL createLogoUrl(String nodeValue) {
		try {
			return new URL(nodeValue);
		} catch (MalformedURLException e) {
			Log.e(getClass().getSimpleName(), "Podcast has invalid logo URL", e);
		}
		
		return null;
	}
}
