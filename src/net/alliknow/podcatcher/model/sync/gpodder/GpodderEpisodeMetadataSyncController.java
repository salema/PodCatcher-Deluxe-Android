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

package net.alliknow.podcatcher.model.sync.gpodder;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.dragontek.mygpoclient.api.EpisodeAction;
import com.dragontek.mygpoclient.api.EpisodeActionChanges;

import net.alliknow.podcatcher.model.types.Episode;
import net.alliknow.podcatcher.model.types.EpisodeMetadata;

import org.apache.http.auth.AuthenticationException;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.TimeZone;

/**
 * An episode metadata sync controller for the gpodder.net service. This
 * operates by keeping track of the local changes to episodes and publishing
 * everything once {@link #syncEpisodeMetadata()} is called.
 */
abstract class GpodderEpisodeMetadataSyncController extends GpodderPodcastListSyncController {

    private List<EpisodeAction> actions = Collections
            .synchronizedList(new ArrayList<EpisodeAction>());

    /** The preference key for the last sync time stamp */
    private static final String GPODDER_LAST_SYNC_ACTIONS = "GPODDER_LAST_SYNC_ACTIONS";
    /** The last sync time stamp we are using */
    private long lastSyncTimeStamp;

    /** The date format the gpodder.net service understands */
    private final DateFormat gpodderTimeStampFormat;

    /** The list of episode action the gpodder.net service understands */
    private static enum Action {
        DOWNLOAD, PLAY, DELETE, NEW
    }

    /** The sync running flag */
    private boolean syncRunning = false;
    /** The ignore actions flag */
    private boolean ignoreNewActions = false;

    /** Our async task triggering the actual sync machinery */
    private class SyncEpisodeMetadataTask extends
            AsyncTask<Void, Entry<Episode, EpisodeAction>, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            try {
                Log.d(TAG, "Syncing episode actions, last sync time stamp is: " + lastSyncTimeStamp);

                // 1. Get the episode actions from server and apply them to the
                // local model if the episode is actually present. Giving no
                // device id here will make sure that we get changes from all
                // devices.
                final EpisodeActionChanges changes =
                        client.downloadEpisodeActions(lastSyncTimeStamp);
                // Go walk through action and act on them
                for (EpisodeAction action : changes.actions) {
                    // Only act if we know the podcast
                    if (podcastManager.findPodcastForUrl(action.podcast) == null)
                        continue;

                    // Get us an episode
                    final EpisodeMetadata meta = new EpisodeMetadata();
                    meta.podcastUrl = action.podcast;
                    final Episode episode = meta.marshalEpisode(action.episode);
                    // Act on the episode action
                    if (episode != null)
                        publishProgress(new AbstractMap.SimpleEntry<>(episode, action));
                }

                Log.d(TAG, "Changes occured on the server: " + changes.actions);

                // 2. Upload local changes and clear them from list
                Log.d(TAG, "Sending changes to the server: " + actions);
                final List<EpisodeAction> copy = new ArrayList<>(actions);
                // if (copy.size() > 0)
                lastSyncTimeStamp = client.uploadEpisodeActions(copy);

                // 3. Remove all actions already taken care of (we can call this
                // there since the list is synchronized)
                actions.removeAll(changes.actions);
                actions.removeAll(copy);

                Log.d(TAG, "Changes left after clean-up: " + actions);
            } catch (AuthenticationException | IOException e) {
                cancel(true);

                Log.d(TAG, "Syncing episode data failed!", e);
            }

            return null;
        }

        @Override
        protected void onProgressUpdate(Entry<Episode, EpisodeAction>... values) {
            // Go change local model as needed
            final Episode episode = values[0].getKey();
            final EpisodeAction episodeAction = values[0].getValue();
            final Action action = Action
                    .valueOf(episodeAction.action.toUpperCase(Locale.US).trim());

            // Make sure we do not pick up the same action again
            ignoreNewActions = true;
            switch (action) {
                case PLAY:
                    episodeManager.setResumeAt(episode, episodeAction.position * 1000);
                    break;
                case NEW:
                    episodeManager.setResumeAt(episode, null);
                    break;
                default:
                    break;
            }
            // Re-enable action monitoring
            ignoreNewActions = false;
        }

        protected void onPostExecute(Void nothing) {
            Log.d(TAG, "Done syncing episode actions, new time stamp is: " + lastSyncTimeStamp);
            // Make sure we take note of the time stamp
            preferences.edit().putLong(GPODDER_LAST_SYNC_ACTIONS, lastSyncTimeStamp).apply();

            syncRunning = false;
        }

        @Override
        protected void onCancelled(Void nothing) {
            syncRunning = false;
        }
    }

    protected GpodderEpisodeMetadataSyncController(Context context) {
        super(context);

        // Create the date time format for the time stamp sent to gpodder.net
        final TimeZone zone = TimeZone.getTimeZone("UTC");
        this.gpodderTimeStampFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
        gpodderTimeStampFormat.setTimeZone(zone);

        // Recover the last synced time stamp
        this.lastSyncTimeStamp = preferences.getLong(GPODDER_LAST_SYNC_ACTIONS, 0);
    }

    @Override
    public boolean isRunning() {
        return syncRunning || super.isRunning();
    }

    @Override
    protected synchronized void syncEpisodeMetadata() {
        if (!syncRunning) {
            syncRunning = true;

            new SyncEpisodeMetadataTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, (Void) null);
        }
    }

    @Override
    public void onStateChanged(Episode episode, boolean newState) {
        // TODO Add EpisodeAction to list
    }

    @Override
    public void onResumeAtChanged(Episode episode, Integer millis) {
        if (!ignoreNewActions)
            // Send "new" event when the "resume at" was reset
            if (millis == null)
                actions.add(prepareAction(episode, Action.NEW, 0));
            // Otherwise update the server on the play position
            else
                actions.add(prepareAction(episode, Action.PLAY, millis / 1000));

        Log.d(TAG, "Action waiting in gpodder.net controller: " + actions);
    }

    @Override
    public void onDownloadSuccess(Episode episode) {
        actions.add(prepareAction(episode, Action.DOWNLOAD, 0));
        Log.d(TAG, "Action waiting in gpodder.net controller: " + actions);
    }

    @Override
    public void onDownloadDeleted(Episode episode) {
        actions.add(prepareAction(episode, Action.DELETE, 0));
        Log.d(TAG, "Action waiting in gpodder.net controller: " + actions);
    }

    private EpisodeAction prepareAction(Episode episode, Action action, int position) {
        return new EpisodeAction(
                episode.getPodcast().getUrl(),
                episode.getMediaUrl(),
                action.toString().toLowerCase(Locale.US),
                deviceId,
                gpodderTimeStampFormat.format(new Date()),
                null,
                Action.PLAY.equals(action) ? position : null,
                null);
    }
}
