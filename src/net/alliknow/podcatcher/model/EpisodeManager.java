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

import static android.app.DownloadManager.ACTION_DOWNLOAD_COMPLETE;
import static android.app.DownloadManager.ACTION_NOTIFICATION_CLICKED;
import static android.app.DownloadManager.COLUMN_LOCAL_FILENAME;
import static android.app.DownloadManager.COLUMN_REASON;
import static android.app.DownloadManager.COLUMN_STATUS;
import static android.app.DownloadManager.EXTRA_DOWNLOAD_ID;
import static android.app.DownloadManager.EXTRA_NOTIFICATION_CLICK_DOWNLOAD_IDS;
import static android.app.DownloadManager.STATUS_SUCCESSFUL;
import static android.os.Environment.getExternalStoragePublicDirectory;
import static net.alliknow.podcatcher.Podcatcher.USER_AGENT_KEY;
import static net.alliknow.podcatcher.Podcatcher.USER_AGENT_VALUE;
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

import net.alliknow.podcatcher.BaseActivity.ContentMode;
import net.alliknow.podcatcher.EpisodeActivity;
import net.alliknow.podcatcher.EpisodeListActivity;
import net.alliknow.podcatcher.PodcastActivity;
import net.alliknow.podcatcher.Podcatcher;
import net.alliknow.podcatcher.listeners.OnChangeEpisodeStateListener;
import net.alliknow.podcatcher.listeners.OnChangePlaylistListener;
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
import java.util.TreeMap;
import java.util.concurrent.CountDownLatch;

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

    private CountDownLatch latch = new CountDownLatch(1);

    /** Helper to make playlist methods more efficient */
    private int playlistSize = -1;

    /** The system download manager */
    private DownloadManager downloadManager;
    /** The metadata information held for episodes */
    private Map<URL, EpisodeMetadata> metadata;
    /** Flag to indicate whether metadata is dirty */
    private boolean metadataChanged;

    /** The call-back set for the complete download listeners */
    private Set<OnDownloadEpisodeListener> downloadListeners = new HashSet<OnDownloadEpisodeListener>();
    /** The call-back set for the playlist listeners */
    private Set<OnChangePlaylistListener> playlistListeners = new HashSet<OnChangePlaylistListener>();
    /** The call-back set for the episode state changed listeners */
    private Set<OnChangeEpisodeStateListener> stateListeners = new HashSet<OnChangeEpisodeStateListener>();

    /** The receiver we register for episode downloads */
    private BroadcastReceiver onDownloadComplete = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            // Only react if this actually is a download complete event
            if (ACTION_DOWNLOAD_COMPLETE.equals(intent.getAction())) {
                // Get the download id that finished
                final long downloadId = intent.getLongExtra(EXTRA_DOWNLOAD_ID, -1);
                // Go do the actual work in the episode manager
                if (downloadId >= 0)
                    processDownloadComplete(downloadId);
            }
        }
    };

    /** The receiver we register for download selections */
    private BroadcastReceiver onDownloadClicked = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            // Only react if this actually is a download clicked event
            if (ACTION_NOTIFICATION_CLICKED.equals(intent.getAction())) {
                // Get clicked ids
                final long[] downloadIds = intent
                        .getLongArrayExtra(EXTRA_NOTIFICATION_CLICK_DOWNLOAD_IDS);

                if (downloadIds != null && downloadIds.length > 0) {
                    // Get the download id that was clicked (first if multiple)
                    final long downloadId = downloadIds[0];
                    // Go do the actual work in the episode manager
                    if (downloadId >= 0)
                        processDownloadClicked(downloadId);
                }
            }
        }
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

        // Here we need to release all threads (AsyncTasks) waiting for the
        // episode metadata to finally load
        latch.countDown();
    }

    /**
     * This blocks the calling thread until the episode metadata has become
     * available on the application's start-up. Once the metadata is read, the
     * method return immediately.
     * 
     * @throws InterruptedException When the thread is interrupted while
     *             waiting.
     */
    public void blockUntilEpisodeMetadataIsLoaded() throws InterruptedException {
        latch.await();
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
            if (meta == null) {
                meta = new EpisodeMetadata();
                metadata.put(episode.getMediaUrl(), meta);
            }

            // Start download if the episode is not there
            if (!new File(podcastDir, subPath).exists()) {
                // Make sure podcast directory exists
                new File(podcastDir, sanitizeAsFilename(episode.getPodcast().getName())).mkdirs();

                // Create the request
                Request download = new Request(Uri.parse(episode.getMediaUrl().toString()))
                        .setDestinationInExternalPublicDir(Environment.DIRECTORY_PODCASTS, subPath)
                        .setTitle(episode.getName())
                        .setDescription(episode.getPodcast().getName())
                        // We overwrite the AndroidDownloadManager user agent
                        // string here because there are servers out there (e.g.
                        // ORF.at) that apparently block downloads based on this
                        // information
                        .addRequestHeader(USER_AGENT_KEY, USER_AGENT_VALUE)
                        // Make sure our download dont end up in the http cache
                        .addRequestHeader("Cache-Control", "no-store");

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
            putAdditionalEpisodeInformation(episode, meta);

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
     * Shortcut to check whether there is any download action going on with this
     * episode.
     * 
     * @param episode Episode to check for.
     * @return <code>true</code> iff the episode is downloading or already
     *         downloaded.
     */
    public boolean isDownloadingOrDownloaded(Episode episode) {
        return isDownloading(episode) || isDownloaded(episode);
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
     * @return The current playlist. Might be empty but not <code>null</code>.
     */
    public List<Episode> getPlaylist() {
        // The resulting playlist
        TreeMap<Integer, Episode> playlist = new TreeMap<Integer, Episode>();

        // Find playlist entries from metadata
        Iterator<Entry<URL, EpisodeMetadata>> iterator = metadata.entrySet().iterator();
        while (iterator.hasNext()) {
            Entry<URL, EpisodeMetadata> entry = iterator.next();

            // Find records for playlist entries
            if (entry.getValue().playlistPosition != null) {
                // Create and add the downloaded episode
                Episode playlistEntry = entry.getValue().marshalEpisode(entry.getKey());
                playlist.put(entry.getValue().playlistPosition, playlistEntry);
            }
        }

        // Since we have the playlist here, we could just as well set this and
        // make the other methods return faster
        this.playlistSize = playlist.size();

        return new ArrayList<Episode>(playlist.values());
    }

    /**
     * @return Whether the current playlist has any entries.
     */
    public boolean isPlaylistEmpty() {
        if (playlistSize == -1 && metadata != null) {
            playlistSize = 0;

            for (EpisodeMetadata meta : metadata.values())
                if (meta.playlistPosition != null)
                    playlistSize++;
        }

        return playlistSize <= 0;
    }

    /**
     * Check whether a specific episode already exists in the playlist.
     * 
     * @param episode Episode to check for.
     * @return <code>true</code> iff present in playlist.
     */
    public boolean isInPlaylist(Episode episode) {
        return getPlaylistPosition(episode) != -1;
    }

    /**
     * Find the position of the given episode in the playlist.
     * 
     * @param episode Episode to find.
     * @return The position of the episode (staring at 0) or -1 if not present.
     */
    public int getPlaylistPosition(Episode episode) {
        int result = -1;

        if (episode != null) {
            // Find metadata information holder
            EpisodeMetadata meta = metadata.get(episode.getMediaUrl());
            if (meta != null && meta.playlistPosition != null)
                result = meta.playlistPosition;
        }

        return result;
    }

    /**
     * Add an episode to the playlist. The episode will be appended to the end
     * of the list.
     * 
     * @param episode The episode to add.
     */
    public void appendToPlaylist(Episode episode) {
        if (episode != null) {
            // Only append the episode if it is not already part of the playlist
            final List<Episode> playlist = getPlaylist();
            if (!playlist.contains(episode)) {
                final int position = playlist.size();

                // Find or create the metadata information holder
                EpisodeMetadata meta = metadata.get(episode.getMediaUrl());
                if (meta == null) {
                    meta = new EpisodeMetadata();
                    metadata.put(episode.getMediaUrl(), meta);
                }

                // Put metadata information
                meta.playlistPosition = position;
                putAdditionalEpisodeInformation(episode, meta);

                // Increment counter
                if (playlistSize != -1)
                    playlistSize++;

                // Alert listeners
                for (OnChangePlaylistListener listener : playlistListeners)
                    listener.onPlaylistChanged();

                // Mark metadata record as dirty
                metadataChanged = true;
            }
        }
    }

    /**
     * Delete given episode off the playlist.
     * 
     * @param episode Episode to pop.
     */
    public void removeFromPlaylist(Episode episode) {
        if (episode != null) {
            // Find the metadata information holder
            EpisodeMetadata meta = metadata.get(episode.getMediaUrl());
            if (meta != null && meta.playlistPosition != null) {
                // Update the playlist positions for all entries beyond the one
                // we are removing
                Iterator<Entry<URL, EpisodeMetadata>> iterator = metadata.entrySet().iterator();
                while (iterator.hasNext()) {
                    EpisodeMetadata other = iterator.next().getValue();

                    // Find records for playlist entries
                    if (other.playlistPosition != null
                            && other.playlistPosition > meta.playlistPosition)
                        other.playlistPosition--;
                }

                // Reset the playlist position for given episode
                meta.playlistPosition = null;

                // Decrement counter
                if (playlistSize != -1)
                    playlistSize--;

                // Alert listeners
                for (OnChangePlaylistListener listener : playlistListeners)
                    listener.onPlaylistChanged();

                // Mark metadata record as dirty
                metadataChanged = true;
            }
        }
    }

    /**
     * Add a playlist listener.
     * 
     * @param listener Listener to add.
     * @see OnChangePlaylistListener
     */
    public void addPlaylistListener(OnChangePlaylistListener listener) {
        playlistListeners.add(listener);
    }

    /**
     * Remove a playlist listener.
     * 
     * @param listener Listener to remove.
     * @see OnChangePlaylistListener
     */
    public void removePlaylistListener(OnChangePlaylistListener listener) {
        playlistListeners.remove(listener);
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

    private void putAdditionalEpisodeInformation(Episode episode, EpisodeMetadata meta) {
        meta.episodeName = episode.getName();
        meta.episodePubDate = episode.getPubDate();
        meta.episodeDescription = episode.getDescription();
        meta.podcastName = episode.getPodcast().getName();
        meta.podcastUrl = episode.getPodcast().getUrl().toString();
    }

    private void processDownloadComplete(long downloadId) {
        // TODO we might face some need for synchronization / concurrent
        // modification here?
        // Check if this was a download we care for
        for (EpisodeMetadata meta : metadata.values())
            if (meta.downloadId != null && meta.downloadId == downloadId) {
                // Find download result information
                Cursor result = downloadManager.query(new Query().setFilterById(downloadId));
                // There should be information on the download
                if (result.moveToFirst())
                    // Download was a success
                    if (STATUS_SUCCESSFUL == result.getInt(result.getColumnIndex(COLUMN_STATUS))) {
                        // Get the path to the new local file and put in
                        // as metadata information
                        meta.filePath = result.getString(result
                                .getColumnIndex(COLUMN_LOCAL_FILENAME));

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

                        final int status = result.getInt(result.getColumnIndex(COLUMN_STATUS));
                        final int reason = result.getInt(result.getColumnIndex(COLUMN_REASON));
                        Log.e(getClass().getSimpleName(), "Download failed (status/reason): "
                                + status + "/" + reason);
                    }

                // Close cursor
                result.close();
                // Mark metadata record as dirty
                metadataChanged = true;
            }
    }

    private void processDownloadClicked(long downloadId) {
        // Find download from metadata
        Iterator<Entry<URL, EpisodeMetadata>> iterator = metadata.entrySet().iterator();
        while (iterator.hasNext()) {
            final Entry<URL, EpisodeMetadata> entry = iterator.next();
            final EpisodeMetadata data = entry.getValue();

            // Only act if we care for this download
            if (data.downloadId != null && data.downloadId == downloadId) {
                // Create the downloading episode
                Episode download = entry.getValue().marshalEpisode(entry.getKey());
                if (download != null) {
                    Intent intent = new Intent(podcatcher.getApplicationContext(),
                            PodcastActivity.class)
                            .putExtra(EpisodeListActivity.MODE_KEY, ContentMode.SINGLE_PODCAST)
                            .putExtra(EpisodeListActivity.PODCAST_URL_KEY,
                                    download.getPodcast().getUrl().toString())
                            .putExtra(EpisodeActivity.EPISODE_URL_KEY,
                                    download.getMediaUrl().toString())
                            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
                                    Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);

                    // Make the app switch to it.
                    podcatcher.startActivity(intent);
                }
            }
        }
    }
}
