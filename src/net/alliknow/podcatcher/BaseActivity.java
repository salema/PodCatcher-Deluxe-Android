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
import net.alliknow.podcatcher.view.ViewMode;

/**
 * Podcatcher base activity. Defines some common functionality useful for all
 * activities.
 */
public abstract class BaseActivity extends Activity {

    /** The podcast manager handle */
    protected PodcastManager podcastManager;
    /** The episode manager handle */
    protected EpisodeManager episodeManager;

    /** The currently active view mode */
    protected ViewMode viewMode;

    /** The podcatcher website URL */
    private static final String PODCATCHER_WEBSITE = "http://www.podcatcher-deluxe.com";
    /** The podcatcher help website URL */
    private static final String PODCATCHER_HELPSITE = "http://www.podcatcher-deluxe.com/help";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // This will suggest to the Android system, that the volume to be
        // changed for this app (all its activities) is the music stream
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        // Set the data manager
        podcastManager = PodcastManager.getInstance();
        episodeManager = EpisodeManager.getInstance();

        // Set the view mode member
        viewMode = ViewMode.determineViewMode(getResources());
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
