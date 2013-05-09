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

package net.alliknow.podcatcher.model.tasks;

import android.os.AsyncTask;
import android.util.Log;

import net.alliknow.podcatcher.listeners.OnLoadPlaylistListener;
import net.alliknow.podcatcher.model.EpisodeManager;
import net.alliknow.podcatcher.model.types.Episode;

import java.lang.ref.WeakReference;
import java.util.List;

/**
 * Get the playlist from the episode manager.
 */
public class LoadPlaylistTask extends AsyncTask<Void, Void, List<Episode>> {

    /** Call back */
    private WeakReference<OnLoadPlaylistListener> listener;

    /**
     * Create new task.
     * 
     * @param listener Callback to be alerted on completion. The listener is
     *            held as a weak reference, so you can safely call this from an
     *            activity without leaking it.
     */
    public LoadPlaylistTask(OnLoadPlaylistListener listener) {
        this.listener = new WeakReference<OnLoadPlaylistListener>(listener);
    }

    @Override
    protected List<Episode> doInBackground(Void... nothing) {
        try {
            // Block if episode metadata not yet available
            EpisodeManager.getInstance().blockUntilEpisodeMetadataIsLoaded();
            // Get the playlist
            return EpisodeManager.getInstance().getPlaylist();
        } catch (Exception e) {
            Log.w(getClass().getSimpleName(), "Load failed for playlist", e);

            cancel(true);
        }

        return null;
    }

    @Override
    protected void onPostExecute(List<Episode> playlist) {
        // Playlist available
        if (listener.get() != null)
            listener.get().onPlaylistLoaded(playlist);
        else
            Log.w(getClass().getSimpleName(), "Playlist loaded, but no listener attached");
    }
}
