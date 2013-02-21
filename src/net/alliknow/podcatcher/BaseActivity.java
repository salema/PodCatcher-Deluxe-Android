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
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import net.alliknow.podcatcher.model.PodcastManager;

/**
 * Podcatcher base activity. Defines some common functionality useful for all
 * activities.
 */
public abstract class BaseActivity extends Activity {

    /** The podcast manager handle */
    protected PodcastManager podcastManager;

    /** The currently active view mode */
    protected int viewMode;
    /** These are the four view modes we want adapt to. */
    /**
     * Small and normal screens (smallest width < 600dp) in portrait orientation
     */
    protected static final int SMALL_PORTRAIT_VIEW = 0;
    /**
     * Small and normal screens (smallest width < 600dp) in square or landscape
     * orientation
     */
    protected static final int SMALL_LANDSCAPE_VIEW = 1;
    /**
     * Large and extra-large screens (smallest width >= 600dp) in portrait
     * orientation
     */
    protected static final int LARGE_PORTRAIT_VIEW = 2;
    /**
     * Large and extra-large screens (smallest width >= 600dp) in square or
     * landscape orientation
     */
    protected static final int LARGE_LANDSCAPE_VIEW = 3;

    /**
     * The amount of dp establishing the border between small and large screen
     * buckets
     */
    private static final int MIN_PIXEL_LARGE = 600;

    /** The podcatcher website URL */
    private static final String PODCATCHER_WEBSITE = "http://www.podcatcher-deluxe.com";
    /** The podcatcher help website URL */
    private static final String PODCATCHER_HELPSITE = "http://www.podcatcher-deluxe.com/help";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set the data manager
        podcastManager = PodcastManager.getInstance();

        // Set the view mode member
        viewMode = determineViewMode();
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

    private int determineViewMode() {
        // Get config information
        Configuration config = getResources().getConfiguration();

        // Determine view mode
        switch (config.orientation) {
            case Configuration.ORIENTATION_PORTRAIT:
                return config.smallestScreenWidthDp >= MIN_PIXEL_LARGE ?
                        LARGE_PORTRAIT_VIEW : SMALL_PORTRAIT_VIEW;
            default: // Landscape and square
                return config.smallestScreenWidthDp >= MIN_PIXEL_LARGE ?
                        LARGE_LANDSCAPE_VIEW : SMALL_LANDSCAPE_VIEW;
        }
    }
}
