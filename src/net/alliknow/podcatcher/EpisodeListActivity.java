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
import net.alliknow.podcatcher.model.types.Episode;
import net.alliknow.podcatcher.model.types.Podcast;
import net.alliknow.podcatcher.model.types.Progress;
import net.alliknow.podcatcher.view.fragments.EpisodeFragment;
import net.alliknow.podcatcher.view.fragments.EpisodeListFragment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Show list of episodes activity. This is thought of an abstract activity for
 * an app only consisting of an episode list view, the player and the ability to
 * show an {@link ShowEpisodeActivity} on top. Sub-classes could extends or
 * simply show this layout.
 */
public abstract class EpisodeListActivity extends EpisodeActivity implements
        OnLoadPodcastListener, OnLoadPodcastLogoListener, OnSelectEpisodeListener {

    /** The current episode list fragment */
    protected EpisodeListFragment episodeListFragment;

    /**
     * Key used to save the current setting for
     * <code>multiplePodcastsMode</code> in bundle
     */
    public static final String MODE_KEY = "MODE_KEY";

    /** Flag to indicate whether we are in multiple podcast mode */
    protected boolean multiplePodcastsMode = false;

    /** The podcast we are showing episodes for */
    protected Podcast currentPodcast;
    /** Key used to store podcast URL in intent or bundle */
    public static final String PODCAST_URL_KEY = "podcast_url";

    /** The current episode list */
    protected List<Episode> currentEpisodeList;

    @Override
    protected void findFragments() {
        super.findFragments();

        // The episode list fragment
        if (episodeListFragment == null)
            episodeListFragment = (EpisodeListFragment) findByTagId(R.string.episode_list_fragment_tag);

        // We have to do this here instead of onCreate since we can only react
        // on the call-backs properly once we have our fragment
        podcastManager.addLoadPodcastListener(this);
        podcastManager.addLoadPodcastLogoListener(this);
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Make sure dividers (if any) reflect selection state
        updateDivider();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        podcastManager.removeLoadPodcastListener(this);
        podcastManager.removeLoadPodcastLogoListener(this);
    }

    @Override
    public void onPodcastLoadProgress(Podcast podcast, Progress progress) {
        if (!multiplePodcastsMode && podcast.equals(currentPodcast))
            episodeListFragment.showProgress(progress);
    }

    @Override
    public void onPodcastLoaded(Podcast podcast) {
        // Update list fragment to show episode list
        // Select all podcasts
        if (multiplePodcastsMode) {
            // TODO decide on this: episodeList.addAll(list.subList(0,
            // list.size() > 100 ? 100 : list.size() - 1));
            if (podcast.getEpisodeNumber() > 0) {
                currentEpisodeList.addAll(podcast.getEpisodes());
                Collections.sort(currentEpisodeList);
                // Make sure this is a copy
                episodeListFragment.setEpisodeList(new ArrayList<Episode>(currentEpisodeList));
            }
        } // Select single podcast
        else if (podcast.equals(currentPodcast)) {
            currentEpisodeList = podcast.getEpisodes();
            episodeListFragment.setEpisodeList(currentEpisodeList);
        }

        // Additionally, if on large device, process clever selection update
        updateEpisodeListSelection();
        // Update other UI
        updateDivider();
        updateActionBar();
    }

    @Override
    public void onPodcastLoadFailed(Podcast failedPodcast) {
        // The podcast we are waiting for failed to load
        if (!multiplePodcastsMode && failedPodcast.equals(currentPodcast))
            episodeListFragment.showLoadFailed();
        // The last podcast failed to load and none of the others had any
        // episodes to show in the list
        else if (multiplePodcastsMode && podcastManager.getLoadCount() == 0
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
                // Make sure selection matches in list fragment
                updateEpisodeListSelection();

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

                // Set the episode
                episodeFragment.setEpisode(selectedEpisode);
                episodeFragment.setShowEpisodeDate(true);

                break;
            case SMALL_PORTRAIT:
                // Send intent to open episode as a new activity
                Intent intent = new Intent(this, ShowEpisodeActivity.class);
                intent.putExtra(EPISODE_URL_KEY, selectedEpisode.getMediaUrl().toString());

                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_left);
        }

        updatePlayer();
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

        updatePlayer();
        updateDivider();
    }

    /**
     * Update the episode list to select the correct episode
     */
    protected void updateEpisodeListSelection() {
        switch (viewMode) {
            case LARGE_PORTRAIT:
            case LARGE_LANDSCAPE:
                // Make sure the episode selection in the list is updated
                if (currentEpisodeList != null && currentEpisodeList.contains(currentEpisode))
                    episodeListFragment.select(currentEpisodeList.indexOf(currentEpisode));
                else
                    episodeListFragment.selectNone();

                break;
            default:
                episodeListFragment.selectNone();
        }
    }

    /**
     * Update the divider views to reflect current selection state.
     */
    protected void updateDivider() {
        colorDivider(R.id.divider_first, currentPodcast != null || multiplePodcastsMode);
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
