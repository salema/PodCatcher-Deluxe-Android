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
import android.preference.PreferenceManager;
import android.view.View;

import net.alliknow.podcatcher.listeners.OnEnterAuthorizationListener;
import net.alliknow.podcatcher.listeners.OnLoadDownloadsListener;
import net.alliknow.podcatcher.listeners.OnLoadPlaylistListener;
import net.alliknow.podcatcher.listeners.OnLoadPodcastListener;
import net.alliknow.podcatcher.listeners.OnLoadPodcastLogoListener;
import net.alliknow.podcatcher.listeners.OnReorderEpisodeListener;
import net.alliknow.podcatcher.listeners.OnReverseSortingListener;
import net.alliknow.podcatcher.listeners.OnSelectPodcastListener;
import net.alliknow.podcatcher.listeners.OnToggleFilterListener;
import net.alliknow.podcatcher.model.tasks.LoadDownloadsTask;
import net.alliknow.podcatcher.model.tasks.LoadPlaylistTask;
import net.alliknow.podcatcher.model.types.Episode;
import net.alliknow.podcatcher.model.types.Podcast;
import net.alliknow.podcatcher.model.types.Progress;
import net.alliknow.podcatcher.view.ContentSpinner;
import net.alliknow.podcatcher.view.fragments.AuthorizationFragment;
import net.alliknow.podcatcher.view.fragments.EpisodeListFragment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
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
        OnSelectPodcastListener, OnLoadDownloadsListener, OnLoadPlaylistListener,
        OnReorderEpisodeListener, OnToggleFilterListener, OnReverseSortingListener {

    /** Key used to save the current content mode in bundle */
    public static final String MODE_KEY = "MODE_KEY";
    /** Key used to store podcast URL in intent or bundle */
    public static final String PODCAST_URL_KEY = "podcast_url";

    /** The current episode list fragment */
    protected EpisodeListFragment episodeListFragment;
    /** The content mode selection spinner view */
    protected ContentSpinner contentSpinner;

    /** The current episode set (ordered) */
    protected SortedSet<Episode> currentEpisodeSet;
    /** The filtered episode list */
    protected List<Episode> filteredEpisodeList;

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
    public void onReverseOrder() {
        selection.setEpisodeOrderReversed(!selection.isEpisodeOrderReversed());

        if (currentEpisodeSet != null)
            setSortedAndFilteredEpisodeList(currentEpisodeSet);
    }

    @Override
    public void onToggleFilter() {
        selection.setEpisodeFilterEnabled(!selection.isEpisodeFilterEnabled());

        if (currentEpisodeSet != null)
            setSortedAndFilteredEpisodeList(currentEpisodeSet);
    }

    @Override
    public void onToggleLoad() {
        super.onToggleLoad();

        // This might have changed the state of some episodes
        episodeListFragment.refresh();
    }

    @Override
    public void onPodcastSelected(Podcast podcast) {
        selection.setPodcast(podcast);
        selection.setMode(ContentMode.SINGLE_PODCAST);

        this.currentEpisodeSet = null;

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
                updateSorting();
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
        selection.resetPodcast();
        selection.setMode(ContentMode.ALL_PODCASTS);

        // We need to use a set here to avoid duplicates
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
                updateSorting();
                updateFilter();
                updateDivider();

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
    public void onDownloadsSelected() {
        selection.resetPodcast();
        selection.setMode(ContentMode.DOWNLOADS);

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

                new LoadDownloadsTask(this).execute((Void) null);

                break;
            case SMALL_PORTRAIT:
                // This case should be handled by sub-classes
                break;
        }
    }

    @Override
    public void onDownloadsLoaded(List<Episode> downloads) {
        this.currentEpisodeSet = new TreeSet<Episode>(downloads);
        setSortedAndFilteredEpisodeList(currentEpisodeSet);

        updateActionBar();
        updateDivider();
    }

    @Override
    public void onPlaylistSelected() {
        selection.resetPodcast();
        selection.setMode(ContentMode.PLAYLIST);

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
                episodeListFragment.setEnableSwipeReorder(true);

                new LoadPlaylistTask(this).execute((Void) null);

                break;
            case SMALL_PORTRAIT:
                // This case should be handled by sub-classes
                break;
        }
    }

    @Override
    public void onPlaylistLoaded(List<Episode> playlist) {
        this.currentEpisodeSet = new TreeSet<Episode>(playlist);
        setSortedAndFilteredEpisodeList(playlist);

        updateActionBar();
        updateDivider();
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
            updateSorting();
            updateFilter();
            updateDivider();
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
        }
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
        // Select all podcasts
        if (selection.isAll()) {
            if (currentEpisodeSet.addAll(podcast.getEpisodes()))
                setSortedAndFilteredEpisodeList(currentEpisodeSet);
        } // Select single podcast
        else if (selection.isSingle() && podcast.equals(selection.getPodcast())) {
            currentEpisodeSet = new TreeSet<Episode>(podcast.getEpisodes());
            addSpecialEpisodes(podcast);
            setSortedAndFilteredEpisodeList(currentEpisodeSet);
        }

        // Additionally, if on large device, process clever selection update
        if (!view.isSmall()) {
            updateEpisodeListSelection();
            updateDivider();
        }

        // We may want to auto-download the latest episode
        if (shouldAutoDownloadLatestEpisode(podcast))
            episodeManager.download(podcast.getEpisodes().get(0));

        updateActionBar();
    }

    @Override
    public void onPodcastLoadFailed(Podcast failedPodcast) {
        // The podcast we are waiting for failed to load
        if (selection.isSingle() && failedPodcast.equals(selection.getPodcast())) {
            this.currentEpisodeSet = new TreeSet<Episode>();
            addSpecialEpisodes(failedPodcast);
            // We might at least be able to show the downloaded episodes
            if (currentEpisodeSet != null && currentEpisodeSet.size() > 0) {
                setSortedAndFilteredEpisodeList(currentEpisodeSet);

                updateActionBar();
            }
            else {
                currentEpisodeSet = null;
                episodeListFragment.showLoadFailed();
            }
        }
        // The last podcast failed to load and none of the others had any
        // episodes to show in the list
        else if (selection.isAll() && podcastManager.getLoadCount() == 0
                && (currentEpisodeSet == null || currentEpisodeSet.isEmpty()))
            episodeListFragment.showLoadFailed();
        // One of many podcasts failed to load
        else if (selection.isAll())
            showToast(getString(R.string.podcast_load_multiple_error, failedPodcast.getName()));

        // Update UI
        updateActionBar();
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

            updateDivider();
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

            updateDivider();
        }
    }

    @Override
    public void onMoveEpisodeDown(Episode episode) {
        // Only accept movements if in playlist mode
        if (ContentMode.PLAYLIST.equals(selection.getMode())) {
            final int currentPosition = episodeManager.getPlaylistPosition(episode);
            final int playlistLength = episodeManager.getPlaylistSize();
            // Only move episode if it is actually in the playlist
            if (currentPosition >= 0) {
                episodeManager.removeFromPlaylist(episode);
                // If the episode is at the end of the playlist, send it back up
                episodeManager.insertAtPlaylistPosition(episode,
                        currentPosition == playlistLength - 1 ? 0 : currentPosition + 1);
            }

            onPlaylistLoaded(episodeManager.getPlaylist());
        }
    }

    @Override
    public void onMoveEpisodeUp(Episode episode) {
        // Only accept movements if in playlist mode
        if (ContentMode.PLAYLIST.equals(selection.getMode())) {
            final int currentPosition = episodeManager.getPlaylistPosition(episode);
            // Only move episode if it is actually in the playlist
            if (currentPosition > 0) {
                episodeManager.removeFromPlaylist(episode);
                episodeManager.insertAtPlaylistPosition(episode, currentPosition - 1);
            } // When at top, move to the bottom end
            else if (currentPosition == 0) {
                episodeManager.removeFromPlaylist(episode);
                episodeManager.appendToPlaylist(episode);
            }

            onPlaylistLoaded(episodeManager.getPlaylist());
        }
    }

    @Override
    public void onDownloadProgress(Episode episode, int percent) {
        if (!view.isSmallPortrait())
            super.onDownloadProgress(episode, percent);

        // Check whether the episode is potentially currently displayed
        if (filteredEpisodeList != null && filteredEpisodeList.contains(episode))
            episodeListFragment.showProgress(filteredEpisodeList.indexOf(episode), percent);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        super.onSharedPreferenceChanged(sharedPreferences, key);

        if (key.equals(SettingsActivity.KEY_THEME_COLOR)) {
            // Make the UI reflect the change
            if (episodeListFragment != null)
                episodeListFragment.setThemeColors(themeColor, lightThemeColor);
            updateDivider();
        }
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
     * Update the sorting menu icon visibility.
     */
    protected void updateSorting() {
        episodeListFragment.setSortMenuItemVisibility(
                currentEpisodeSet != null && !currentEpisodeSet.isEmpty()
                        && !ContentMode.PLAYLIST.equals(selection.getMode()),
                selection.isEpisodeOrderReversed());
    }

    /**
     * Update the filter menu icon visibility.
     */
    protected void updateFilter() {
        episodeListFragment.setFilterMenuItemVisibility(
                currentEpisodeSet != null && !currentEpisodeSet.isEmpty()
                        && !ContentMode.PLAYLIST.equals(selection.getMode()),
                selection.isEpisodeFilterEnabled());
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
        colorDivider(R.id.divider_second, currentEpisodeSet != null && selection.isEpisodeSet()
                && currentEpisodeSet.contains(selection.getEpisode()));
    }

    /**
     * Filter, sort and, set the current episode list to show in the episode
     * list fragment.
     */
    private void setSortedAndFilteredEpisodeList(Collection<Episode> completeList) {
        filteredEpisodeList = new ArrayList<Episode>(completeList);

        // Further refine the episode list if not in playlist mode
        if (!ContentMode.PLAYLIST.equals(selection.getMode())) {
            // Apply the filter
            if (selection.isEpisodeFilterEnabled()) {
                Iterator<Episode> iterator = filteredEpisodeList.iterator();

                while (iterator.hasNext())
                    if (episodeManager.getState(iterator.next()))
                        iterator.remove();
            }

            // We might need to reverse the order of our list,
            // but there is no need for sorting since we already come
            // from a sorted set.
            if (selection.isEpisodeOrderReversed())
                Collections.reverse(filteredEpisodeList);
        }

        // Make sure the episode list fragment show the right empty view
        if (ContentMode.DOWNLOADS.equals(selection.getMode()))
            episodeListFragment.setEmptyStringId(R.string.downloads_none);
        else if (ContentMode.PLAYLIST.equals(selection.getMode()))
            episodeListFragment.setEmptyStringId(R.string.playlist_empty);
        else if (selection.isEpisodeFilterEnabled()
                && filteredEpisodeList.isEmpty() && !completeList.isEmpty())
            episodeListFragment.setEmptyStringId(R.string.episodes_no_new);
        else
            episodeListFragment.setEmptyStringId(R.string.episode_none);

        // Make sure the episode list fragment show the right info box
        if (ContentMode.PLAYLIST.equals(selection.getMode()) && filteredEpisodeList.size() > 1)
            episodeListFragment.setShowTopInfoBox(true, getString(R.string.playlist_swipe_reorder));
        else if (selection.isEpisodeFilterEnabled()) {
            final int filteredCount = completeList.size() - filteredEpisodeList.size();

            episodeListFragment.setShowTopInfoBox(filteredCount > 0, getResources()
                    .getQuantityString(R.plurals.episodes_filtered, filteredCount, filteredCount));
        }
        else
            episodeListFragment.setShowTopInfoBox(false, null);

        episodeListFragment.setEpisodeList(filteredEpisodeList);
        updateEpisodeListSelection();

        // Update other UI
        updateSorting();
        updateFilter();
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

    /**
     * Set the action bar subtitle to reflect multiple podcast load progress
     */
    protected void updateActionBarSubtitleOnMultipleLoad() {
        final int podcastCount = podcastManager.size();
        final int loadingPodcastCount = podcastManager.getLoadCount();

        // Load finished for all podcasts and there are episode
        if (loadingPodcastCount == 0 && currentEpisodeSet != null) {
            final int episodeCount = currentEpisodeSet.size();
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

    private void addSpecialEpisodes(Podcast podcast) {
        if (currentEpisodeSet != null && podcast != null) {
            // Downloads
            for (Episode episode : episodeManager.getDownloads())
                if (podcast.equals(episode.getPodcast()) && !currentEpisodeSet.contains(episode))
                    currentEpisodeSet.add(episode);
            // Playlist
            for (Episode episode : episodeManager.getPlaylist())
                if (podcast.equals(episode.getPodcast()) && !currentEpisodeSet.contains(episode))
                    currentEpisodeSet.add(episode);
        }
    }

    private boolean shouldAutoDownloadLatestEpisode(Podcast podcast) {
        if (podcast == null || podcast.getEpisodeNumber() == 0)
            return false;
        else {
            final Episode latestEpisode = podcast.getEpisodes().get(0);

            return PreferenceManager.getDefaultSharedPreferences(this)
                    .getBoolean(SettingsActivity.AUTO_DOWNLOAD_KEY, false)
                    && ((Podcatcher) getApplication()).isOnFastConnection()
                    && !episodeManager.getState(latestEpisode);
        }
    }
}
