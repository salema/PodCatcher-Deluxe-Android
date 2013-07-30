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
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import net.alliknow.podcatcher.R;
import net.alliknow.podcatcher.model.PodcastManager;
import net.alliknow.podcatcher.model.types.Podcast;

/**
 * A list item view to represent a podcast.
 */
public class PodcastListItemView extends RelativeLayout {

    /** Our podcast manager handle */
    private final PodcastManager podcastManager;

    /** The title text view */
    private TextView titleTextView;
    /** The caption text view */
    private TextView captionTextView;
    /** The podcast logo view */
    private ImageView logoView;
    /** The load progress view */
    private HorizontalProgressView progressView;

    /**
     * Create a podcast item list view.
     * 
     * @param context Context for the view to live in.
     * @param attrs View attributes.
     */
    public PodcastListItemView(Context context, AttributeSet attrs) {
        super(context, attrs);

        this.podcastManager = PodcastManager.getInstance();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        titleTextView = (TextView) findViewById(R.id.list_item_title);
        captionTextView = (TextView) findViewById(R.id.list_item_caption);
        logoView = (ImageView) findViewById(R.id.podcast_logo);
        progressView = (HorizontalProgressView) findViewById(R.id.list_item_progress);
    }

    /**
     * Make the view update all its child to represent input given.
     * 
     * @param podcast Podcast to represent.
     * @param showLogo Whether the podcast logo should show (if available).
     * @param showProgress Whether the progress view should be visible.
     */
    public void show(final Podcast podcast, boolean showLogo, boolean showProgress) {
        // 1. Set podcast title
        titleTextView.setText(podcast.getName());

        // 2. Set caption
        final int episodeNumber = podcast.getEpisodeNumber();
        captionTextView.setText(getResources().getQuantityString(
                R.plurals.episodes, episodeNumber, episodeNumber));
        captionTextView.setVisibility(episodeNumber != 0 ? VISIBLE : GONE);

        // 3. Set podcast logo if available
        final boolean showLogoView = showLogo && podcast.getLogo() != null;
        logoView.setVisibility(showLogoView ? VISIBLE : GONE);
        logoView.setImageBitmap(showLogoView ? podcast.getLogo() : null);

        // 4. Show/hide progress view
        progressView.setVisibility(podcastManager.isLoading(podcast)
                && showProgress ? VISIBLE : GONE);
    }
}
