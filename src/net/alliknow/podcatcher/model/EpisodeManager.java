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
import android.app.DownloadManager.Request;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import net.alliknow.podcatcher.Podcatcher;
import net.alliknow.podcatcher.model.types.Episode;

/**
 * Manager to handle episode specific activities.
 */
public class EpisodeManager extends BroadcastReceiver {

    /** The single instance */
    private static EpisodeManager manager;
    /** The application itself */
    private Podcatcher podcatcher;

    /**
     * Init the episode manager.
     * 
     * @param app The podcatcher application object (also a singleton).
     */
    private EpisodeManager(Podcatcher app) {
        // We use some of its method below, so we keep a reference to the
        // application object.
        this.podcatcher = app;

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

    public void download(Episode currentEpisode) {
        DownloadManager downloadManager = (DownloadManager)
                podcatcher.getSystemService(Context.DOWNLOAD_SERVICE);

        Request download = new Request(Uri.parse(currentEpisode.getMediaUrl().toString()))
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_PODCASTS,
                        currentEpisode.getMediaUrl().getFile())
                .setTitle(currentEpisode.getName())
                .setDescription(currentEpisode.getPodcast().getName());

        downloadManager.enqueue(download);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(getClass().getSimpleName(), "Action: " + intent.getAction());
    }

}
