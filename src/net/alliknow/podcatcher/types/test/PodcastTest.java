package net.alliknow.podcatcher.types.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import net.alliknow.podcatcher.types.Podcast;

import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class PodcastTest {

	@Test
	public final void testGetName() {
		String name = null;
		Podcast podcast = new Podcast(name, null);
		assertEquals(name, podcast.getName());
		
		name = "";
		podcast = new Podcast(name, null);
		assertEquals(name, podcast.getName());
		
		name = "Test";
		podcast = new Podcast(name, null);
		assertEquals(name, podcast.getName());
	}

	@Test
	public final void testToString() {
		String name = null;
		Podcast podcast = new Podcast(name, null);
		assertNotNull(podcast.toString());
		
		name = null;
		podcast = new Podcast(name, null);
		assertEquals("Unnamed podcast", podcast.toString());
		
		name = "My Podcast";
		podcast = new Podcast(name, null);
		assertEquals(name, podcast.toString());
	}
	
	@Test
	public final void testGetEpisodes() {
		Podcast podcast = new Podcast(null, null);
		assertNotNull(podcast.getEpisodes());
		assertEquals(0, podcast.getEpisodes().size());
		
		for (ExamplePodcast ep : ExamplePodcast.values()) {
			podcast = new Podcast(ep.name(), ep.getURL());
			podcast.setRssFile(loadRssFile(podcast));
			assertNotNull(podcast.getEpisodes());
			assertTrue(podcast.getEpisodes().size() > 0);
		}
	}
	
	@Test
	public final void testGetLogoUrl() {
		for (ExamplePodcast ep : ExamplePodcast.values()) {
			Podcast podcast = new Podcast(ep.name(), ep.getURL());
			podcast.setRssFile(loadRssFile(podcast));
			if (podcast.getLogoUrl() == null) System.out.println(podcast.getName());
			assertNotNull(podcast.getLogoUrl());
		}
	}
	
	@Test
	public final void testGetAge() {
		for (ExamplePodcast ep : ExamplePodcast.values()) {
			Podcast podcast = new Podcast(ep.name(), ep.getURL());
			assertTrue(podcast.getAge() == 0);
		}
		
		for (ExamplePodcast ep : ExamplePodcast.values()) {
			Podcast podcast = new Podcast(ep.name(), ep.getURL());
			podcast.setRssFile(loadRssFile(podcast));
			System.out.println(podcast.getName() + ": " + podcast.getAge());
			assertTrue(podcast.getAge() > 0);
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
