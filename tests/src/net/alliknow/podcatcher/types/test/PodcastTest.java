package net.alliknow.podcatcher.types.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import net.alliknow.podcatcher.types.Episode;
import net.alliknow.podcatcher.types.Podcast;

import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class PodcastTest {

	@Test
	public final void testEquals() {
		for (ExamplePodcast ep : ExamplePodcast.values()) {
			Podcast podcast = new Podcast(ep.name(), ep.getURL());
			
			assertFalse(podcast.equals(null));
			assertTrue(podcast.equals(podcast));
			assertFalse(podcast.equals(new Object()));
			assertFalse(podcast.equals(new Episode(null, null)));
			assertFalse(podcast.equals(ep));
			assertFalse(podcast.equals(new Podcast(null, null)));
				
			for (ExamplePodcast ep2 : ExamplePodcast.values()) {
				Podcast other = new Podcast(ep2.name(), ep2.getURL());
				
				if (ep.equals(ep2)) assertTrue(podcast.equals(other));
				else assertFalse(podcast.equals(other));
			}
		}	
	}
	
	@Test
	public final void testHashCode() {
		for (ExamplePodcast ep : ExamplePodcast.values()) {
			Podcast podcast = new Podcast(ep.name(), ep.getURL());
			assertTrue(podcast.hashCode() != 0);
			
			for (ExamplePodcast ep2 : ExamplePodcast.values()) {
				Podcast other = new Podcast(ep2.name(), ep2.getURL());
				
				if (podcast.equals(other)) assertTrue(podcast.hashCode() == other.hashCode());
				else assertTrue(podcast.hashCode() != other.hashCode());
			}			
		}	
	}
	
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
		
		for (ExamplePodcast ep : ExamplePodcast.values()) {
			podcast = new Podcast(null, ep.getURL());
			podcast.setRssFile(loadRssFile(podcast));
			assertNotNull(podcast.getName());
		}
	}
	
	@Test
	public final void testGetEncoding() {
		for (ExamplePodcast ep : ExamplePodcast.values()) {
			Podcast podcast = new Podcast(ep.name(), ep.getURL());
			podcast.setRssFile(loadRssFile(podcast));
			assertNotNull(podcast.getEncoding());
		}
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
	public final void testNeedsReload() {
		for (ExamplePodcast ep : ExamplePodcast.values()) {
			Podcast podcast = new Podcast(ep.name(), ep.getURL());
			assertTrue(podcast.needsReload());
		}
		
		for (ExamplePodcast ep : ExamplePodcast.values()) {
			Podcast podcast = new Podcast(ep.name(), ep.getURL());
			podcast.setRssFile(loadRssFile(podcast));
			assertTrue(! podcast.needsReload());
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
