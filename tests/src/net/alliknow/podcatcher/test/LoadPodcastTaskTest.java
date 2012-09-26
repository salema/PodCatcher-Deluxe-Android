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
package net.alliknow.podcatcher.test;

import java.net.MalformedURLException;
import java.net.URL;

import net.alliknow.podcatcher.tasks.LoadPodcastTask;
import net.alliknow.podcatcher.tasks.LoadPodcastTask.PodcastLoader;
import net.alliknow.podcatcher.types.Podcast;
import net.alliknow.podcatcher.types.test.ExamplePodcast;
import android.test.AndroidTestCase;

/**
 * @author Kevin Hausmann
 *
 */
public class LoadPodcastTaskTest extends AndroidTestCase {

	private class MockPodcastLoader implements PodcastLoader {

		private Podcast result;
		private boolean failed;
		
		@Override
		public void onPodcastLoaded(Podcast podcast) {
			this.result = podcast;
			this.failed = false;
			
			synchronized(this) {
	            notifyAll();
	        }
		}

		@Override
		public void onPodcastLoadFailed(Podcast podcast) {
			this.result = podcast;
			this.failed = true;
			
			synchronized(this) {
	            notifyAll();
	        }
		}

		public Podcast getResult() {
			return result;
		}

		public boolean hasFailed() {
			return failed;
		}
	}
	
	public final void testLoadPodcast() throws InterruptedException, MalformedURLException {
		MockPodcastLoader mockLoader = new MockPodcastLoader();
		LoadPodcastTask task = new LoadPodcastTask(mockLoader);
		
		task.execute(new Podcast(ExamplePodcast.DAILYBACON.name(), ExamplePodcast.DAILYBACON.getURL()));
		
		synchronized (mockLoader) {
			mockLoader.wait(10000);
	    }
		
		assertNotNull(mockLoader.getResult());
		
		task = new LoadPodcastTask(mockLoader);
		task.execute(new Podcast(ExamplePodcast.DAILYBACON.name(), new URL("http://bla")));
		
		synchronized (mockLoader) {
			mockLoader.wait(10000);
	    }
		
		assertTrue(mockLoader.hasFailed());
	}

}
