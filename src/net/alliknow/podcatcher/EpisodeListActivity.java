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

import android.content.Intent;
import android.view.View;

import net.alliknow.podcatcher.listeners.OnLoadPodcastListener;
import net.alliknow.podcatcher.listeners.OnLoadPodcastLogoListener;
import net.alliknow.podcatcher.listeners.OnSelectPodcastListener;
import net.alliknow.podcatcher.listeners.OnToggleFilterListener;
import net.alliknow.podcatcher.model.types.Episode;
import net.alliknow.podcatcher.model.types.Podcast;
import net.alliknow.podcatcher.model.types.Progress;
import net.alliknow.podcatcher.view.fragments.EpisodeListFragment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Show list of episodes activity. This is thought of as an abstract activity
 * for an app only consisting of an episode list view, the player and the
 * ability to show an {@link ShowEpisodeActivity} on top. Sub-classes could
 * extend or simply show this layout.
 */
public abstract class EpisodeListActivity extends EpisodeActivity implements
        OnLoadPodcastListener, OnLoadPodcastLogoListener, OnSelectPodcastListener,
        OnToggleFilterListener {

    /** Key used to save the current content mode in bundle */
    public static final String MODE_KEY = "MODE_KEY";
    /** Key used to store podcast URL in intent or bundle */
    public static final String PODCAST_URL_KEY = "podcast_url";

    /** The current episode list fragment */
    protected EpisodeListFragment episodeListFragment;

    /** The current episode list */
    protected List<Episode> currentEpisodeList;
    /** The filtered episode list */
    protected List<Episode> filteredEpisodeList;
    /** Flag indicating whether we filter the episode list */
    protected boolean filterActive = false;

    @Override
    protected void findFragments() {
        super.findFragments();

        // The episode list fragment
        if (episodeListFragment == null)
            episodeListFragment = (EpisodeListFragment) findByTagId(R.string.episode_list_fragment_tag);
    }

    @Override
    protected void registerListeners() {
        super.registerListeners();

        // We have to do this here instead of onCreate since we can only react
        // on the call-backs properly once we have our fragment
        podcastManager.addLoadPodcastListener(this);
        podcastManager.addLoadPodcastLogoListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Make sure dividers (if any) reflect selection state
        updateDivider();
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Persist state of episode metadata
        episodeManager.saveState();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        podcastManager.removeLoadPodcastListener(this);
        podcastManager.removeLoadPodcastLogoListener(this);
    }

    @Override
    public void onToggleFilter() {
        filterActive = !filterActive;

        if (currentEpisodeList != null)
            setFilteredEpisodeList();

        updateFilter();
    }

    @Override
    public void onPodcastSelected(Podcast podcast) {
        selection.setPodcast(podcast);
        selection.setMode(ContentMode.SINGLE_PODCAST);

        this.currentEpisodeList = null;

        switch (view) {
            case SMALL_LANDSCAPE:
                // This will go back to the list view in case we are showing
                // episode details
                getFragmentManager().popBackStackImmediate();
                // There is no break here on purpose, we need to run the code
                // below as well
            case LARGE_PORTRAIT:
            case LARGE_LANDSCAPE:
                // List fragment is visible, make it show progress UI
                episodeListFragment.resetAndSpin();
                // Update other UI
                updateFilter();
                updateDivider();

                // Load podcast
                podcastManager.load(podcast);

                break;
            case SMALL_PORTRAIT:
                // This case should be handled by sub-classes
                break;
        }
    }

    @Override
    public void onAllPodcastsSelected() {
        selection.setPodcast(null);
        selection.setMode(ContentMode.ALL_PODCASTS);

        this.currentEpisodeList = new ArrayList<Episode>();

        switch (view) {
            case SMALL_LANDSCAPE:
                // This will go back to the list view in case we are showing
                // episode details
                getFragmentManager().popBackStackImmediate();
                // There is no break here on purpose, we need to run the code
                // below as well
            case LARGE_PORTRAIT:
            case LARGE_LANDSCAPE:
                // List fragment is visible, make it show progress UI
                episodeListFragment.resetAndSpin();
                episodeListFragment.setShowPodcastNames(true);
                // Update other UI
                updateFilter();
                updateDivider();

                // Go load all podcasts
                for (Podcast podcast : podcastManager.getPodcastList())
                    podcastManager.load(podcast);

                break;
            case SMALL_PORTRAIT:
                // This case should be handled by sub-classes
                break;
        }
    }

    @Override
    public void onDownloadsSelected() {
        selection.setPodcast(null);
        selection.setMode(ContentMode.DOWNLOADS);

        this.currentEpisodeList = episodeManager.getDownloads();

        switch (view) {
            case SMALL_LANDSCAPE:
                // This will go back to the list view in case we are showing
                // episode details
                getFragmentManager().popBackStackImmediate();
                // There is no break here on purpose, we need to run the code
                // below as well
            case LARGE_PORTRAIT:
            case LARGE_LANDSCAPE:
                // List fragment is visible, make it show progress UI
                episodeListFragment.resetAndSpin();
                episodeListFragment.setShowPodcastNames(true);
                // Update other UI
                updateFilter();
                updateDivider();

                // Set the list of downloads
                setFilteredEpisodeList();

                break;
            case SMALL_PORTRAIT:
                // This case should be handled by sub-classes
                break;
        }
    }

    @Override
    public void onPlaylistSelected() {
        selection.setPodcast(null);
        selection.setMode(ContentMode.PLAYLIST);

        this.currentEpisodeList = episodeManager.getPlaylist();

        switch (view) {
            case SMALL_LANDSCAPE:
                // This will go back to the list view in case we are showing
                // episode details
                getFragmentManager().popBackStackImmediate();
                // There is no break here on purpose, we need to run the code
                // below as well
            case LARGE_PORTRAIT:
            case LARGE_LANDSCAPE:
                // List fragment is visible, make it show progress UI
                episodeListFragment.resetAndSpin();
                episodeListFragment.setShowPodcastNames(true);
                // Update other UI
                updateFilter();
                updateDivider();

                // Set the playlist
                setFilteredEpisodeList();

                break;
            case SMALL_PORTRAIT:
                // This case should be handled by sub-classes
                break;
        }
    }

    @Override
    public void onNoPodcastSelected() {
        selection.setPodcast(null);
        selection.setMode(ContentMode.SINGLE_PODCAST);

        this.currentEpisodeList = null;

        if (!view.isSmallPortrait()) {
            // If there is an episode list visible, reset it
            episodeListFragment.selectNone();
            episodeListFragment.resetUi();

            // Update other UI
            updateDivider();
        }
    }

    @Override
    public void onPodcastLoadProgress(Podcast podcast, Progress progress) {
        if (selection.isSingle() && podcast.equals(selection.getPodcast()))
            episodeListFragment.showProgress(progress);
    }

    @Override
    public void onPodcastLoaded(Podcast podcast) {
        // Update list fragment to show episode list
        // Select all podcasts
        if (selection.isAll()) {
            if (podcast.getEpisodeNumber() > 0) {
                currentEpisodeList.addAll(podcast.getEpisodes());
                Collections.sort(currentEpisodeList);
                // Make sure this is a copy
                setFilteredEpisodeList();
            }
        } // Select single podcast
        else if (selection.isSingle() && podcast.equals(selection.getPodcast())) {
            currentEpisodeList = podcast.getEpisodes();
            setFilteredEpisodeList();
        }

        // Additionally, if on large device, process clever selection update
        if (!view.isSmall()) {
            updateEpisodeListSelection();
            updateDivider();
        }

        updateFilter();
        updateActionBar();
    }

    @Override
    public void onPodcastLoadFailed(Podcast failedPodcast) {
        // The podcast we are waiting for failed to load
        if (selection.isSingle() && failedPodcast.equals(selection.getPodcast()))
            episodeListFragment.showLoadFailed();
        // The last podcast failed to load and none of the others had any
        // episodes to show in the list
        else if (selection.isAll() && podcastManager.getLoadCount() == 0
                && (currentEpisodeList == null || currentEpisodeList.isEmpty()))
            episodeListFragment.showLoadFailed();
    }

    @Override
    public void onPodcastLogoLoaded(Podcast podcast) {
        // pass
    }

    @Override
    public void onPodcastLogoLoadFailed(Podcast podcast) {
        // pass
    }

    @Override
    public void onEpisodeSelected(Episode selectedEpisode) {
        super.onEpisodeSelected(selectedEpisode);

        if (!view.isSmall())
            // Make sure selection matches in list fragment
            updateEpisodeListSelection();
        else if (view.isSmallPortrait()) {
            // Send intent to open episode as a new activity
            Intent intent = new Intent(this, ShowEpisodeActivity.class);

            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_left);
        }

        updateDivider();
    }

    @Override
    public void onNoEpisodeSelected() {
        super.onNoEpisodeSelected();

        if (episodeListFragment != null)
            episodeListFragment.selectNone();
        updateDivider();
    }

    /**
     * Filter and set the current episode list to show in the episode list
     * fragment.
     */
    protected void setFilteredEpisodeList() {
        filteredEpisodeList = new ArrayList<Episode>(currentEpisodeList);

        if (filterActive) {
            Iterator<Episode> iterator = filteredEpisodeList.iterator();

            while (iterator.hasNext())
                if (episodeManager.getState(iterator.next()))
                    iterator.remove();
        }

        episodeListFragment.setEpisodeList(filteredEpisodeList);
        updateEpisodeListSelection();
    }

    /**
     * Make sure the episode list selection matches current state.
     */
    protected void updateEpisodeListSelection() {
        if (!view.isSmall()) {
            // Make sure the episode selection in the list is updated
            if (filteredEpisodeList != null && filteredEpisodeList.contains(selection.getEpisode()))
                episodeListFragment.select(filteredEpisodeList.indexOf(selection.getEpisode()));
            else
                episodeListFragment.selectNone();
        } else
            episodeListFragment.selectNone();
    }

    /**
     * Update the filter menu icon visibility.
     */
    protected void updateFilter() {
        episodeListFragment.setFilterMenuItemVisibility(
                currentEpisodeList != null && !currentEpisodeList.isEmpty(), filterActive);
    }

    @Override
    protected void updateDownloadUi() {
        if (!view.isSmallPortrait())
            super.updateDownloadUi();

        episodeListFragment.refresh();
    }

    @Override
    protected void updatePlaylistUi() {
        if (!view.isSmallPortrait())
            super.updatePlaylistUi();

        episodeListFragment.refresh();
    }

    @Override
    protected void updateStateUi() {
        if (!view.isSmallPortrait())
            super.updateStateUi();

        episodeListFragment.refresh();
    }

    /**
     * Update the divider views to reflect current selection state.
     */
    protected void updateDivider() {
        colorDivider(R.id.divider_first, selection.isPodcastSet() || !selection.isSingle());
        colorDivider(R.id.divider_second, currentEpisodeList != null
                && currentEpisodeList.indexOf(selection.getEpisode()) >= 0);
    }

    private void colorDivider(int dividerViewId, boolean colorId) {
        if (getWindow() != null && getWindow().findViewById(dividerViewId) != null) {
            View divider = getWindow().findViewById(dividerViewId);
            divider.setBackgroundResource(colorId ? R.color.divider_on : R.color.divider_off);
        }
    }
}
