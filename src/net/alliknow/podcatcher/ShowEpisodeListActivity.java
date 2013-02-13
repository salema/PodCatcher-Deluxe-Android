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

import android.os.Bundle;
import android.view.MenuItem;

import net.alliknow.podcatcher.model.types.Episode;
import net.alliknow.podcatcher.model.types.Podcast;
import net.alliknow.podcatcher.view.fragments.EpisodeListFragment;

import java.util.ArrayList;

/**
 * Activity to show only the episode list and possibly the player. Used in small
 * portrait view mode only.
 */
public class ShowEpisodeListActivity extends EpisodeListActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check if we need this activity at all
        if (viewMode != SMALL_PORTRAIT_VIEW) {
            finish();
        } else {
            // Set the content view
            setContentView(R.layout.main);
            // Set fragment members
            findFragments();

            // During initial setup, plug in the episode list fragment.
            if (savedInstanceState == null && episodeListFragment == null) {
                episodeListFragment = new EpisodeListFragment();
                getFragmentManager()
                        .beginTransaction()
                        .add(R.id.content, episodeListFragment,
                                getResources().getString(R.string.episode_list_fragment_tag))
                        .commit();
            }

            // Prepare UI
            episodeListFragment.resetAndSpin();
            processIntent();
        }
    }

    private void processIntent() {
        // Get the load mode
        multiplePodcastsMode = getIntent().getExtras().getBoolean(PodcastActivity.MODE_KEY);

        // Get URL of podcast to load
        String podcastUrl = getIntent().getExtras().getString(PODCAST_URL_KEY);
        currentPodcast = podcastManager.findPodcastForUrl(podcastUrl);

        // We are in select all mode
        if (multiplePodcastsMode) {
            currentEpisodeList = new ArrayList<Episode>();

            for (Podcast podcast : podcastManager.getPodcastList())
                podcastManager.load(podcast);
        } // Single podcast to load
        else {
            // Go load it if found
            if (currentPodcast != null)
                podcastManager.load(currentPodcast);
            else
                episodeListFragment.showLoadFailed();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // This is called when the Home (Up) button is pressed
                finish();

                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void updateActionBar() {
        if (currentPodcast != null) {
            getActionBar().setTitle(currentPodcast.getName());

            if (currentPodcast.getEpisodes().isEmpty())
                getActionBar().setSubtitle(null);
            else {
                int episodeCount = currentPodcast.getEpisodes().size();
                getActionBar().setSubtitle(episodeCount == 1 ?
                        getResources().getString(R.string.one_episode) :
                        episodeCount + " " + getResources().getString(R.string.episodes));
            }
        } else {
            getActionBar().setTitle(R.string.app_name);
            getActionBar().setSubtitle(null);
        }

        // Enable navigation
        getActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    protected void updatePlayer() {
        super.updatePlayer();

        // Make sure to show episode title in player
        if (playerFragment != null) {
            playerFragment.setLoadMenuItemVisibility(false, false);
            playerFragment.setPlayerTitleVisibility(true);
        }
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_right);
    }
}
