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
import net.alliknow.podcatcher.model.types.Episode;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Manager to handle episode specific activities.
 */
public class EpisodeManager extends BroadcastReceiver {

    /** The single instance */
    private static EpisodeManager manager;
    /** The application itself */
    private Podcatcher podcatcher;

    /** The system download manager */
    private DownloadManager downloadManager;

    private Map<Episode, EpisodeMetadata> metadata = new HashMap<Episode, EpisodeMetadata>();

    /** Characters not allowed in filenames */
    private static final String RESERVED_CHARS = "|\\?*<\":>+[]/'";

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

        // Register as a receiver for download event so we are alerted when a
        // download completes
        podcatcher.registerReceiver(this,
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

    public void download(Episode episode) {
        if (!(isDownloading(episode) || isDownloaded(episode))) {
            String path = createPath(episode);

            Request download = new Request(Uri.parse(episode.getMediaUrl().toString()))
                    .setDestinationInExternalPublicDir(Environment.DIRECTORY_PODCASTS, path)
                    .setTitle(episode.getName())
                    .setDescription(episode.getPodcast().getName());

            long id = downloadManager.enqueue(download);

            EpisodeMetadata meta = new EpisodeMetadata();
            meta.downloadId = id;
            metadata.put(episode, meta);

            Log.i(getClass().getSimpleName(), "Start download for episode: " + episode);
            Log.i(getClass().getSimpleName(), "Download id: " + id);
            Log.i(getClass().getSimpleName(), "Download path: " + path);
        }
    }

    public void deleteDownload(Episode episode) {
        if (isDownloading(episode) || isDownloaded(episode)) {
            EpisodeMetadata meta = metadata.get(episode);

            if (meta != null) {
                downloadManager.remove(meta.downloadId);

                Log.i(getClass().getSimpleName(), "Deleted download for episode: " + episode);
                Log.i(getClass().getSimpleName(), "Download id: " + meta.downloadId);
                Log.i(getClass().getSimpleName(), "Download path: " + meta.filePath);

                meta.downloadId = null;
            }
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(getClass().getSimpleName(), "Action: " + intent.getAction());

        long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0);

        // Check if this was a download we care for
        for (EpisodeMetadata meta : metadata.values())
            if (meta.downloadId == downloadId) {

                Query query = new Query();
                query.setFilterById(downloadId);
                Cursor c = downloadManager.query(query);

                c.moveToFirst();

                Log.i(getClass().getName(), "COLUMN_ID: " +
                        c.getLong(c.getColumnIndex(DownloadManager.COLUMN_ID)));
                Log.i(getClass().getName(), "COLUMN_BYTES_DOWNLOADED_SO_FAR: " +
                        c.getLong(c.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)));
                Log.i(getClass().getName(), "COLUMN_LAST_MODIFIED_TIMESTAMP: " +
                        c.getLong(c.getColumnIndex(DownloadManager.COLUMN_LAST_MODIFIED_TIMESTAMP)));
                Log.i(getClass().getName(), "COLUMN_LOCAL_URI: " +
                        c.getString(c.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)));
                Log.i(getClass().getName(), "COLUMN_STATUS: " +
                        c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS)));
                Log.i(getClass().getName(), "COLUMN_REASON: " +
                        c.getInt(c.getColumnIndex(DownloadManager.COLUMN_REASON)));

                if (DownloadManager.STATUS_SUCCESSFUL == c.getInt(c
                        .getColumnIndex(DownloadManager.COLUMN_STATUS))) {

                    Log.i(getClass().getSimpleName(), "Completed download for episode");
                    Log.i(getClass().getSimpleName(), "Download id: " + meta.downloadId);
                    Log.i(getClass().getSimpleName(),
                            "Local path: " + c.getString(
                                    c.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)));

                    meta.filePath = c.getString(c.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));
                }
                else {
                    Log.i(getClass().getSimpleName(), "Failed to download episode");
                    Log.i(getClass().getSimpleName(), "Download id: " + meta.downloadId);
                }
            }
    }

    public boolean isDownloaded(Episode episode) {
        EpisodeMetadata meta = metadata.get(episode);

        return meta != null &&
                meta.downloadId != null &&
                meta.filePath != null;
    }

    public boolean isDownloading(Episode episode) {
        EpisodeMetadata meta = metadata.get(episode);

        return meta != null &&
                meta.downloadId != null &&
                meta.filePath == null;
    }

    private String createPath(Episode episode) {
        // Make sure podcast directory exisits
        new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PODCASTS),
                sanitizeAsFilename(episode.getPodcast().getName())).mkdir();

        // Extract file ending
        String remoteFile = episode.getMediaUrl().getFile();
        String fileEnding = remoteFile.substring(remoteFile.lastIndexOf('.'));

        return sanitizeAsFilename(episode.getPodcast().getName()) + "/" +
                sanitizeAsFilename(episode.getName()) + fileEnding;
    }

    private String sanitizeAsFilename(String name) {
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < name.length(); i++)
            if (RESERVED_CHARS.indexOf(name.charAt(i)) == -1)
                builder.append(name.charAt(i));

        return builder.toString();
    }

    private class EpisodeMetadata {
        private Long downloadId;
        private String filePath;
    }
}
