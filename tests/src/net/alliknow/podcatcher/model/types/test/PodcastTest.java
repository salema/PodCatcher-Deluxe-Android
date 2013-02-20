
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
public class PodcastTest extends TestCase {

    private List<Podcast> examplePodcasts;

    @Override
    protected void setUp() throws Exception {
        System.out.println("Set up test \"Podcasts\" by loading example podcasts...");

        final Date start = new Date();
        examplePodcasts = Utils.getExamplePodcasts();

        System.out.println("Waited " + (new Date().getTime() - start.getTime())
                + "ms for example podcasts...");
    }

    public final void testEquals() {
        for (Podcast podcast : examplePodcasts) {
            assertFalse(podcast.equals(null));
            assertTrue(podcast.equals(podcast));
            assertFalse(podcast.equals(new Object()));
            assertFalse(podcast.equals(new Episode(null)));
            assertFalse(podcast.equals(new Podcast(null, null)));

            for (Podcast other : examplePodcasts) {
                if (podcast.getUrl().equals(other.getUrl()))
                    assertTrue(podcast.equals(other));
                else
                    assertFalse(podcast.equals(other));
            }
        }
    }

    public final void testHashCode() {
        for (Podcast podcast : examplePodcasts) {
            assertTrue(podcast.hashCode() != 0);

            for (Podcast other : examplePodcasts) {
                if (podcast.equals(other))
                    assertTrue(podcast.hashCode() == other.hashCode());
                else
                    assertTrue(podcast.hashCode() != other.hashCode());
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

        for (Podcast ep : examplePodcasts) {
            podcast = new Podcast(null, ep.getUrl());
            podcast.parse(Utils.getParser(podcast));
            assertNotNull(podcast.getName());
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

    public final void testGetEpisodeNumber() throws XmlPullParserException, IOException {
        Podcast podcast = new Podcast(null, null);
        assertEquals(0, podcast.getEpisodeNumber());

        for (Podcast podcast1 : examplePodcasts) {
            podcast1.parse(Utils.getParser(podcast1));
            assertTrue(podcast1.getEpisodeNumber() > 0);
        }
    }

    public final void testGetEpisodes() throws XmlPullParserException, IOException {
        Podcast podcast = new Podcast(null, null);
        assertNotNull(podcast.getEpisodes());

        for (Podcast podcast1 : examplePodcasts) {
            podcast1.parse(Utils.getParser(podcast1));
            assertNotNull(podcast1.getEpisodes());
        }
    }

    public final void testGetLogoUrl() throws XmlPullParserException, IOException {
        for (Podcast podcast : examplePodcasts) {
            podcast.parse(Utils.getParser(podcast));
            assertNotNull(podcast.getLogoUrl());
        }
    }

    public final void testLastLoaded() throws XmlPullParserException, IOException {
        for (Podcast podcast : examplePodcasts) {
            assertNull(podcast.getLastLoaded());
        }

        for (Podcast podcast : examplePodcasts) {
            podcast.parse(Utils.getParser(podcast));
            assertNotNull(podcast.getLastLoaded());
        }
    }
}
