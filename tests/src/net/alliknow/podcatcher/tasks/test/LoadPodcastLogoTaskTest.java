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

import java.net.MalformedURLException;

import net.alliknow.podcatcher.tasks.LoadPodcastLogoTask;
import net.alliknow.podcatcher.tasks.LoadPodcastLogoTask.PodcastLogoLoader;
import net.alliknow.podcatcher.test.Utils;
import net.alliknow.podcatcher.types.Podcast;
import net.alliknow.podcatcher.types.test.ExamplePodcast;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.test.AndroidTestCase;

/**
 * @author Kevin Hausmann
 *
 */
public class LoadPodcastLogoTaskTest extends AndroidTestCase {
	private class MockPodcastLogoLoader implements PodcastLogoLoader {

		private Bitmap result;
		private boolean failed;
		
		public Bitmap getResult() {
			return result;
		}

		public boolean hasFailed() {
			return failed;
		}

		@Override
		public void onPodcastLogoLoaded(Bitmap logo) {
			this.result = logo;
			this.failed = false;
			
			synchronized(this) {
	            notifyAll();
	        }
		}

		@Override
		public void onPodcastLogoLoadFailed() {
			this.failed = true;
			
			synchronized(this) {
	            notifyAll();
	        }
		}
	}
	
	public final void testLoadPodcastLogo() throws InterruptedException, MalformedURLException {
		MockPodcastLogoLoader mockLoader = new MockPodcastLogoLoader();
		LoadPodcastLogoTask task = new LoadPodcastLogoTask(mockLoader);
		
		// Actual example Podcast
		for (ExamplePodcast ep : ExamplePodcast.values()) {
			Podcast podcast = new Podcast(ep.name(), ep.getURL());
			podcast.setRssFile(Utils.loadRssFile(podcast));
			
			task = new LoadPodcastLogoTask(mockLoader);
			task.execute(podcast);
			
			synchronized (mockLoader) { mockLoader.wait(); }
			
			assertEquals(AsyncTask.Status.FINISHED, task.getStatus());
			assertFalse(task.isCancelled());
			assertFalse(mockLoader.hasFailed());
			assertNotNull(mockLoader.getResult());
			assertTrue(mockLoader.getResult().getByteCount() > 0);
			
			System.out.println("Tested \"" + ep + "\" - okay...");
		}
	}
}
