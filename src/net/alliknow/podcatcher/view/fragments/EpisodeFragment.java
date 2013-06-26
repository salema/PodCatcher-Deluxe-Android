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

package net.alliknow.podcatcher.view.fragments;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.VideoView;

import net.alliknow.podcatcher.R;
import net.alliknow.podcatcher.listeners.OnDownloadEpisodeListener;
import net.alliknow.podcatcher.listeners.OnRequestFullscreenListener;
import net.alliknow.podcatcher.model.types.Episode;
import net.alliknow.podcatcher.view.Utils;

/**
 * Fragment showing episode details.
 */
public class EpisodeFragment extends Fragment {

    /** The listener for the menu item */
    private OnDownloadEpisodeListener downloadListener;
    /** The listener for the video view */
    private OnRequestFullscreenListener fullscreenListener;
    /** The currently shown episode */
    private Episode currentEpisode;

    /** Flag for show download menu item state */
    private boolean showDownloadMenuItem = false;
    /** Flag for the state of the download menu item */
    private boolean downloadMenuItemState = true;
    /** Flag to indicate whether the episode date should be shown */
    private boolean showEpisodeDate = false;
    /** Flag for show new icon state */
    private boolean showNewStateIcon = false;
    /** Flag for show download icon state */
    private boolean showDownloadIcon = false;
    /** Flag for the state of the download icon */
    private boolean downloadIconState = true;

    /** Separator for date and podcast name */
    private static final String SEPARATOR = " • ";

    /** Status flag indicating that our view is created */
    private boolean viewCreated = false;

    /** The download episode menu bar item */
    private MenuItem downloadMenuItem;

    /** The empty view */
    private View emptyView;
    /** The episode title view */
    private TextView titleView;
    /** The podcast title view */
    private TextView subtitleView;
    /** The state icon view */
    private ImageView stateIconView;
    /** The download icon view */
    private ImageView downloadIconView;
    /** The divider view between title and description */
    private View dividerView;
    /** The episode video view */
    private VideoView videoView;
    /** The episode description web view */
    private WebView descriptionView;

    /** Flag to indicate whether video surface is available */
    private boolean videoSurfaceAvailable = false;
    /** Our video surface holder callback to update availability */
    private VideoCallback videoCallback = new VideoCallback();

    /** The callback implementation */
    private final class VideoCallback implements SurfaceHolder.Callback {
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            videoSurfaceAvailable = true;
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            // pass
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            videoSurfaceAvailable = false;
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // Make sure our listener is present
        try {
            this.downloadListener = (OnDownloadEpisodeListener) activity;
            this.fullscreenListener = (OnRequestFullscreenListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnDownloadEpisodeListener and OnRequestFullscreenListener");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        return inflater.inflate(R.layout.episode, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Find UI widgets
        emptyView = getView().findViewById(android.R.id.empty);
        titleView = (TextView) getView().findViewById(R.id.episode_title);
        subtitleView = (TextView) getView().findViewById(R.id.podcast_title);
        stateIconView = (ImageView) getView().findViewById(R.id.state_icon);
        downloadIconView = (ImageView) getView().findViewById(R.id.download_icon);
        dividerView = getView().findViewById(R.id.episode_divider);

        videoView = (VideoView) getView().findViewById(R.id.episode_video);
        videoView.getHolder().addCallback(videoCallback);
        videoView.setOnTouchListener(new OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                Log.i(getTag(), "Fullscreen requested");
                fullscreenListener.onRequestFullscreen();

                return true;
            }
        });

        descriptionView = (WebView) getView().findViewById(R.id.episode_description);

        viewCreated = true;

        // This will make sure we show the right information once the view
        // controls are established (the episode might have been set earlier)
        if (currentEpisode != null) {
            setEpisode(currentEpisode, true);
            setNewIconVisibility(showNewStateIcon);
            setDownloadIconVisibility(showDownloadIcon, downloadIconState);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.episode, menu);

        downloadMenuItem = menu.findItem(R.id.episode_download_menuitem);
        setDownloadMenuItemVisibility(showDownloadMenuItem, downloadMenuItemState);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.episode_download_menuitem:
                // Tell activity to load/unload the current episode
                downloadListener.onToggleDownload();

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onDestroyView() {
        viewCreated = false;
        videoView.getHolder().removeCallback(videoCallback);

        super.onDestroyView();
    }

    /**
     * @return The surface holder for the video view.
     */
    public SurfaceHolder getSurfaceHolder() {
        return videoView.getHolder();
    }

    /**
     * @return Whether the surface is available for playback.
     */
    public boolean isVideoSurfaceAvailable() {
        return videoSurfaceAvailable;
    }

    /**
     * Set the size of the video currently played back and make the fragment's
     * video view adjust to the size, respecting the video aspect ratio.
     * 
     * @param width The current video's width.
     * @param height The current video's height
     */
    public void setVideoSize(int width, int height) {
        LayoutParams layoutParams = videoView.getLayoutParams();

        layoutParams.height = (int) (((float) height / (float) width) *
                (float) videoView.getWidth());
        Log.i(getClass().getSimpleName(), "Video view height set to " + height);

        videoView.setLayoutParams(layoutParams);
    }

