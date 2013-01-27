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

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import net.alliknow.podcatcher.R;
import net.alliknow.podcatcher.listeners.OnSelectEpisodeListener;
import net.alliknow.podcatcher.model.types.Episode;
import net.alliknow.podcatcher.view.adapters.EpisodeListAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * List fragment to display the list of episodes as part of the podcast
 * activity.
 */
public class EpisodeListFragment extends PodcatcherListFragment {

    /** The activity we are in (listens to user selection) */
    private OnSelectEpisodeListener selectionListener;

    /** Flag to store whether podcast names should be shown for episodes */
    private boolean showPodcastNames = false;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // Make sure our listener is present
        try {
            this.selectionListener = (OnSelectEpisodeListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnSelectEpisodeListener");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        return inflater.inflate(R.layout.episode_list, container, false);
    }

    @Override
    public void onListItemClick(ListView list, View view, int position, long id) {
        Episode selectedEpisode = (Episode) adapter.getItem(position);

        selectionListener.onEpisodeSelected(selectedEpisode);
    }

    public void setShowPodcastNames(boolean show) {
        this.showPodcastNames = show;
    }

    public void setEpisodes(List<Episode> episodeList) {
        showProgress = false;
        showLoadFailed = false;

        setListAdapter(new EpisodeListAdapter(getActivity(),
                new ArrayList<Episode>(episodeList), showPodcastNames));

        // Update UI
        if (episodeList.isEmpty())
            emptyView.setText(R.string.no_episodes);
        updateUiElementVisibility();
    }

    @Override
    protected void reset() {
        if (emptyView != null)
            emptyView.setText(R.string.no_podcast_selected);

        showPodcastNames = false;

        super.reset();
    }

    /**
     * Show error view.
     */
    @Override
    public void showLoadFailed() {
        progressView.showError(R.string.error_podcast_load);

        super.showLoadFailed();
    }
}
