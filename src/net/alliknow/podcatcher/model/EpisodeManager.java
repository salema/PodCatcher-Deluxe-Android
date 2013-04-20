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

import static android.os.Environment.getExternalStoragePublicDirectory;
import static net.alliknow.podcatcher.Podcatcher.sanitizeAsFilename;

import android.app.DownloadManager;
import android.app.DownloadManager.Query;
import android.app.DownloadManager.Request;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import net.alliknow.podcatcher.EpisodeActivity;
import net.alliknow.podcatcher.EpisodeListActivity;
import net.alliknow.podcatcher.EpisodeListActivity.ContentMode;
import net.alliknow.podcatcher.PodcastActivity;
import net.alliknow.podcatcher.Podcatcher;
import net.alliknow.podcatcher.listeners.OnChangeEpisodeStateListener;
import net.alliknow.podcatcher.listeners.OnDownloadEpisodeListener;
import net.alliknow.podcatcher.listeners.OnLoadEpisodeMetadataListener;
import net.alliknow.podcatcher.model.tasks.StoreEpisodeMetadataTask;
import net.alliknow.podcatcher.model.types.Episode;
import net.alliknow.podcatcher.model.types.EpisodeMetadata;
import net.alliknow.podcatcher.model.types.Podcast;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Manager to handle episode specific activities.
 */
public class EpisodeManager implements OnLoadEpisodeMetadataListener {

    /** The file name to store episode metadata information under */
    public static final String METADATA_FILENAME = "episodes.xml";

    /** The single instance */
    private static EpisodeManager manager;
    /** The application itself */
    private Podcatcher podcatcher;

    /** The system download manager */
    private DownloadManager downloadManager;
    /** The metadata information held for episodes */
    private Map<URL, EpisodeMetadata> metadata;
    /** Flag to indicate whether metadata is dirty */
    private boolean metadataChanged;

    /** The call-back set for the complete download listeners */
    private Set<OnDownloadEpisodeListener> downloadListeners = new HashSet<OnDownloadEpisodeListener>();
    /** The call-back set for the episode state changed listeners */
    private Set<OnChangeEpisodeStateListener> stateListeners = new HashSet<OnChangeEpisodeStateListener>();

