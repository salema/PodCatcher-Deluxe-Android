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

import static android.app.DownloadManager.ACTION_NOTIFICATION_CLICKED;
import static android.app.DownloadManager.EXTRA_NOTIFICATION_CLICK_DOWNLOAD_IDS;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;

import net.alliknow.podcatcher.BaseActivity.ContentMode;
import net.alliknow.podcatcher.EpisodeActivity;
import net.alliknow.podcatcher.EpisodeListActivity;
import net.alliknow.podcatcher.PodcastActivity;
import net.alliknow.podcatcher.Podcatcher;
import net.alliknow.podcatcher.listeners.OnDownloadEpisodeListener;
import net.alliknow.podcatcher.listeners.OnLoadDownloadsListener;
import net.alliknow.podcatcher.model.tasks.LoadDownloadsTask;
import net.alliknow.podcatcher.model.tasks.remote.DownloadEpisodeTask;
import net.alliknow.podcatcher.model.tasks.remote.DownloadEpisodeTask.DownloadTaskListener;
import net.alliknow.podcatcher.model.types.Episode;
import net.alliknow.podcatcher.model.types.EpisodeMetadata;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

/**
 * This class is the part of the episode manager stack that handles the download
 * and deletion of episodes. It uses the Android {@link DownloadManager} API to
 * carry out the downloads.
 * 
 * @see EpisodeManager
 */
