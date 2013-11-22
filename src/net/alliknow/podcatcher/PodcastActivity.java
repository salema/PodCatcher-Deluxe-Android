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

import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager.OnBackStackChangedListener;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.SparseBooleanArray;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;

import android.widget.ListView;
import net.alliknow.podcatcher.listeners.ContextMenuListener;
import net.alliknow.podcatcher.listeners.OnChangePodcastListListener;
import net.alliknow.podcatcher.listeners.OnLoadPodcastListListener;
import net.alliknow.podcatcher.listeners.OnSelectPodcastListener;
import net.alliknow.podcatcher.model.tasks.remote.LoadPodcastTask.PodcastLoadError;
import net.alliknow.podcatcher.model.types.Episode;
import net.alliknow.podcatcher.model.types.Podcast;
import net.alliknow.podcatcher.model.types.Progress;
import net.alliknow.podcatcher.view.ContextMenuView;
import net.alliknow.podcatcher.view.fragments.AuthorizationFragment;
import net.alliknow.podcatcher.view.fragments.EpisodeListFragment;
import net.alliknow.podcatcher.view.fragments.PodcastListFragment;
import net.alliknow.podcatcher.view.fragments.PodcastListFragment.LogoViewMode;

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
        // Add extra fragments needed in some view modes
        plugFragments();
        // Make sure the podcast list knows about our theme colors.
        podcastListFragment.setThemeColors(themeColor, lightThemeColor);
        // Make sure the layout matches the preference
        updateLayout();

        // 2. Register listeners (done after the fragments are available so we
        // do not end up getting call-backs without the possibility to act on
        // them).
        registerListeners();

        // 3. Init/restore the app as needed
        // If we are newly starting up and the podcast list is empty, show add
        // podcast dialog (this is used in onPodcastListLoaded(), since we only
        // know then, whether the list is actually empty). Also do not show it
        // if we are given an URL in the intent, because this will trigger the
        // dialog anyway.
        isInitialAppStart = (savedInstanceState == null);
        hasPodcastToAdd = (getIntent().getData() != null);
        needsUiUpdateOnResume = !isInitialAppStart;
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

        lastFocusedFragment = podcastListFragment;

        mMenu = new PodcastMenu();
        mEpisodeContextMenu = (ContextMenuView) findViewById(R.id.context_menu_view);
        mPodcastContextMenu = new PodcastContextMenu();
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
//        if (view.isSmall() && podcastListFragment == null) {
//            podcastListFragment = new PodcastListFragment();
//            getFragmentManager()
//                    .beginTransaction()
//                    .add(R.id.content, podcastListFragment,
//                            getString(R.string.podcast_list_fragment_tag))
//                    .commit();
//        }
        // On small screens in landscape mode, add the episode list fragment
        if (view.isSmallLandscape() && episodeListFragment == null) {
            episodeListFragment = new EpisodeListFragment();
            episodeListFragment.setThemeColors(themeColor, lightThemeColor);

            getFragmentManager()
                    .beginTransaction()
                    .add(R.id.right, episodeListFragment,
                            getString(R.string.episode_list_fragment_tag))
                    .commit();
        }
    }

    @Override
    protected void registerListeners() {
        super.registerListeners();

        // Register as listener to the podcast data manager
        podcastManager.addLoadPodcastListListener(this);
        podcastManager.addChangePodcastListListener(this);
    }

    ;

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
        updateLogoViewMode();
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
            updateLogoViewMode();
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
            updateLogoViewMode();
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
        updateLogoViewMode();
    }

    @Override
    public void onPlaylistSelected() {
        super.onPlaylistSelected();

        // Prepare podcast list fragment
        podcastListFragment.selectNone();

        if (view.isSmallPortrait())
            showEpisodeListActivity();

        // Update UI
        updateLogoViewMode();
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
            updateLogoViewMode();
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

        updateLogoViewMode();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        super.onSharedPreferenceChanged(sharedPreferences, key);

        if (key.equals(SettingsActivity.KEY_THEME_COLOR) && podcastListFragment != null) {
            // Make the UI reflect the change
            podcastListFragment.setThemeColors(themeColor, lightThemeColor);
        } else if (key.equals(SettingsActivity.KEY_WIDE_EPISODE_LIST)) {
            updateLayout();
        }
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
    protected void updateLayout() {
        final boolean useWide = preferences
                .getBoolean(SettingsActivity.KEY_WIDE_EPISODE_LIST, false);

        switch (view) {
            case LARGE_PORTRAIT:
                setMainColumnWidthWeight(episodeListFragment.getView(), useWide ? 3.5f : 3f);

                break;
            case LARGE_LANDSCAPE:
                setMainColumnWidthWeight(episodeListFragment.getView(), useWide ? 3.5f : 3f);
                setMainColumnWidthWeight(findViewById(R.id.right_column), useWide ? 3.5f : 4f);

                break;
            default:
                // Nothing to do in small views
                break;
        }
    }

    /**
     * Update the logo view mode according to current app state.
     */
    protected void updateLogoViewMode() {
        LogoViewMode logoViewMode = LogoViewMode.NONE;

        if (view.isLargeLandscape() && selection.isSingle())
            logoViewMode = LogoViewMode.LARGE;
        else if (view.isSmallPortrait())
            logoViewMode = LogoViewMode.SMALL;

        podcastListFragment.updateLogo(logoViewMode);
    }

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

        if (view.isSmallPortrait()) {
            playerFragment.setLoadMenuItemVisibility(false, false, false);
            playerFragment.setPlayerTitleVisibility(true);
        }
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
        if (mMenu.mIsOpened) {
            // menu hides automatically
            lastFocusedFragment.getView().requestFocus();
            return;
        }
        if (mEpisodeContextMenu.isOpened() || mPodcastContextMenu.mIsOpened) {
            // menu hides automatically
            lastFocusedFragment.getView().requestFocus();
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
                if (!mMenu.mIsOpened && !mEpisodeContextMenu.isOpened()) {
                    boolean consumed = lastFocusedFragment.getView().dispatchKeyEvent(event);
                    if (!consumed) {
                            moveFocusToLeft();
                    }
                    return true;
                }
                break;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                    boolean consumed = lastFocusedFragment.getView().dispatchKeyEvent(event);
                    if (!consumed) {
                        moveFocusToRight();
                    }
                    return true;

            case KeyEvent.KEYCODE_DPAD_UP:
//                if (isMenuFocused) {
//                    getActionBar().getCustomView().dispatchKeyEvent(event);
//                    return true;
//                } else {
                lastFocusedFragment.getView().dispatchKeyEvent(event);
                return true;
//                }
//            case KeyEvent.KEYCODE_DPAD_DOWN:
//                if (isMenuFocused) {
//                    return !getActionBar().getCustomView().isFocused();
//                }
        }
        return super.onKeyDown(keyCode, event);
    }

    private void moveFocusToLeft() {
        if (lastFocusedFragment == episodeFragment) {
            lastFocusedFragment = episodeListFragment;
            episodeListFragment.getView().requestFocus();
        } else if (lastFocusedFragment == episodeListFragment) {
            lastFocusedFragment = podcastListFragment;
            podcastListFragment.getView().requestFocus();
        }
        // otherwise podcast list fragment has focus,
        // it's on the left edge of the screen,
        // do nothing
    }

    private void moveFocusToRight() {
        if (lastFocusedFragment == podcastListFragment) {
            lastFocusedFragment = episodeListFragment;
            episodeListFragment.getView().requestFocus();
        } else if (lastFocusedFragment == episodeListFragment) {
            lastFocusedFragment = episodeFragment;
            episodeFragment.getView().requestFocus();
        }
        // otherwise episode fragment has focus,
        // it's on the right edge of the screen,
        // do nothing
    }

    public boolean isMenuOpened() {
        return mMenu.mIsOpened || mEpisodeContextMenu.isOpened() || mPodcastContextMenu.mIsOpened;
    }

    @Override
    public void onEpisodeContextMenuOpen(Episode episode) {
        mEpisodeContextMenu.initialize(episode);
        mEpisodeContextMenu.show();
    }

    @Override
    public void onEpisodeContextMenuClose() {
        mEpisodeContextMenu.hide();
        lastFocusedFragment.getView().requestFocus();
    }

    @Override
    public void onPodcastContextMenuOpen(Podcast podcast) {
        mPodcastContextMenu.show(podcast);
    }

    @Override
    public void onPodcastContextMenuClose() {
        mPodcastContextMenu.hide();
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

        ListView lvMenu;

        public PodcastMenu() {
            lvMenu = (ListView) findViewById(R.id.main_menu);
            lvMenu.setAdapter(
                    new ArrayAdapter<String>(PodcastActivity.this, android.R.layout.simple_list_item_1, formList())
            );
            lvMenu.setOnItemClickListener(this);
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

            lastFocusedFragment.getView().requestFocus();
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
            if (!hasFocus) {
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

            lastFocusedFragment.getView().requestFocus();
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
