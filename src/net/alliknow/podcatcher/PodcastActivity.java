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

import android.app.ActionBar;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager.OnBackStackChangedListener;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.TranslateAnimation;
import android.widget.*;
import android.widget.LinearLayout.LayoutParams;

import net.alliknow.podcatcher.listeners.ContextMenuListener;
import net.alliknow.podcatcher.listeners.OnChangePodcastListListener;
import net.alliknow.podcatcher.listeners.OnLoadPodcastListListener;
import net.alliknow.podcatcher.listeners.OnSelectPodcastListener;
import net.alliknow.podcatcher.model.tasks.remote.LoadPodcastTask.PodcastLoadError;
import net.alliknow.podcatcher.model.types.Episode;
import net.alliknow.podcatcher.model.types.Podcast;
import net.alliknow.podcatcher.model.types.Progress;
import net.alliknow.podcatcher.view.ContextMenuView;
import net.alliknow.podcatcher.view.ControllerView;
import net.alliknow.podcatcher.view.ExpandCollapseAnimation;
import net.alliknow.podcatcher.view.fragments.AuthorizationFragment;
import net.alliknow.podcatcher.view.fragments.PodcastListFragment;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Our main activity class. Works as the main controller. Depending on the view
 * state, other activities cooperate.
 */
public class PodcastActivity extends EpisodeListActivity implements OnBackStackChangedListener,
        OnLoadPodcastListListener, OnChangePodcastListListener, OnSelectPodcastListener, ContextMenuListener {

    /**
     * The request code to identify import calls
     */
    private static final int IMPORT_FROM_SIMPLE_PODCATCHER_CODE = 18;
    /**
     * The import from Simple Podcatcher action
     */
    private static final String IMPORT_ACTION = "com.podcatcher.deluxe.action.IMPORT";
    /**
     * The key to find imported podcast name list under
     */
    private static final String IMPORT_PODCAST_NAMES_KEY = "podcast_names_key";
    /**
     * The key to find imported podcast url list under
     */
    private static final String IMPORT_PODCAST_URLS_KEY = "podcast_urls_key";

    /**
     * The current podcast list fragment
     */
    protected PodcastListFragment podcastListFragment;

    /**
     * Flag indicating whether the app should show the add podcast dialog if the
     * list of podcasts is empty.
     */
    private boolean isInitialAppStart = false;
    /**
     * Flag indicating the intent given onCreate contains data we want to use as
     * a podcast URL.
     */
    private boolean hasPodcastToAdd = false;

    /**
     * Flag indicating that the onResume() method has to make sure the UI
     * matches the current selection state.
     */
    private boolean needsUiUpdateOnResume;

    private Fragment lastFocusedFragment;
    private boolean isMenuFocused = false;

    private PodcastMenu mMenu;
    private ContextMenuView mEpisodeContextMenu;
    private PodcastContextMenu mPodcastContextMenu;

    private ViewGroup podcastListFragmentLayout;
    private ViewGroup episodeListFragmentLayout;
    private ViewGroup episodeFragmentLayout;
    private ViewGroup commonLayout;

    private ImageView moreButton;

    public void fragmentSelected(Fragment fragment) {
        boolean inTouchMode = getWindow().getDecorView().isInTouchMode();
        lastFocusedFragment = fragment;
        if (mMenu.mIsOpened) {
            mMenu.hide();
        }
        if (mPodcastContextMenu.mIsOpened) {
            mPodcastContextMenu.hide();
        }
        updateBackgrounds(lastFocusedFragment);
    }

    private void updateBackgrounds(Fragment selected) {
        podcastListFragment.updateBackground(selected == podcastListFragment);
        episodeListFragment.updateBackground(selected == episodeListFragment);
        episodeFragment.updateBackground(selected == episodeFragment);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Enable strict mode when on debug
        if (((Podcatcher) getApplication()).isInDebugMode())
            StrictMode.enableDefaults();

        // 1. Create the UI via XML layouts and fragments
        // Inflate the main content view (depends on view mode)
        setContentView(R.layout.main);
        // Make sure all fragment member handles are properly set
        findFragments();

        // 2. Register listeners (done after the fragments are available so we
        // do not end up getting call-backs without the possibility to act on
        // them).
        registerListeners();

        lastFocusedFragment = podcastListFragment;

        mMenu = new PodcastMenu();
        mPodcastContextMenu = new PodcastContextMenu();

        podcastListFragmentLayout = (ViewGroup) findViewById(R.id.podcast_list_layout);
        episodeListFragmentLayout = (ViewGroup) findViewById(R.id.episode_list_layout);
        episodeFragmentLayout = (ViewGroup) findViewById(R.id.episode_layout);
        commonLayout = (ViewGroup) findViewById(R.id.move_me);

        moreButton = (ImageView) findViewById(R.id.more_button);
        moreButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (v.isInTouchMode() && selection.getEpisode() != null) {
                    showEpisodeBlock(selection.getEpisode());
                }
            }
        });

        getActionBar().setCustomView(R.layout.action_bar);
        getActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);

        getWindow().getDecorView().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mMenu.mIsOpened && v != mMenu.lvMenu) {
                    mMenu.hide();
                }
            }
        });

        // 3. Init/restore the app as needed
        // If we are newly starting up and the podcast list is empty, show add
        // podcast dialog (this is used in onPodcastListLoaded(), since we only
        // know then, whether the list is actually empty). Also do not show it
        // if we are given an URL in the intent, because this will trigger the
        // dialog anyway.
        isInitialAppStart = (savedInstanceState == null);
        hasPodcastToAdd = (getIntent().getData() != null);
        needsUiUpdateOnResume = true;
        // Check if podcast list is available - if so, set it
        List<Podcast> podcastList = podcastManager.getPodcastList();
        if (podcastList != null) {
            onPodcastListLoaded(podcastList);

            // We only reset our state if the podcast list is available, because
            // otherwise we will not be able to select anything.
            if (getIntent().hasExtra(MODE_KEY))
                onNewIntent(getIntent());
        }

        // Finally we might also be called freshly with a podcast feed to add
        if (getIntent().getData() != null)
            onNewIntent(getIntent());
    }

    @Override
    protected void findFragments() {
        super.findFragments();

        // The podcast list fragment to use
        if (podcastListFragment == null)
            podcastListFragment = (PodcastListFragment) findByTagId(R.string.podcast_list_fragment_tag);
    }

    @Override
    protected void registerListeners() {
        super.registerListeners();

        // Register as listener to the podcast data manager
        podcastManager.addLoadPodcastListListener(this);
        podcastManager.addChangePodcastListListener(this);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        // This is an external call to add a new podcast
        if (intent.getData() != null) {
            Intent addPodcast = new Intent(this, AddPodcastActivity.class);
            addPodcast.setData(intent.getData());

            // We need to cut back the selection here when is small portrait
            // mode to prevent other activities from covering the add podcast
            // dialog
            if (view.isSmallPortrait())
                selection.reset();

            startActivity(addPodcast);
            // Reset data to prevent this intent from fire again on the next
            // configuration change
            intent.setData(null);
        }
        // This is an internal call to update the selection
        else if (intent.hasExtra(MODE_KEY)) {
            selection.setFullscreenEnabled(false);

            selection.setMode((ContentMode) intent.getSerializableExtra(MODE_KEY));
            selection.setPodcast(podcastManager.findPodcastForUrl(
                    intent.getStringExtra(PODCAST_URL_KEY)));
            selection.setEpisode(podcastManager.findEpisodeForUrl(
                    intent.getStringExtra(EPISODE_URL_KEY)));

            needsUiUpdateOnResume = true;
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();

        // Prevent duplicate login dialog
        final DialogFragment authFragment = (DialogFragment)
                getFragmentManager().findFragmentByTag(AuthorizationFragment.TAG);

        if (view.isSmallPortrait() && authFragment != null)
            authFragment.dismiss();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (needsUiUpdateOnResume) {
            needsUiUpdateOnResume = false;

            // Restore UI to match selection:
            // Re-select previously selected podcast(s)
            if (selection.isAll())
                onAllPodcastsSelected(true);
            else if (selection.isSingle() && selection.isPodcastSet())
                onPodcastSelected(selection.getPodcast(), true);
            else if (ContentMode.DOWNLOADS.equals(selection.getMode()))
                onDownloadsSelected();
            else if (ContentMode.PLAYLIST.equals(selection.getMode()))
                onPlaylistSelected();
            else
                onNoPodcastSelected(true);

            // Re-select previously selected episode
            if (selection.isEpisodeSet())
                onEpisodeSelected(selection.getEpisode(), true);
            else
                onNoEpisodeSelected(true);
        }

        // Make sure we are alerted on back stack changes. This needs to be
        // added after re-selection of the current content.
        getFragmentManager().addOnBackStackChangedListener(this);
        // Set podcast logo view mode
//        updateLogoViewMode();
    }

    @Override
    public void onEpisodeSelected(Episode selectedEpisode) {
        super.onEpisodeSelected(selectedEpisode);
        updateEpisodeBlock(selectedEpisode);
        if (mMenu.mIsOpened) {
            mMenu.hide();
        }
        if (mPodcastContextMenu.mIsOpened) {
            mPodcastContextMenu.hide();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
//        isMenuFocused = false;
//        toggleMenuFocus();
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Disable listener (would interfere with resume)
        getFragmentManager().removeOnBackStackChangedListener(this);

        // Make sure we persist the podcast manager state
        podcastManager.saveState();
    }

    @Override
    protected void onStop() {
        super.onStop();

        // Make sure our http cache is written to disk
        ((Podcatcher) getApplication()).flushHttpCache();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Unregister the listeners
        podcastManager.removeLoadPodcastListListener(this);
        podcastManager.removeChangePodcastListListener(this);
    }

    @Override
    public void onBackStackChanged() {
        // This only needed in small landscape mode and in case
        // we go back to the episode list
        if (view.isSmallLandscape()
                && getFragmentManager().getBackStackEntryCount() == 0) {
            onNoEpisodeSelected();
        }
    }

    @Override
    public void onPodcastListLoaded(List<Podcast> podcastList) {
        // Make podcast list show
        podcastListFragment.setPodcastList(podcastList);

        // Make action bar show number of podcasts
//        updateActionBar();

        // If podcast list is empty we try to import from Simple Podcatcher
        if (podcastManager.size() == 0 && isInitialAppStart && !hasPodcastToAdd) {
            try {
                Intent importFromSimple = new Intent(IMPORT_ACTION);
                startActivityForResult(importFromSimple, IMPORT_FROM_SIMPLE_PODCATCHER_CODE);
            } catch (ActivityNotFoundException ex) {
                // Simple Podcatcher is not installed, we do not need to call
                // onActivityResult() since the system will do this
            }
        }
        // If enabled, we run the "select all on start-up" action
        else if (podcastManager.size() > 0 && isInitialAppStart
                && ((Podcatcher) getApplication()).isOnline()
                && preferences.getBoolean(SettingsActivity.KEY_SELECT_ALL_ON_START, false)) {
            onAllPodcastsSelected();
            selection.setEpisodeFilterEnabled(true);
        }

        podcastListFragment.getView().requestFocus();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Only run this if we were called back from onPodcastListLoaded(). This
        // means that we have no podcasts available and this is app start-up
        // time.
        if (requestCode == IMPORT_FROM_SIMPLE_PODCATCHER_CODE) {
            boolean needsAddPodcastDialog = true;

            // Find if we got some podcasts
            if (data != null) {
                final List<String> names = data.getStringArrayListExtra(IMPORT_PODCAST_NAMES_KEY);
                final List<String> urls = data.getStringArrayListExtra(IMPORT_PODCAST_URLS_KEY);
                // Yes, we got some podcasts from the Simple Podcatcher
                if (names != null && names.size() > 0) {
                    // Make sure dialog does not pop up
                    needsAddPodcastDialog = false;
                    // Import all podcasts
                    for (String name : names) {
                        final int index = names.indexOf(name);

                        try {
                            podcastManager.addPodcast(new Podcast(name, new URL(urls.get(index))));
                        } catch (MalformedURLException e) {
                            // pass
                        }
                    }
                }
            }

            // If nothing is there, show add podcasts dialog
            if (needsAddPodcastDialog) {
                isInitialAppStart = false;

                // On the very first start of the app, show the first run dialog
                if (preferences.getBoolean(SettingsActivity.KEY_FIRST_RUN, true))
                    startActivity(new Intent(this, FirstRunActivity.class));
                    // Otherwise, just show the add podcast dialog
                else
                    startActivity(new Intent(this, AddPodcastActivity.class));
            }
        }
    }

    @Override
    public void onPodcastAdded(Podcast podcast) {
        // Update podcast list
        podcastListFragment.addPodcast(podcast);
        // Update UI
//        updateActionBar();

        switch (view) {
            case SMALL_PORTRAIT:
                // Nothing is selected, just show the new podcast list
                selection.reset();
                break;
            case SMALL_LANDSCAPE:
                // Select the new podcast...
                selection.resetEpisode();
                selection.setPodcast(podcast);
                // .. but only run selection onResume()
                needsUiUpdateOnResume = true;
                break;
            case LARGE_PORTRAIT:
            case LARGE_LANDSCAPE:
                // Immediately select new podcast
                onPodcastSelected(podcast);
                break;
        }
    }

    @Override
    public void onPodcastRemoved(Podcast podcast) {
        // Update podcast list
        podcastListFragment.removePodcast(podcast);
        // Update UI
//        updateActionBar();

        // Reset selection if deleted
        if (podcast.equals(selection.getPodcast()))
            onNoPodcastSelected();
        else if (selection.isPodcastSet())
            onPodcastSelected(selection.getPodcast(), true);
    }

    @Override
    public void onPodcastSelected(Podcast podcast) {
        onPodcastSelected(podcast, false);
    }

    private void onPodcastSelected(Podcast podcast, boolean forceReload) {
        if (forceReload || !podcast.equals(selection.getPodcast())) {
            super.onPodcastSelected(podcast);

            if (view.isSmallPortrait())
                showEpisodeListActivity();
            else
                // Select in podcast list
                podcastListFragment.select(podcastManager.indexOf(podcast));

            // Update UI
//            updateLogoViewMode();
        }

        if (mMenu.mIsOpened) {
            mMenu.hide();
        }
        if (mPodcastContextMenu.mIsOpened) {
            mPodcastContextMenu.hide();
        }
    }

    @Override
    public void onAllPodcastsSelected() {
        onAllPodcastsSelected(false);
    }

    private void onAllPodcastsSelected(boolean forceReload) {
        if (forceReload || !selection.isAll()) {
            super.onAllPodcastsSelected();

            // Prepare podcast list fragment
            podcastListFragment.selectAll();

            if (view.isSmallPortrait())
                showEpisodeListActivity();

            // Update UI
//            updateLogoViewMode();
        }
    }

    @Override
    public void onDownloadsSelected() {
        super.onDownloadsSelected();

        // Prepare podcast list fragment
        podcastListFragment.selectNone();

        if (view.isSmallPortrait())
            showEpisodeListActivity();

        // Update UI
//        updateLogoViewMode();
    }

    @Override
    public void onPlaylistSelected() {
        super.onPlaylistSelected();

        // Prepare podcast list fragment
        podcastListFragment.selectNone();

        if (view.isSmallPortrait())
            showEpisodeListActivity();

        // Update UI
//        updateLogoViewMode();
    }

    @Override
    public void onNoPodcastSelected() {
        onNoPodcastSelected(false);
    }

    private void onNoPodcastSelected(boolean forceReload) {
        if (forceReload || selection.getPodcast() != null) {
            super.onNoPodcastSelected();

            // Reset podcast list fragment
            podcastListFragment.selectNone();
            // Update UI
//            updateLogoViewMode();
        }
    }

    @Override
    public void onPodcastLoadProgress(Podcast podcast, Progress progress) {
        // Only react on progress here, if the activity is visible
        if (!view.isSmallPortrait()) {
            super.onPodcastLoadProgress(podcast, progress);

            // We are in select all mode, show progress in podcast list
            if (selection.isAll())
                podcastListFragment.showProgress(podcastManager.indexOf(podcast), progress);
        }
    }

    @Override
    public void onPodcastLoaded(Podcast podcast) {
        // This will display the number of episodes
        podcastListFragment.refresh();

        // Tell the podcast manager to load podcast logo
        podcastManager.loadLogo(podcast);

        // In small portrait mode, work is done in separate activity
        if (!view.isSmallPortrait())
            super.onPodcastLoaded(podcast);
    }

    @Override
    public void onPodcastLoadFailed(Podcast failedPodcast, PodcastLoadError code) {
        podcastListFragment.refresh();

        // Tell the podcast manager to load podcast logo even though the podcast
        // failed to load since the podcast logo might be available offline.
        podcastManager.loadLogo(failedPodcast);

        // In small portrait mode, work is done in separate activity
        if (!view.isSmallPortrait())
            super.onPodcastLoadFailed(failedPodcast, code);
    }

    @Override
    public void onPodcastLogoLoaded(Podcast podcast) {
        super.onPodcastLogoLoaded(podcast);

        // marshal it to the podcast list fragment
        podcastListFragment.onPodcastLogoLoaded(podcast);

//        updateLogoViewMode();
    }

    @Override
    public void onDownloadProgress(Episode episode, int percent) {
        // In small portrait mode, there is a separate episode list activity
        // that will handle this
        if (!view.isSmallPortrait())
            super.onDownloadProgress(episode, percent);
    }

    @Override
    public void onDownloadFailed(Episode episode) {
        super.onDownloadFailed(episode);

        showToast(getString(R.string.download_failed, episode.getName()));
    }

    /**
     * Update the layout to match user's preference
     */
//    protected void updateLayout() {
//        final boolean useWide = preferences
//                .getBoolean(SettingsActivity.KEY_WIDE_EPISODE_LIST, false);
//
//        switch (view) {
//            case LARGE_PORTRAIT:
//                setMainColumnWidthWeight(episodeListFragment.getView(), useWide ? 3.5f : 3f);
//
//                break;
//            case LARGE_LANDSCAPE:
//                setMainColumnWidthWeight(episodeListFragment.getView(), useWide ? 3.5f : 3f);
//                setMainColumnWidthWeight(findViewById(R.id.right_column), useWide ? 3.5f : 4f);
//
//                break;
//            default:
//                // Nothing to do in small views
//                break;
//        }
//    }

    /**
     * Update the logo view mode according to current app state.
     */
//    protected void updateLogoViewMode() {
//        LogoViewMode logoViewMode = LogoViewMode.NONE;
//
//        if (view.isLargeLandscape() && selection.isSingle())
//            logoViewMode = LogoViewMode.LARGE;
//        else if (view.isSmallPortrait())
//            logoViewMode = LogoViewMode.SMALL;
//
//        podcastListFragment.updateLogo(logoViewMode);
//    }

//    @Override
//    protected void updateActionBar() {
//        // Disable the home button (only used in overlaying activities)
//        getActionBar().setHomeButtonEnabled(false);

//        if (!view.isSmall() && selection.isAll())
//            updateActionBarSubtitleOnMultipleLoad();
//        else
//            contentSpinner.setSubtitle(null);
//    }
    @Override
    protected void updateDownloadUi() {
        if (!view.isSmallPortrait())
            super.updateDownloadUi();
    }

    @Override
    protected void updatePlaylistUi() {
        if (!view.isSmallPortrait())
            super.updatePlaylistUi();
    }

    @Override
    protected void updateStateUi() {
        if (!view.isSmallPortrait())
            super.updateStateUi();

        podcastListFragment.refresh();
    }

    @Override
    protected void updatePlayerUi() {
        super.updatePlayerUi();

//        if (view.isSmallPortrait()) {
//            playerFragment.setLoadMenuItemVisibility(false, false, false);
//            playerFragment.setPlayerTitleVisibility(true);
//        }
    }

    private void showEpisodeListActivity() {
        // We need to launch a new activity to display the episode list
        Intent intent = new Intent(this, ShowEpisodeListActivity.class);

        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_left);
    }

    private void setMainColumnWidthWeight(View view, float weight) {
        view.setLayoutParams(
                new LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, weight));
    }

    @Override
    public void onBackPressed() {
        boolean inTouchMode = getWindow().getDecorView().isInTouchMode();
        if (mMenu.mIsOpened || mPodcastContextMenu.mIsOpened) {
            // menu hides automatically
            mMenu.hide();
            if (!inTouchMode) {
                lastFocusedFragment.getView().requestFocus();
            }
            return;
        }
        if (episodeFragmentShown) {
            hideEpisodeBlock();
            if (!inTouchMode) {
                episodeListFragment.getView().requestFocus();
            }
            return;
        }
        super.onBackPressed();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case 84:                        // MENU button
                mMenu.toggle();
                break;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                if (podcastListFragment.getView().hasFocus()) {
                    return true;
                }
                break;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                if (episodeListFragment.getView().hasFocus() && selection.getEpisode() != null) {
                    showEpisodeBlock(selection.getEpisode());
                    return true;
                }
                if (episodeFragment.getView().hasFocus()) {
                    return true;
                }
                break;

            case KeyEvent.KEYCODE_DPAD_UP:
                if (mMenu.mIsOpened) {
                    mMenu.controllerView.requestFocus();
                } else {
//                    lastFocusedFragment.getView().onKeyDown(keyCode, event);
                    if (lastFocusedFragment == episodeFragment) {
                        episodeFragment.onKey(null, keyCode, event);
                    }
                }
                return true;

            case KeyEvent.KEYCODE_DPAD_DOWN:
                if (lastFocusedFragment == episodeFragment) {
                    episodeFragment.onKey(null, keyCode, event);
                }
                return true;
//                if (isMenuFocused) {
//                    return !getActionBar().getCustomView().isFocused();
//                }
        }
        return super.onKeyDown(keyCode, event);
    }

