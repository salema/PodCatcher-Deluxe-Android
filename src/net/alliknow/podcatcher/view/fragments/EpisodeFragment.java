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

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.TextView;

import net.alliknow.podcatcher.R;
import net.alliknow.podcatcher.model.types.Episode;

/**
 * Fragment showing episode details.
 */
public class EpisodeFragment extends Fragment {

    /** The currently shown episode */
    private Episode currentEpisode;

    /** Status flag indicating that our view is created */
    private boolean viewCreated = false;

    /** The empty view */
    private View emptyView;
    /** The episode title view */
    private TextView episodeTitleView;
    /** The podcast title view */
    private TextView podcastTitleView;
    /** The divider view between title and description */
    private View dividerView;
    /** The episode description web view */
    private WebView episodeDetailView;

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
        episodeTitleView = (TextView) getView().findViewById(R.id.episode_title);
        podcastTitleView = (TextView) getView().findViewById(R.id.podcast_title);
        episodeDetailView = (WebView) getView().findViewById(R.id.episode_description);
        dividerView = getView().findViewById(R.id.episode_divider);

        viewCreated = true;

        // This will make sure we show the right information once the view
        // controls are established (the episode might have been set earlier)
        if (currentEpisode != null)
            setEpisode(currentEpisode);
    }

    @Override
    public void onDestroyView() {
        viewCreated = false;

        super.onDestroyView();
    }

    /**
     * Set the displayed episode, all UI will be updated.
     * 
     * @param selectedEpisode Episode to show.
     */
    public void setEpisode(Episode selectedEpisode) {
        // Set handle to episode in case we are not resumed
        this.currentEpisode = selectedEpisode;

        // If the fragment's view is actually visible and the episode is valid,
        // show episode information
        if (viewCreated && currentEpisode != null) {
            episodeTitleView.setText(currentEpisode.getName());
            podcastTitleView.setText(currentEpisode.getPodcastName());
            episodeDetailView.loadDataWithBaseURL(null, currentEpisode.getDescription(),
                    "text/html",
                    "utf-8", null);
        }

        // Update the UI widget's visibility to reflect state
        updateUiElementVisibility();
    }

    private void updateUiElementVisibility() {
        if (viewCreated) {
            emptyView.setVisibility(currentEpisode == null ? VISIBLE : GONE);

            episodeTitleView.setVisibility(currentEpisode == null ? GONE : VISIBLE);
            podcastTitleView.setVisibility(currentEpisode == null ? GONE : VISIBLE);
            dividerView.setVisibility(currentEpisode == null ? GONE : VISIBLE);
            episodeDetailView.setVisibility(currentEpisode == null ? GONE : VISIBLE);
        }
    }
}
