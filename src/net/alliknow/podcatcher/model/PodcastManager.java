/** Copyright 2012-2014 Kevin Hausmann
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

import android.annotation.TargetApi;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.UserManager;
import android.preference.PreferenceManager;

import net.alliknow.podcatcher.GetRestrictionsReceiver;
import net.alliknow.podcatcher.Podcatcher;
import net.alliknow.podcatcher.SettingsActivity;
import net.alliknow.podcatcher.listeners.OnChangePodcastListListener;
import net.alliknow.podcatcher.listeners.OnLoadPodcastListListener;
import net.alliknow.podcatcher.listeners.OnLoadPodcastListener;
import net.alliknow.podcatcher.listeners.OnLoadPodcastLogoListener;
import net.alliknow.podcatcher.model.tasks.StorePodcastListTask;
import net.alliknow.podcatcher.model.tasks.remote.LoadPodcastLogoTask;
import net.alliknow.podcatcher.model.tasks.remote.LoadPodcastTask;
import net.alliknow.podcatcher.model.tasks.remote.LoadPodcastTask.PodcastLoadError;
import net.alliknow.podcatcher.model.types.Episode;
import net.alliknow.podcatcher.model.types.Podcast;
import net.alliknow.podcatcher.model.types.Progress;

import org.xmlpull.v1.XmlPullParser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.RejectedExecutionException;

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
    /** Maximum byte size for the logo to load when on mobile connection */
    public static final int MAX_LOGO_SIZE_MOBILE = 500000;

    /** Max stale time we accept from http cache on fast connections */
    private static final int MAX_STALE = 60 * 60; // one hour
    /** Max stale time we accept from http cache on mobile connections */
    private static final int MAX_STALE_MOBILE = 60 * 60 * 4; // 4 hours
    /** Max stale time we accept from http cache when offline */
    private static final int MAX_STALE_OFFLINE = 60 * 60 * 24 * 7; // 1 week

    /** The name of the file we store our saved podcasts in (as OPML) */
    public static final String OPML_FILENAME = "podcasts.opml";
    /** The OPML file encoding */
    public static final String OPML_FILE_ENCODING = "utf8";

    /** The list of podcasts we know */
    private List<Podcast> podcastList;
    /** Flag to indicate whether podcast list is dirty */
    private boolean podcastListChanged;

    /**
     * Flag to indicate whether we run in a restricted profile and should block
     * explicit podcasts from loading and suggestions from the being added
     */
    private boolean blockExplicit = false;

    /** The current podcast load tasks */
    private Map<Podcast, LoadPodcastTask> loadPodcastTasks = new HashMap<>();
    /** The current podcast logo load tasks */
    private Map<Podcast, LoadPodcastLogoTask> loadPodcastLogoTasks = new HashMap<>();

    /** The call-back set for the podcast list load listeners */
    private Set<OnLoadPodcastListListener> loadPodcastListListeners = new HashSet<>();
    /** The call-back set for the podcast list changed listeners */
    private Set<OnChangePodcastListListener> changePodcastListListeners = new HashSet<>();
    /** The call-back set for the podcast load listeners */
    private Set<OnLoadPodcastListener> loadPodcastListeners = new HashSet<>();
    /** The call-back set for the podcast logo load listeners */
    private Set<OnLoadPodcastLogoListener> loadPodcastLogoListeners = new HashSet<>();

    /** This is the background update task */
    private class PodcastUpdateTask extends TimerTask {

        @Override
        public void run() {
            final boolean online = podcatcher.isOnline();
            // This is the current time minus the time to life for the podcast
            // minus some extra time to make sure we refresh before it if
            // actually due
            final Date triggerIfLoadedBefore = new Date(new Date().getTime() -
                    (podcatcher.isOnFastConnection() ? TIME_TO_LIFE : TIME_TO_LIFE_MOBILE) -
                    1000 * 60 * 6); // trigger if six minutes before reload

            // There are some conditions here: We need to be online and there
            // should not be too many threads open
            if (online && (loadPodcastTasks.size() + loadPodcastLogoTasks.size() < 50))
                for (Podcast podcast : podcastList) {
                    // There are more conditions here: The podcast is not
                    // currently loading, and has not been loaded recently
                    if (!loadPodcastTasks.containsKey(podcast) &&
                            (podcast.getLastLoaded() == null || podcast.getLastLoaded().before(
                                    triggerIfLoadedBefore))) {
                        // Download podcast RSS feed (async)
                        final LoadPodcastTask task = new LoadPodcastTask(PodcastManager.this);
                        task.setBlockExplicitEpisodes(blockExplicit);
                        try {
                            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, podcast);

                            // Keep task reference, so we can cancel the load
                            // and determine whether a task for this podcast is
                            // already running
                            loadPodcastTasks.put(podcast, task);
                        } catch (RejectedExecutionException ree) {
                            // Skip update
                        }
                    }
                }
        }
    }

    /**
     * Init the podcast data.
     * 
     * @param app The podcatcher application object (also a singleton).
     */
    private PodcastManager(Podcatcher app) {
        // We use some of its method below, so we keep a reference to the
        // application object.
        this.podcatcher = app;

        // Check for preferences
        this.blockExplicit = checkForRestrictedProfileBlocksExplicit();
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
    public void onPodcastListLoaded(List<Podcast> list, Uri input) {
        // Set the member
        this.podcastList = list;
        this.podcastListChanged = false;

        // Put some nice sample podcasts for testing
        // if (podcatcher.isInDebugMode())
        // putSamplePodcasts();

        // Alert call-backs (if any)
        for (OnLoadPodcastListListener listener : loadPodcastListListeners)
            listener.onPodcastListLoaded(getPodcastList(), input);

        // Go load all podcast logo available offline
        for (Podcast podcast : podcastList)
            loadLogo(podcast, true);

        // Run podcast update task every five minutes
        final int fiveMinutes = 1000 * 60 * 5;
        final boolean isSelectAllOnStart = PreferenceManager.getDefaultSharedPreferences(
                podcatcher.getApplicationContext()).getBoolean(
                SettingsActivity.KEY_SELECT_ALL_ON_START, false);
        new Timer().schedule(new PodcastUpdateTask(),
                isSelectAllOnStart || podcatcher.isInDebugMode() ?
                        fiveMinutes : 0, fiveMinutes);
    }

    @Override
    public void onPodcastListLoadFailed(Uri inputFile, Exception error) {
        // This should not happen, the app's private OPML file could not be
        // read. We simply push an empty list and pretend things are fine.
        onPodcastListLoaded(new ArrayList<Podcast>(), inputFile);
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
            return new ArrayList<>(podcastList);
    }

    /**
     * Load data for given podcast from its URL. This is an async load, so this
     * method will return immediately. Implement the appropriate call-back to
     * monitor the load process and to get its result. Note that the async task
     * might be held back until the episode metadata has finished loading.
     * 
     * @param podcast Podcast to load.
     * @see OnLoadPodcastListener
     * @see EpisodeManager#blockUntilEpisodeMetadataIsLoaded()
     */
    public void load(Podcast podcast) {
        // Only load podcast if not too old
        if (!shouldReload(podcast))
            onPodcastLoaded(podcast);
        // Only start the load task if it is not already active
        else if (!loadPodcastTasks.containsKey(podcast)) {
            // Download podcast RSS feed (async)
            final LoadPodcastTask task = new LoadPodcastTask(this);
            task.setBlockExplicitEpisodes(blockExplicit);
            // We will accept stale versions from the cache in certain
            // situations
            task.setMaxStale(podcatcher.isOnline() ?
                    podcatcher.isOnFastConnection() ? MAX_STALE : MAX_STALE_MOBILE
                    : MAX_STALE_OFFLINE);

            try {
                task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, podcast);

                // Keep task reference, so we can cancel the load and determine
                // whether a task for this podcast is already running
                loadPodcastTasks.put(podcast, task);
            } catch (RejectedExecutionException ree) {
                // Skip update TODO We might need a better solution here?
            }
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
        // Clear the failed count for this podcast
        podcast.resetFailedLoadAttempts();

        // Notify listeners
        if (blockExplicit && podcast.isExplicit())
            onPodcastLoadFailed(podcast, PodcastLoadError.EXPLICIT_BLOCKED);
        else
            for (OnLoadPodcastListener listener : loadPodcastListeners)
                listener.onPodcastLoaded(podcast);
    }

    @Override
    public void onPodcastLoadFailed(Podcast podcast, PodcastLoadError code) {
        // Remove from the map of loading task
        loadPodcastTasks.remove(podcast);
        // Increment the failed load attempt count
        podcast.incrementFailedLoadAttempts();

        // Notify listeners
        for (OnLoadPodcastListener listener : loadPodcastListeners)
            listener.onPodcastLoadFailed(podcast, code);
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
        loadLogo(podcast, false);
    }

    private void loadLogo(Podcast podcast, boolean localOnly) {
        // Only load podcast logo if it is not there yet
        if (podcast.isLogoCached())
            onPodcastLogoLoaded(podcast);
        // Only start the load task if it is not already active
        else if (!loadPodcastLogoTasks.containsKey(podcast)) {
            // Start logo download
            LoadPodcastLogoTask task = new LoadPodcastLogoTask(podcatcher, this);

            // Limit logo size download unless we are on a fast network.
            if (!podcatcher.isOnFastConnection())
                task.setLoadLimit(MAX_LOGO_SIZE_MOBILE);
            // Only use cached file if offline (even if stale)
            task.setLocalOnly(!podcatcher.isOnline() || localOnly);

            try {
                // Go for it!
                task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, podcast);

                // Keep task reference, so we can cancel the load and determine
                // whether a task for this podcast logo is already running
                loadPodcastLogoTasks.put(podcast, task);
            } catch (RejectedExecutionException ree) {
                // Skip logo loading
            }
        }
    }

    @Override
    public void onPodcastLogoLoaded(Podcast podcast) {
        loadPodcastLogoTasks.remove(podcast);

        for (OnLoadPodcastLogoListener listener : loadPodcastLogoListeners)
            listener.onPodcastLogoLoaded(podcast);
    }

    @Override
    public void onPodcastLogoLoadFailed(Podcast podcast) {
        loadPodcastLogoTasks.remove(podcast);

        for (OnLoadPodcastLogoListener listener : loadPodcastLogoListeners)
            listener.onPodcastLogoLoadFailed(podcast);
    }

    /**
     * Add a new podcast to the list of podcasts.
     * {@link OnChangePodcastListListener}s will be notified. If the podcast
     * already is in the list, it will not be added and no notification takes
     * place.
     * 
     * @param newPodcast Podcast to add. Given <code>null</code> will make the
     *            method return without any action.
     * @see OnChangePodcastListListener
     */
    public void addPodcast(Podcast newPodcast) {
        if (newPodcast != null)
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
            }
    }

    /**
     * Remove a podcast from the list of podcasts.
     * {@link OnChangePodcastListListener}s will be notified. If the given index
     * is out of bounds, no podcast is removed and no notification takes place.
     * 
     * @param index Index of podcast to remove.
     * @see OnChangePodcastListListener
     */
    public void removePodcast(int index) {
        if (index >= 0 && index < size()) {
            // Remove podcast at given position
            Podcast removedPodcast = podcastList.remove(index);

            // Alert listeners of removed podcast
            for (OnChangePodcastListListener listener : changePodcastListListeners)
                listener.onPodcastRemoved(removedPodcast);

            // Mark podcast list dirty
            podcastListChanged = true;
        }
    }

    /**
     * @return Whether the app runs in a restricted environment where access to
     *         podcast with explicit content is blocked.
     */
    public boolean blockExplicit() {
        return blockExplicit;
    }

    /**
     * Set and permanently store the username/password combination for the given
     * podcast.
     * 
     * @param podcast Podcast to set credentials for. Needs to be in the
     *            manager's list.
     * @param username Username to set.
     * @param password Password to set.
     */
    public void setCredentials(Podcast podcast, String username, String password) {
        if (podcastList.contains(podcast)) {
            podcast.setUsername(username);
            podcast.setPassword(password);

            // Mark podcast list dirty
            podcastListChanged = true;
        }
    }

    /**
     * Make sure the podcast manager persists its state as needed.
     */
    @SuppressWarnings("unchecked")
    public void saveState() {
        // Store podcast list if dirty
        if (podcastListChanged && podcastList != null) {
            final StorePodcastListTask task = new StorePodcastListTask(podcatcher, null);
            task.setWriteAuthorization(true);
            task.execute(new ArrayList<>(podcastList));

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
                if (podcast.getUrl().equals(url))
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
        if (podcastList != null && url != null) {
            // Go try find the episode
            for (Podcast podcast : podcastList)
                for (Episode episode : podcast.getEpisodes())
                    if (episode.getMediaUrl().equals(url))
                        return episode;
        }

        return null;
    }

    /**
     * Find the episode object for given URL in the podcast given. Note that
     * this will only search episodes currently loaded.
     * 
     * @param episodeUrl URL of episode to look for.
     * @param podcastUrl URL of the podcast to look in.
     * @return The episode object, or <code>null</code> if not found.
     */
    public Episode findEpisodeForUrl(String episodeUrl, String podcastUrl) {
        // Make sure search only runs once the podcast list is actually
        // available.
        if (podcastList != null && episodeUrl != null) {
            // No podcast info given, use regular method
            if (podcastUrl == null)
                return findEpisodeForUrl(episodeUrl);
            else {
                final Podcast podcast = findPodcastForUrl(podcastUrl);

                // Go try find the episode
                if (podcast != null)
                    for (Episode episode : podcast.getEpisodes())
                        if (episode.getMediaUrl().equals(episodeUrl))
                            return episode;
            }
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
     * Check whether we are in a restricted profile and should filter out
     * podcasts (suggestions) with explicit content.
     * 
     * @return Whether the app is run by a restricted user with restricted
     *         access to podcasts and suggestions (<code>true</code>) .
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private boolean checkForRestrictedProfileBlocksExplicit() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            final Bundle restrictions = ((UserManager) podcatcher
                    .getSystemService(Context.USER_SERVICE))
                    .getApplicationRestrictions(podcatcher.getPackageName());

            return restrictions != null && restrictions
                    .getBoolean(GetRestrictionsReceiver.BLOCK_EXPLICIT_RESTRICTION_KEY);
        }
        else
            return false;
    }

    /**
     * Clear list. Add a small number of sample podcast to the list for testing.
     * Sort list.
     */
    private void putSamplePodcasts() {
        podcastList.clear();

        podcastList.add(new Podcast("This American Life",
                "http://feeds.thisamericanlife.org/talpodcast"));
        podcastList.add(new Podcast("Radiolab",
                "http://feeds.wnyc.org/radiolab"));
        podcastList.add(new Podcast("Linux' Outlaws",
                "http://feeds.feedburner.com/linuxoutlaws"));
        podcastList.add(new Podcast("GEO",
                "http://www.geo.de/GEOaudio/index.xml"));
        podcastList.add(new Podcast("SGU",
                "https://www.theskepticsguide.org/premium"));
        podcastList.add(new Podcast("Planet Money",
                "http://www.npr.org/rss/podcast.php?id=510289"));
        podcastList.add(new Podcast("Freakonomics",
                "http://feeds.feedburner.com/freakonomicsradio"));
        podcastList.add(new Podcast("neo",
                "http://www.zdf.de/ZDFmediathek/podcast/1446344?view=podcast"));
        podcastList.add(new Podcast("Little Letter for Gaelic Learners",
                "http://downloads.bbc.co.uk/podcasts/scotland/litirbheag/rss.xml"));

        Collections.sort(podcastList);
    }
}
