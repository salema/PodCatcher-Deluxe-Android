/** Copyright 2012 Kevin Hausmann
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
package net.alliknow.podcatcher.tasks.test;

import java.util.concurrent.CountDownLatch;

import net.alliknow.podcatcher.tasks.LoadPodcastTask;
import net.alliknow.podcatcher.tasks.LoadPodcastTask.PodcastLoader;
import net.alliknow.podcatcher.types.Podcast;
import net.alliknow.podcatcher.types.test.ExamplePodcast;
import android.os.AsyncTask;
import android.test.InstrumentationTestCase;
import android.util.Log;

/**
 * @author Kevin Hausmann
 *
 */
public class LoadPodcastTaskTest extends InstrumentationTestCase {

	private CountDownLatch signal = null;
	
	private class MockPodcastLoader implements PodcastLoader {

		private Podcast result;
		private boolean failed;
		
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

		public Podcast getResult() {
			return result;
		}

		public boolean hasFailed() {
			return failed;
		}
	}
	
	public final void testLoadPodcast() throws Throwable {
		System.out.println("Start testing");
		Log.d("Test", "STARTED");
		final MockPodcastLoader mockLoader = new MockPodcastLoader();
				
		// Actual example Podcast
		for (ExamplePodcast ep : ExamplePodcast.values()) {
			final LoadPodcastTask task = new LoadPodcastTask(mockLoader);
			final Podcast podcast = new Podcast(ep.name(), ep.getURL());
			System.out.println("Testing \"" + ep + "\" - okay...");
			signal = new CountDownLatch(1);
			
			runTestOnUiThread(new Runnable() {
				
				@Override
				public void run() {
					task.execute(podcast);	
				}
			});
			
			signal.await();
			
			assertEquals(AsyncTask.Status.FINISHED, task.getStatus());
			assertFalse(task.isCancelled());
			assertFalse(mockLoader.hasFailed());
			assertNotNull(mockLoader.getResult());
			assertFalse(mockLoader.getResult().getEpisodes().isEmpty());
			assertFalse(mockLoader.getResult().needsReload());
			
			System.out.println("Tested \"" + ep + "\" - okay...");
		}
		/*
		// null
		task = new LoadPodcastTask(mockLoader);
		task.execute((Podcast)null);
		
		synchronized (mockLoader) {
			mockLoader.wait(10000);
	    }
		
		assertEquals(AsyncTask.Status.FINISHED, task.getStatus());
		assertTrue(mockLoader.hasFailed());
		
		// null URL
		task = new LoadPodcastTask(mockLoader);
		task.execute(new Podcast(null, null));
		
		synchronized (mockLoader) {
			mockLoader.wait(10000);
	    }
		
		assertEquals(AsyncTask.Status.FINISHED, task.getStatus());
		assertTrue(mockLoader.hasFailed());
		assertTrue(mockLoader.getResult().needsReload());
		
		// bad URL
		task = new LoadPodcastTask(mockLoader);
		task.execute(new Podcast("Mist", new URL("http://bla")));
		
		synchronized (mockLoader) {
			mockLoader.wait(10000);
	    }
		
		assertEquals(AsyncTask.Status.FINISHED, task.getStatus());
		assertTrue(mockLoader.hasFailed());
		assertTrue(mockLoader.getResult().needsReload());*/
	}

}