//    @Override
//    public boolean onKeyUp(int keyCode, KeyEvent event) {
//        switch (keyCode) {
//            case KeyEvent.KEYCODE_DPAD_DOWN:
//            case KeyEvent.KEYCODE_DPAD_UP:
//                lastFocusedFragment.getView().onKeyUp(keyCode, event);
//        }
//        return super.onKeyUp(keyCode, event);
//    }

    public void showEpisodeBlock(Episode episode) {
        episodeFragment.setEpisode(episode);
        if (!episodeFragmentShown) {
//            final Integer delta = getResources().getInteger(R.integer.podcast_list_width_delta);

            final int startOffset = 200;
            final int duration = getResources().getInteger(R.integer.episode_show_anim_duration);
            final int podcastListWidth = podcastListFragmentLayout.getWidth();

//            Animation podcastListAnimation = new TranslateAnimation(
//                    0, -1 * podcastListWidth, 0, 0
//            );
//            podcastListAnimation.setDuration(duration);
//            podcastListAnimation.setFillAfter(true);
//            podcastListAnimation.setAnimationListener(new Animation.AnimationListener() {
//                @Override
//                public void onAnimationStart(Animation animation) {
//                }
//
//                @Override
//                public void onAnimationEnd(Animation animation) {
////                    podcastListFragmentLayout.setVisibility(View.GONE);
////                    episodeListFragmentLayout.startAnimation(episodeListAnimation);
//                }
//
//                @Override
//                public void onAnimationRepeat(Animation animation) {
//                }
//            });
            Animation podcastListAnimation = new ExpandCollapseAnimation(
                    podcastListFragmentLayout,
                    (int) getResources().getDimension(R.dimen.podcast_list_collapsed_width),
                    ExpandCollapseAnimation.DIRECTION_COLLAPSE
            );
            podcastListAnimation.setDuration(duration);
//
//            final Animation episodeListAnimation = new ExpandCollapseAnimation(
//                    episodeListFragmentLayout,
//                    (int) getResources().getDimension(R.dimen.episode_list_collapsed_width),
//                    ExpandCollapseAnimation.DIRECTION_COLLAPSE
//            );
//            episodeListAnimation.setDuration(duration);
//
//            Animation episodeListTranslate = new TranslateAnimation(
//                    0, -1 * podcastListWidth, 0, 0
//            );
//            episodeListTranslate.setDuration(duration);
//            episodeListTranslate.setAnimationListener(new Animation.AnimationListener() {
//                @Override
//                public void onAnimationStart(Animation animation) {
//                }
//
//                @Override
//                public void onAnimationEnd(Animation animation) {
//                    podcastListFragmentLayout.setVisibility(View.INVISIBLE);
//
////                    podcastListFragmentLayout.setVisibility(View.GONE);
////                    episodeListFragmentLayout.startAnimation(episodeListAnimation);
//                }
//
//                @Override
//                public void onAnimationRepeat(Animation animation) {
//                }
//            });
//            episodeListTranslate.setFillAfter(true);
//
            Animation episodeAnimation = AnimationUtils.loadAnimation(this, R.anim.context_menu_expand);
//            Animation episodeAnimation = new TranslateAnimation(
//                    episodeFragmentLayout.getWidth(), 0, 0, 0
//            );
            episodeAnimation.setDuration(duration);
            episodeAnimation.setFillAfter(true);
            episodeAnimation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
//                    podcastListFragmentLayout.setVisibility(View.GONE);
                    episodeFragmentLayout.setVisibility(View.VISIBLE);
                    episodeFragment.focusOnMenu();
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    episodeFragmentShown = true;
                    episodeFragment.getView().requestFocus();
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }
            });
