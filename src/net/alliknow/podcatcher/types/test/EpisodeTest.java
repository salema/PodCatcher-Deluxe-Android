package net.alliknow.podcatcher.types.test;

import static org.junit.Assert.assertTrue;

import java.io.IOException;

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
			Episode episode = podcast.getEpisodes().get(0);
			assertTrue(episode.getName() != null);
			assertTrue(episode.getName().length() > 0);
		}	
	}

	@Test
	public final void testGetMediaUrl() {
		for (ExamplePodcast ep : ExamplePodcast.values()) {
			Podcast podcast = new Podcast(ep.name(), ep.getURL());
			podcast.setRssFile(loadRssFile(podcast));
			Episode episode = podcast.getEpisodes().get(0);
			assertTrue(episode.getMediaUrl() != null);
		}
	}

	private Document loadRssFile(Podcast podcast) {
		try {
			return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(podcast.getUrl().openStream());
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
