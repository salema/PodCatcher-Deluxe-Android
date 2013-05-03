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

package net.alliknow.podcatcher.model;

import android.net.http.HttpResponseCache;
import android.os.AsyncTask;
import android.text.Html;
import android.util.Log;

import net.alliknow.podcatcher.Podcatcher;
import net.alliknow.podcatcher.listeners.OnChangePodcastListListener;
import net.alliknow.podcatcher.listeners.OnLoadPodcastListListener;
import net.alliknow.podcatcher.listeners.OnLoadPodcastListener;
import net.alliknow.podcatcher.listeners.OnLoadPodcastLogoListener;
import net.alliknow.podcatcher.model.tasks.StorePodcastListTask;
import net.alliknow.podcatcher.model.tasks.remote.LoadPodcastLogoTask;
import net.alliknow.podcatcher.model.tasks.remote.LoadPodcastTask;
import net.alliknow.podcatcher.model.types.Episode;
import net.alliknow.podcatcher.model.types.Podcast;
import net.alliknow.podcatcher.model.types.Progress;

import org.xmlpull.v1.XmlPullParser;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Our model class. Holds all the podcast and episode model data and offers
 * various methods to retrieve the information as needed by the different
 * activities, fragments and services. Since this is used in the application
 * sub-class only, there is never more than one instance of this around. You
 * should never have to create this yourself.
 */