    /** The receiver we register for episode downloads */
    private BroadcastReceiver onDownloadComplete = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            // Get the download id that finished
            long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0);

            // Check if this was a download we care for
            for (EpisodeMetadata meta : metadata.values())
                if (meta.downloadId != null && meta.downloadId == downloadId) {
                    // Find download result information
                    Cursor result = downloadManager.query(new Query().setFilterById(downloadId));
                    // There should be information on the download
                    if (result.moveToFirst())
                        // Download was a success
                        if (DownloadManager.STATUS_SUCCESSFUL == result.getInt(
                                result.getColumnIndex(DownloadManager.COLUMN_STATUS))) {

                            // Get the path to the new local file and put in as
                            // metadata information
                            meta.filePath = result.getString(result
                                    .getColumnIndex(DownloadManager.COLUMN_LOCAL_FILENAME));

                            for (OnDownloadEpisodeListener listener : downloadListeners)
                                listener.onDownloadSuccess();
                        }
                        // Download failed
                        else {
                            downloadManager.remove(downloadId);

                            meta.downloadId = null;
                            meta.filePath = null;

                            for (OnDownloadEpisodeListener listener : downloadListeners)
                                listener.onDownloadFailed();

                            final int status = result.getInt(result
                                    .getColumnIndex(DownloadManager.COLUMN_STATUS));
                            final int reason = result.getInt(
                                    result.getColumnIndex(DownloadManager.COLUMN_REASON));
                            Log.e(getClass().getSimpleName(),
                                    "Download failed (status/reason): " + status + "/" + reason);
                        }

                    // Close cursor
                    result.close();
                    // Mark metadata record as dirty
                    metadataChanged = true;
                }
        };
    };

    /** The receiver we register for download selections */
    private BroadcastReceiver onDownloadClicked = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            // Get clicked ids
            long[] downloadIds = intent
                    .getLongArrayExtra(DownloadManager.EXTRA_NOTIFICATION_CLICK_DOWNLOAD_IDS);

            if (downloadIds != null && downloadIds.length > 0) {
                // Get the download id that was clicked (first if multiple)
                long downloadId = downloadIds[0];

                // Find download from metadata
                Iterator<Entry<URL, EpisodeMetadata>> iterator = metadata.entrySet().iterator();
                while (iterator.hasNext()) {
                    Entry<URL, EpisodeMetadata> entry = iterator.next();
                    // Only act if we care for this download
                    if (entry.getValue().downloadId != null
                            && entry.getValue().downloadId == downloadId) {

                        // Create the downloading episode
                        Episode download = entry.getValue().marshalEpisode(entry.getKey());
                        // Make the app switch to it.
                        podcatcher.startActivity(
                                new Intent(podcatcher.getApplicationContext(),
                                        PodcastActivity.class)
                                        .putExtra(EpisodeListActivity.MODE_KEY,
                                                ContentMode.SINGLE_PODCAST)
                                        .putExtra(EpisodeListActivity.PODCAST_URL_KEY,
                                                download.getPodcast().getUrl().toString())
                                        .putExtra(EpisodeActivity.EPISODE_URL_KEY,
                                                download.getMediaUrl().toString())
                                        .addFlags(
                                                Intent.FLAG_ACTIVITY_CLEAR_TOP |
                                                        Intent.FLAG_ACTIVITY_NEW_TASK
                                                        | Intent.FLAG_ACTIVITY_SINGLE_TOP));
                    }
                }
            }
        };
    };

    /**
     * Init the episode manager.
     * 
     * @param app The podcatcher application object (also a singleton).
     */
    private EpisodeManager(Podcatcher app) {
        // We use some of its method below, so we keep a reference to the
        // application object.
        this.podcatcher = app;

        // Get handle to the system download manager which does all the
        // downloading for us
        downloadManager = (DownloadManager)
                podcatcher.getSystemService(Context.DOWNLOAD_SERVICE);

        // Register as a receiver for download events so we are alerted when a
        // download completes (both successfully or failed)
        podcatcher.registerReceiver(onDownloadComplete,
                new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        // Register as a receiver for downloads selections so we are alerted
        // when a download is clicked in the DownloadManager UI
        podcatcher.registerReceiver(onDownloadClicked,
                new IntentFilter(DownloadManager.ACTION_NOTIFICATION_CLICKED));
    }

    /**
     * Get the singleton instance of the episode manager.
     * 
     * @param podcatcher Application handle.
     * @return The singleton instance.
     */
    public static EpisodeManager getInstance(Podcatcher podcatcher) {
        // If not done, create single instance
        if (manager == null)
            manager = new EpisodeManager(podcatcher);

        return manager;
    }

    /**
     * Get the singleton instance of the podcast manager.
     * 
     * @return The singleton instance.
     */
    public static EpisodeManager getInstance() {
        // We make sure in Application.onCreate() that this method is not called
        // unless the other one with the application instance actually set ran
        // to least once
        return manager;
    }

    @Override
    public void onEpisodeMetadataLoaded(Map<URL, EpisodeMetadata> metadata) {
        this.metadata = metadata;
        this.metadataChanged = false;
    }

    /**
     * Persist the manager's data to disk.
     */
    @SuppressWarnings("unchecked")
    public void saveState() {
        // Store cleaned matadata if dirty
        if (metadataChanged) {
            // Store a copy of the actual map, since there might come in changes
            // to the metadata while the task is running and that would lead to
            // a concurrent modification exception.
            new StoreEpisodeMetadataTask(podcatcher)
                    .execute(new HashMap<URL, EpisodeMetadata>(metadata));

            // Reset the flag, so the list will only be saved if changed again
            metadataChanged = false;
        }

    }

    /**
     * Initiate a download for the given episode. Will do nothing if the episode
     * is already downloaded or is currently downloading.
     * 
     * @param episode Episode to get.
     */
    public void download(Episode episode) {
        if (episode != null && !isDownloadingOrDownloaded(episode)) {
            // Find the podcast directory and the path to store episode under
            File podcastDir = getExternalStoragePublicDirectory(Environment.DIRECTORY_PODCASTS);
            String subPath = getSubPath(episode);
            // We need to put a download id. If the episode is already
            // downloaded (i.e. the file exists) and we somehow missed to catch
            // it, zero will work just fine.
            long id = 0;
            // Find or create the metadata information holder
            EpisodeMetadata meta = metadata.get(episode.getMediaUrl());
            if (meta == null)
                meta = new EpisodeMetadata();

            // Start download if the episode is not there
            if (!new File(podcastDir, subPath).exists()) {
                // Make sure podcast directory exists
                new File(podcastDir, sanitizeAsFilename(episode.getPodcast().getName())).mkdir();

                // Create the request
                Request download = new Request(Uri.parse(episode.getMediaUrl().toString()))
                        .setDestinationInExternalPublicDir(Environment.DIRECTORY_PODCASTS, subPath)
                        .setTitle(episode.getName())
                        .setDescription(episode.getPodcast().getName());

                // Start the download
                id = downloadManager.enqueue(download);
            } // The episode is already there, alert listeners
            else {
                meta.filePath = new File(podcastDir, subPath).getAbsolutePath();

                for (OnDownloadEpisodeListener listener : downloadListeners)
                    listener.onDownloadSuccess();
            }

            // Put metadata information
            meta.downloadId = id;
            meta.episodeName = episode.getName();
            meta.episodePubDate = episode.getPubDate();
            meta.episodeDescription = episode.getDescription();
            meta.podcastName = episode.getPodcast().getName();
            meta.podcastUrl = episode.getPodcast().getUrl().toString();
            metadata.put(episode.getMediaUrl(), meta);

            // Mark metadata record as dirty
            metadataChanged = true;
        }
    }

    /**
     * Cancel the download for given episode and delete all downloaded content.
     * 
     * @param episode Episode to delete download for.
     */
    public void deleteDownload(Episode episode) {
        if (episode != null && isDownloadingOrDownloaded(episode)) {
            // Find the metadata information holder
            EpisodeMetadata meta = metadata.get(episode.getMediaUrl());

            if (meta != null) {
                // This should delete the download and remove any information
                downloadManager.remove(meta.downloadId);

                meta.downloadId = null;
                meta.filePath = null;

                // Mark metadata record as dirty
                metadataChanged = true;
            }

            // Find the podcast directory and the path the episode is stored
            // under
            File podcastDir = getExternalStoragePublicDirectory(Environment.DIRECTORY_PODCASTS);
            String subPath = getSubPath(episode);
            // Make sure the file is deleted since this might not have taken
            // care of by remove() above
            new File(podcastDir, subPath).delete();
        }
    }

    /**
     * Check whether given episode is already downloaded and available on the
     * filesystem.
     * 
     * @param episode Episode to check for.
     * @return <code>true</code> if the episode is downloaded and available.
     */
    public boolean isDownloaded(Episode episode) {
        if (episode == null)
            return false;
        else {
            EpisodeMetadata meta = metadata.get(episode.getMediaUrl());

            return isDownloaded(meta);
        }
    }

    /**
     * Check whether given episode is currently downloading.
     * 
     * @param episode Episode to check for.
     * @return <code>true</code> if the episode is currently in the process of
     *         being downloaded.
     */
    public boolean isDownloading(Episode episode) {
        if (episode == null)
            return false;
        else {
            EpisodeMetadata meta = metadata.get(episode.getMediaUrl());

            return meta != null
                    && meta.downloadId != null
                    && meta.filePath == null;
        }
    }

    /**
     * Get the list of downloaded episodes. Returns only episodes fully
     * available locally. The episodes are sorted by date, latest first.
     * 
     * @return The list of downloaded episodes (might be empty, but not
     *         <code>null</code>)
     */
    public List<Episode> getDownloads() {
        // Create empty result list
        List<Episode> result = new ArrayList<Episode>();

        // Find downloads from metadata
        Iterator<Entry<URL, EpisodeMetadata>> iterator = metadata.entrySet().iterator();
        while (iterator.hasNext()) {
            Entry<URL, EpisodeMetadata> entry = iterator.next();

            // Find records for downloaded episodes
            if (isDownloaded(entry.getValue())) {
                // Create and add the downloaded episode
                Episode download = entry.getValue().marshalEpisode(entry.getKey());

                if (download != null)
                    result.add(download);
            }
        }

        // Sort and return the list
        Collections.sort(result);
        return result;
    }

    /**
     * Get the absolute, local path to a downloaded episode.
     * 
     * @param episode Episode to get local path for.
     * @return The complete local path to the downloaded episode or
     *         <code>null</code> if the episode is not available locally.
     * @see #isDownloaded(Episode)
     */
    public String getLocalPath(Episode episode) {
        if (episode == null)
            return null;
        else {
            EpisodeMetadata meta = metadata.get(episode.getMediaUrl());

            return meta == null ? null : meta.filePath;
        }
    }

    /**
     * Add a download listener.
     * 
     * @param listener Listener to add.
     * @see OnDownloadEpisodeListener
     */
    public void addDownloadListener(OnDownloadEpisodeListener listener) {
        downloadListeners.add(listener);
    }

    /**
     * Remove a download listener.
     * 
     * @param listener Listener to remove.
     * @see OnDownloadEpisodeListener
     */
    public void removeDownloadListener(OnDownloadEpisodeListener listener) {
        downloadListeners.remove(listener);
    }

    /**
     * Set the old/new state for an episode.
     * 
     * @param episode Episode to set state for (not <code>null</code>).
     * @param isOld State to set, give <code>null</code> to reset the value to
     *            the default.
     */
    public void setState(Episode episode, Boolean isOld) {
        if (episode != null && episode.getMediaUrl() != null) {
            EpisodeMetadata meta = metadata.get(episode.getMediaUrl());

            // Metadata not yet created
            if (meta == null && isOld != null && isOld) {
                meta = new EpisodeMetadata();
                meta.isOld = isOld;

                metadata.put(episode.getMediaUrl(), meta);
            } // Metadata available
            else if (meta != null)
                // We do not need to set this if false, simply remove the record
                meta.isOld = (isOld != null && isOld ? true : null);

            // Mark metadata record as dirty
            metadataChanged = true;

            // Alert listeners
            for (OnChangeEpisodeStateListener listener : stateListeners)
                listener.onStateChanged(episode);
        }
    }

    /**
     * Get the state information for an episode.
     * 
     * @param episode Episode to get old/new state for.
     * @return The state: <code>true</code> if the episode is marked old,
     *         <code>false</code> otherwise.
     */
    public boolean getState(Episode episode) {
        if (episode != null && episode.getMediaUrl() != null) {
            EpisodeMetadata meta = metadata.get(episode.getMediaUrl());

            if (meta != null && meta.isOld != null)
                return meta.isOld;
        }

        return false;
    }

    /**
     * Count the number of episodes not marked old for given podcast.
     * 
     * @param podcast Podcast to count for.
     * @return The number of new episode in the podcast.
     */
    public int getNewEpisodeCount(Podcast podcast) {
        int count = 0;

        if (podcast != null)
            for (Episode episode : podcast.getEpisodes())
                if (!getState(episode))
                    count++;

        return count;
    }

    /**
     * Add a state changed listener.
     * 
     * @param listener Listener to add.
     * @see OnChangeEpisodeStateListener
     */
    public void addStateChangedListener(OnChangeEpisodeStateListener listener) {
        stateListeners.add(listener);
    }

    /**
     * Remove a download listener.
     * 
     * @param listener Listener to remove.
     * @see OnChangeEpisodeStateListener
     */
    public void removeStateChangedListener(OnChangeEpisodeStateListener listener) {
        stateListeners.remove(listener);
    }

    /**
     * Set the resume time meta data field for an episode.
     * 
     * @param episode Episode to set resume time for.
     * @param at Time in millis from the start of the episode's media file to
     *            resume playback from. Give <code>null</code> to reset.
     */
    public void setResumeAt(Episode episode, Integer at) {
        if (episode != null && episode.getMediaUrl() != null) {
            EpisodeMetadata meta = metadata.get(episode.getMediaUrl());

            // Metadata not yet created
            if (meta == null && at != null) {
                meta = new EpisodeMetadata();
                meta.resumeAt = at;

                metadata.put(episode.getMediaUrl(), meta);
            } // Metadata available
            else if (meta != null)
                meta.resumeAt = at;

            // Mark metadata record as dirty
            metadataChanged = true;
        }
    }

    /**
     * Get the resume time meta data field for an episode.
     * 
     * @param episode Episode to get resume time for.
     * @return The resume time as millis from the start or zero if not set.
     */
    public int getResumeAt(Episode episode) {
        if (episode != null && episode.getMediaUrl() != null) {
            EpisodeMetadata meta = metadata.get(episode.getMediaUrl());

            if (meta != null && meta.resumeAt != null)
                return meta.resumeAt;
        }

        return 0;
    }

    /**
     * Find the path relative to the base directory the local episode file
     * should be located in.
     * 
     * @param episode Episode to create sub-path for.
     * @return The relative path
     */
    private String getSubPath(Episode episode) {
        // Extract file ending
        String remoteFile = episode.getMediaUrl().getPath();
        String fileEnding = remoteFile.substring(remoteFile.lastIndexOf('.'));

        return sanitizeAsFilename(episode.getPodcast().getName()) + File.separatorChar +
                sanitizeAsFilename(episode.getName()) + fileEnding;
    }

    private boolean isDownloaded(EpisodeMetadata meta) {
        return meta != null
                && meta.downloadId != null
                && meta.filePath != null
                && new File(meta.filePath).exists();

    }

    /**
     * Shortcut to check whether there is any download action going on with this
     * episode.
     * 
     * @param episode Episode to check for.
     * @return <code>true</code> iff the episode is downloading or already
     *         downloaded.
     */
    private boolean isDownloadingOrDownloaded(Episode episode) {
        return isDownloading(episode) || isDownloaded(episode);
    }
}
