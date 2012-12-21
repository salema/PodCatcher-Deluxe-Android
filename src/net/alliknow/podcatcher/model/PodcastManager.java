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
package net.alliknow.podcatcher.model;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.alliknow.podcatcher.Podcatcher;
import net.alliknow.podcatcher.listeners.OnLoadPodcastListListener;
import net.alliknow.podcatcher.listeners.OnLoadPodcastListener;
import net.alliknow.podcatcher.listeners.OnLoadPodcastLogoListener;
import net.alliknow.podcatcher.model.tasks.LoadPodcastListTask;
import net.alliknow.podcatcher.model.tasks.Progress;
import net.alliknow.podcatcher.model.tasks.StorePodcastListTask;
import net.alliknow.podcatcher.model.tasks.remote.LoadPodcastLogoTask;
import net.alliknow.podcatcher.model.tasks.remote.LoadPodcastTask;
import net.alliknow.podcatcher.model.types.Podcast;
import android.graphics.Bitmap;
import android.text.Html;
import android.util.Log;

/**
 * Our model class.
 * Holds all the podcast and episode model data and offers
 * various methods to retrieve the information as needed
 * by the different activities, fragments and services.
 * Since this is used in the application sub-class only,
 * there is never more than one instance of this around.
 * You should never have to create this yourself.
 */
public class PodcastManager implements OnLoadPodcastListListener, OnLoadPodcastListener, OnLoadPodcastLogoListener {

	/** The single instance */
	private static PodcastManager manager;
	/** The application itself */
	private Podcatcher podcatcher;
	
	/** The name of the file we store our saved podcasts in (as OPML) */
	public static final String OPML_FILENAME = "podcasts.opml";
	/** The OPML file encoding */
	public static final String OPML_FILE_ENCODING = "utf8";
	
	/** The list of podcasts we know */
	private List<Podcast> podcastList;
	/** The list of podcast suggestions */
	private List<Podcast> podcastSuggestions;
	
	/** The current podcast load tasks */
	private Map<Podcast, LoadPodcastTask> loadPodcastTasks = new HashMap<Podcast, LoadPodcastTask>();
	/** The current podcast logo load tasks */
	private Map<Podcast, LoadPodcastLogoTask> loadPodcastLogoTasks = new HashMap<Podcast, LoadPodcastLogoTask>();
	
    /** The call-back set for the podcast list load */
    private Set<OnLoadPodcastListListener> loadPodcastListListeners = new HashSet<OnLoadPodcastListListener>();
    /** The call-back set for the podcast load */
    private Set<OnLoadPodcastListener> loadPodcastListeners = new HashSet<OnLoadPodcastListener>();
    /** The call-back set for the podcast logo load */
    private Set<OnLoadPodcastLogoListener> loadPodcastLogoListeners = new HashSet<OnLoadPodcastLogoListener>();
        
    /**
     * Init the podcast data.
	 * @param app The podcatcher application object (also a singleton).
	 */
	private PodcastManager(Podcatcher app) {
		this.podcatcher = app;
		
		// Load list of podcasts from OPML file
		LoadPodcastListTask loadListTask = 
				new LoadPodcastListTask(podcatcher.getApplicationContext(), this);
		loadListTask.execute((Void)null);
	}
	
	public static PodcastManager getInstance(Podcatcher podcatcher) {
		if (manager == null)
			manager = new PodcastManager(podcatcher);
		
		return manager;
	}

	@Override
	public void onPodcastListLoaded(List<Podcast> podcastList) {
		this.podcastList = podcastList;
		
		if (podcatcher.isInDebugMode()) putSamplePodcasts();
		
		// Alert call-backs
		if (loadPodcastListListeners.isEmpty()) Log.d(getClass().getSimpleName(), "Podcast list loaded, but no listeners set.");
		else for (OnLoadPodcastListListener listener : loadPodcastListListeners)
			 listener.onPodcastListLoaded(getPodcastList());
	}
	
