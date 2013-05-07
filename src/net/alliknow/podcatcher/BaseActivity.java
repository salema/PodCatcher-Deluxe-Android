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

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import net.alliknow.podcatcher.model.EpisodeManager;
import net.alliknow.podcatcher.model.PodcastManager;
import net.alliknow.podcatcher.model.types.Episode;
import net.alliknow.podcatcher.model.types.Podcast;
import net.alliknow.podcatcher.view.ViewMode;

/**
 * Podcatcher base activity. Defines some common functionality useful for all
 * activities.
 */
public abstract class BaseActivity extends Activity {

    /** The podcatcher website URL */
    public static final String PODCATCHER_WEBSITE = "http://www.podcatcher-deluxe.com";
    /** The podcatcher help website URL */
    public static final String PODCATCHER_HELPSITE = "http://www.podcatcher-deluxe.com/help";

    /** The podcast manager handle */
    protected PodcastManager podcastManager;
    /** The episode manager handle */
    protected EpisodeManager episodeManager;

    /** The currently active view mode */
    protected ViewMode view;
    /** The currently active selection */
    protected ContentSelection selection;

    /** The options available for the content mode */
    public static enum ContentMode {
        /** Show single podcast */
        SINGLE_PODCAST,

        /** Show all podcast */
        ALL_PODCASTS,

        /** Show downloads */
        DOWNLOADS,

        /** Show playlist */
        PLAYLIST
    };

    /**
     * Content selection singleton, makes the user selection of podcasts,
     * episodes, etc. available to all activities across activity recreations.
     */
    protected static class ContentSelection {
        /** The single instance */
        private static ContentSelection instance;

        /** Flag to indicate whether we are in single or multiple podcast mode */
        private ContentMode mode = ContentMode.SINGLE_PODCAST;

        /** The podcast we are showing episodes for */
        private Podcast currentPodcast;
        /** The selected episode */
        private Episode currentEpisode;

        private ContentSelection() {
            // Nothing to do here
        }

        /**
         * Get the single instance representing the current user selection in
         * the app.
         * 
         * @return The single instance.
         */
        public static ContentSelection getInstance() {
            if (instance == null)
                instance = new ContentSelection();

            return instance;
        }

        /**
         * @return The currently selected mode. Default and init state is single
         *         podcast.
         * @see ContentMode
         */
        public ContentMode getMode() {
            return mode;
        }

        /**
         * @param mode The mode to set.
         * @see ContentMode
         */
        public void setMode(ContentMode mode) {
            this.mode = mode;
        }

        /**
         * @return The currently selected podcast. Might be <code>null</code> to
         *         indicate no selection.
         */
        public Podcast getPodcast() {
            return currentPodcast;
        }

        /**
         * @param podcast The selected podcast to set.
         */
        public void setPodcast(Podcast podcast) {
            this.currentPodcast = podcast;
        }

        /**
         * @return The currently selected episode. Might be <code>null</code>
         *         indicating that no selection took place.
         */
        public Episode getEpisode() {
            return currentEpisode;
        }

        /**
         * @param episode The episode to set.
         */
        public void setEpisode(Episode episode) {
            this.currentEpisode = episode;
        }

        /**
         * @return Whether the app is currently in single podcast mode.
         */
        public boolean isSingle() {
            return ContentMode.SINGLE_PODCAST.equals(mode);
        }

        /**
         * @return Whether the app is currently in all podcasts mode.
         */
        public boolean isAll() {
            return ContentMode.ALL_PODCASTS.equals(mode);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // This will suggest to the Android system, that the volume to be
        // changed for this app (all its activities) is the music stream
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        // Set the selection member
        selection = ContentSelection.getInstance();
        // Set the view mode member
        view = ViewMode.determineViewMode(getResources());

        // Set the data managers
        podcastManager = PodcastManager.getInstance();
        episodeManager = EpisodeManager.getInstance();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Add generic menu items (help, web site...)
        getMenuInflater().inflate(R.menu.podcatcher, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.about_menuitem:
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(PODCATCHER_WEBSITE)));

                return true;
            case R.id.help_menuitem:
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(PODCATCHER_HELPSITE)));

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Gets the fragment for a given tag string id (resolved via app's
     * resources) from the fragment manager.
     * 
     * @param tagId Id of the tag string in resources.
     * @return The fragment stored under the given tag or <code>null</code> if
     *         not added to the fragment manager.
     */
    protected Fragment findByTagId(int tagId) {
        return getFragmentManager().findFragmentByTag(getString(tagId));
    }
}
