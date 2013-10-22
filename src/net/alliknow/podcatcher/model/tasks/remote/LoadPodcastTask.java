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

import android.util.Log;

import net.alliknow.podcatcher.listeners.OnLoadPodcastListener;
import net.alliknow.podcatcher.model.EpisodeManager;
import net.alliknow.podcatcher.model.types.Podcast;
import net.alliknow.podcatcher.model.types.Progress;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.ByteArrayInputStream;

/**
 * Loads a podcast's RSS file from the server and parses its contents
 * asynchronously.
 * <p>
 * <b>Usage:</b> Implement the {@link OnLoadPodcastListener} interface and give
 * it to the task's constructor to be alerted on completion, progress, or
 * failure. The downloaded file will be used as the podcast's content via
 * {@link Podcast#parse(XmlPullParser)}, use the podcast object given (and
 * returned via callbacks) to access it.
 * </p>
 * <p>
 * <b>Authorization:</b> The task will send the credentials returned by
 * {@link Podcast#getAuthorization()} when requesting the file from the server.
 * If not present or wrong, the task fails and
 * {@link OnLoadPodcastListener#onAuthorizationRequired(Podcast)} will be
 * called. ({@link OnLoadPodcastListener#onPodcastLoadFailed(Podcast)} will
 * <i>not</i> be called.)
 * </p>
 */
public class LoadPodcastTask extends LoadRemoteFileTask<Podcast, Void> {

    /** Maximum byte size for the RSS file to load */
    public static final int MAX_RSS_FILE_SIZE = 2000000;

    /** Call back */
    private OnLoadPodcastListener listener;

    /** Podcast currently loading */
    private Podcast podcast;

    /**
     * Create new task.
     * 
     * @param listener Callback to be alerted on progress and completion.
     */
    public LoadPodcastTask(OnLoadPodcastListener listener) {
        this.listener = listener;
        // We disable the load limit for the podcast feeds because there are
        // huge feeds out there and user's really do not understand why they are
        // unable to access them.
        // this.loadLimit = MAX_RSS_FILE_SIZE;
    }

    @Override
    protected Void doInBackground(Podcast... podcasts) {
        this.podcast = podcasts[0];

        try {
            // 1. Load the file from the Internet
            publishProgress(Progress.CONNECT);

            // Set auth
            this.authorization = podcast.getAuthorization();
            // ... and go get the file
            byte[] podcastRssFile = loadFile(podcast.getUrl());

            if (isCancelled())
                return null;
            else
                publishProgress(Progress.PARSE);

            // 2. Create the parser to use
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            XmlPullParser parser = factory.newPullParser();
            parser.setInput(new ByteArrayInputStream(podcastRssFile), null);

            // 3. Parse as podcast content
            if (!isCancelled())
                podcast.parse(parser);

            // 4. We need to wait here and make sure the episode metadata is
            // available before we return
            EpisodeManager.getInstance().blockUntilEpisodeMetadataIsLoaded();
        } catch (Throwable t) {
            Log.w(getClass().getSimpleName(), "Load failed for podcast \"" + podcast + "\"", t);

            cancel(true);
        } finally {
            publishProgress(Progress.DONE);
        }

        return null;
    }

    @Override
    protected void onProgressUpdate(Progress... progress) {
        if (listener != null)
            listener.onPodcastLoadProgress(podcast, progress[0]);
        else if (listener == null)
            Log.w(getClass().getSimpleName(), "Podcast progress update, but no listener attached");
    }

    @Override
    protected void onPostExecute(Void nothing) {
        // Podcast was loaded
        if (listener != null)
            listener.onPodcastLoaded(podcast);
        else
            Log.w(getClass().getSimpleName(), "Podcast loaded, but no listener attached");
    }

    @Override
    protected void onCancelled(Void nothing) {
        // Background task failed to complete
        if (listener != null)
            if (needsAuthorization)
                listener.onAuthorizationRequired(podcast);
            else
                listener.onPodcastLoadFailed(podcast);
        else
            Log.w(getClass().getSimpleName(), "Podcast failed to load, but no listener attached");
    }
}
