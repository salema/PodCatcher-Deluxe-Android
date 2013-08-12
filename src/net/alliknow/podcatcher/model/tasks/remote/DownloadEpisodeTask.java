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

package net.alliknow.podcatcher.model.tasks.remote;

import static android.app.DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR;
import static android.app.DownloadManager.COLUMN_LOCAL_FILENAME;
import static android.app.DownloadManager.COLUMN_STATUS;
import static android.app.DownloadManager.COLUMN_TOTAL_SIZE_BYTES;
import static android.app.DownloadManager.STATUS_FAILED;
import static android.app.DownloadManager.STATUS_SUCCESSFUL;
import static net.alliknow.podcatcher.Podcatcher.USER_AGENT_KEY;
import static net.alliknow.podcatcher.Podcatcher.USER_AGENT_VALUE;
import static net.alliknow.podcatcher.Podcatcher.sanitizeAsFilename;

import android.app.DownloadManager;
import android.app.DownloadManager.Query;
import android.app.DownloadManager.Request;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;

import net.alliknow.podcatcher.Podcatcher;
import net.alliknow.podcatcher.SettingsActivity;
import net.alliknow.podcatcher.model.types.Episode;
import net.alliknow.podcatcher.preferences.DownloadFolderPreference;

import java.io.File;

/**
 * Async task that triggers the download of an episode. The task will be alive
 * and busy in its doInBackground() method as long as the download takes. It
 * will publish updates of the download's progress to the call-back attached.
 * Use a new task for each episode you want to download.
 */
public class DownloadEpisodeTask extends AsyncTask<Episode, Long, Void> {

    /**
     * The amount of time we wait (in ms) before checking on the download's
     * status again.
     */
    private static final long DOWNLOAD_STATUS_POLL_INTERVALL = 1000;

    /** The podcatcher app handle */
    private Podcatcher podcatcher;
    /** The listener (episode manager) we report to */
    private DownloadTaskListener listener;
    /** The system download manager */
    private DownloadManager downloadManager;

    /** The episode we are downloading */
    private Episode episode;
    /** The file the episode is downloaded to */
    private File episodeFile;
    /** The current percentage state of the download [0...100] */
    private int percentProgress;

    /** The interface to implement by the call-back for this task */
    public interface DownloadTaskListener {

        /**
         * Called on the listener when the episode is enqueued.
         * 
         * @param episode The episode now downloading.
         * @param id The download manager id for the download.
         */
        public void onEpisodeEnqueued(Episode episode, long id);

        /**
         * Called on the listener when the progress of the download for the
         * episode advanced.
         * 
         * @param episode The episode downloading.
         * @param percent The percent value currently downloaded [0...100].
         */
        public void onEpisodeDownloadProgressed(Episode episode, int percent);

        /**
         * Called on the listener if the episode requested to be downloaded is
         * already available on the device's storage.
         * 
         * @param episode The episode the task was started for.
         * @param episodeFile The local file.
         */
        public void onEpisodeDownloaded(Episode episode, File episodeFile);

        /**
         * Called on the listener when the download for the episode fails for
         * some reason.
         * 
         * @param episode The episode the download failed for.
         */
        public void onEpisodeDownloadFailed(Episode episode);
    }

    /**
     * Create a new task.
     * 
     * @param podcatcher
     * @param listener
     */
    public DownloadEpisodeTask(Podcatcher podcatcher, DownloadTaskListener listener) {
        this.podcatcher = podcatcher;
        this.listener = listener;

        // Get handle to the system download manager which does all the
        // downloading for us
        downloadManager = (DownloadManager)
                podcatcher.getSystemService(Context.DOWNLOAD_SERVICE);
    }

    @Override
    protected Void doInBackground(Episode... params) {
        this.episode = params[0];

        // Find the podcast directory and the path to store episode under
        final File podcastDir = new File(PreferenceManager.getDefaultSharedPreferences(podcatcher)
                .getString(SettingsActivity.DOWNLOAD_FOLDER_KEY,
                        DownloadFolderPreference.getDefaultDownloadFolder().getAbsolutePath()));
        final String subPath = getSubPath(episode);
        // The actual episode file
        final File localFile = new File(podcastDir, subPath);

        // The episode is already there, alert listener
        if (localFile.exists())
            this.episodeFile = localFile;
        // Start download because the episode is not there
        else {
            // Make sure podcast directory exists
            new File(podcastDir, sanitizeAsFilename(episode.getPodcast().getName())).mkdirs();

            // Create the request
            Request download = new Request(Uri.parse(episode.getMediaUrl().toString()))
                    .setDestinationUri(Uri.fromFile(new File(podcastDir, subPath)))
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
            try {
                final long id = downloadManager.enqueue(download);
                // We need to tell our listener about the download id, to
                // separate it from percentage done, put minus sign
                publishProgress(id > 0 ? id * -1 : id);

                // Start checking the download manager for status updates
                boolean finished = false;
                while (!isCancelled() && !finished) {
                    // Wait between polls
                    try {
                        Thread.sleep(DOWNLOAD_STATUS_POLL_INTERVALL);
                    } catch (InterruptedException e) {
                    }

                    // Find download information
                    final Cursor info = downloadManager.query(new Query().setFilterById(id));
                    // There should be information on the download
                    if (info.moveToFirst()) {
                        final int state = info.getInt(info.getColumnIndex(COLUMN_STATUS));
                        switch (state) {
                            case STATUS_SUCCESSFUL:
                                this.episodeFile = new File(info.getString(
                                        info.getColumnIndex(COLUMN_LOCAL_FILENAME)));

                                finished = true;
                                break;
                            case STATUS_FAILED:
                                downloadManager.remove(id);

                                cancel(false);
                                break;
                            default:
                                // Update progress
                                final long total = info.getLong(info
                                        .getColumnIndex(COLUMN_TOTAL_SIZE_BYTES));
                                final long progress = info.getLong(info
                                        .getColumnIndex(COLUMN_BYTES_DOWNLOADED_SO_FAR));

                                if (total > 0 && progress > 0 && total >= progress)
                                    publishProgress((long) (((float) progress / (float) total) * 100));
                        }
                    }
                    // Close cursor
                    info.close();
                }
            } catch (SecurityException se) {
                // This happens if the download manager has not the rights
                // to write to the selected downloads directory
                cancel(true);

                // TODO Find a better solution here, e.g. download the file
                // to some temp folder and move it the the wanted
                // destination when the download completed
            }
        }

        return null;
    }

    @Override
    protected void onProgressUpdate(Long... values) {
        long progress = values[0];

        // This is the download id (because it is < 0, see above)
        if (progress < 0)
            listener.onEpisodeEnqueued(episode, progress * -1);
        // This is the percentage of download done
        else if (progress > 0 && progress != percentProgress) {
            percentProgress = (int) progress;
            listener.onEpisodeDownloadProgressed(episode, percentProgress);
        }
    }

    @Override
    protected void onPostExecute(Void result) {
        // If the episodeFile member is set, the download was successful
        if (episodeFile != null)
            listener.onEpisodeDownloaded(episode, episodeFile);
        else
            onCancelled(result);
    }

    @Override
    protected void onCancelled(Void result) {
        listener.onEpisodeDownloadFailed(episode);
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
        final String remoteFile = episode.getMediaUrl().getPath();
        final String fileEnding = remoteFile.substring(remoteFile.lastIndexOf('.'));

        return sanitizeAsFilename(episode.getPodcast().getName()) + File.separatorChar +
                sanitizeAsFilename(episode.getName()) + fileEnding;
    }
}
