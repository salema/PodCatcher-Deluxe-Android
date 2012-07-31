package net.alliknow.podcatcher.types.test;

import static org.junit.Assert.*;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Date;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import net.alliknow.podcatcher.types.Episode;
import net.alliknow.podcatcher.types.Podcast;

import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class EpisodeTest {

	@Test
	public final void testGetName() {
		for (ExamplePodcast ep : ExamplePodcast.values()) {
			Podcast podcast = new Podcast(ep.name(), ep.getURL());
			podcast.setRssFile(loadRssFile(podcast));
			for (Episode episode : podcast.getEpisodes()) {
				assertNotNull(episode.getName());
				assertTrue(episode.getName().length() > 0);
				assertFalse(episode.getName().contains("\n"));
				assertFalse(episode.getName().contains("\r"));
				assertFalse(episode.getName().contains("\r\n"));
			}
		}	
	}

	@Test
	public final void testGetMediaUrl() {
		for (ExamplePodcast ep : ExamplePodcast.values()) {
			Podcast podcast = new Podcast(ep.name(), ep.getURL());
			podcast.setRssFile(loadRssFile(podcast));
			for (Episode episode : podcast.getEpisodes()) {
				assertNotNull(episode.getMediaUrl());
			}
		}
	}
	
	@Test
	public final void testGetPodcast() {
		for (ExamplePodcast ep : ExamplePodcast.values()) {
			Podcast podcast = new Podcast(ep.name(), ep.getURL());
			podcast.setRssFile(loadRssFile(podcast));
			for (Episode episode : podcast.getEpisodes()) {
				assertEquals(episode.getPodcast(), podcast);
			}
		}
	}
	
	@Test
	public final void testGetPubDate() {
		for (ExamplePodcast ep : ExamplePodcast.values()) {
			Podcast podcast = new Podcast(ep.name(), ep.getURL());
			podcast.setRssFile(loadRssFile(podcast));
			for (Episode episode : podcast.getEpisodes()) {
				assertNotNull(episode.getPubDate());
				assertTrue(episode.getPubDate().after(new Date(0)));
				assertTrue(episode.getPubDate().before(new Date()));
			}
		}
	}

	private Document loadRssFile(Podcast podcast) {
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			dbf.setNamespaceAware(true);
			
			return dbf.newDocumentBuilder().parse(podcast.getUrl().openStream());
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
		
		return null;
	}
}
