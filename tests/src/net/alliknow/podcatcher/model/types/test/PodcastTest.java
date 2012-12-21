package net.alliknow.podcatcher.model.types.test;

import java.io.IOException;

import junit.framework.TestCase;
import net.alliknow.podcatcher.model.test.Utils;
import net.alliknow.podcatcher.model.types.Episode;
import net.alliknow.podcatcher.model.types.Podcast;

import org.xmlpull.v1.XmlPullParserException;

public class PodcastTest extends TestCase {

	public final void testEquals() {
		for (ExamplePodcast ep : ExamplePodcast.values()) {
			Podcast podcast = new Podcast(ep.name(), ep.getURL());
			
			assertFalse(podcast.equals(null));
			assertTrue(podcast.equals(podcast));
			assertFalse(podcast.equals(new Object()));
			assertFalse(podcast.equals(new Episode(null)));
			assertFalse(podcast.equals(ep));
			assertFalse(podcast.equals(new Podcast(null, null)));
				
			for (ExamplePodcast ep2 : ExamplePodcast.values()) {
				Podcast other = new Podcast(ep2.name(), ep2.getURL());
				
				if (ep.equals(ep2)) assertTrue(podcast.equals(other));
				else assertFalse(podcast.equals(other));
			}
		}	
	}
	
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
	
	public final void testGetName() throws XmlPullParserException, IOException {
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
			podcast.parse(Utils.getParser(podcast));
			assertNotNull(podcast.getName());
		}
	}
	
	public final void testGetEncoding() throws XmlPullParserException, IOException {
		for (ExamplePodcast ep : ExamplePodcast.values()) {
			Podcast podcast = new Podcast(ep.name(), ep.getURL());
			System.out.println(podcast);
			podcast.parse(Utils.getParser(podcast));
			assertNotNull(podcast.getEncoding());
		}
	}

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
	
	public final void testGetEpisodes() throws XmlPullParserException, IOException {
		Podcast podcast = new Podcast(null, null);
		assertNotNull(podcast.getEpisodes());
		assertEquals(0, podcast.getEpisodes().size());
		
		for (ExamplePodcast ep : ExamplePodcast.values()) {
			podcast = new Podcast(ep.name(), ep.getURL());
			podcast.parse(Utils.getParser(podcast));
			assertNotNull(podcast.getEpisodes());
			assertTrue(podcast.getEpisodes().size() > 0);
		}
	}
	
	public final void testGetLogoUrl() throws XmlPullParserException, IOException {
		for (ExamplePodcast ep : ExamplePodcast.values()) {
			Podcast podcast = new Podcast(ep.name(), ep.getURL());
			podcast.parse(Utils.getParser(podcast));
			assertNotNull(podcast.getLogoUrl());
		}
	}
	
	public final void testNeedsReload() throws XmlPullParserException, IOException {
		for (ExamplePodcast ep : ExamplePodcast.values()) {
			Podcast podcast = new Podcast(ep.name(), ep.getURL());
			assertTrue(podcast.needsReload());
		}
		
		for (ExamplePodcast ep : ExamplePodcast.values()) {
			Podcast podcast = new Podcast(ep.name(), ep.getURL());
			podcast.parse(Utils.getParser(podcast));
			assertTrue(! podcast.needsReload());
		}
	}
}
