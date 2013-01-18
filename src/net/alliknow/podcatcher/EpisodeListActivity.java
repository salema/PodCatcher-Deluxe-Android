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
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;

import net.alliknow.podcatcher.listeners.OnLoadPodcastListener;
import net.alliknow.podcatcher.listeners.OnLoadPodcastLogoListener;
import net.alliknow.podcatcher.listeners.OnSelectPodcastListener;
import net.alliknow.podcatcher.model.tasks.Progress;
import net.alliknow.podcatcher.model.types.Episode;
import net.alliknow.podcatcher.model.types.Podcast;
import net.alliknow.podcatcher.view.fragments.EpisodeFragment;
import net.alliknow.podcatcher.view.fragments.EpisodeListFragment;
import net.alliknow.podcatcher.view.fragments.PodcastListFragment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Show list of episodes activity.
 */
public class EpisodeListActivity extends EpisodeActivity implements
        OnSelectPodcastListener, OnLoadPodcastListener, OnLoadPodcastLogoListener {

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

        // Re-select previously selected podcast
        if (currentPodcast != null && viewMode != SMALL_PORTRAIT_VIEW)
            onPodcastSelected(currentPodcast);
        else if (currentPodcast == null)
            onNoPodcastSelected();

        // Hide logo in small portrait
        if (viewMode == SMALL_PORTRAIT_VIEW)
            findPodcastListFragment().showLogo(false);

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
    public void onPodcastSelected(Podcast podcast) {
        this.currentPodcast = podcast;
        this.currentEpisode = null;
        this.currentEpisodeList = null;
        this.multiplePodcastsMode = false;

        // Stop loading previous tasks
        podcastManager.cancelAllLoadTasks();

        switch (viewMode) {
            case SMALL_LANDSCAPE_VIEW:
                // This will go back to the list view in case we are showing
                // episode details
                getFragmentManager().popBackStack();
                // There is no break here on purpose, we need to run the code
                // below as well
            case LARGE_PORTRAIT_VIEW:
            case LARGE_LANDSCAPE_VIEW:
                // Select in podcast list
                findPodcastListFragment().select(podcastManager.indexOf(podcast));
                // List fragment is visible, make it show progress UI
                EpisodeListFragment episodeListFragment = findEpisodeListFragment();
                episodeListFragment.resetAndSpin();
                updateDivider();

                // Load podcast
                podcastManager.load(podcast);
                break;
            case SMALL_PORTRAIT_VIEW:
                // We need to launch a new activity to display the episode list
                Intent intent = new Intent(this, ShowPodcastActivity.class);
                intent.putExtra(EpisodeListActivity.PODCAST_URL_KEY, podcast.getUrl()
                        .toString());
                intent.putExtra(MODE_KEY, false);

                startActivity(intent);
        }
    }

    @Override
    public void onAllPodcastsSelected() {
        this.currentPodcast = null;
        this.currentEpisode = null;
        this.currentEpisodeList = new ArrayList<Episode>();
        this.multiplePodcastsMode = true;

        // Stop loading previous tasks
        podcastManager.cancelAllLoadTasks();

        switch (viewMode) {
            case SMALL_LANDSCAPE_VIEW:
                // This will go back to the list view in case we are showing
                // episode details
                getFragmentManager().popBackStack();
                // There is no break here on purpose, we need to run the code
                // below as well
            case LARGE_PORTRAIT_VIEW:
            case LARGE_LANDSCAPE_VIEW:
                findPodcastListFragment().selectAll();
                // List fragment is visible, make it show progress UI
                EpisodeListFragment episodeListFragment = findEpisodeListFragment();
                episodeListFragment.resetAndSpin();
                updateDivider();

                for (Podcast podcast : podcastManager.getPodcastList())
                    podcastManager.load(podcast);

                break;
            case SMALL_PORTRAIT_VIEW:
                // We need to launch a new activity to display the episode list
                Intent intent = new Intent(this, ShowPodcastActivity.class);
                intent.putExtra(MODE_KEY, true);

                startActivity(intent);
        }
    }

    @Override
    public void onNoPodcastSelected() {
        this.currentPodcast = null;
        this.currentEpisode = null;
        this.currentEpisodeList = null;
        this.multiplePodcastsMode = false;

        findPodcastListFragment().selectNone();

        // If there is an episode list visible, reset it
        EpisodeListFragment episodeListFragment = findEpisodeListFragment();
        if (episodeListFragment != null)
            episodeListFragment.resetUi();

        updateDivider();
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
        super.onEpisodeSelected(selectedEpisode);

        updateDivider();
    }

    @Override
    public void onNoEpisodeSelected() {
        super.onNoEpisodeSelected();

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
