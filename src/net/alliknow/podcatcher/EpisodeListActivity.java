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
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;

import net.alliknow.podcatcher.listeners.OnLoadPodcastListener;
import net.alliknow.podcatcher.listeners.OnLoadPodcastLogoListener;
import net.alliknow.podcatcher.listeners.OnSelectEpisodeListener;
import net.alliknow.podcatcher.model.tasks.Progress;
import net.alliknow.podcatcher.model.types.Episode;
import net.alliknow.podcatcher.model.types.Podcast;
import net.alliknow.podcatcher.view.fragments.EpisodeFragment;
import net.alliknow.podcatcher.view.fragments.EpisodeListFragment;
import net.alliknow.podcatcher.view.fragments.PodcastListFragment;

import java.util.Collections;
import java.util.List;

/**
 * Show list of episodes activity.
 */
public class EpisodeListActivity extends EpisodeActivity implements
        OnLoadPodcastListener, OnLoadPodcastLogoListener, OnSelectEpisodeListener {

    /**
     * Key used to save the current setting for
     * <code>multiplePodcastsMode</code> in bundle
     */
    public static final String MODE_KEY = "MODE_KEY";

    /** Flag to indicate whether we are in multiple podcast mode */
    protected boolean multiplePodcastsMode = false;

    /** The podcast we are showing episodes for */
    protected Podcast currentPodcast;
    /** The current episode list */
    protected List<Episode> currentEpisodeList;

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        if (savedInstanceState != null)
            multiplePodcastsMode = savedInstanceState.getBoolean(MODE_KEY);
    }

    @Override
    protected void onResume() {
        super.onResume();

        podcastManager.addLoadPodcastListener(this);
        podcastManager.addLoadPodcastLogoListener(this);

        // Make sure dividers (if any) reflect selection state
        updateDivider();
    }

    @Override
    protected void onPause() {
        super.onPause();

        podcastManager.removeLoadPodcastListener(this);
        podcastManager.removeLoadPodcastLogoListener(this);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // outState.putString(ShowEpisodeListActivity.PODCAST_URL_KEY,
        // selectedPodcast.getUrl().toString());
        outState.putBoolean(MODE_KEY, multiplePodcastsMode);
    }

    @Override
    public void onPodcastLoadProgress(Podcast podcast, Progress progress) {
        switch (viewMode) {
            case LARGE_PORTRAIT_VIEW:
            case LARGE_LANDSCAPE_VIEW:
            case SMALL_LANDSCAPE_VIEW:
                if (multiplePodcastsMode)
                    findPodcastListFragment().showProgress(podcastManager.indexOf(podcast),
                            progress);
                // No break here intentionally, code down below should run
            case SMALL_PORTRAIT_VIEW:
                if (!multiplePodcastsMode && podcast.equals(currentPodcast))
                    findEpisodeListFragment().showProgress(progress);
        }
    }

    @Override
    public void onPodcastLoaded(Podcast podcast) {
        switch (viewMode) {
            case LARGE_LANDSCAPE_VIEW:
            case LARGE_PORTRAIT_VIEW:
            case SMALL_LANDSCAPE_VIEW:
                // This will display the number of episodes
                PodcastListFragment podcastListFragment = findPodcastListFragment();
                podcastListFragment.refresh();

                // Tell the podcast manager to load podcast logo
                podcastManager.loadLogo(podcast,
                        podcastListFragment.getLogoViewWidth(),
                        podcastListFragment.getLogoViewHeight());

                // No break here intentionally, code down below should run
            case SMALL_PORTRAIT_VIEW:
                // Update list fragment to show episode list
                EpisodeListFragment episodeListFragment = findEpisodeListFragment();

                if (multiplePodcastsMode) {
                    // TODO decide on this: episodeList.addAll(list.subList(0,
                    // list.size() > 100 ? 100 : list.size() - 1));
                    if (podcast.getEpisodes().size() > 0) {
                        currentEpisodeList.addAll(podcast.getEpisodes());
                        Collections.sort(currentEpisodeList);
                        episodeListFragment.setEpisodes(currentEpisodeList);
                    }
                }
                else if (podcast.equals(currentPodcast)) {
                    currentEpisodeList = podcast.getEpisodes();
                    episodeListFragment.setEpisodes(currentEpisodeList);

                }
        }

        // Additionally, if on large device, process clever selection update
        if (viewMode == LARGE_LANDSCAPE_VIEW || viewMode == LARGE_PORTRAIT_VIEW) {
            EpisodeListFragment episodeListFragment = findEpisodeListFragment();
            EpisodeFragment episodeFragment = findEpisodeFragment();

            if (currentEpisodeList != null && currentEpisodeList.contains(currentEpisode))
                episodeListFragment.select(currentEpisodeList.indexOf(currentEpisode));
        }
    }

    @Override
    public void onPodcastLoadFailed(Podcast failedPodcast) {
        switch (viewMode) {
            case LARGE_LANDSCAPE_VIEW:
            case LARGE_PORTRAIT_VIEW:
            case SMALL_LANDSCAPE_VIEW:
                // This will display the number of episodes
                findPodcastListFragment().refresh();

                // No break here intentionally, code down below should run
            case SMALL_PORTRAIT_VIEW:
                // TODO What happens in multiple podcast mode?
                if (!multiplePodcastsMode)
                    findEpisodeListFragment().showLoadFailed();
        }
    }

    @Override
    public void onPodcastLogoLoaded(Podcast podcast, Bitmap logo) {
        if (podcast.equals(currentPodcast))
            findPodcastListFragment().showLogo(logo);
    }

    @Override
    public void onPodcastLogoLoadFailed(Podcast podcast) {
        // pass
    }

    @Override
    public void onEpisodeSelected(Episode selectedEpisode) {
        this.currentEpisode = selectedEpisode;

        switch (viewMode) {
            case LARGE_PORTRAIT_VIEW:
            case LARGE_LANDSCAPE_VIEW:
                // Set episode in episode fragment
                findEpisodeFragment().setEpisode(selectedEpisode);
                // Make sure selection matches in list fragment
                // findEpisodeListFragment().selectEpisode(selectedEpisode);
                break;
            case SMALL_LANDSCAPE_VIEW:
                // Find, and if not already done create, episode fragment
                EpisodeFragment episodeFragment = findEpisodeFragment();
                if (episodeFragment == null)
                    episodeFragment = new EpisodeFragment();
                // Add the fragment to the UI, placing the list fragment
                FragmentTransaction transaction = getFragmentManager().beginTransaction();
                transaction.replace(R.id.content, episodeFragment, episodeFragmentTag);
                transaction.addToBackStack(null);
                transaction.commit();
                // Set the episode
                episodeFragment.setEpisode(selectedEpisode);
                break;
            case SMALL_PORTRAIT_VIEW:
                // Send intent to open episode as a new activity
                Intent intent = new Intent(this, ShowEpisodeActivity.class);
                intent.putExtra(PODCAST_URL_KEY, selectedEpisode.getPodcastUrl());
                intent.putExtra(EPISODE_URL_KEY, selectedEpisode.getMediaUrl().toExternalForm());

                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_left);
        }

        updateDivider();
    }

    @Override
    public void onNoEpisodeSelected() {
        this.currentEpisode = null;

        // If there is a episode fragment, reset it
        EpisodeListFragment episodeListFragment = findEpisodeListFragment();
        if (episodeListFragment != null)
            episodeListFragment.selectNone();

        updateDivider();
    }

    protected void updateDivider() {
        colorDivider(R.id.divider_first, currentPodcast != null || multiplePodcastsMode);
        colorDivider(R.id.divider_second, currentEpisode != null);
    }

    protected void colorDivider(int dividerViewId, boolean color) {
        if (getWindow() != null && getWindow().findViewById(dividerViewId) != null) {
            View divider = getWindow().findViewById(dividerViewId);
            divider.setBackgroundResource(color ? R.color.divider_on : R.color.divider_off);
        }
    }
}