    /**
     * Set the displayed episode, all UI will be updated. Only has any effect if
     * the episode given is not <code>null</code> and different from the episode
     * currently displayed.
     * 
     * @param selectedEpisode Episode to show.
     */
    public void setEpisode(Episode selectedEpisode) {
        setEpisode(selectedEpisode, false);
    }

    private void setEpisode(Episode selectedEpisode, boolean forceReload) {
        if (forceReload || (selectedEpisode != null && !selectedEpisode.equals(currentEpisode))) {
            // Set handle to episode in case we are not resumed
            this.currentEpisode = selectedEpisode;

            // If the fragment's view is actually visible and the episode is
            // valid,
            // show episode information
            if (viewCreated && currentEpisode != null) {
                // Title and sub-title
                titleView.setText(currentEpisode.getName());
                subtitleView.setText(currentEpisode.getPodcast().getName());
                // Episode publication data
                if (showEpisodeDate && currentEpisode.getPubDate() != null)
                    subtitleView.setText(subtitleView.getText() + SEPARATOR
                            + Utils.getRelativePubDate(currentEpisode));
                // Episode duration
                if (currentEpisode.getDuration() != null)
                    subtitleView.setText(subtitleView.getText() + SEPARATOR
                            + currentEpisode.getDuration());
                // Find valid episode description
                String description = currentEpisode.getLongDescription();
                if (description == null)
                    description = currentEpisode.getDescription();
                if (description == null)
                    description = getString(R.string.episode_no_description);
                // Set episode description
                descriptionView.loadDataWithBaseURL(null, description, "text/html", "utf-8", null);
            }

            // Update the UI widget's visibility to reflect state
            updateUiElementVisibility();
        }
    }

    /**
     * Set whether the fragment should show the download menu item. You can call
     * this any time and can expect it to happen on menu creation at the latest.
     * You also have to set the download menu state, <code>true</code> for
     * "Download" and <code>false</code> for "Delete".
     * 
     * @param show Whether to show the download menu item.
     * @param download State of the download menu item (download / delete)
     */
    public void setDownloadMenuItemVisibility(boolean show, boolean download) {
        this.showDownloadMenuItem = show;
        this.downloadMenuItemState = download;

        // Only do it right away if resumed and menu item is available,
        // otherwise onResume or the menu creation callback will call us.
        if (downloadMenuItem != null) {
            downloadMenuItem.setVisible(show);

            downloadMenuItem.setTitle(download ? R.string.download : R.string.remove);
            downloadMenuItem.setIcon(download ? R.drawable.ic_menu_download
                    : R.drawable.ic_menu_delete);
        }
    }

    /**
     * Set whether the fragment should show the episode state icon to indicate
     * that the episode is new (not marked old).
     * 
     * @param show Whether to show the new icon.
     */
    public void setNewIconVisibility(boolean show) {
        this.showNewStateIcon = show;

        if (viewCreated)
            stateIconView.setVisibility(show ? VISIBLE : GONE);
    }

    /**
     * Set whether the fragment should show the download icon. You can call this
     * any time and can expect it to happen on fragment resume at the latest.
     * You also have to set the download icon state, <code>true</code> for
     * "is downloaded" and <code>false</code> for "is currently downloading".
     * 
     * @param show Whether to show the download menu item.
     * @param downloaded State of the download menu item (download / delete)
     */
    public void setDownloadIconVisibility(boolean show, boolean downloaded) {
        this.showDownloadIcon = show;
        this.downloadIconState = downloaded;

        // Only do it right away if resumed and menu item is available,
        // otherwise onResume or the menu creation callback will call us.
        if (viewCreated) {
            downloadIconView.setVisibility(show ? VISIBLE : GONE);
            downloadIconView.setImageResource(downloaded ?
                    R.drawable.ic_media_downloaded : R.drawable.ic_media_downloading);
        }
    }

    /**
     * Set whether the fragment should show the episode date for the episode
     * shown. Change will be reflected upon next call of
     * {@link #setEpisode(Episode)}
     * 
     * @param show Whether to show the episode date.
     */
    public void setShowEpisodeDate(boolean show) {
        this.showEpisodeDate = show;
    }

    private void updateUiElementVisibility() {
        if (viewCreated) {
            emptyView.setVisibility(currentEpisode == null ? VISIBLE : GONE);

            titleView.setVisibility(currentEpisode == null ? GONE : VISIBLE);
            subtitleView.setVisibility(currentEpisode == null ? GONE : VISIBLE);
            dividerView.setVisibility(currentEpisode == null ? GONE : VISIBLE);
            descriptionView.setVisibility(currentEpisode == null ? GONE : VISIBLE);
        }
    }

    public void setShowVideoView(boolean showVideo) {
        if (viewCreated)
            videoView.setVisibility(showVideo ? VISIBLE : GONE);
    }
}
