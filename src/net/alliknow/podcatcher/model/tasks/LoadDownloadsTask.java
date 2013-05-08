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

import net.alliknow.podcatcher.listeners.OnLoadDownloadsListener;
import net.alliknow.podcatcher.model.EpisodeManager;
import net.alliknow.podcatcher.model.types.Episode;

import java.util.List;

/**
 * Get the list of downloads from the episode manager
 */
public class LoadDownloadsTask extends AsyncTask<Void, Void, List<Episode>> {

    /** Call back */
    private OnLoadDownloadsListener listener;

    /**
     * Create new task.
     * 
     * @param listener Callback to be alerted on completion.
     */
    public LoadDownloadsTask(OnLoadDownloadsListener listener) {
        this.listener = listener;
    }

    @Override
    protected List<Episode> doInBackground(Void... nothing) {
        try {
            // Block if episode metadata not yet available
            EpisodeManager.getInstance().blockUntilEpisodeMetadataIsLoaded();
            // Get the list of downloads
            return EpisodeManager.getInstance().getDownloads();
        } catch (Exception e) {
            Log.w(getClass().getSimpleName(), "Load failed for download list", e);

            cancel(true);
        }

        return null;
    }

    @Override
    protected void onPostExecute(List<Episode> downloads) {
        // List of download available
        if (listener != null)
            listener.onDownloadsLoaded(downloads);
        else
            Log.w(getClass().getSimpleName(),
                    "List of downloads available loaded, but no listener attached");
    }
}
