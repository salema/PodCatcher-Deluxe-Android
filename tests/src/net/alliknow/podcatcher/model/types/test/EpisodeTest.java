
package net.alliknow.podcatcher.model.types.test;

import junit.framework.TestCase;

import net.alliknow.podcatcher.model.test.Utils;
import net.alliknow.podcatcher.model.types.Episode;
import net.alliknow.podcatcher.model.types.Podcast;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.Date;
import java.util.List;

@SuppressWarnings("javadoc")
public class EpisodeTest extends TestCase {

    private static List<Podcast> examplePodcasts;

    @Override
    protected void setUp() throws Exception {
        if (examplePodcasts == null) {
            System.out.println("Set up test \"Podcasts\" by loading example podcasts...");

            final Date start = new Date();
            examplePodcasts = Utils.getExamplePodcasts();

            System.out.println("Waited " + (new Date().getTime() - start.getTime())
                    + "ms for example podcasts...");

            int size = examplePodcasts.size();
            int index = 0;

            for (Podcast ep : examplePodcasts) {
                System.out.println("---- Parsing podcast " + ++index + "/" + size + " ----");
                ep.parse(Utils.getParser(ep));
            }
        }
    }

    public final void testEquals() throws XmlPullParserException, IOException {
        for (Podcast podcast : examplePodcasts) {
            Episode first = null;
            for (Episode episode : podcast.getEpisodes()) {
                assertFalse(episode.equals(null));
                assertTrue(episode.equals(episode));
                assertFalse(episode.equals(new Object()));
                assertFalse(episode.equals(new Podcast(null, null)));
                assertFalse(episode.equals(podcast));

                if (podcast.getEpisodes().indexOf(episode) == 0)
                    first = episode;
                else if (first != null)
                    assertFalse(first.equals(episode));
            }
        }
    }

    public final void testHashCode() throws XmlPullParserException, IOException {
        for (Podcast podcast : examplePodcasts) {

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
        for (Podcast podcast : examplePodcasts) {
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
        for (Podcast podcast : examplePodcasts) {
            for (Episode episode : podcast.getEpisodes()) {
                assertNotNull(episode.getMediaUrl());
            }
        }
    }

    public final void testGetPodcastName() throws XmlPullParserException, IOException {
        for (Podcast podcast : examplePodcasts) {
            for (Episode episode : podcast.getEpisodes()) {
                assertEquals(episode.getPodcast().getName(), podcast.getName());
            }
        }
    }

    public final void testGetPubDate() throws XmlPullParserException, IOException {
        for (Podcast podcast : examplePodcasts) {
            for (Episode episode : podcast.getEpisodes()) {
                assertNotNull(episode.getPubDate());
                assertTrue(episode.getPubDate().after(new Date(0)));
                assertTrue(episode.getPubDate().before(new Date()));
            }
        }
    }
}
