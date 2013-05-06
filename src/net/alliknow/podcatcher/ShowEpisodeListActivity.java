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

import android.app.ActionBar;
import android.os.Bundle;
import android.view.MenuItem;

import net.alliknow.podcatcher.model.types.Episode;
import net.alliknow.podcatcher.model.types.Podcast;
import net.alliknow.podcatcher.model.types.Progress;
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
        if (!viewMode.isSmallPortrait())
            finish();
        else {
            // 1. Set the content view
            setContentView(R.layout.main);

            // 2. Set, find, create the fragments
            findFragments();
            // During initial setup, plug in the episode list fragment.
            if (savedInstanceState == null && episodeListFragment == null) {
                episodeListFragment = new EpisodeListFragment();
                getFragmentManager()
                        .beginTransaction()
                        .add(R.id.content, episodeListFragment,
                                getString(R.string.episode_list_fragment_tag))
                        .commit();
            }

            // 3. Register the listeners needed to function as a controller
            registerListeners();

            // Prepare UI
            episodeListFragment.resetAndSpin();
            processIntent();
        }
    }

    private void processIntent() {
        // Get the load mode
        contentMode = (ContentMode) getIntent().getExtras().getSerializable(
                EpisodeListActivity.MODE_KEY);
        episodeListFragment.setShowPodcastNames(!contentMode.equals(ContentMode.SINGLE_PODCAST));

        // We are in select all mode
        if (contentMode.equals(ContentMode.ALL_PODCASTS)) {
            currentEpisodeList = new ArrayList<Episode>();

            for (Podcast podcast : podcastManager.getPodcastList())
                podcastManager.load(podcast);
        } // Single podcast to load
        else if (contentMode.equals(ContentMode.SINGLE_PODCAST)) {
            // Get URL of podcast to load
            String podcastUrl = getIntent().getExtras().getString(PODCAST_URL_KEY);
            currentPodcast = podcastManager.findPodcastForUrl(podcastUrl);

            // Go load it if found
            if (currentPodcast != null)
                podcastManager.load(currentPodcast);
            else
                episodeListFragment.showLoadFailed();
        } // Downloads mode
        else if (contentMode.equals(ContentMode.DOWNLOADS)) {
            this.currentEpisodeList = episodeManager.getDownloads();

            setFilteredEpisodeList();
        } // Playlist mode
        else if (contentMode.equals(ContentMode.PLAYLIST)) {
            this.currentEpisodeList = episodeManager.getPlaylist();

            setFilteredEpisodeList();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        updateFilter();
        updateDownloadUi();
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
    public void onPodcastLoadProgress(Podcast podcast, Progress progress) {
        super.onPodcastLoadProgress(podcast, progress);

        if (contentMode.equals(ContentMode.ALL_PODCASTS))
            updateActionBarSubtitleOnMultipleLoad();
    }

    @Override
    public void onPodcastLoadFailed(Podcast failedPodcast) {
        super.onPodcastLoadFailed(failedPodcast);

        updateActionBar();
    }

    @Override
    protected void updateActionBar() {
        final ActionBar bar = getActionBar();

        switch (contentMode) {
            case SINGLE_PODCAST:
                if (currentPodcast == null) {
                    bar.setTitle(R.string.app_name);
                    bar.setSubtitle(null);
                }
                else {
                    bar.setTitle(currentPodcast.getName());
                    if (currentPodcast.getEpisodes().isEmpty())
                        bar.setSubtitle(null);
                    else {
                        final int episodeCount = currentPodcast.getEpisodeNumber();
                        bar.setSubtitle(episodeCount == 1 ? getString(R.string.one_episode) :
                                episodeCount + " " + getString(R.string.episodes));
                    }
                }
                break;
            case ALL_PODCASTS:
                bar.setTitle(R.string.app_name);
                updateActionBarSubtitleOnMultipleLoad();
                break;
            case DOWNLOADS:
                bar.setTitle(R.string.app_name);
                bar.setSubtitle(R.string.downloads);
                break;
            case PLAYLIST:
                bar.setTitle(R.string.app_name);
                bar.setSubtitle(R.string.playlist);
                break;
        }

        // Enable navigation
        getActionBar().setDisplayHomeAsUpEnabled(true);
    }

    /**
     * Set the action bar subtitle to reflect multiple podcast load progress
     */
    private void updateActionBarSubtitleOnMultipleLoad() {
        final ActionBar bar = getActionBar();

        final int podcastCount = podcastManager.size();
        final int loadingPodcastCount = podcastManager.getLoadCount();

        // Load finished for all popdcast and there are episode
        if (loadingPodcastCount == 0 && currentEpisodeList != null) {
            final int episodeCount = currentEpisodeList.size();
            bar.setSubtitle(episodeCount == 1 ? getString(R.string.one_episode) :
                    episodeCount + " " + getString(R.string.episodes));
        }
        // Load finished but no episodes
        else if (loadingPodcastCount == 0)
            bar.setSubtitle(podcastCount == 1 ? getString(R.string.one_podcast_selected) :
                    podcastCount + " " + getString(R.string.podcasts_selected));
        // Load in progress
        else
            bar.setSubtitle((podcastCount - loadingPodcastCount) + " "
                    + getString(R.string.of) + " " + podcastCount + " "
                    + getString(R.string.podcasts_selected));
    }

    @Override
    protected void updatePlayerUi() {
        super.updatePlayerUi();

        // Make sure to show episode title in player
        playerFragment.setLoadMenuItemVisibility(false, false);
        playerFragment.setPlayerTitleVisibility(true);
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_right);
    }
}
