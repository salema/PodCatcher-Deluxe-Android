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
package net.alliknow.podcatcher.services.test;

import java.util.concurrent.CountDownLatch;

import net.alliknow.podcatcher.services.PlayEpisodeService;
import net.alliknow.podcatcher.services.PlayEpisodeService.PlayServiceBinder;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.test.AndroidTestCase;

/**
 * @author Kevin Hausmann
 *
 */
public class PlayEpisodeServiceTest extends AndroidTestCase {

	/** Play service */
	private PlayEpisodeService service;
	/** Whether we are currently bound to the service */
	private boolean bound;
	
	private CountDownLatch signal = null;
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		
		signal = new CountDownLatch(1);
		
		// Bind to service
        Intent intent = new Intent(getContext(), PlayEpisodeService.class);
        getContext().bindService(intent, connection, Context.BIND_AUTO_CREATE);
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		
		// Unbind from the service
        if (bound) {
            getContext().unbindService(connection);
            bound = false;
        }
	}	
	
	public final void testPlay() throws InterruptedException {
		signal.await();
		assertNotNull(service);
		
//		service.playEpisode(null);
//		
//		Podcast podcast = new Podcast(ExamplePodcast.GEO.name(), ExamplePodcast.GEO.getURL());
//		podcast.setRssFile(Utils.loadRssFile(podcast));
//		service.playEpisode(podcast.getEpisodes().get(0));
//		service.pause();
//		service.resume();
//		
//		wait(2000);
//		service.pause();
//		service.resume();
	}
	
//	public final void testGetPosition() throws InterruptedException {
//		synchronized (connection) { connection.wait(10000); }
//		assertNotNull(service);
//		assertTrue(service.getCurrentPosition() > -1);
//		
//		Podcast podcast = new Podcast(ExamplePodcast.GEO.name(), ExamplePodcast.GEO.getURL());
//		podcast.setRssFile(Utils.loadRssFile(podcast));
//		service.playEpisode(podcast.getEpisodes().get(0));
//		
//		synchronized (this) { wait(10000); }
//		
//		assertTrue(service.getCurrentPosition() > 0);
//	}
	
	/** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection connection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder serviceBinder) {
        	PlayServiceBinder binder = (PlayServiceBinder) serviceBinder;
            service = binder.getService();
            bound = true;
            
            signal.countDown();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            bound = false;
        }
    };
}
