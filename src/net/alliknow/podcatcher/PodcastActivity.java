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

    /**
     * Flag indicating whether the podcast list changed while the activity was
     * paused or stopped.
     */
    private boolean podcastListChanged = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Enable strict mode when on debug
        if (((Podcatcher) getApplication()).isInDebugMode())
            StrictMode.enableDefaults();

        // 1. Register listeners
        // Register as listener to the podcast data manager
        podcastManager.addLoadPodcastListListener(this);
        podcastManager.addChangePodcastListListener(this);
        // Make sure we are alerted on back stack changes
        getFragmentManager().addOnBackStackChangedListener(this);

        // 2. Create the UI via XML layouts and fragments
        // Inflate the main content view (depends on view mode)
        setContentView(R.layout.main);
        // Make sure all fragment member handles are properly set
        findFragments();
        // Add extra fragments needed in some view modes
        plugFragments();

        // 3. Init/restore the app as needed
        // Check if podcast list is available - if so, set it
        List<Podcast> podcastList = podcastManager.getPodcastList();
        if (podcastList != null) {
            onPodcastListLoaded(podcastList);

            // We only reset our state if the podcast list is available and this
            // is not the initial create of the activity
            if (savedInstanceState != null)
                restoreState(savedInstanceState);
        }
    }

    @Override
    protected void findFragments() {
        super.findFragments();

        // The podcast list fragment to use
        if (podcastListFragment == null)
            podcastListFragment = (PodcastListFragment) findByTagId(R.string.podcast_list_fragment_tag);
    }

    /**
     * In certain view modes, we need to add some fragments because they are not
     * set in the layout XML files. Member variables will be set if needed.
     */
    private void plugFragments() {
        // On small screens, add the podcast list fragment
        if ((viewMode == SMALL_PORTRAIT_VIEW || viewMode == SMALL_LANDSCAPE_VIEW)
                && podcastListFragment == null) {
            podcastListFragment = new PodcastListFragment();
            getFragmentManager()
                    .beginTransaction()
                    .add(R.id.content, podcastListFragment,
                            getResources().getString(R.string.podcast_list_fragment_tag))
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

    /**
     * Restore members and selection state from bundle.
     * 
     * @param savedInstanceState Restore information.
     */
    private void restoreState(Bundle savedInstanceState) {
        // Recover members
        multiplePodcastsMode = savedInstanceState.getBoolean(MODE_KEY);
        currentPodcast = podcastManager.findPodcastForUrl(
                savedInstanceState.getString(PODCAST_URL_KEY));
        currentEpisode = podcastManager.findEpisodeForUrl(
                savedInstanceState.getString(EPISODE_URL_KEY));

        restoreSelection();
    }

    /**
     * Restore selection to match member variables
     */
    private void restoreSelection() {
        // Re-select previously selected podcast(s)
        if (multiplePodcastsMode)
            onAllPodcastsSelected();
        else if (currentPodcast != null)
            onPodcastSelected(currentPodcast);
        else
            onNoPodcastSelected();

        // Re-select previously selected episode
        if (currentEpisode != null)
            onEpisodeSelected(currentEpisode);
        else
            onNoEpisodeSelected();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        // Recover members
        multiplePodcastsMode = intent.getBooleanExtra(MODE_KEY, false);
        currentPodcast = podcastManager.findPodcastForUrl(
                intent.getStringExtra(PODCAST_URL_KEY));
        currentEpisode = podcastManager.findEpisodeForUrl(
                intent.getStringExtra(EPISODE_URL_KEY));

        restoreSelection();
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Load all podcasts? TODO Make this a preference

        // Set podcast logo view mode
        updateLogoViewMode();
        // Make sure dividers (if any) reflect selection state
        updateDivider();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Podcast list has been changed while we were stopped
        if (podcastListChanged) {
            // Reset flag
            podcastListChanged = false;

            // Show the last podcast added
            if (currentPodcast != null && !multiplePodcastsMode)
                onPodcastSelected(currentPodcast);
            // Selected podcast was deleted
            else if (currentPodcast == null && !multiplePodcastsMode)
                onNoPodcastSelected();
        }
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
    public void onBackStackChanged() {
        // This only needed in small landscape mode and in case
        // we go back to the episode list
        if (viewMode == SMALL_LANDSCAPE_VIEW
                && getFragmentManager().getBackStackEntryCount() == 0) {
            onNoEpisodeSelected();
        }
    }

    @Override
    public void onPodcastListLoaded(List<Podcast> podcastList) {
        // Make podcast list show
        podcastListFragment.setPodcastList(podcastList);

        // Make action bar show number of podcasts
        updateActionBar();

        // If podcast list is empty we show dialog on startup
        if (podcastManager.size() == 0)
            startActivity(new Intent(this, AddPodcastActivity.class));
    }

    @Override
    public void onPodcastAdded(Podcast podcast) {
        // Pick up the change in onRestart()
        podcastListChanged = true;

        // Set the member
        currentPodcast = podcast;
        // Update podcast list
        podcastListFragment.setPodcastList(podcastManager.getPodcastList());
    }

    @Override
    public void onPodcastRemoved(Podcast podcast) {
        // Reset member if deleted
        if (podcast.equals(currentPodcast)) {
            currentPodcast = null;

            // Pick up the change in onRestart()
            podcastListChanged = true;
        }

        // Update podcast list
        podcastListFragment.setPodcastList(podcastManager.getPodcastList());
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
        if (viewMode != SMALL_PORTRAIT_VIEW)
            super.onPodcastLoadProgress(podcast, progress);

        if (viewMode != SMALL_PORTRAIT_VIEW && multiplePodcastsMode)
            podcastListFragment.showProgress(podcastManager.indexOf(podcast), progress);
    }

    @Override
    public void onPodcastLoaded(Podcast podcast) {
        // This will display the number of episodes
        podcastListFragment.refresh();

        // In small portrait mode, work is done in separate activity
        if (viewMode != SMALL_PORTRAIT_VIEW) {
            // All the work is done upstairs
            super.onPodcastLoaded(podcast);

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

    /**
     * Update the logo view mode according to current app state.
     */
    protected void updateLogoViewMode() {
        if (podcastListFragment != null) {
            // Set podcast logo view mode
            podcastListFragment.setLogoVisibility(
                    viewMode == LARGE_LANDSCAPE_VIEW && !multiplePodcastsMode ?
                            LogoViewMode.LARGE : LogoViewMode.SMALL);
        }
    }

    @Override
    protected void updateActionBar() {
        getActionBar().setTitle(R.string.app_name);

        // Disable the home button (only used in overlaying activities)
        getActionBar().setHomeButtonEnabled(false);

        if (podcastManager.getPodcastList() != null) {
            int podcastCount = podcastManager.size();
            getActionBar().setSubtitle(podcastCount == 1 ?
                    getResources().getString(R.string.one_podcast_selected) :
                    podcastCount + " " + getResources().getString(R.string.podcasts_selected));
        } else {
            getActionBar().setSubtitle(null);
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
