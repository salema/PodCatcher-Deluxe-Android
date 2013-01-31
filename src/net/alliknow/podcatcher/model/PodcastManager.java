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

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.text.Html;
import android.util.Log;

import net.alliknow.podcatcher.Podcatcher;
import net.alliknow.podcatcher.listeners.OnChangePodcastListListener;
import net.alliknow.podcatcher.listeners.OnLoadPodcastListListener;
import net.alliknow.podcatcher.listeners.OnLoadPodcastListener;
import net.alliknow.podcatcher.listeners.OnLoadPodcastLogoListener;
import net.alliknow.podcatcher.model.tasks.LoadPodcastListTask;
import net.alliknow.podcatcher.model.tasks.Progress;
import net.alliknow.podcatcher.model.tasks.StorePodcastListTask;
import net.alliknow.podcatcher.model.tasks.remote.LoadPodcastLogoTask;
import net.alliknow.podcatcher.model.tasks.remote.LoadPodcastTask;
import net.alliknow.podcatcher.model.types.Episode;
import net.alliknow.podcatcher.model.types.Podcast;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
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

    /** The name of the file we store our saved podcasts in (as OPML) */
    public static final String OPML_FILENAME = "podcasts.opml";
    /** The OPML file encoding */
    public static final String OPML_FILE_ENCODING = "utf8";

    /** The list of podcasts we know */
    private List<Podcast> podcastList;

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

    /**
     * Init the podcast data.
     * 
     * @param app The podcatcher application object (also a singleton).
     */
    private PodcastManager(Podcatcher app) {
        // We use some of its method below, so we keep a reference to the
        // application object.
        this.podcatcher = app;

        // Load list of podcasts from OPML file on start-up, listeners will be
        // notified below.
        LoadPodcastListTask loadListTask =
                new LoadPodcastListTask(podcatcher.getApplicationContext(), this);
        loadListTask.execute((Void) null);
    }

    /**
     * Get the singleton instance of the podcast manager.
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

    @Override
    public void onPodcastListLoaded(List<Podcast> podcastList) {
        // Set the member
        this.podcastList = podcastList;

        // Put some nice sample podcasts for testing
        if (podcatcher.isInDebugMode())
            putSamplePodcasts();

        // Alert call-backs (if any)
        if (loadPodcastListListeners.isEmpty())
            Log.d(getClass().getSimpleName(), "Podcast list loaded, but no listeners set.");
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
        if (!podcast.needsReload())
            onPodcastLoaded(podcast);
        // Only start the load task if it is not already active
        else if (!loadPodcastTasks.containsKey(podcast)) {
            // Download podcast RSS feed (async)
            LoadPodcastTask loadPodcastTask = new LoadPodcastTask(this);
            loadPodcastTask.preventZippedTransfer(podcatcher.isOnFastConnection());
            // loadPodcastTask.execute(podcast);
            loadPodcastTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, podcast);

            // Keep task reference, so we can cancel the load and determine
            // whether the podcast is already loading
            loadPodcastTasks.put(podcast, loadPodcastTask);
        }
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
            Log.d(getClass().getSimpleName(), "Podcast loaded, but no listeners attached.");
        else
            for (OnLoadPodcastListener listener : loadPodcastListeners)
                listener.onPodcastLoaded(podcast);
    }

    @Override
    public void onPodcastLoadFailed(Podcast podcast) {
        // Remove from the map of loading task
        loadPodcastTasks.remove(podcast);

        // Notify listeners
        if (loadPodcastListeners.isEmpty())
            Log.d(getClass().getSimpleName(), "Podcast failed to load, but no listeners set.");
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
     * @param width Width the logo can be sampled down to. Give zero (0) to
     *            disable sampling.
     * @param height Height the logo can be sampled down to.
     * @see OnLoadPodcastLogoListener
     */
    public void loadLogo(Podcast podcast, int width, int height) {
        // Only start the load task if it is not already active
        if (!loadPodcastLogoTasks.containsKey(podcast)) {
            LoadPodcastLogoTask loadPodcastLogoTask = new LoadPodcastLogoTask(this, width, height);
            loadPodcastLogoTask.setLoadLimit(podcatcher.isOnFastConnection() ?
                    LoadPodcastLogoTask.MAX_LOGO_SIZE_WIFI
                    : LoadPodcastLogoTask.MAX_LOGO_SIZE_MOBILE);
            loadPodcastLogoTask.execute(podcast);

            loadPodcastLogoTasks.put(podcast, loadPodcastLogoTask);
        }
    }

    @Override
    public void onPodcastLogoLoaded(Podcast podcast, Bitmap logo) {
        loadPodcastLogoTasks.remove(podcast);

        podcast.setLogo(logo);

        if (loadPodcastLogoListeners.isEmpty())
            Log.d(getClass().getSimpleName(), "Podcast logo loaded, but no listener set.");
        else
            for (OnLoadPodcastLogoListener listener : loadPodcastLogoListeners)
                listener.onPodcastLogoLoaded(podcast, logo);
    }

    @Override
    public void onPodcastLogoLoadFailed(Podcast podcast) {
        loadPodcastLogoTasks.remove(podcast);

        if (loadPodcastLogoListeners.isEmpty())
            Log.d(getClass().getSimpleName(), "Podcast logo failed to load, but no listener set.");
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
    @SuppressWarnings("unchecked")
    public void addPodcast(Podcast newPodcast) {
        // Check whether the new podcast is already added
        if (!contains(newPodcast)) {
            // Add the new podcast
            podcastList.add(newPodcast);
            Collections.sort(podcastList);

            // Alert listeners of new podcast
            for (OnChangePodcastListListener listener : changePodcastListListeners)
                listener.onPodcastAdded(newPodcast);

            // Store changed list
            new StorePodcastListTask(podcatcher.getApplicationContext()).execute(podcastList);
        } else
            Log.d(getClass().getSimpleName(), "Podcast \"" + newPodcast.getName()
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
    @SuppressWarnings("unchecked")
    public void remove(int index) {
        if (index >= 0 && index < size()) {
            // Remove podcast at given position
            Podcast removedPodcast = podcastList.remove(index);

            // Alert listeners of removed podcast
            for (OnChangePodcastListListener listener : changePodcastListListeners)
                listener.onPodcastRemoved(removedPodcast);

            // Store changed list
            new StorePodcastListTask(podcatcher.getApplicationContext()).execute(podcastList);
        } else
            Log.w(getClass().getSimpleName(), "Attempted to remove podcast at invalid position: "
                    + index);
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
        // Find the podcast object
        for (Podcast podcast : podcastList)
            if (podcast.getUrl().toString().equals(url))
                return podcast;

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
        for (Podcast podcast : podcastList)
            if (podcast.getEpisodes().size() > 0)
                for (Episode episode : podcast.getEpisodes())
                    if (episode.getMediaUrl().toString().equals(url))
                        return episode;

        return null;
    }

    /**
     * Stop and cancel all load tasks.
     */
    public void cancelAllLoadTasks() {
        for (LoadPodcastTask task : loadPodcastTasks.values())
            task.cancel(true);
        for (LoadPodcastLogoTask task : loadPodcastLogoTasks.values())
            task.cancel(true);

        loadPodcastTasks.clear();
        loadPodcastLogoTasks.clear();
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
     * Clear list. Add a small number of sample podcast to the list for testing.
     * Sort list.
     */
    private void putSamplePodcasts() {
        podcastList.clear();

        podcastList.add(createPodcast("This American Life",
                "http://feeds.thisamericanlife.org/talpodcast"));
        podcastList.add(createPodcast("Radiolab", "http://feeds.wnyc.org/radiolab"));
        podcastList
                .add(createPodcast("Linux' Outlaws", "http://feeds.feedburner.com/linuxoutlaws"));
        podcastList.add(createPodcast("GEO", "http://www.geo.de/GEOaudio/index.xml"));
        podcastList.add(createPodcast("Mäuse", "http://podcast.wdr.de/maus.xml"));
        podcastList.add(createPodcast("D&uuml;de", "http://feeds.feedburner.com/UhhYeahDude"));
        podcastList.add(createPodcast("neo",
                "http://www.zdf.de/ZDFmediathek/podcast/1446344?view=podcast"));

        // Remove null elements if accidentally create and added above
        while (podcastList.remove(null))
            ;

        Collections.sort(podcastList);
    }

    private Podcast createPodcast(String name, String url) {
        try {
            return new Podcast(Html.fromHtml(name).toString(), new URL(url));
        } catch (MalformedURLException e) {
            Log.e("Podcatcher", "Cannot add sample podcast: " + name, e);
            return null;
        }
    }
}
