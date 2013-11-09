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

import static net.alliknow.podcatcher.view.fragments.AuthorizationFragment.USERNAME_PRESET_KEY;

import android.app.ActionBar;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;

import net.alliknow.podcatcher.listeners.OnEnterAuthorizationListener;
import net.alliknow.podcatcher.listeners.OnLoadPodcastListener;
import net.alliknow.podcatcher.listeners.OnLoadPodcastLogoListener;
import net.alliknow.podcatcher.listeners.OnReverseSortingListener;
import net.alliknow.podcatcher.listeners.OnSelectPodcastListener;
import net.alliknow.podcatcher.model.types.Episode;
import net.alliknow.podcatcher.model.types.Podcast;
import net.alliknow.podcatcher.model.types.Progress;
import net.alliknow.podcatcher.view.ContentSpinner;
import net.alliknow.podcatcher.view.fragments.AuthorizationFragment;
import net.alliknow.podcatcher.view.fragments.EpisodeListFragment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Show list of episodes activity. This is thought of as an abstract activity
 * for an app only consisting of an episode list view, the player and the
 * ability to show an {@link ShowEpisodeActivity} on top. Sub-classes could
 * extend or simply show this layout.
 */
public abstract class EpisodeListActivity extends EpisodeActivity implements
        OnLoadPodcastListener, OnEnterAuthorizationListener, OnLoadPodcastLogoListener,
        OnSelectPodcastListener, OnReverseSortingListener {

    /** Key used to save the current content mode in bundle */
    public static final String MODE_KEY = "mode_key";
    /** Key used to store podcast URL in intent or bundle */
    public static final String PODCAST_URL_KEY = "podcast_url_key";

    /** The current episode list fragment */
    protected EpisodeListFragment episodeListFragment;
    /** The content mode selection spinner view */
    protected ContentSpinner contentSpinner;

    /** The current episode set (ordered) */
    private SortedSet<Episode> currentEpisodeSet;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Create the content mode spinner and add it to the action bar
        contentSpinner = new ContentSpinner(this, this);
        getActionBar().setCustomView(contentSpinner);
        // Make sure the action bar has the right display options set
        getActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM
                | ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_USE_LOGO);
    }

    @Override
    protected void findFragments() {
        super.findFragments();

        // The episode list fragment
        if (episodeListFragment == null)
            episodeListFragment = (EpisodeListFragment) findByTagId(R.string.episode_list_fragment_tag);

        // Make sure the episode fragment know our theme colors
        if (episodeListFragment != null)
            episodeListFragment.setThemeColors(themeColor, lightThemeColor);
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
        updateDividerUi();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        podcastManager.removeLoadPodcastListener(this);
        podcastManager.removeLoadPodcastLogoListener(this);
    }

    @Override
    public void onReverseOrder() {
        selection.setEpisodeOrderReversed(!selection.isEpisodeOrderReversed());

        updateEpisodeListUi();
        updateSortingUi();
    }

    @Override
    public void onPodcastSelected(Podcast podcast) {
        selection.setPodcast(podcast);
        selection.setMode(ContentMode.SINGLE_PODCAST);

        this.currentEpisodeSet = new TreeSet<Episode>();

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
                updateSortingUi();
                updateDividerUi();

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
        selection.resetPodcast();
        selection.setMode(ContentMode.ALL_PODCASTS);

        this.currentEpisodeSet = new TreeSet<Episode>();

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
                if (podcastManager.size() > 0)
                    episodeListFragment.resetAndSpin();
                else
                    episodeListFragment.resetUi();

                episodeListFragment.setShowPodcastNames(true);
                // Update other UI
                updateSortingUi();
                updateDividerUi();

                // Go load all podcasts
                for (Podcast podcast : podcastManager.getPodcastList())
                    podcastManager.load(podcast);

                // Action bar needs update after loading has started
                updateActionBar();
                break;
            case SMALL_PORTRAIT:
                // This case should be handled by sub-classes
                break;
        }
    }

    @Override
    public void onNoPodcastSelected() {
        selection.resetPodcast();
        selection.setMode(ContentMode.SINGLE_PODCAST);

        this.currentEpisodeSet = null;

        if (!view.isSmallPortrait()) {
            // If there is an episode list visible, reset it
            episodeListFragment.selectNone();
            episodeListFragment.resetUi();

            // Update other UI
            updateSortingUi();
            updateDividerUi();
        }
    }

    @Override
    public void onAuthorizationRequired(Podcast podcast) {
        if (selection.isSingle() && podcast.equals(selection.getPodcast())) {
            // Ask the user for authorization
            final AuthorizationFragment authorizationFragment = new AuthorizationFragment();

            if (podcast.getUsername() != null) {
                // Create bundle to make dialog aware of username to pre-set
                final Bundle args = new Bundle();
                args.putString(USERNAME_PRESET_KEY, podcast.getUsername());
                authorizationFragment.setArguments(args);
            }

            authorizationFragment.show(getFragmentManager(), AuthorizationFragment.TAG);
        } else
            onPodcastLoadFailed(podcast);
    }

    @Override
    public void onSubmitAuthorization(String username, String password) {
        if (selection.isPodcastSet()) {
            final Podcast podcast = selection.getPodcast();
            podcastManager.setCredentials(podcast, username, password);

            // We need to unselect the podcast here in order to make it
            // selectable again...
            selection.setPodcast(null);

            onPodcastSelected(podcast);
        }
    }

    @Override
    public void onCancelAuthorization() {
        if (selection.isPodcastSet())
            onPodcastLoadFailed(selection.getPodcast());
    }

    @Override
    public void onPodcastLoadProgress(Podcast podcast, Progress progress) {
        try {
            if (selection.isSingle() && podcast.equals(selection.getPodcast()))
                episodeListFragment.showProgress(progress);
        } catch (NullPointerException nep) {
            // When the load progress comes to quickly, the fragment might not
            // be present yet, pass...
        }
    }

    @Override
    public void onPodcastLoaded(Podcast podcast) {
        // Update list fragment to show episode list
        if (selection.isAll() || selection.isSingle() && podcast.equals(selection.getPodcast())) {
            currentEpisodeSet.addAll(podcast.getEpisodes());
            updateEpisodeListUi();
        }

        // Additionally, if on large device, process clever selection update
        if (!view.isSmall()) {
            updateEpisodeListSelection();
            updateDividerUi();
        }

        updateActionBar();
        updateSortingUi();
    }

    @Override
    public void onPodcastLoadFailed(Podcast failedPodcast) {
        // The podcast we are waiting for failed to load
        if (selection.isSingle() && failedPodcast.equals(selection.getPodcast()))
            episodeListFragment.showLoadFailed();
        // One of potentially many podcasts failed
        else if (selection.isAll()) {
            // The last podcast failed and we have no episodes at all
            if (podcastManager.getLoadCount() == 0 && currentEpisodeSet.isEmpty())
                episodeListFragment.showLoadFailed();
            // One of many podcasts failed to load
            else
                showToast(getString(R.string.podcast_load_multiple_error, failedPodcast.getName()));
        }

        // Update other UI
        updateActionBar();
        updateSortingUi();
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
        onEpisodeSelected(selectedEpisode, false);
    }

    protected void onEpisodeSelected(Episode selectedEpisode, boolean forceReload) {
        if (forceReload || !selectedEpisode.equals(selection.getEpisode())) {
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

            updateDividerUi();
        }
    }

    @Override
    public void onNoEpisodeSelected() {
        onNoEpisodeSelected(false);
    }

    protected void onNoEpisodeSelected(boolean forceReload) {
        if (forceReload || selection.getEpisode() != null) {
            super.onNoEpisodeSelected();

            if (episodeListFragment != null)
                episodeListFragment.selectNone();

            updateDividerUi();
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        super.onSharedPreferenceChanged(sharedPreferences, key);

        if (key.equals(SettingsActivity.KEY_THEME_COLOR)) {
            // Make the UI reflect the change
            if (episodeListFragment != null)
                episodeListFragment.setThemeColors(themeColor, lightThemeColor);
            updateDividerUi();
        }
    }

    /**
     * Make sure the episode list selection matches current state.
     */
    protected void updateEpisodeListSelection() {
        if (!view.isSmall())
            // Make sure the episode selection in the list is updated
            episodeListFragment.select(selection.getEpisode());
        else
            episodeListFragment.selectNone();
    }

    /**
     * Update the sorting menu icon visibility.
     */
    protected void updateSortingUi() {
        episodeListFragment.setSortMenuItemVisibility(
                currentEpisodeSet != null && !currentEpisodeSet.isEmpty(),
                selection.isEpisodeOrderReversed());
    }

    /**
     * Update the divider views to reflect current selection state.
     */
    protected void updateDividerUi() {
        colorDivider(R.id.divider_first, selection.isPodcastSet() || !selection.isSingle());
        colorDivider(R.id.divider_second, currentEpisodeSet != null && selection.isEpisodeSet()
                && currentEpisodeSet.contains(selection.getEpisode()));
    }

    /**
     * Set the action bar subtitle to reflect multiple podcast load progress
     */
    protected void updateActionBarSubtitleOnMultipleLoad() {
        final int podcastCount = podcastManager.size();
        final int loadingPodcastCount = podcastManager.getLoadCount();

        // Load finished for all podcasts and there are episode
        if (loadingPodcastCount == 0 && currentEpisodeSet != null) {
            final int episodeCount = currentEpisodeSet.size();

            if (episodeCount == 0)
                contentSpinner.setSubtitle(null);
            else
                contentSpinner.setSubtitle(getResources()
                        .getQuantityString(R.plurals.episodes, episodeCount, episodeCount));
        }
        // Load finished but no episodes
        else if (loadingPodcastCount == 0)
            contentSpinner.setSubtitle(getResources()
                    .getQuantityString(R.plurals.podcasts, podcastCount, podcastCount));
        // Load in progress
        else
            contentSpinner.setSubtitle(getString(R.string.podcast_load_multiple_progress,
                    (podcastCount - loadingPodcastCount), podcastCount));
    }

    /**
     * Set the current episode list to show in the episode list fragment using
     * {@link #currentEpisodeSet} as the basis. This will filter and reverse the
     * list as needed. Will have no effect if {@link #currentEpisodeSet} is
     * <code>null</code>.
     */
    private void updateEpisodeListUi() {
        if (currentEpisodeSet != null) {
            final List<Episode> filteredList = new ArrayList<Episode>(currentEpisodeSet);

            // We might need to reverse the order of our list,
            // but there is no need for sorting since we already come
            // from a sorted set.
            if (selection.isEpisodeOrderReversed())
                Collections.reverse(filteredList);

            // Make sure the episode list fragment show the right empty view
            else if (selection.isAll())
                episodeListFragment.setEmptyStringId(R.string.episode_none_all_podcasts);
            else
                episodeListFragment.setEmptyStringId(R.string.episode_none);

            // Finally set the list and make sure selection matches
            episodeListFragment.setEpisodeList(filteredList);
            updateEpisodeListSelection();
        }
    }

    private void colorDivider(int dividerViewId, boolean applyColor) {
        if (getWindow() != null && getWindow().findViewById(dividerViewId) != null) {
            View divider = getWindow().findViewById(dividerViewId);

            if (applyColor)
                divider.setBackgroundColor(themeColor);
            else
                divider.setBackgroundColor(getResources().getColor(R.color.divider_off));
        }
    }
}
