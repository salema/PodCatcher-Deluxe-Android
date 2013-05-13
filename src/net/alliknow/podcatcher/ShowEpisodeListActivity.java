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

import net.alliknow.podcatcher.model.tasks.LoadDownloadsTask;
import net.alliknow.podcatcher.model.tasks.LoadPlaylistTask;
import net.alliknow.podcatcher.model.types.Episode;
import net.alliknow.podcatcher.model.types.Podcast;
import net.alliknow.podcatcher.view.fragments.EpisodeListFragment;

import java.util.List;

/**
 * Activity to show only the episode list and possibly the player. Used in small
 * portrait view mode only.
 */
public class ShowEpisodeListActivity extends EpisodeListActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check if we need this activity at all
        if (!view.isSmallPortrait())
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

            // 4. Act according to selection
            if (selection.isAll())
                onAllPodcastsSelected();
            else if (ContentMode.DOWNLOADS.equals(selection.getMode()))
                onDownloadsSelected();
            else if (ContentMode.PLAYLIST.equals(selection.getMode()))
                onPlaylistSelected();
            else if (selection.isSingle() && selection.isPodcastSet())
                onPodcastSelected(selection.getPodcast());
            else
                episodeListFragment.showLoadFailed();
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
                // Unselect podcast
                selection.resetPodcast();

                // This is called when the Home (Up) button is pressed
                finish();
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_right);

                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        // Unselect podcast
        selection.resetPodcast();

        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_right);
    }

    @Override
    public void onPodcastSelected(Podcast podcast) {
        super.onPodcastSelected(podcast);

        // Init the list view...
        episodeListFragment.resetAndSpin();
        // ...and start loading
        podcastManager.load(podcast);
    }

    @Override
    public void onAllPodcastsSelected() {
        super.onAllPodcastsSelected();

        // Init the list view...
        if (podcastManager.size() > 0)
            episodeListFragment.resetAndSpin();
        else
            episodeListFragment.resetUi();
        episodeListFragment.setShowPodcastNames(true);
        // ...and go get the data
        for (Podcast podcast : podcastManager.getPodcastList())
            podcastManager.load(podcast);

        updateActionBar();
    }

    @Override
    public void onDownloadsSelected() {
        super.onDownloadsSelected();

        episodeListFragment.resetAndSpin();
        episodeListFragment.setShowPodcastNames(true);

        new LoadDownloadsTask(this).execute((Void) null);
    }

    @Override
    public void onDownloadsLoaded(List<Episode> downloads) {
        super.onDownloadsLoaded(downloads);

        updateActionBar();
    }

    @Override
    public void onPlaylistSelected() {
        super.onPlaylistSelected();

        episodeListFragment.resetAndSpin();
        episodeListFragment.setShowPodcastNames(true);

        new LoadPlaylistTask(this).execute((Void) null);
    }

    @Override
    public void onPlaylistLoaded(List<Episode> playlist) {
        super.onPlaylistLoaded(playlist);

        updateActionBar();
    }

    @Override
    public void onPodcastLoaded(Podcast podcast) {
        super.onPodcastLoaded(podcast);

        updateActionBar();
    }

    @Override
    public void onPodcastLoadFailed(Podcast failedPodcast) {
        super.onPodcastLoadFailed(failedPodcast);

        updateActionBar();
    }

    @Override
    protected void updateActionBar() {
        contentSpinner.setTitle(getString(R.string.app_name));

        switch (selection.getMode()) {
            case SINGLE_PODCAST:
                if (!selection.isPodcastSet()) {
                    contentSpinner.setSubtitle(null);
                }
                else {
                    if (selection.getPodcast().getEpisodes().isEmpty())
                        contentSpinner.setSubtitle(null);
                    else {
                        final int episodeCount = selection.getPodcast().getEpisodeNumber();
                        contentSpinner
                                .setSubtitle(episodeCount == 1 ? getString(R.string.one_episode) :
                                        episodeCount + " " + getString(R.string.episodes));
                    }
                }
                break;
            case ALL_PODCASTS:
                updateActionBarSubtitleOnMultipleLoad();
                break;
            case DOWNLOADS:
                contentSpinner.setSubtitle(getString(R.string.downloads));
                break;
            case PLAYLIST:
                contentSpinner.setSubtitle(getString(R.string.playlist));
                break;
        }

        // Enable navigation
        getActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    protected void updatePlayerUi() {
        super.updatePlayerUi();

        // Make sure to show episode title in player
        playerFragment.setLoadMenuItemVisibility(false, false);
        playerFragment.setPlayerTitleVisibility(true);
    }

    /**
     * Set the action bar subtitle to reflect multiple podcast load progress
     */
    private void updateActionBarSubtitleOnMultipleLoad() {
        final int podcastCount = podcastManager.size();
        final int loadingPodcastCount = podcastManager.getLoadCount();

        // Load finished for all podcasts and there are episode
        if (loadingPodcastCount == 0 && currentEpisodeList != null) {
            final int episodeCount = currentEpisodeList.size();

            if (episodeCount == 0)
                contentSpinner.setSubtitle(null);
            else if (episodeCount == 1)
                contentSpinner.setSubtitle(getString(R.string.one_episode));
            else
                contentSpinner.setSubtitle(episodeCount + " " + getString(R.string.episodes));
        }
        // Load finished but no episodes
        else if (loadingPodcastCount == 0)
            contentSpinner.setSubtitle(podcastCount == 1 ? getString(R.string.one_podcast_selected)
                    : podcastCount + " " + getString(R.string.podcasts_selected));
        // Load in progress
        else
            contentSpinner.setSubtitle((podcastCount - loadingPodcastCount) + " "
                    + getString(R.string.of) + " " + podcastCount + " "
                    + getString(R.string.podcasts_selected));
    }
}
