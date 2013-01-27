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

import android.app.FragmentManager.OnBackStackChangedListener;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.StrictMode;

import net.alliknow.podcatcher.listeners.OnChangePodcastListListener;
import net.alliknow.podcatcher.listeners.OnLoadPodcastListListener;
import net.alliknow.podcatcher.listeners.OnSelectPodcastListener;
import net.alliknow.podcatcher.model.tasks.Progress;
import net.alliknow.podcatcher.model.types.Episode;
import net.alliknow.podcatcher.model.types.Podcast;
import net.alliknow.podcatcher.view.fragments.EpisodeListFragment;
import net.alliknow.podcatcher.view.fragments.PodcastListFragment;
import net.alliknow.podcatcher.view.fragments.PodcastListFragment.LogoViewMode;

import java.util.ArrayList;
import java.util.List;

/**
 * Our main activity class. Works as the main controller. Depending on the view
 * state, other activities cooperate.
 */
public class PodcastActivity extends EpisodeListActivity implements OnBackStackChangedListener,
        OnLoadPodcastListListener, OnChangePodcastListListener, OnSelectPodcastListener {

    /** The current podcast list fragment */
    protected PodcastListFragment podcastListFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Enable strict mode when on debug
        if (((Podcatcher) getApplication()).isInDebugMode())
            StrictMode.enableDefaults();

        // Register as listener to the podcast data manager
        podcastManager.addLoadPodcastListListener(this);
        podcastManager.addChangePodcastListListener(this);

        // Make sure we are alerted on back stack changes
        getFragmentManager().addOnBackStackChangedListener(this);

        // Inflate the main content view (depends on view mode)
        setContentView(R.layout.main);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        // Recover members (used to restore in onResume)
        if (savedInstanceState != null) {
            multiplePodcastsMode = savedInstanceState.getBoolean(MODE_KEY);
            currentPodcast = podcastManager.findPodcastForUrl(
                    savedInstanceState.getString(PODCAST_URL_KEY));
            currentEpisode = podcastManager.findEpisodeForUrl(
                    savedInstanceState.getString(EPISODE_URL_KEY));
        }
    }

    @Override
    public void onBackStackChanged() {
        // This only needed in small landscape mode and in case
        // we go back to the episode list
        if (viewMode == SMALL_LANDSCAPE_VIEW
                && getFragmentManager().getBackStackEntryCount() == 0) {
            onNoEpisodeSelected();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Create and add fragments as needed
        String podcastListFragmentTag = getResources()
                .getString(R.string.podcast_list_fragment_tag);

        podcastListFragment = (PodcastListFragment) getFragmentManager().findFragmentByTag(
                podcastListFragmentTag);

        // On small screens, add the podcast list fragment
        if ((viewMode == SMALL_PORTRAIT_VIEW || viewMode == SMALL_LANDSCAPE_VIEW)
                && podcastListFragment == null) {
            podcastListFragment = new PodcastListFragment();

            getFragmentManager()
                    .beginTransaction()
                    .add(R.id.content, podcastListFragment, podcastListFragmentTag)
                    .commit();
        }
        // On small screens in landscape mode, add the episode list fragment
        if (viewMode == SMALL_LANDSCAPE_VIEW && episodeListFragment == null) {
            episodeListFragment = new EpisodeListFragment();

            getFragmentManager()
                    .beginTransaction()
                    .add(R.id.right, episodeListFragment,
                            getResources().getString(R.string.episode_list_fragment_tag))
                    .commit();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Load all podcasts? TODO Make this a preference

        // Check if podcast list is available - if so, set it
        List<Podcast> podcastList = podcastManager.getPodcastList();
        if (podcastList != null)
            onPodcastListLoaded(podcastList);

        // Re-select previously selected podcast(s)
        if (multiplePodcastsMode && viewMode != SMALL_PORTRAIT_VIEW)
            onAllPodcastsSelected();
        else if (currentPodcast != null && viewMode != SMALL_PORTRAIT_VIEW)
            onPodcastSelected(currentPodcast);
        else
            onNoPodcastSelected();

        // Re-select previously selected episode
        if (currentEpisode != null && viewMode != SMALL_PORTRAIT_VIEW)
            onEpisodeSelected(currentEpisode);
        else
            onNoEpisodeSelected();

        // Set podcast logo view mode
        updateLogoViewMode();
        // Make sure dividers (if any) reflect selection state
        updateDivider();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean(MODE_KEY, multiplePodcastsMode);
        if (currentPodcast != null)
            outState.putString(PODCAST_URL_KEY, currentPodcast.getUrl().toString());
        if (currentEpisode != null)
            outState.putString(EPISODE_URL_KEY, currentEpisode.getMediaUrl().toString());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Unregister the listeners
        podcastManager.removeLoadPodcastListListener(this);
        podcastManager.removeChangePodcastListListener(this);
        getFragmentManager().removeOnBackStackChangedListener(this);
    }

    @Override
    public void onPodcastListLoaded(List<Podcast> podcastList) {
        // Make podcast list show
        if (podcastListFragment != null)
            podcastListFragment.setPodcastList(podcastList);

        // If podcast list is empty we show dialog on startup
        if (podcastList.isEmpty())
            startActivity(new Intent(this, AddPodcastActivity.class));
    }

    @Override
    public void onPodcastAdded(Podcast podcast) {
        // There is nothing more to do here since we are paused
        // the selection will be picked up on resume.
        this.currentPodcast = podcast;

        // Update podcast list
        podcastListFragment.setPodcastList(podcastManager.getPodcastList());
    }

    @Override
    public void onPodcastRemoved(Podcast podcast) {
        if (podcast.equals(currentPodcast))
            this.currentPodcast = null;
    }

    @Override
    public void onPodcastSelected(Podcast podcast) {
        this.currentPodcast = podcast;
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
                podcastListFragment.select(podcastManager.indexOf(podcast));
                // List fragment is visible, make it show progress UI
                episodeListFragment.resetAndSpin();
                // Update other UI
                updateLogoViewMode();
                updateDivider();

                // Load podcast
                podcastManager.load(podcast);
                break;
            case SMALL_PORTRAIT_VIEW:
                // We need to launch a new activity to display the episode list
                Intent intent = new Intent(this, ShowEpisodeListActivity.class);
                intent.putExtra(EpisodeListActivity.PODCAST_URL_KEY,
                        podcast.getUrl().toString());
                intent.putExtra(MODE_KEY, false);

                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_left);
        }
    }

    @Override
    public void onAllPodcastsSelected() {
        this.currentPodcast = null;
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
                // Prepare podcast list fragment
                podcastListFragment.selectAll();
                // List fragment is visible, make it show progress UI
                episodeListFragment.resetAndSpin();
                episodeListFragment.setShowPodcastNames(true);
                // Update other UI
                updateLogoViewMode();
                updateDivider();

                for (Podcast podcast : podcastManager.getPodcastList())
                    podcastManager.load(podcast);

                break;
            case SMALL_PORTRAIT_VIEW:
                // We need to launch a new activity to display the episode list
                Intent intent = new Intent(this, ShowEpisodeListActivity.class);
                intent.putExtra(MODE_KEY, true);

                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_left);
        }
    }

    @Override
    public void onNoPodcastSelected() {
        this.currentPodcast = null;
        this.currentEpisodeList = null;
        this.multiplePodcastsMode = false;

        podcastListFragment.selectNone();

        switch (viewMode) {
            case SMALL_LANDSCAPE_VIEW:
            case LARGE_PORTRAIT_VIEW:
            case LARGE_LANDSCAPE_VIEW:
                // If there is an episode list visible, reset it
                episodeListFragment.selectNone();
                episodeListFragment.resetUi();

                // Update other UI
                updateLogoViewMode();
                updateDivider();
                break;
        }
    }

    @Override
    public void onPodcastLoadProgress(Podcast podcast, Progress progress) {
        super.onPodcastLoadProgress(podcast, progress);

        if (viewMode != SMALL_PORTRAIT_VIEW && multiplePodcastsMode)
            podcastListFragment.showProgress(podcastManager.indexOf(podcast), progress);
    }

    @Override
    public void onPodcastLoaded(Podcast podcast) {
        // In small portrait mode, work is done in separate activity
        if (viewMode != SMALL_PORTRAIT_VIEW) {
            // All the work is done upstairs
            super.onPodcastLoaded(podcast);

            // This will display the number of episodes
            podcastListFragment.refresh();

            // Tell the podcast manager to load podcast logo
            podcastManager.loadLogo(podcast,
                    podcastListFragment.getLogoViewWidth(),
                    podcastListFragment.getLogoViewHeight());
        }
    }

    @Override
    public void onPodcastLoadFailed(Podcast failedPodcast) {
        super.onPodcastLoadFailed(failedPodcast);

        if (viewMode != SMALL_PORTRAIT_VIEW)
            podcastListFragment.refresh();
    }

    @Override
    public void onPodcastLogoLoaded(Podcast podcast, Bitmap logo) {
        super.onPodcastLogoLoaded(podcast, logo);

        if (podcast.equals(currentPodcast))
            podcastListFragment.showLogo(logo);
    }

    protected void updateLogoViewMode() {
        if (podcastListFragment != null) {
            // Set podcast logo view mode
            podcastListFragment.setLogoVisibility(
                    viewMode == LARGE_LANDSCAPE_VIEW && !multiplePodcastsMode ?
                            LogoViewMode.LARGE : LogoViewMode.SMALL);
        }
    }

    @Override
    protected void updatePlayer() {
        super.updatePlayer();

        if (viewMode == SMALL_PORTRAIT_VIEW && playerFragment != null) {
            playerFragment.setLoadMenuItemVisibility(false, false);
            playerFragment.setPlayerTitleVisibility(true);
        }
    }
}
