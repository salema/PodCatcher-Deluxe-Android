
package net.alliknow.podcatcher.model.types.test;

import junit.framework.TestCase;

import net.alliknow.podcatcher.model.test.Utils;
import net.alliknow.podcatcher.model.types.Episode;
import net.alliknow.podcatcher.model.types.Podcast;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.Date;

public class EpisodeTest extends TestCase {

    public final void testEquals() throws XmlPullParserException, IOException {
        for (ExamplePodcast ep : ExamplePodcast.values()) {
            Podcast podcast = new Podcast(ep.name(), ep.getURL());
            podcast.parse(Utils.getParser(podcast));

            Episode first = null;
            for (Episode episode : podcast.getEpisodes()) {
                assertFalse(episode.equals(null));
                assertTrue(episode.equals(episode));
                assertFalse(episode.equals(new Object()));
                assertFalse(episode.equals(new Podcast(null, null)));
                assertFalse(episode.equals(ep));
                assertFalse(episode.equals(podcast));
                assertFalse(episode.equals(new Episode(null)));

                if (podcast.getEpisodes().indexOf(episode) == 0)
                    first = episode;
                else if (first != null)
                    assertFalse(first.equals(episode));
            }
        }
    }

    public final void testHashCode() throws XmlPullParserException, IOException {
        for (ExamplePodcast ep : ExamplePodcast.values()) {
            Podcast podcast = new Podcast(ep.name(), ep.getURL());
            podcast.parse(Utils.getParser(podcast));

            Episode first = null;
            for (Episode episode : podcast.getEpisodes()) {
                assertTrue(episode.hashCode() != 0);

                if (podcast.getEpisodes().indexOf(episode) == 0)
                    first = episode;
                else if (first != null)
                    assertFalse(first.hashCode() == episode.hashCode());
            }
        }
    }

    public final void testGetName() throws XmlPullParserException, IOException {
        for (ExamplePodcast ep : ExamplePodcast.values()) {
            Podcast podcast = new Podcast(ep.name(), ep.getURL());
            podcast.parse(Utils.getParser(podcast));
            for (Episode episode : podcast.getEpisodes()) {
                assertNotNull(episode.getName());
                assertTrue(episode.getName().length() > 0);
                assertFalse(episode.getName().contains("\n"));
                assertFalse(episode.getName().contains("\r"));
                assertFalse(episode.getName().contains("\r\n"));
            }
        }
    }

    public final void testGetMediaUrl() throws XmlPullParserException, IOException {
        for (ExamplePodcast ep : ExamplePodcast.values()) {
            Podcast podcast = new Podcast(ep.name(), ep.getURL());
            podcast.parse(Utils.getParser(podcast));
            for (Episode episode : podcast.getEpisodes()) {
                assertNotNull(episode.getMediaUrl());
            }
        }
    }

    public final void testGetPodcastName() throws XmlPullParserException, IOException {
        for (ExamplePodcast ep : ExamplePodcast.values()) {
            Podcast podcast = new Podcast(ep.name(), ep.getURL());
            podcast.parse(Utils.getParser(podcast));
            for (Episode episode : podcast.getEpisodes()) {
                assertEquals(episode.getPodcastName(), podcast.getName());
            }
        }
    }

    public final void testGetPubDate() throws XmlPullParserException, IOException {
        for (ExamplePodcast ep : ExamplePodcast.values()) {
            Podcast podcast = new Podcast(ep.name(), ep.getURL());
            podcast.parse(Utils.getParser(podcast));
            for (Episode episode : podcast.getEpisodes()) {
                assertNotNull(episode.getPubDate());
                assertTrue(episode.getPubDate().after(new Date(0)));
                assertTrue(episode.getPubDate().before(new Date()));
            }
        }
    }
}
