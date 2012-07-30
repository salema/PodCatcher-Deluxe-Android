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
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import net.alliknow.podcatcher.tags.RSS;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * The podcast type.
 * 
 * @author Kevin Hausmann
 */
public class Podcast {

	private String name;
	private URL url;
	
	public Podcast(String name, URL url) {
		this.name = name;
		this.url = url;
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
	 * but an empty list when encountering problems.
	 * @return The list of episodes as listed in the feed.
	 */
	public List<Episode> getEpisodes() {
		List<Episode> episodes = new ArrayList<Episode>();
		if (this.url == null) return episodes;
		
		try {
			Document podcastFile = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(this.url.openStream());
			NodeList episodeNodes = podcastFile.getElementsByTagName(RSS.ITEM);
			
			for (int index = 0; index < episodeNodes.getLength(); index++) {
				NodeList itemNodes = episodeNodes.item(index).getChildNodes();
				
				for (int index2 = 0; index2 < itemNodes.getLength(); index2++) {
					if (itemNodes.item(index2).getNodeName() == RSS.TITLE) 
						episodes.add(new Episode(itemNodes.item(index2).getNodeValue()));
				}
			}
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return episodes;
	}
		
	@Override
	public String toString() {
		if (name == null) return "Unnamed podcast";
		if (url == null) return name;
		else return name + " at " + url.toString();
	}
}
