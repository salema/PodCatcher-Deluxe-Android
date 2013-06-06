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

package net.alliknow.podcatcher.view.adapters;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import net.alliknow.podcatcher.R;
import net.alliknow.podcatcher.model.EpisodeManager;
import net.alliknow.podcatcher.model.PodcastManager;
import net.alliknow.podcatcher.model.types.Podcast;
import net.alliknow.podcatcher.view.HorizontalProgressView;

import java.util.List;

/**
 * Adapter class used for the list of podcasts.
 */
public class PodcastListAdapter extends PodcatcherBaseListAdapter {

    /** The list our data resides in */
    protected List<Podcast> list;
    /** Member flag to indicate whether we show the podcast logo */
    protected boolean showLogoView = false;

    /** The podcast manager handle */
    private final PodcastManager podcastManager;
    /** The episode manager handle */
    private final EpisodeManager episodeManager;

    /**
     * Create new adapter.
     * 
     * @param context The current context.
     * @param podcastList List of podcasts to wrap (not <code>null</code>).
     */
    public PodcastListAdapter(Context context, List<Podcast> podcastList) {
        super(context);

        this.list = podcastList;
        this.podcastManager = PodcastManager.getInstance();
        this.episodeManager = EpisodeManager.getInstance();
    }

    /**
     * Set whether the podcast logo should be shown. This will redraw the list
     * and take effect immediately.
     * 
     * @param show Whether to show each podcast's logo.
     */
    public void setShowLogo(boolean show) {
        this.showLogoView = show;

        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public Object getItem(int position) {
        return list.get(position);
    }

    @Override
    public long getItemId(int position) {
        return list.get(position).hashCode();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get the return view (possibly recycle a used one)
        View listItemView = findReturnView(convertView, parent, R.layout.podcast_list_item);

        // Set list item color background
        setBackgroundColorForPosition(listItemView, position);

        // Find podcast to represent
        final Podcast podcast = list.get(position);

        // Set the text to display for title
        setText(listItemView, R.id.list_item_title, podcast.getName());
        // Set the text to display as caption
        TextView captionView = (TextView) listItemView.findViewById(R.id.list_item_caption);
        captionView.setText(createCaption(podcast));
        captionView.setVisibility(podcast.getEpisodeNumber() != 0 ? VISIBLE : GONE);

        // Check whether we should show the podcast logo
        boolean show = showLogoView && podcast.getLogo() != null;
        // Set the podcast logo if available and wanted
        ImageView logoView = (ImageView) listItemView.findViewById(R.id.podcast_logo);
        logoView.setVisibility(show ? VISIBLE : GONE);
        logoView.setImageBitmap(show ? podcast.getLogo() : null);

        // Show progress on select all podcasts?
        HorizontalProgressView progressView = (HorizontalProgressView)
                listItemView.findViewById(R.id.list_item_progress);
        progressView.setVisibility(podcastManager.isLoading(podcast)
                && selectAll ? VISIBLE : GONE);

        return listItemView;
    }

    private String createCaption(Podcast podcast) {
        final int episodeCount = podcast.getEpisodeNumber();
        final int newEpisodeCount = episodeManager.getNewEpisodeCount(podcast);

        String caption = "";

        if (newEpisodeCount == 0)
            caption += resources.getString(R.string.episodes_no_new);
        else
            caption += resources
                    .getQuantityString(R.plurals.episodes_new, newEpisodeCount, newEpisodeCount);

        return caption +
                " (" + episodeCount + " " + resources.getString(R.string.episodes_total) + ")";
    }
}
