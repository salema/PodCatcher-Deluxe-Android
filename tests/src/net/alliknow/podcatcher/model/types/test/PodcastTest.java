
package net.alliknow.podcatcher.model.types.test;

import android.util.Base64;

import net.alliknow.podcatcher.model.types.Podcast;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

@SuppressWarnings("javadoc")
public class PodcastTest extends TypeTest {

    @Override
    protected void setUp() throws Exception {
        sampleSize = 25;

        super.setUp();
    }

    public final void testEquals() {
        for (Podcast podcast : examplePodcasts) {
            assertFalse(podcast.equals(null));
            assertTrue(podcast.equals(podcast));
            assertFalse(podcast.equals(new Object()));
            assertFalse(podcast.equals(new Podcast(null, null)));
            assertTrue(podcast.equals(new Podcast(null, podcast.getUrl())));

            for (Podcast other : examplePodcasts) {
                if (podcast.getUrl().equals(other.getUrl()))
                    assertTrue(podcast.equals(other));
                else
                    assertFalse(podcast.equals(other));
            }
        }
    }

    public final void testHashCode() {
        assertTrue(new Podcast(null, null).hashCode() != 0);

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

    public final void testCompareTo() {
        assertTrue(new Podcast(null, null).compareTo(new Podcast(null, null)) == 0);
        assertEquals("Bla".compareTo("ABZ"),
                new Podcast("Bla", null).compareTo(new Podcast("ABZ", null)));
        assertEquals("ABC".compareTo("ABZ"),
                new Podcast("ABC", null).compareTo(new Podcast("ABZ", null)));
        assertEquals("ABC".compareTo("ABC"),
                new Podcast("ABC", null).compareTo(new Podcast("ABC", null)));

        for (Podcast podcast : examplePodcasts) {
            for (Podcast other : examplePodcasts) {
                if (podcast.equals(other))
                    assertEquals(0, podcast.compareTo(other));
                else
                    assertTrue(podcast.compareTo(other) != 0);
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
            assertNotNull(ep.getName());
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

    public final void testGetEpisodeNumber() {
        assertEquals(0, new Podcast(null, null).getEpisodeNumber());

        for (Podcast podcast : examplePodcasts)
            assertTrue(podcast.getEpisodeNumber() > 0);
    }

    public final void testGetEpisodes() {
        assertNotNull(new Podcast(null, null).getEpisodes());

        for (Podcast podcast : examplePodcasts)
            assertNotNull(podcast.getEpisodes());
    }

    public final void testGetLogoUrl() {
        assertNull(new Podcast(null, null).getLogoUrl());

        for (Podcast podcast : examplePodcasts)
            assertNotNull("Podcast " + podcast.getName() + " has no logo!", podcast.getLogoUrl());
    }

    public final void testLastLoaded() {
        assertNull(new Podcast(null, null).getLastLoaded());

        for (Podcast podcast : examplePodcasts)
            assertNotNull(podcast.getLastLoaded());
    }

    public final void testIsExplicit() {
        assertFalse(new Podcast(null, null).isExplicit());
    }

    public final void testgetAuth() {
        Podcast podcast = new Podcast(null, null);
        assertNull(podcast.getAuthorization());

        podcast.setUsername("kevin");
        podcast.setPassword("monkey");
        assertNotNull(podcast.getAuthorization());
        assertEquals(podcast.getAuthorization(),
                "Basic " + Base64.encodeToString("kevin:monkey".getBytes(), Base64.NO_WRAP));
    }
}
