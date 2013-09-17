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

package net.alliknow.podcatcher.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import net.alliknow.podcatcher.R;
import net.alliknow.podcatcher.model.EpisodeManager;
import net.alliknow.podcatcher.model.types.Episode;

/**
 * A list item view to represent an episode.
 */
public class EpisodeListItemView extends RelativeLayout {

    /** Our episode manager handle */
    private final EpisodeManager episodeManager;

    /** String to use if no episode publication date available */
    private static final String NO_DATE = "---";
    /** Separator for date and podcast name */
    private static final String SEPARATOR = " • ";

    /** The title text view */
    private TextView titleTextView;
    /** The caption text view */
    private TextView captionTextView;
    /** The progress bar holder layout */
    private View progressBarHolder;
    /** The progress bar view */
    private ProgressBar progressBarView;
    /** The playlist position view */
    private TextView playlistPositionView;
    /** The download icon view */
    private ImageView downloadIconView;
    /** The download icon view */
    private ImageView resumeIconView;
    /** The state icon view */
    private ImageView stateIconView;

    /**
     * Create an episode item list view.
     * 
     * @param context Context for the view to live in.
     * @param attrs View attributes.
     */
    public EpisodeListItemView(Context context, AttributeSet attrs) {
        super(context, attrs);

        this.episodeManager = EpisodeManager.getInstance();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        titleTextView = (TextView) findViewById(R.id.list_item_title);
        captionTextView = (TextView) findViewById(R.id.list_item_caption);
        progressBarHolder = findViewById(R.id.list_item_progress_holder);
        progressBarView = (ProgressBar) findViewById(R.id.list_item_progress);
        playlistPositionView = (TextView) findViewById(R.id.playlist_position);
        downloadIconView = (ImageView) findViewById(R.id.download_icon);
        resumeIconView = (ImageView) findViewById(R.id.resume_icon);
        stateIconView = (ImageView) findViewById(R.id.state_icon);
    }

    /**
     * Make the view update all its child to represent input given.
     * 
     * @param episode Episode to represent.
     * @param showPodcastName Whether the podcast name should show.
     */
    public void show(final Episode episode, boolean showPodcastName) {
        // 0. Get episode state
        final boolean downloading = episodeManager.isDownloading(episode);

        // 1. Set episode title
        titleTextView.setText(createTitle(episode));

        // 2. Set caption
        captionTextView.setText(createCaption(episode, showPodcastName));
        captionTextView.setVisibility(downloading ? GONE : VISIBLE);

        // 3. Hide/show progress bar
        progressBarHolder.setVisibility(downloading ? VISIBLE : GONE);
        // We need to reset the progress here, because the view might be
        // recycled and it should not show another episode's progress
        if (downloading)
            updateProgress(episodeManager.getDownloadProgress(episode));

        // 4. Update the metadata to show for this episode
        updateMetadata(episode);
    }

    /**
     * Update the episode progress indicator to the progress given. Does not
     * change the visibility of the progress view.
     * 
     * @param percent Progress to show.
     */
    public void updateProgress(int percent) {
        // Show progress in progress bar
        if (percent >= 0 && percent <= 100) {
            progressBarView.setIndeterminate(false);
            progressBarView.setProgress(percent);
        } else
            progressBarView.setIndeterminate(true);
    }

    private String createTitle(Episode episode) {
        final String episodeName = episode.getName();
        final String redundantPrefix = episode.getPodcast().getName() + " ";
        final String redundantPrefixAlt = episode.getPodcast().getName() + ": ";
        // Remove podcast name from the episode title because it takes to much
        // space and is redundant anyway
        if (episodeName.startsWith(redundantPrefix))
            return episodeName.substring(redundantPrefix.length(), episodeName.length());
        else if (episodeName.startsWith(redundantPrefixAlt))
            return episodeName.substring(redundantPrefixAlt.length(), episodeName.length());
        else
            return episodeName;
    }

    private String createCaption(Episode episode, boolean showPodcastName) {
        String result = NO_DATE;

        // Episode has no date, should not happen
        if (episode.getPubDate() == null && showPodcastName)
            result = episode.getPodcast().getName();
        // This is the interesting case
        else if (episode.getPubDate() != null) {
            // Get a nice time span string for the age of the episode
            String dateString = Utils.getRelativePubDate(episode);

            // Append podcast name
            if (showPodcastName)
                result = dateString + SEPARATOR + episode.getPodcast().getName();
            // Omit podcast name
            else
                result = dateString;
        }

        return result;
    }

    private void updateMetadata(Episode episode) {
        // Okay, so this gets a bit messy, we have a lot of cases to cover.
        // 1. Find all the information we need to make the view look right
        final boolean downloading = episodeManager.isDownloading(episode);
        final boolean downloaded = episodeManager.isDownloaded(episode);
        final boolean downloadIconShows = downloaded || downloading;
        final boolean isNew = !episodeManager.getState(episode);
        final boolean willResume = episodeManager.getResumeAt(episode) > 0;
        final int position = episodeManager.getPlaylistPosition(episode);

        // 2. Set the view content and visibility accordingly
        if (downloading)
            downloadIconView.setImageResource(R.drawable.ic_media_downloading);
        else if (downloaded)
            downloadIconView.setImageResource(R.drawable.ic_media_downloaded);

        playlistPositionView.setText(String.valueOf(position + 1));

        downloadIconView.setVisibility(downloading || downloaded ? View.VISIBLE : View.GONE);
        resumeIconView.setVisibility(willResume ? View.VISIBLE : View.GONE);
        playlistPositionView.setVisibility(position >= 0 ? View.VISIBLE : View.GONE);
        stateIconView.setVisibility(isNew ? View.VISIBLE : View.GONE);

        // 3. Fix the layout params of our views in the lower right corner
        // depending on the metadata showing:
        adjustLayout(playlistPositionView, true, -1);
        adjustLayout(resumeIconView, position < 0, R.id.playlist_position);
        adjustLayout(downloadIconView, position < 0 && !willResume,
                willResume ? R.id.resume_icon : R.id.playlist_position);

        // 4. Switch right hand anchor for the main content to whatever metadata
        // is showing:
        LayoutParams params = (RelativeLayout.LayoutParams) findViewById(
                R.id.list_item_main_content).getLayoutParams();
        params.addRule(RelativeLayout.LEFT_OF, downloadIconShows ? R.id.download_icon :
                willResume ? R.id.resume_icon : isNew ? R.id.state_icon : R.id.playlist_position);
        findViewById(R.id.list_item_main_content).setLayoutParams(params);
    }

    private void adjustLayout(View view, boolean atParentRight, int isLeftOf) {
        LayoutParams params = (RelativeLayout.LayoutParams) view.getLayoutParams();

        params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, atParentRight ? RelativeLayout.TRUE : 0);
        params.addRule(RelativeLayout.LEFT_OF, atParentRight ? -1 : isLeftOf);

        view.setLayoutParams(params);
    }
}