//            episodeAnimation.setStartOffset(startOffset);

//            Animation commonAnimation = new TranslateAnimation(
//                    0, -1 * podcastListWidth, 0, 0
//            );
//            commonAnimation.setDuration(duration);
//            commonAnimation.setFillAfter(true);

            Animation moreButtonAnimation = AnimationUtils.loadAnimation(this, R.anim.disappear);
            moreButtonAnimation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                    episodeFragmentLayout.setVisibility(View.VISIBLE);
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    moreButton.setVisibility(View.GONE);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }
            });

//            commonLayout.startAnimation(commonAnimation);
//            episodeListFragmentLayout.startAnimation(episodeListAnimation);
            podcastListFragmentLayout.setAnimation(podcastListAnimation);
//            podcastListFragmentLayout.startAnimation(episodeListAnimation);
//            episodeListFragmentLayout.setAnimation(episodeListTranslate);
            moreButton.setAnimation(moreButtonAnimation);
            episodeFragmentLayout.startAnimation(episodeAnimation);
        } else {
            episodeFragment.getView().requestFocus();
        }
    }

    public void updateEpisodeBlock(Episode episode) {
        if (episodeFragmentShown) {
            episodeFragment.setEpisode(episode);
        }
    }

    public void hideEpisodeBlock() {
        if (episodeFragmentShown) {

            final int startOffset = 200;

            Animation podcastListAnimation = new ExpandCollapseAnimation(
                    podcastListFragmentLayout,
                    (int) getResources().getDimension(R.dimen.podcast_list_expanded_width),
                    ExpandCollapseAnimation.DIRECTION_EXPAND
            );
            podcastListAnimation.setDuration(getResources().getInteger(R.integer.episode_show_anim_duration));

            final Animation moreButtonAnimation = AnimationUtils.loadAnimation(this, R.anim.appear);
            moreButtonAnimation.setStartOffset(startOffset);

            Animation episodeAnimation = AnimationUtils.loadAnimation(this, R.anim.context_menu_collapse);
            episodeAnimation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                    podcastListFragment.getView().requestFocus();
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    episodeFragmentLayout.setVisibility(View.GONE);
                    episodeFragmentShown = false;
                    moreButton.setVisibility(View.VISIBLE);
                    moreButton.startAnimation(moreButtonAnimation);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }
            });
            episodeAnimation.setStartOffset(startOffset);

            podcastListFragmentLayout.startAnimation(podcastListAnimation);
            episodeFragmentLayout.startAnimation(episodeAnimation);
        }
    }

    @Override
    public void onEpisodeContextMenuOpen(Episode episode) {
//        mEpisodeContextMenu.initialize(episode);
//        mEpisodeContextMenu.show();
//        episodeFragment.setEpisode(episode);
        showEpisodeBlock(episode);
    }

    @Override
    public void onEpisodeContextMenuClose() {
//        mEpisodeContextMenu.hide();
//        lastFocusedFragment.getView().requestFocus();
//        hideEpisodeBlock();
    }

    @Override
    public void onPodcastContextMenuOpen(Podcast podcast) {
        mPodcastContextMenu.show(podcast);
    }

    @Override
    public void onPodcastContextMenuClose() {
        mPodcastContextMenu.hide();
    }

    @Override
    public void onPodcastListFocused() {
        if (episodeFragmentShown) {
            hideEpisodeBlock();
        }
    }

    public void onMenuButtonClick(View v) {
        mMenu.toggle();
    }

    public class PodcastMenu implements AdapterView.OnItemClickListener, View.OnFocusChangeListener {

        private static final int ITEM_ADD_PODCAST = 0;
        private static final int ITEM_REVERSE_ORDER = 1;
        private static final int ITEM_FILTER = 2;
        private static final int ITEM_SELECT_ALL = 3;
        private static final int ITEM_DOWNLOADS = 4;
        private static final int ITEM_PLAYLIST = 5;
        private static final int ITEM_PREFERENCES = 6;
        private static final int ITEM_HELP = 7;
        private static final int ITEM_ABOUT = 8;

        boolean mIsOpened = false;

        LinearLayout layout;
        ControllerView controllerView;
        ListView lvMenu;

        public PodcastMenu() {
            layout = (LinearLayout) findViewById(R.id.main_menu);

            lvMenu = (ListView) layout.findViewById(R.id.main_menu_list);
            lvMenu.setAdapter(
                    new ArrayAdapter<String>(PodcastActivity.this, R.layout.options_menu_item, formList())
            );
            lvMenu.setOnItemClickListener(this);

            controllerView = (ControllerView) layout.findViewById(R.id.controller_view);
        }

        private List<String> formList() {
            List<String> list = new ArrayList<String>(6);
            list.add(getString(R.string.menu_add));
            list.add(getString(R.string.menu_reverse_order));
            list.add(getString(R.string.menu_filter));
            list.add(getString(R.string.podcast_select_all));
            list.add(getString(R.string.downloads));
            list.add(getString(R.string.playlist));
            list.add(getString(R.string.preferences));
            list.add(getString(R.string.help));
            list.add(getString(R.string.about));
            return list;
        }

        public void show() {
            layout.setVisibility(View.VISIBLE);
            layout.setEnabled(true);
            mIsOpened = true;
            Animation animation = AnimationUtils.loadAnimation(PodcastActivity.this, R.anim.menu_show);
            layout.setAnimation(animation);
            animation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                    selectFirst();
                    lvMenu.requestFocus();
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }
            });

            controllerView.setOnFocusChangeListener(this);
            lvMenu.setOnFocusChangeListener(this);
        }

        public void hide() {
            if (!mIsOpened) {
                return;
            }
            lastFocusedFragment.getView().requestFocus();
//            layout.clearFocus();
            mIsOpened = false;
            layout.setAnimation(AnimationUtils.loadAnimation(PodcastActivity.this, R.anim.menu_hide));
            layout.setVisibility(View.INVISIBLE);
            layout.setEnabled(false);
            layout.setOnFocusChangeListener(null);

//            if (!layout.isInTouchMode()) {
//            }
        }

        public void toggle() {
            if (mIsOpened) {
                hide();
            } else {
                show();
            }
        }

        private void selectFirst() {
            if (lvMenu != null) {
                lvMenu.setSelection(0);
            }
        }

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            switch (position) {
                case ITEM_ADD_PODCAST:
                    startActivity(new Intent(PodcastActivity.this, AddPodcastActivity.class));
                    break;

                case ITEM_REVERSE_ORDER:
                    onReverseOrder();
                    break;

                case ITEM_FILTER:
                    onToggleFilter();
                    break;

//                case ITEM_DOWNLOAD:
//                    onToggleDownload();
//                    break;

                case ITEM_SELECT_ALL:
                    onAllPodcastsSelected();
                    break;

                case ITEM_DOWNLOADS:
                    onDownloadsSelected();
                    break;

                case ITEM_PLAYLIST:
                    onPlaylistSelected();
                    break;

                case ITEM_PREFERENCES:
                    startActivity(new Intent(PodcastActivity.this, SettingsActivity.class));
                    break;

                case ITEM_HELP:
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(PODCATCHER_HELPSITE)));
                    } catch (ActivityNotFoundException e) {
                        // We are in a restricted profile without a browser
                        showToast(getString(R.string.no_browser));
                    }
                    break;

                case ITEM_ABOUT:
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(PODCATCHER_WEBSITE)));
                    } catch (ActivityNotFoundException e) {
                        // We are in a restricted profile without a browser
                        showToast(getString(R.string.no_browser));
                    }
                    break;
            }

            hide();
        }

        @Override
        public void onFocusChange(View v, boolean hasFocus) {
            if (!layout.hasFocus()) {
                hide();
            }
        }
    }

    public class PodcastContextMenu implements AdapterView.OnItemClickListener, View.OnFocusChangeListener {

        private static final int ITEM_DOWNLOAD_PODCAST = 0;
        private static final int ITEM_REMOVE_PODCAST = 1;

        boolean mIsOpened = false;
        Podcast mPodcast;

        ListView lvMenu;

        public PodcastContextMenu() {
            lvMenu = (ListView) findViewById(R.id.podcast_context_menu);
            lvMenu.setAdapter(
                    new ArrayAdapter<String>(PodcastActivity.this, android.R.layout.simple_list_item_1, formList())
            );
            lvMenu.setOnItemClickListener(this);
        }

        private List<String> formList() {
            List<String> list = new ArrayList<String>(2);
            list.add(getString(R.string.podcast_context_download));
            list.add(getString(R.string.podcast_context_remove));
            return list;
        }

        public void show(Podcast podcast) {
            mPodcast = podcast;
            lvMenu.setVisibility(View.VISIBLE);
            lvMenu.setEnabled(true);
            mIsOpened = true;
            Animation animation = AnimationUtils.loadAnimation(PodcastActivity.this, R.anim.menu_show);
            lvMenu.setAnimation(animation);
            animation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                    selectFirst();
                    lvMenu.requestFocus();
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }
            });
            lvMenu.setOnFocusChangeListener(this);
        }

        public void hide() {
            if (!mIsOpened) {
                return;
            }
            lvMenu.clearFocus();
            mIsOpened = false;
            lvMenu.setAnimation(AnimationUtils.loadAnimation(PodcastActivity.this, R.anim.menu_hide));
            lvMenu.setVisibility(View.INVISIBLE);
            lvMenu.setEnabled(false);
            lvMenu.setOnFocusChangeListener(null);
            mPodcast = null;

            if (!getWindow().getDecorView().isInTouchMode()) {
                lastFocusedFragment.getView().requestFocus();
            }
        }

        public void toggle(Podcast podcast) {
            if (mIsOpened) {
                hide();
            } else {
                show(podcast);
            }
        }

        private void selectFirst() {
            if (lvMenu != null) {
                lvMenu.setSelection(0);
            }
        }

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            // Get the checked positions
//            SparseBooleanArray checkedItems = podcastListFragment.getListView().getCheckedItemPositions();
//            ArrayList<Integer> positions = new ArrayList<Integer>();
//
//            // Prepare list of podcast positions to send to the triggered activity
//            for (int index = 0; index < podcastListFragment.getListView().getCount(); index++)
//                if (checkedItems.get(index))
//                    positions.add(index);

            ArrayList<Integer> positions = new ArrayList<Integer>(1);
            positions.add(podcastManager.indexOf(mPodcast));

            switch (position) {
                case ITEM_DOWNLOAD_PODCAST:
                    // Prepare export activity
                    Intent export = new Intent(PodcastActivity.this, ExportOpmlActivity.class);
                    export.putIntegerArrayListExtra(PODCAST_POSITION_LIST_KEY, positions);

                    // Go export podcasts
                    startActivity(export);
                    break;

                case ITEM_REMOVE_PODCAST:
                    // Prepare deletion activity
                    Intent remove = new Intent(PodcastActivity.this, RemovePodcastActivity.class);
                    remove.putIntegerArrayListExtra(PODCAST_POSITION_LIST_KEY, positions);

                    // Go remove podcasts
                    startActivity(remove);
                    break;
            }

            hide();
        }

        @Override
        public void onFocusChange(View v, boolean hasFocus) {
            if (!hasFocus) {
                hide();
            }
        }

    }
}
