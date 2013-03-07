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

import net.alliknow.podcatcher.Podcatcher;
import net.alliknow.podcatcher.listeners.OnLoadEpisodeMetadataListener;
import net.alliknow.podcatcher.model.tasks.StoreEpisodeMetadataTask;
import net.alliknow.podcatcher.model.types.Episode;
import net.alliknow.podcatcher.model.types.EpisodeMetadata;

import java.io.File;
import java.net.URL;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

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

                            // TODO Alert listeners

                            Log.i(getClass().getSimpleName(), "Completed download for episode");
                            Log.i(getClass().getSimpleName(), "Download id: " + meta.downloadId);
                            Log.i(getClass().getSimpleName(), "Local path: " + meta.filePath);
                        }
                        // Download failed
                        else {
                            downloadManager.remove(downloadId);

                            meta.downloadId = null;
                            meta.filePath = null;

                            // TODO Alert listeners

                            Log.i(getClass().getSimpleName(), "Failed to download episode");
                            Log.i(getClass().getSimpleName(), "Download id: " + downloadId);
                        }

                    // Close cursor
                    result.close();
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

        // TODO house keeping? what about download that finish/were deleted
        // while the app was closed? How to avoid many, many old entries that do
        // not change anymore? (Maybe this should be done by the load task to
        // keep it off the main thread?)
    }

    /**
     * Persist the manager's data to disk.
     */
    @SuppressWarnings("unchecked")
    public void saveState() {
        // Do house keeping and remove all metadata instances without data
        Iterator<Entry<URL, EpisodeMetadata>> iterator = metadata.entrySet().iterator();

        while (iterator.hasNext()) {
            Entry<URL, EpisodeMetadata> entry = iterator.next();

            if (!entry.getValue().hasData())
                iterator.remove();
        }

        // Store cleaned record
        new StoreEpisodeMetadataTask(podcatcher).execute(metadata);
    }

    /**
     * Initiate a download for the given episode. Will do nothing if the episode
     * is already downloaded or is currently downloading.
     * 
     * @param episode Episode to get.
     */
    public void download(Episode episode) {
        if (!(isDownloading(episode) || isDownloaded(episode))) {
            // Make sure podcast directory exists
            new File(getExternalStoragePublicDirectory(Environment.DIRECTORY_PODCASTS),
                    sanitizeAsFilename(episode.getPodcast().getName())).mkdir();

            String subPath = getSubPath(episode);

            Request download = new Request(Uri.parse(episode.getMediaUrl().toString()))
                    .setDestinationInExternalPublicDir(Environment.DIRECTORY_PODCASTS, subPath)
                    .setTitle(episode.getName())
                    .setDescription(episode.getPodcast().getName());

            long id = downloadManager.enqueue(download);

            EpisodeMetadata meta = new EpisodeMetadata();
            meta.downloadId = id;
            metadata.put(episode.getMediaUrl(), meta);

            Log.i(getClass().getSimpleName(), "Start download for episode: " + episode);
            Log.i(getClass().getSimpleName(), "Download id: " + id);
            Log.i(getClass().getSimpleName(), "Download path: " + subPath);
        }
    }

    /**
     * Cancel the download for given episode and delete all downloaded content.
     * 
     * @param episode Episode to delete download for.
     */
    public void deleteDownload(Episode episode) {
        if (isDownloading(episode) || isDownloaded(episode)) {
            EpisodeMetadata meta = metadata.get(episode.getMediaUrl());

            if (meta != null) {
                downloadManager.remove(meta.downloadId);

                Log.i(getClass().getSimpleName(), "Deleted download for episode: " + episode);
                Log.i(getClass().getSimpleName(), "Download id: " + meta.downloadId);
                Log.i(getClass().getSimpleName(), "Download path: " + meta.filePath);

                meta.downloadId = null;
                meta.filePath = null;
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
        EpisodeMetadata meta = metadata.get(episode.getMediaUrl());

        return meta != null
                && meta.downloadId != null
                && meta.filePath != null
                && new File(meta.filePath).exists();
    }

    /**
     * Check whether given episode is currently downloading.
     * 
     * @param episode Episode to check for.
     * @return <code>true</code> if the episode is currently in the process of
     *         being downloaded.
     */
    public boolean isDownloading(Episode episode) {
        EpisodeMetadata meta = metadata.get(episode.getMediaUrl());

        return meta != null
                && meta.downloadId != null
                && meta.filePath == null;
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
        EpisodeMetadata meta = metadata.get(episode.getMediaUrl());

        return meta == null ? null : meta.filePath;
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
}