	/**
	 * Load data for given podcast from its URL.
	 * This is an async load, so this method will return
	 * immediately. Implement the appropriate call-back
	 * to monitor the load process and to get its result.
	 * @param podcast Podcast to load.
	 */
	public void load(Podcast podcast) {
		// Only start the load task if it is not already active
		if (! loadPodcastTasks.containsKey(podcast)) {
			// Download podcast RSS feed (async)
			LoadPodcastTask loadPodcastTask = new LoadPodcastTask(this);
			loadPodcastTask.preventZippedTransfer(podcatcher.isOnFastConnection());
			loadPodcastTask.execute(podcast);
			
			loadPodcastTasks.put(podcast, loadPodcastTask);
		}
	}

	@Override
	public void onPodcastLoadProgress(Podcast podcast, Progress progress) {
		for (OnLoadPodcastListener listener : loadPodcastListeners)
			listener.onPodcastLoadProgress(podcast, progress);
	}

	@Override
	public void onPodcastLoaded(Podcast podcast) {
		// Remove from the map of loading task
		loadPodcastTasks.remove(podcast);
		
		if (loadPodcastListeners.isEmpty()) Log.d(getClass().getSimpleName(), "Podcast loaded, but no listeners attached.");
		else for (OnLoadPodcastListener listener : loadPodcastListeners)
			listener.onPodcastLoaded(podcast);
	}

	@Override
	public void onPodcastLoadFailed(Podcast podcast) {
		// Remove from the map of loading task
		loadPodcastTasks.remove(podcast);
		
		if (loadPodcastListeners.isEmpty()) Log.d(getClass().getSimpleName(), "Podcast failed to load, but no listeners set.");
		else for (OnLoadPodcastListener listener : loadPodcastListeners)
			listener.onPodcastLoadFailed(podcast);
	}
	
	/**
	 * Load logo for given podcast from its URL.
	 * This is an async load, so this method will return
	 * immediately. Implement the appropriate call-back
	 * to monitor the load process and to get its result.
	 * @param podcast Podcast to load logo for.
	 * @param width Width the logo can be sampled down to.
	 * Give zero (0) to disable sampling.
	 * @param height Height the logo can be sampled down to.
	 */
	public void loadLogo(Podcast podcast, int width, int height) {
		// Only start the load task if it is not already active
		if (! loadPodcastLogoTasks.containsKey(podcast)) {
			LoadPodcastLogoTask loadPodcastLogoTask = new LoadPodcastLogoTask(this, width, height);
			loadPodcastLogoTask.setLoadLimit(podcatcher.isOnFastConnection() ? 
					LoadPodcastLogoTask.MAX_LOGO_SIZE_WIFI : LoadPodcastLogoTask.MAX_LOGO_SIZE_MOBILE);
			loadPodcastLogoTask.execute(podcast);
			
			loadPodcastLogoTasks.put(podcast, loadPodcastLogoTask);
		}
	}
	
	@Override
	public void onPodcastLogoLoaded(Podcast podcast, Bitmap logo) {
		loadPodcastLogoTasks.remove(podcast);
		
		if (loadPodcastLogoListeners.isEmpty()) Log.d(getClass().getSimpleName(), "Podcast logo loaded, but no listener set.");
		else for (OnLoadPodcastLogoListener listener : loadPodcastLogoListeners)
			listener.onPodcastLogoLoaded(podcast, logo);
	}

	@Override
	public void onPodcastLogoLoadFailed(Podcast podcast) {
		loadPodcastLogoTasks.remove(podcast);
		
		if (loadPodcastLogoListeners.isEmpty()) Log.d(getClass().getSimpleName(), "Podcast logo failed to load, but no listener set.");
		else for (OnLoadPodcastLogoListener listener : loadPodcastLogoListeners)
			listener.onPodcastLogoLoadFailed(podcast);
	}
	
	public boolean addLoadPodcastListListener(OnLoadPodcastListListener listener) {
		return loadPodcastListListeners.add(listener);
	}
	
	public boolean removeLoadPodcastListListener(OnLoadPodcastListListener listener) {
		return loadPodcastListListeners.remove(listener);
	}
	
	public boolean addLoadPodcastListener(OnLoadPodcastListener listener) {
		return loadPodcastListeners.add(listener);
	}
	