public class PodcastManager implements OnLoadPodcastListListener, OnLoadPodcastListener,
        OnLoadPodcastLogoListener {

    /** The single instance */
    private static PodcastManager manager;
    /** The application itself */
    private Podcatcher podcatcher;

    /**
     * The time podcast content is buffered on non-mobile connections (in
     * milliseconds). If older, we will to reload.
     */
    public static final int TIME_TO_LIFE = 30 * 60 * 1000;
    /**
     * The minimum time podcast content is buffered on mobile connections (in
     * milliseconds). If older, we will to reload.
     */
    public static final int TIME_TO_LIFE_MOBILE = 60 * 60 * 1000;
    /** Maximum byte size for the logo to load */
    public static final int MAX_LOGO_SIZE = 500000;

    /** The name of the file we store our saved podcasts in (as OPML) */
    public static final String OPML_FILENAME = "podcasts.opml";

    /** The list of podcasts we know */
    private List<Podcast> podcastList;
    /** Flag to indicate whether podcast list is dirty */
    private boolean podcastListChanged;

    /** The maximum size we sample podcast logos down to */
    private static final int LOGO_DIMENSION = 250;

    /** The current podcast load tasks */
    private Map<Podcast, LoadPodcastTask> loadPodcastTasks = new HashMap<Podcast, LoadPodcastTask>();
    /** The current podcast logo load tasks */
    private Map<Podcast, LoadPodcastLogoTask> loadPodcastLogoTasks = new HashMap<Podcast, LoadPodcastLogoTask>();

    /** The call-back set for the podcast list load listeners */
    private Set<OnLoadPodcastListListener> loadPodcastListListeners = new HashSet<OnLoadPodcastListListener>();
    /** The call-back set for the podcast list changed listeners */
    private Set<OnChangePodcastListListener> changePodcastListListeners = new HashSet<OnChangePodcastListListener>();
    /** The call-back set for the podcast load listeners */
    private Set<OnLoadPodcastListener> loadPodcastListeners = new HashSet<OnLoadPodcastListener>();
    /** The call-back set for the podcast logo load listeners */
    private Set<OnLoadPodcastLogoListener> loadPodcastLogoListeners = new HashSet<OnLoadPodcastLogoListener>();

    /** Static inner thread class to pull flushing the cache off the main thread */
    private static final Thread flushHttpCache = new Thread() {

        @Override
        public void run() {
            final HttpResponseCache cache = HttpResponseCache.getInstalled();
            if (cache != null)
                cache.flush();
        }
    };

    /**
     * Init the podcast data.
     * 
     * @param app The podcatcher application object (also a singleton).
     */
    private PodcastManager(Podcatcher app) {
        // We use some of its method below, so we keep a reference to the
        // application object.
        this.podcatcher = app;
    }

    /**
     * Get the singleton instance of the podcast manager, which grants access to
     * the global podcast data model. The returned manager object is a
     * singleton, all calls to this method will always return the same single
     * instance of the podcast manager.
     * 
     * @param podcatcher Application handle.
     * @return The singleton instance.
     */
    public static PodcastManager getInstance(Podcatcher podcatcher) {
        // If not done, create single instance
        if (manager == null)
            manager = new PodcastManager(podcatcher);

        return manager;
    }

    /**
     * Get the singleton instance of the podcast manager, which grants access to
     * the global podcast data model. The returned manager object is a
     * singleton, all calls to this method will always return the same single
     * instance of the podcast manager.
     * 
     * @return The singleton instance.
     */
    public static PodcastManager getInstance() {
        // We make sure in Application.onCreate() that this method is not called
        // unless the other one with the application instance actually set ran
        // to least once
        return manager;
    }

    @Override
    public void onPodcastListLoaded(List<Podcast> list) {
        // Set the member
        this.podcastList = list;
        this.podcastListChanged = false;

        // Put some nice sample podcasts for testing
        if (podcatcher.isInDebugMode())
            putSamplePodcasts();

        // Alert call-backs (if any)
        if (loadPodcastListListeners.isEmpty())
            Log.w(getClass().getSimpleName(), "Podcast list loaded, but no listeners set.");
        else
            for (OnLoadPodcastListListener listener : loadPodcastListListeners)
                listener.onPodcastListLoaded(getPodcastList());
    }

    /**
     * Get the list of podcast currently known. This will come as a sorted,
     * shallow-copied list. Use the <code>add</code> and <code>remove</code>
     * methods to alter it. The method will return <code>null</code> if the list
     * in not available yet (we are still starting up), you should register a
     * load listener to be notified on load completion.
     * 
     * @return The podcast list, or <code>null</code> if not available.
     * @see OnLoadPodcastListListener
     */
    public List<Podcast> getPodcastList() {
        if (podcastList == null)
            return null;
        // return copy in order to make sure
        // nobody changes this list on us.
        else
            return new ArrayList<Podcast>(podcastList);
    }

    /**
     * Load data for given podcast from its URL. This is an async load, so this
     * method will return immediately. Implement the appropriate call-back to
     * monitor the load process and to get its result.
     * 
     * @param podcast Podcast to load.
     * @see OnLoadPodcastListener
     */
    public void load(Podcast podcast) {
        // Only load podcast if not too old
        if (!shouldReload(podcast))
            onPodcastLoaded(podcast);
        // Only start the load task if it is not already active
        else if (!loadPodcastTasks.containsKey(podcast)) {
            // Store time stamp to avoid loading this logo too often
            podcast.setLastLoadLogoAttempt(new Date());
            // Download podcast RSS feed (async)
            LoadPodcastTask task = new LoadPodcastTask(this);
            task.setOnlyIfCached(!podcatcher.isOnline());
            task.preventZippedTransfer(podcatcher.isOnFastConnection());
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, podcast);

            // Keep task reference, so we can cancel the load and determine
            // whether a task for this podcast is already running
            loadPodcastTasks.put(podcast, task);
        }
    }

    /**
     * Check whether a podcast is currently loading.
     * 
     * @param podcast Podcast to check for.
     * @return <code>true</code> iff loading.
     */
    public boolean isLoading(Podcast podcast) {
        return loadPodcastTasks.containsKey(podcast);
    }

    /**
     * @return The number of podcasts currently loading.
     */
    public int getLoadCount() {
        return loadPodcastTasks.size();
    }

    @Override
    public void onPodcastLoadProgress(Podcast podcast, Progress progress) {
        // Notify listeners
        for (OnLoadPodcastListener listener : loadPodcastListeners)
            listener.onPodcastLoadProgress(podcast, progress);
    }

    @Override
    public void onPodcastLoaded(Podcast podcast) {
        // Remove from the map of loading task
        loadPodcastTasks.remove(podcast);

        // Notify listeners
        if (loadPodcastListeners.isEmpty())
            Log.w(getClass().getSimpleName(), "Podcast loaded, but no listeners attached.");
        else
            for (OnLoadPodcastListener listener : loadPodcastListeners)
                listener.onPodcastLoaded(podcast);

        flushHttpCache.start();
    }

    @Override
    public void onPodcastLoadFailed(Podcast podcast) {
        // Remove from the map of loading task
        loadPodcastTasks.remove(podcast);

        // Notify listeners
        if (loadPodcastListeners.isEmpty())
            Log.w(getClass().getSimpleName(), "Podcast failed to load, but no listeners set.");
        else
            for (OnLoadPodcastListener listener : loadPodcastListeners)
                listener.onPodcastLoadFailed(podcast);
    }

    /**
     * Load logo for given podcast from its URL. This is an async load, so this
     * method will return immediately. Implement the appropriate call-back to
     * monitor the load process and to get its result.
     * 
     * @param podcast Podcast to load logo for.
     * @see OnLoadPodcastLogoListener
     */
    public void loadLogo(Podcast podcast) {
        // Only load podcast logo if it is not there yet
        if (!shouldReloadLogo(podcast))
            onPodcastLogoLoaded(podcast);
        // Only start the load task if it is not already active
        else if (!loadPodcastLogoTasks.containsKey(podcast)) {
            LoadPodcastLogoTask task = new LoadPodcastLogoTask(this, LOGO_DIMENSION, LOGO_DIMENSION);
            task.setOnlyIfCached(!podcatcher.isOnline());
            task.setLoadLimit(MAX_LOGO_SIZE);
            task.execute(podcast);

            loadPodcastLogoTasks.put(podcast, task);
        }
    }

    @Override
    public void onPodcastLogoLoaded(Podcast podcast) {
        loadPodcastLogoTasks.remove(podcast);

        if (loadPodcastLogoListeners.isEmpty())
            Log.w(getClass().getSimpleName(), "Podcast logo loaded, but no listener set.");
        else
            for (OnLoadPodcastLogoListener listener : loadPodcastLogoListeners)
                listener.onPodcastLogoLoaded(podcast);

        flushHttpCache.start();
    }

    @Override
    public void onPodcastLogoLoadFailed(Podcast podcast) {
        loadPodcastLogoTasks.remove(podcast);

        if (loadPodcastLogoListeners.isEmpty())
            Log.w(getClass().getSimpleName(), "Podcast logo failed to load, but no listener set.");
        else
            for (OnLoadPodcastLogoListener listener : loadPodcastLogoListeners)
                listener.onPodcastLogoLoadFailed(podcast);
    }

    /**
     * Add a new podcast to the list of podcasts.
     * {@link OnChangePodcastListListener}s will be notified. If the podcast
     * already is in the list, it will not be added and no notification takes
     * place.
     * 
     * @param newPodcast Podcast to add.
     * @see OnChangePodcastListListener
     */
    public void addPodcast(Podcast newPodcast) {
        // Check whether the new podcast is already added
        if (!contains(newPodcast)) {
            // Add the new podcast
            podcastList.add(newPodcast);
            Collections.sort(podcastList);

            // Alert listeners of new podcast
            for (OnChangePodcastListListener listener : changePodcastListListeners)
                listener.onPodcastAdded(newPodcast);

            // Mark podcast list dirty
            podcastListChanged = true;
        } else
            Log.i(getClass().getSimpleName(), "Podcast \"" + newPodcast.getName()
                    + "\" is already in list.");
    }

    /**
     * Remove a podcast from the list of podcasts.
     * {@link OnChangePodcastListListener}s will be notified. If the given index
     * is out of bounds, no podcast is removed and no notification takes place.
     * 
     * @param index Index of podcast to remove.
     * @see OnChangePodcastListListener
     */
    public void remove(int index) {
        if (index >= 0 && index < size()) {
            // Remove podcast at given position
            Podcast removedPodcast = podcastList.remove(index);

            // Alert listeners of removed podcast
            for (OnChangePodcastListListener listener : changePodcastListListeners)
                listener.onPodcastRemoved(removedPodcast);

            // Mark podcast list dirty
            podcastListChanged = true;
        } else
            Log.w(getClass().getSimpleName(), "Attempted to remove podcast at invalid position: "
                    + index);
    }

    /**
     * Make sure the podcast manager persists its state as needed.
     */
    @SuppressWarnings("unchecked")
    public void saveState() {
        // Store podcast list if dirty
        if (podcastListChanged) {
            new StorePodcastListTask(podcatcher.getApplicationContext()).execute(podcastList);

            // Reset the flag, so the list will only be saved if changed again
            podcastListChanged = false;
        }
    }

    /**
     * @return The number of podcasts available to the manager.
     */
    public int size() {
        if (podcastList == null)
            return 0;
        else
            return podcastList.size();
    }

    /**
     * Find the index (position) of given podcast in the list of podcasts.
     * 
     * @param podcast Podcast to look for.
     * @return The podcast index, or -1 if not in the list.
     */
    public int indexOf(Podcast podcast) {
        if (podcastList == null)
            return -1;
        else
            return podcastList.indexOf(podcast);
    }

    /**
     * Check whether the given podcast is in the list of podcasts.
     * 
     * @param podcast Podcast to look for.
     * @return <code>true</code> iff the podcast is present in list.
     */
    public boolean contains(Podcast podcast) {
        return indexOf(podcast) != -1;
    }

    /**
     * Find the podcast object for given URL.
     * 
     * @param url URL of podcast to look up.
     * @return The podcast object, or <code>null</code> if not found.
     */
    public Podcast findPodcastForUrl(String url) {
        // Make sure search only runs once the podcast list is actually
        // available.
        if (podcastList != null) {

            // Find the podcast object
            for (Podcast podcast : podcastList)
                if (podcast.getUrl().toString().equals(url))
                    return podcast;
        }

        return null;
    }

    /**
     * Find the episode object for given URL. Note that this will only search
     * episodes currently loaded.
     * 
     * @param url URL of episode to look for.
     * @return The episode object, or <code>null</code> if not found.
     */
    public Episode findEpisodeForUrl(String url) {
        // Make sure search only runs once the podcast list is actually
        // available.
        if (podcastList != null) {

            // Go try find the episode
            for (Podcast podcast : podcastList)
                if (podcast.getEpisodeNumber() > 0)
                    for (Episode episode : podcast.getEpisodes())
                        if (episode.getMediaUrl().toString().equals(url))
                            return episode;
        }

        return null;
    }

    /**
     * Add load podcast list listener.
     * 
     * @param listener Listener to add.
     * @see OnLoadPodcastListListener
     */
    public void addLoadPodcastListListener(OnLoadPodcastListListener listener) {
        loadPodcastListListeners.add(listener);
    }

    /**
     * Remove load podcast list listener.
     * 
     * @param listener Listener to remove.
     * @see OnLoadPodcastListListener
     */
    public void removeLoadPodcastListListener(OnLoadPodcastListListener listener) {
        loadPodcastListListeners.remove(listener);
    }

    /**
     * Add podcast list change listener.
     * 
     * @param listener Listener to add.
     * @see OnChangePodcastListListener
     */
    public void addChangePodcastListListener(OnChangePodcastListListener listener) {
        changePodcastListListeners.add(listener);
    }

    /**
     * Remove podcast list change listener.
     * 
     * @param listener Listener to remove.
     * @see OnChangePodcastListListener
     */
    public void removeChangePodcastListListener(OnChangePodcastListListener listener) {
        changePodcastListListeners.remove(listener);
    }

    /**
     * Add load podcast listener.
     * 
     * @param listener Listener to add.
     * @see OnLoadPodcastListener
     */
    public void addLoadPodcastListener(OnLoadPodcastListener listener) {
        loadPodcastListeners.add(listener);
    }

    /**
     * Remove load podcast listener.
     * 
     * @param listener Listener to remove.
     * @see OnLoadPodcastListener
     */
    public void removeLoadPodcastListener(OnLoadPodcastListener listener) {
        loadPodcastListeners.remove(listener);
    }

    /**
     * Add load podcast logo listener.
     * 
     * @param listener Listener to add.
     * @see OnLoadPodcastLogoListener
     */
    public void addLoadPodcastLogoListener(OnLoadPodcastLogoListener listener) {
        loadPodcastLogoListeners.add(listener);
    }

    /**
     * Remove load podcast logo listener.
     * 
     * @param listener Listener to remove.
     * @see OnLoadPodcastLogoListener
     */
    public void removeLoadPodcastLogoListener(OnLoadPodcastLogoListener listener) {
        loadPodcastLogoListeners.remove(listener);
    }

    /**
     * Whether the podcast content is old enough to need reloading. This relates
     * to the time that {@link #parse(XmlPullParser)} has last been called on
     * the object and has nothing to do with the updating of the podcast RSS
     * file on the provider's server.
     * 
     * @param Podcast to check.
     * @return <code>true</code> iff time to live expired or the podcast has
     *         never been loaded.
     */
    private boolean shouldReload(Podcast podcast) {
        // Has never been loaded
        if (podcast.getLastLoaded() == null)
            return true;
        // Has been loaded and we are now offline
        else if (!podcatcher.isOnline())
            return false;
        // Check age
        else {
            final long age = new Date().getTime() - podcast.getLastLoaded().getTime();
            return age > (podcatcher.isOnFastConnection() ? TIME_TO_LIFE : TIME_TO_LIFE_MOBILE);
        }
    }

    /**
     * Check whether we should reload the podcast logo. This depends on a
     * combination of things including whether there is a logo and when the last
     * effort has been made to get it.
     * 
     * @param Podcast to check.
     * @return <code>true</code> iff it seems worth our time and resources to
     *         try reload the podcast logo.
     */
    private boolean shouldReloadLogo(Podcast podcast) {
        return podcast != null && podcast.getLastLoadLogoAttempt() == null
                && podcast.getLogo() == null;
    }

    /**
     * Clear list. Add a small number of sample podcast to the list for testing.
     * Sort list.
     */
    private void putSamplePodcasts() {
        podcastList.clear();

        podcastList.add(createPodcast("This American Life",
                "http://feeds.thisamericanlife.org/talpodcast"));
        podcastList.add(createPodcast("Radiolab", "http://feeds.wnyc.org/radiolab"));
        podcastList.add(createPodcast("Linux' Outlaws",
                "http://feeds.feedburner.com/linuxoutlaws"));
        podcastList.add(createPodcast("GEO", "http://www.geo.de/GEOaudio/index.xml"));
        podcastList.add(createPodcast("Mäuse", "http://podcast.wdr.de/maus.xml"));
        podcastList.add(createPodcast("D&uuml;de", "http://feeds.feedburner.com/UhhYeahDude"));
        podcastList.add(createPodcast("neo",
                "http://www.zdf.de/ZDFmediathek/podcast/1446344?view=podcast"));
        podcastList.add(createPodcast("Little Letter for Gaelic Learners",
                "http://downloads.bbc.co.uk/podcasts/scotland/litirbheag/rss.xml"));

        // Remove null elements if accidentally create and added above
        while (podcastList.remove(null))
            ;

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