public abstract class EpisodeDownloadManager extends EpisodeBaseManager implements
        DownloadTaskListener {

    /** The current number of downloaded episodes we know of */
    protected int downloadsSize = -1;

    /** The call-back set for the complete download listeners */
    private Set<OnDownloadEpisodeListener> downloadListeners = new HashSet<OnDownloadEpisodeListener>();

    /**
     * Init the download episode manager.
     * 
     * @param app The podcatcher application object (also a singleton).
     */
    protected EpisodeDownloadManager(Podcatcher app) {
        super(app);

        // Register as a receiver for downloads selections so we are alerted
        // when a download is clicked in the DownloadManager UI
        podcatcher.registerReceiver(onDownloadClicked,
                new IntentFilter(DownloadManager.ACTION_NOTIFICATION_CLICKED));
    }

    /**
     * Initiate a download for the given episode. Will do nothing if the episode
     * is already downloaded or is currently downloading.
     * 
     * @param episode Episode to get.
     */
    public void download(Episode episode) {
        if (episode != null && metadata != null && !isDownloadingOrDownloaded(episode)) {
            // Find or create the metadata information holder
            EpisodeMetadata meta = metadata.get(episode.getMediaUrl());
            if (meta == null) {
                meta = new EpisodeMetadata();
                metadata.put(episode.getMediaUrl(), meta);
            }

            // We need to put a download id. If the episode is already
            // downloaded (i.e. the file exists) and we somehow missed to catch
            // it, zero will work just fine.
            meta.downloadId = 0l;
            putAdditionalEpisodeInformation(episode, meta);

            // Mark metadata record as dirty
            metadataChanged = true;

            // Start the actual download
            new DownloadEpisodeTask(podcatcher, this)
                    .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, episode);
        }
    }

    @Override
    public void onEpisodeEnqueued(Episode episode, long id) {
        // Find the metadata record for the episode
        final EpisodeMetadata meta = metadata.get(episode.getMediaUrl());
        if (meta != null) {
            meta.downloadId = id;

            // Mark metadata record as dirty
            metadataChanged = true;
        }
    }

    @Override
    public void onEpisodeDownloadProgressed(Episode episode, int percent) {
        // pass, we are not using this so far
        // Log.i("DOWNLOAD_PROGRESS", percent + " of " + episode);
    }

    @Override
    public void onEpisodeDownloaded(Episode episode, File episodeFile) {
        // Find the metadata record for the episode
        final EpisodeMetadata meta = metadata.get(episode.getMediaUrl());
        if (meta != null) {
            meta.filePath = episodeFile.getAbsolutePath();

            for (OnDownloadEpisodeListener listener : downloadListeners)
                listener.onDownloadSuccess();

            // Update counter
            if (downloadsSize != -1)
                downloadsSize++;

            // Mark metadata record as dirty
            metadataChanged = true;
        }
    }

    @Override
    public void onEpisodeDownloadFailed(Episode episode) {
        // Find the metadata record for the episode
        final EpisodeMetadata meta = metadata.get(episode.getMediaUrl());
        if (meta != null) {
            meta.downloadId = null;
            meta.filePath = null;

            for (OnDownloadEpisodeListener listener : downloadListeners)
                listener.onDownloadFailed();

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
        if (episode != null && metadata != null && isDownloadingOrDownloaded(episode)) {
            // Find the metadata information holder
            final EpisodeMetadata meta = metadata.get(episode.getMediaUrl());
            if (meta != null) {
                final long downloadId = meta.downloadId;
                // Go async when accessing download manager
                new Thread() {
                    @Override
                    public void run() {
                        // This should delete the download and remove all
                        // information
                        ((DownloadManager) podcatcher.getSystemService(Context.DOWNLOAD_SERVICE))
                                .remove(downloadId);
                    };
                }.start();

                // Make sure the file is deleted since this might not have taken
                // care of by DownloadManager.remove() above
                if (meta.filePath != null)
                    new File(meta.filePath).delete();

                meta.downloadId = null;
                meta.filePath = null;

                // Alert listeners
                for (OnDownloadEpisodeListener listener : downloadListeners)
                    listener.onDownloadDeleted();

                // Mark metadata record as dirty
                metadataChanged = true;
                // Decrement counter
                if (downloadsSize != -1)
                    downloadsSize--;
            }
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
        if (episode != null && metadata != null)
            return isDownloaded(metadata.get(episode.getMediaUrl()));
        else
            return false;
    }

    /**
     * Check whether given episode is currently downloading.
     * 
     * @param episode Episode to check for.
     * @return <code>true</code> if the episode is currently in the process of
     *         being downloaded.
     */
    public boolean isDownloading(Episode episode) {
        if (episode != null && metadata != null) {
            final EpisodeMetadata meta = metadata.get(episode.getMediaUrl());

            return meta != null
                    && meta.downloadId != null
                    && meta.filePath == null;
        }
        else
            return false;
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
     * available locally. The episodes are sorted by date, latest first. Only
     * call this if you are sure the metadata is already available, if in doubt
     * use {@link LoadDownloadsTask}.
     * 
     * @return The list of downloaded episodes (might be empty, but not
     *         <code>null</code>).
     * @see LoadDownloadsTask
     * @see OnLoadDownloadsListener
     */
    public List<Episode> getDownloads() {
        // Create empty result list
        List<Episode> result = new ArrayList<Episode>();

        // This is only possible if the metadata is available
        if (metadata != null) {
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

            // Since we have the downloads list here, we could just as well set
            // this and make the other methods return faster
            this.downloadsSize = result.size();
        }

        // Sort and return the list
        Collections.sort(result);
        return result;
    }

    /**
     * @return The number of downloaded episodes.
     */
    public int getDownloadsSize() {
        if (downloadsSize == -1 && metadata != null)
            initDownloadsCounter();

        return downloadsSize == -1 ? 0 : downloadsSize;
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
        if (episode != null && metadata != null) {
            final EpisodeMetadata meta = metadata.get(episode.getMediaUrl());

            return meta == null ? null : meta.filePath;
        }
        else
            return null;
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

    private void initDownloadsCounter() {
        this.downloadsSize = 0;

        for (EpisodeMetadata meta : metadata.values())
            if (isDownloaded(meta))
                downloadsSize++;
    }

    private boolean isDownloaded(EpisodeMetadata meta) {
        return meta != null
                && meta.downloadId != null
                && meta.filePath != null
                && new File(meta.filePath).exists();
    }

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

    private void processDownloadClicked(long downloadId) {
        // Nothing we can do if the meta data is not available
        if (metadata != null) {
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
                                .addFlags(
                                        Intent.FLAG_ACTIVITY_CLEAR_TOP |
                                                Intent.FLAG_ACTIVITY_NEW_TASK
                                                | Intent.FLAG_ACTIVITY_SINGLE_TOP);

                        // Make the app switch to it.
                        podcatcher.startActivity(intent);
                    }
                }
            }
        }
    }
}
