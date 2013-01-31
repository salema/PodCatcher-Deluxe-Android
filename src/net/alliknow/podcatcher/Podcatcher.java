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

package net.alliknow.podcatcher;

import android.app.Application;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import net.alliknow.podcatcher.model.PodcastManager;
import net.alliknow.podcatcher.model.SuggestionManager;

/**
 * Our application subclass. Holds global state and model. The Podcatcher
 * application object is created on application startup and will be alive for
 * all the app's lifetime. Its main purpose is to hold handles to the singleton
 * instances of our model data and data managers. In addition, it provides some
 * generic convenience methods.
 */
public class Podcatcher extends Application {

    /**
     * Podcast data manager to be used across all the app's components (namely
     * in the controller) to access and alter the podcast data. The data manager
     * is implemented as a singleton.
     */
    private PodcastManager podcastManager;
    /** Suggestion data manager, persistent and global singleton object. */
    private SuggestionManager suggestionManager;

    @Override
    public void onCreate() {
        super.onCreate();

        // This will only run once in the lifetime of the app
        // since the application is an implicit singleton.
        podcastManager = PodcastManager.getInstance(this);
        // dito
        suggestionManager = SuggestionManager.getInstance(this);
    }

    /**
     * Grant access to the global podcast data model. The returned manager
     * object is a singleton, all calls to this method will always return the
     * same single instance of the podcast manager.
     * 
     * @return The manager handle.
     */
    public PodcastManager getPodcastManager() {
        return podcastManager;
    }

    /**
     * Grant access to the global suggestion data model. The returned manager
     * object is a singleton, all calls to this method will always return the
     * same single instance of the suggestion manager.
     * 
     * @return The manager handle.
     */
    public SuggestionManager getSuggestionManager() {
        return suggestionManager;
    }

    /**
     * Checks whether the device is currently on a fast network (such as wifi)
     * as opposed to a mobile network.
     * 
     * @return <code>true</code> iff we have fast (and potentially free)
     *         Internet access.
     */
    public boolean isOnFastConnection() {
        ConnectivityManager manager =
                (ConnectivityManager) getApplicationContext()
                        .getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = manager.getActiveNetworkInfo();

        if (activeNetwork == null)
            return false;
        else
            switch (activeNetwork.getType()) {
                case ConnectivityManager.TYPE_ETHERNET:
                case ConnectivityManager.TYPE_WIFI:
                case ConnectivityManager.TYPE_WIMAX:
                    return true;
                default:
                    return false;
            }
    }

    /**
     * Checks whether the app is in debug mode.
     * 
     * @return <code>true</code> iff in debug.
     */
    public boolean isInDebugMode() {
        boolean debug = false;

        PackageManager manager = getApplicationContext().getPackageManager();
        try
        {
            ApplicationInfo info = manager.getApplicationInfo(
                    getApplicationContext().getPackageName(), 0);
            debug = (0 != (info.flags &= ApplicationInfo.FLAG_DEBUGGABLE));
        } catch (Exception e) {
        }

        return debug;
    }
}
