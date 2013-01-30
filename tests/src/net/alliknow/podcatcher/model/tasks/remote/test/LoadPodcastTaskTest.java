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
import net.alliknow.podcatcher.model.tasks.Progress;
import net.alliknow.podcatcher.model.tasks.remote.LoadPodcastTask;
import net.alliknow.podcatcher.model.types.Podcast;
import net.alliknow.podcatcher.model.types.test.ExamplePodcast;

import java.net.URL;
import java.util.Date;
import java.util.concurrent.CountDownLatch;

public class LoadPodcastTaskTest extends InstrumentationTestCase {

    private CountDownLatch signal = null;

    private class MockPodcastLoader implements OnLoadPodcastListener {

        protected Podcast result;
        protected boolean failed;

        @Override
        public void onPodcastLoaded(Podcast podcast) {
            this.result = podcast;
            this.failed = false;

            signal.countDown();
        }

        @Override
        public void onPodcastLoadFailed(Podcast podcast) {
            this.result = podcast;
            this.failed = true;

            signal.countDown();
        }

        @Override
        public void onPodcastLoadProgress(Podcast podcast, Progress progress) {
            System.out.println(progress);
        }
    }

    public final void testLoadPodcast() throws Throwable {
        final MockPodcastLoader mockLoader = new MockPodcastLoader();

        // Actual example Podcast
        for (ExamplePodcast ep : ExamplePodcast.values()) {
            LoadPodcastTask task = loadAndWait(mockLoader, new Podcast(ep.name(), ep.getURL()));

            assertFalse(task.isCancelled());
            assertFalse(mockLoader.failed);
            assertNotNull(mockLoader.result);
            assertFalse(mockLoader.result.getEpisodes().isEmpty());
            assertFalse(mockLoader.result.needsReload());

            System.out.println("Tested \"" + ep + "\" - okay...");
        }

        // null
        loadAndWait(mockLoader, (Podcast) null);
        assertTrue(mockLoader.failed);

        // null URL
        loadAndWait(mockLoader, new Podcast(null, null));
        assertTrue(mockLoader.failed);
        assertTrue(mockLoader.result.needsReload());

        // bad URL
        loadAndWait(mockLoader, new Podcast("Mist", new URL("http://bla")));
        assertTrue(mockLoader.failed);
        assertTrue(mockLoader.result.needsReload());
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
