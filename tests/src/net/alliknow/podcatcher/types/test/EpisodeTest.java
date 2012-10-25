package net.alliknow.podcatcher.types.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Date;

import net.alliknow.podcatcher.test.Utils;
import net.alliknow.podcatcher.types.Episode;
import net.alliknow.podcatcher.types.Podcast;

import org.junit.Test;

public class EpisodeTest {

	@Test
	public final void testEquals() {
		for (ExamplePodcast ep : ExamplePodcast.values()) {
			Podcast podcast = new Podcast(ep.name(), ep.getURL());
			podcast.setRssFile(Utils.loadRssFile(podcast));
			
			Episode first = null;
			for (Episode episode : podcast.getEpisodes()) {
				assertFalse(episode.equals(null));
				assertTrue(episode.equals(episode));
				assertFalse(episode.equals(new Object()));
				assertFalse(episode.equals(new Podcast(null, null)));
				assertFalse(episode.equals(ep));
				assertFalse(episode.equals(podcast));
				assertFalse(episode.equals(new Episode(null, null)));
				
				if (podcast.getEpisodes().indexOf(episode) == 0) first = episode;
				else if (first != null) assertFalse(first.equals(episode));
			}
		}	
	}
	
	@Test
	public final void testHashCode() {
		for (ExamplePodcast ep : ExamplePodcast.values()) {
			Podcast podcast = new Podcast(ep.name(), ep.getURL());
			podcast.setRssFile(Utils.loadRssFile(podcast));
			
			Episode first = null;
			for (Episode episode : podcast.getEpisodes()) {
				assertTrue(episode.hashCode() != 0);
				
				if (podcast.getEpisodes().indexOf(episode) == 0) first = episode;
				else if (first != null) assertFalse(first.hashCode() == episode.hashCode());
			}
		}	
	}
	
	@Test
	public final void testGetName() {
		for (ExamplePodcast ep : ExamplePodcast.values()) {
			Podcast podcast = new Podcast(ep.name(), ep.getURL());
			podcast.setRssFile(Utils.loadRssFile(podcast));
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
			podcast.setRssFile(Utils.loadRssFile(podcast));
			for (Episode episode : podcast.getEpisodes()) {
				assertNotNull(episode.getMediaUrl());
			}
		}
	}
	
	@Test
	public final void testGetPodcast() {
		for (ExamplePodcast ep : ExamplePodcast.values()) {
			Podcast podcast = new Podcast(ep.name(), ep.getURL());
			podcast.setRssFile(Utils.loadRssFile(podcast));
			for (Episode episode : podcast.getEpisodes()) {
				assertEquals(episode.getPodcast(), podcast);
			}
		}
	}
	
	@Test
	public final void testGetPubDate() {
		for (ExamplePodcast ep : ExamplePodcast.values()) {
			Podcast podcast = new Podcast(ep.name(), ep.getURL());
			podcast.setRssFile(Utils.loadRssFile(podcast));
			for (Episode episode : podcast.getEpisodes()) {
				assertNotNull(episode.getPubDate());
				assertTrue(episode.getPubDate().after(new Date(0)));
				assertTrue(episode.getPubDate().before(new Date()));
			}
		}
	}
}
