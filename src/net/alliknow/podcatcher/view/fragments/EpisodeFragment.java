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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.TextView;

import net.alliknow.podcatcher.R;
import net.alliknow.podcatcher.model.types.Episode;
import net.alliknow.podcatcher.view.Utils;

/**
 * Fragment showing episode details.
 */
public class EpisodeFragment extends Fragment {

    /** The currently shown episode */
    private Episode currentEpisode;

    /** Flag to indicate whether the episode date should be shown */
    private boolean showEpisodeDate = false;

    /** Separator for date and podcast name */
    private static final String SEPARATOR = " • ";

    /** Status flag indicating that our view is created */
    private boolean viewCreated = false;

    /** The empty view */
    private View emptyView;
    /** The episode title view */
    private TextView titleView;
    /** The podcast title view */
    private TextView subtitleView;
    /** The divider view between title and description */
    private View dividerView;
    /** The episode description web view */
    private WebView descriptionView;

    /** The ad shown under episode description */
    private String ad;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        ad = "<hr style=\"color: gray; width: 100%\">" +
                "<div style=\"color: gray; font-size: smaller; text-align: center; width: 100%\">" +
                getString(R.string.ad) + "</div>";
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
        descriptionView = (WebView) getView().findViewById(R.id.episode_description);
        dividerView = getView().findViewById(R.id.episode_divider);

        viewCreated = true;

        // This will make sure we show the right information once the view
        // controls are established (the episode might have been set earlier)
        if (currentEpisode != null)
            setEpisode(currentEpisode, true);
    }

    @Override
    public void onDestroyView() {
        viewCreated = false;

        super.onDestroyView();
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
                if (currentEpisode.getDurationString() != null)
                    subtitleView.setText(subtitleView.getText() + SEPARATOR
                            + currentEpisode.getDurationString());
                // Find valid episode description
                String description = currentEpisode.getLongDescription();
                if (description == null)
                    description = currentEpisode.getDescription();
                if (description == null)
                    description = getString(R.string.episode_no_description);
                // Set episode description
                descriptionView.loadDataWithBaseURL(null, description + ad,
                        "text/html", "utf-8", null);
            }

            // Update the UI widget's visibility to reflect state
            updateUiElementVisibility();
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
}