	public boolean removeLoadPodcastListener(OnLoadPodcastListener listener) {
		return loadPodcastListeners.remove(listener);
	}
	
	public boolean addLoadPodcastLogoListener(OnLoadPodcastLogoListener listener) {
		return loadPodcastLogoListeners.add(listener);
	}
	
	public boolean removeLoadPodcastLogoListener(OnLoadPodcastLogoListener listener) {
		return loadPodcastLogoListeners.remove(listener);
	}
	
	public List<Podcast> getPodcastList() {
		if (podcastList == null) return null;
		// return copy in order to make sure
		// nobody changes this list on us.
		else return new ArrayList<Podcast>(podcastList);
	}
	
	public List<Podcast> getPodcastSuggestions() {
		if (podcastSuggestions == null) return null;
		// return copy in order to make sure
		// nobody changes this list on us.
		else return new ArrayList<Podcast>(podcastSuggestions);
	}

	public void setPodcastSuggestions(List<Podcast> suggestions) {
		this.podcastSuggestions = suggestions;
	}
	
	@SuppressWarnings("unchecked")
	public void addPodcast(Podcast newPodcast) {
		if (! podcastList.contains(newPodcast)) {
			podcastList.add(newPodcast);
			Collections.sort(podcastList);
			
			// Store changed list
			new StorePodcastListTask(podcatcher.getApplicationContext()).execute(podcastList);
		} else Log.d(getClass().getSimpleName(), "Podcast \"" + newPodcast.getName() + "\" is already in list.");
	}
	
	@SuppressWarnings("unchecked")
	public void remove(int index) {
		if (index >= 0 && index < podcastList.size()) {
			podcastList.remove(index);
		
			// Store changed list
			new StorePodcastListTask(podcatcher.getApplicationContext()).execute(podcastList);
		}
	}

	public int indexOf(Podcast podcast) {
		if (podcastList == null) return -1;
		else return podcastList.indexOf(podcast);
	}
	
	public Podcast get(int position) {
		if (podcastList == null) return null;
		else if (position < 0 || position >= podcastList.size()) return null;
		else return podcastList.get(position);
	}
	
	public int size() {
		if (podcastList == null) return 0;
		else return podcastList.size();
	}

	public boolean contains(Podcast podcast) {
		if (podcastList == null) return false;
		else return podcastList.contains(podcast);
	}
	
	public void cancelAllLoadTasks() {
		for (LoadPodcastTask task : loadPodcastTasks.values()) task.cancel(true);
		for (LoadPodcastLogoTask task : loadPodcastLogoTasks.values()) task.cancel(true);
		
		loadPodcastTasks.clear();
		loadPodcastLogoTasks.clear();
	}
	
	/**
	 * Clear list.
	 * Add a small number of sample podcast to the list for testing.
	 * Sort list.
	 * @param list List to fill.
	 */
	private void putSamplePodcasts() {
		podcastList.clear();
		
		podcastList.add(createPodcast("This American Life", "http://feeds.thisamericanlife.org/talpodcast"));
		podcastList.add(createPodcast("Radiolab", "http://feeds.wnyc.org/radiolab"));
		podcastList.add(createPodcast("Linux' Outlaws", "http://feeds.feedburner.com/linuxoutlaws"));
		podcastList.add(createPodcast("GEO", "http://www.geo.de/GEOaudio/index.xml"));
		podcastList.add(createPodcast("Mäuse", "http://podcast.wdr.de/maus.xml"));
		podcastList.add(createPodcast("D&uuml;de", "http://feeds.feedburner.com/UhhYeahDude"));
		podcastList.add(createPodcast("neo", "http://www.zdf.de/ZDFmediathek/podcast/1446344?view=podcast"));
		
		// Remove null elements if accidentially create and added above
		while (podcastList.remove(null));
		
		Collections.sort(podcastList);
	}
	
	private static Podcast createPodcast(String name, String url) {
		try {
			return new Podcast(Html.fromHtml(name).toString(), new URL(url));
		} catch (MalformedURLException e) {
			Log.e("Podcatcher", "Cannot add sample podcast: " + name, e);
			return null;
		}
	}
}
