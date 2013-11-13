/** Copyright 2012, 2013 Kevin Hausmann
 *
 * This file is part of PodCatcher Deluxe.
 *
 * PodCatcher Deluxe is free software: you can redistribute it 
 * and/or modify it under the terms of the GNU General Public License as 
 * published by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * PodCatcher Deluxe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PodCatcher Deluxe. If not, see <http://www.gnu.org/licenses/>.
 */

package net.alliknow.podcatcher.model.tasks.remote.test;

import android.test.InstrumentationTestCase;

import net.alliknow.podcatcher.listeners.OnLoadPodcastListener;
import net.alliknow.podcatcher.model.tasks.remote.LoadPodcastTask;
import net.alliknow.podcatcher.model.tasks.remote.LoadPodcastTask.PodcastLoadError;
import net.alliknow.podcatcher.model.test.Utils;
import net.alliknow.podcatcher.model.types.Podcast;
import net.alliknow.podcatcher.model.types.Progress;

import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;

@SuppressWarnings("javadoc")
public class LoadPodcastTaskTest extends InstrumentationTestCase {

    private CountDownLatch signal = null;

    private List<Podcast> examplePodcasts;

    private class MockPodcastLoader implements OnLoadPodcastListener {

        protected Podcast result;
        protected boolean failed;

        @Override
        public void onPodcastLoadProgress(Podcast podcast, Progress progress) {
            // System.out.println(progress);
        }

        @Override
        public void onPodcastLoaded(Podcast podcast) {
            this.result = podcast;
            this.failed = false;

            signal.countDown();
        }

        @Override
        public void onPodcastLoadFailed(Podcast podcast, PodcastLoadError code) {
            this.result = podcast;
            this.failed = true;

            signal.countDown();
        }
    }

    @Override
    protected void setUp() throws Exception {
        System.out.println("Set up test \"LoadPodcast\" by loading example podcasts...");

        final Date start = new Date();
        examplePodcasts = Utils.getExamplePodcasts();

        System.out.println("Waited " + (new Date().getTime() - start.getTime())
                + "ms for example podcasts...");
    }

    public final void testLoadPodcast() throws Throwable {
        final MockPodcastLoader mockLoader = new MockPodcastLoader();

        int size = examplePodcasts.size();
        int index = 0;
        int failed = 0;

        // Actual example Podcast
        for (Podcast ep : examplePodcasts) {
            System.out.println("---- New Podcast (" + ++index + "/" + size + ") ----");
            System.out.println("Testing \"" + ep + "\"...");
            LoadPodcastTask task = loadAndWait(mockLoader, new Podcast(ep.getName(), ep.getUrl()));

            if (mockLoader.failed) {
                System.out.println("Podcast " + ep.getName() + " failed!");
                failed++;
            } else {
                assertFalse(task.isCancelled());
                assertNotNull(mockLoader.result);
                assertFalse(mockLoader.result.getEpisodes().isEmpty());
                assertNotNull(mockLoader.result.getLastLoaded());

                System.out.println("Tested \"" + mockLoader.result + "\" - okay...");
            }
        }

        System.out.println("*** Tested all example podcast, failed on " + failed);

        // null
        loadAndWait(mockLoader, (Podcast) null);
        assertTrue(mockLoader.failed);

        // null URL
        loadAndWait(mockLoader, new Podcast(null, null));
        assertTrue(mockLoader.failed);
        assertNull(mockLoader.result.getLastLoaded());

        // bad URL
        loadAndWait(mockLoader, new Podcast("Mist", new URL("http://bla")));
        assertTrue(mockLoader.failed);
        assertNull(mockLoader.result.getLastLoaded());
    }

    private LoadPodcastTask loadAndWait(final MockPodcastLoader mockLoader, final Podcast podcast)
            throws Throwable {
        final LoadPodcastTask task = new LoadPodcastTask(mockLoader);

        signal = new CountDownLatch(1);

        runTestOnUiThread(new Runnable() {

            @Override
            public void run() {
                task.execute(podcast);
            }
        });

        final Date start = new Date();
        signal.await();
        System.out.println("Waited " + (new Date().getTime() - start.getTime())
                + "ms for Podcast \"" + podcast + "\"...");

        return task;
    }
}
