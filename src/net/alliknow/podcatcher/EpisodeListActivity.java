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

import android.app.FragmentTransaction;
import android.content.Intent;
import android.view.View;

import net.alliknow.podcatcher.listeners.OnLoadPodcastListener;
import net.alliknow.podcatcher.listeners.OnLoadPodcastLogoListener;
import net.alliknow.podcatcher.listeners.OnSelectEpisodeListener;
import net.alliknow.podcatcher.listeners.OnToggleFilterListener;
import net.alliknow.podcatcher.model.types.Episode;
import net.alliknow.podcatcher.model.types.Podcast;
import net.alliknow.podcatcher.model.types.Progress;
import net.alliknow.podcatcher.view.fragments.EpisodeFragment;
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
        OnLoadPodcastListener, OnLoadPodcastLogoListener, OnSelectEpisodeListener,
        OnToggleFilterListener {

    /** The current episode list fragment */
    protected EpisodeListFragment episodeListFragment;

    /** The podcast we are showing episodes for */
    protected Podcast currentPodcast;
    /** Key used to store podcast URL in intent or bundle */
    public static final String PODCAST_URL_KEY = "podcast_url";

    /** Member to indicate which mode we are in */
    protected ContentMode contentMode = ContentMode.SINGLE_PODCAST;
    /** Key used to save the current content mode in bundle */
    public static final String MODE_KEY = "MODE_KEY";

    /** The options available for the content mode */
    public enum ContentMode {
        /** Show single podcast */
        SINGLE_PODCAST,

        /** Show all podcast */
        ALL_PODCASTS,

        /** Show downloads */
        DOWNLOADS,

        /** Show playlist */
        PLAYLIST
    };

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
    public void onPodcastLoadProgress(Podcast podcast, Progress progress) {
        if (contentMode.equals(ContentMode.SINGLE_PODCAST) && podcast.equals(currentPodcast))
            episodeListFragment.showProgress(progress);
    }

    @Override
    public void onPodcastLoaded(Podcast podcast) {
        // Update list fragment to show episode list
        // Select all podcasts
        if (contentMode.equals(ContentMode.ALL_PODCASTS)) {
            if (podcast.getEpisodeNumber() > 0) {
                currentEpisodeList.addAll(podcast.getEpisodes());
                Collections.sort(currentEpisodeList);
                // Make sure this is a copy
                setFilteredEpisodeList();
            }
        } // Select single podcast
        else if (contentMode.equals(ContentMode.SINGLE_PODCAST) && podcast.equals(currentPodcast)) {
            currentEpisodeList = podcast.getEpisodes();
            setFilteredEpisodeList();
        }

        // Additionally, if on large device, process clever selection update
        if (!viewMode.isSmall()) {
            updateEpisodeListSelection();
            updateDivider();
        }

        updateFilter();
        updateActionBar();
    }

    @Override
    public void onPodcastLoadFailed(Podcast failedPodcast) {
        // The podcast we are waiting for failed to load
        if (contentMode.equals(ContentMode.SINGLE_PODCAST) && failedPodcast.equals(currentPodcast))
            episodeListFragment.showLoadFailed();
        // The last podcast failed to load and none of the others had any
        // episodes to show in the list
        else if (contentMode.equals(ContentMode.ALL_PODCASTS) && podcastManager.getLoadCount() == 0
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
        this.currentEpisode = selectedEpisode;

        switch (viewMode) {
            case LARGE_PORTRAIT:
            case LARGE_LANDSCAPE:
                // Set episode in episode fragment
                episodeFragment.setEpisode(selectedEpisode);

                // Make sure selection matches in list fragment and the UI is
                // updated
                updateEpisodeListSelection();
                updateDownloadUi();
                updateStateUi();

                break;
            case SMALL_LANDSCAPE:
                // Find, and if not already done create, episode fragment
                if (episodeFragment == null)
                    episodeFragment = new EpisodeFragment();

                // Add the fragment to the UI, replacing the list fragment if it
                // is not already there
                if (getFragmentManager().getBackStackEntryCount() == 0) {
                    FragmentTransaction transaction = getFragmentManager().beginTransaction();
                    transaction.replace(R.id.right, episodeFragment,
                            getString(R.string.episode_fragment_tag));
                    transaction.addToBackStack(null);
                    transaction.commit();
                }

                // Set the episode and update the UI
                episodeFragment.setEpisode(selectedEpisode);
                episodeFragment.setShowEpisodeDate(true);

                updateDownloadUi();
                updateStateUi();

                break;
            case SMALL_PORTRAIT:
                // Send intent to open episode as a new activity
                Intent intent = new Intent(this, ShowEpisodeActivity.class);
                intent.putExtra(EPISODE_URL_KEY, selectedEpisode.getMediaUrl().toString());

                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_left);
        }

        updatePlayerUi();
        updateDivider();
    }

    @Override
    public void onReturnToPlayingEpisode() {
        if (service != null && service.getCurrentEpisode() != null) {
            Episode playingEpisode = service.getCurrentEpisode();

            onEpisodeSelected(playingEpisode);
        }
    }

    @Override
    public void onNoEpisodeSelected() {
        this.currentEpisode = null;

        // If there is a episode fragment, reset it
        if (episodeListFragment != null)
            episodeListFragment.selectNone();

        updatePlayerUi();
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
     * 
     */
    protected void updateEpisodeListSelection() {
        if (!viewMode.isSmall()) {
            // Make sure the episode selection in the list is updated
            if (filteredEpisodeList != null && filteredEpisodeList.contains(currentEpisode))
                episodeListFragment.select(filteredEpisodeList.indexOf(currentEpisode));
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
        if (!viewMode.isSmallPortrait())
            super.updateDownloadUi();

        episodeListFragment.refresh();
    }

    @Override
    protected void updatePlaylistUi() {
        if (!viewMode.isSmallPortrait())
            super.updatePlaylistUi();

        episodeListFragment.refresh();
    }

    @Override
    protected void updateStateUi() {
        if (!viewMode.isSmallPortrait())
            super.updateStateUi();

        episodeListFragment.refresh();
    }

    /**
     * Update the divider views to reflect current selection state.
     */
    protected void updateDivider() {
        colorDivider(R.id.divider_first,
                currentPodcast != null || !contentMode.equals(ContentMode.SINGLE_PODCAST));
        colorDivider(R.id.divider_second,
                currentEpisodeList != null && currentEpisodeList.indexOf(currentEpisode) >= 0);
    }

    private void colorDivider(int dividerViewId, boolean colorId) {
        if (getWindow() != null && getWindow().findViewById(dividerViewId) != null) {
            View divider = getWindow().findViewById(dividerViewId);
            divider.setBackgroundResource(colorId ? R.color.divider_on : R.color.divider_off);
        }
    }
}
