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
import android.util.Log;
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
    private OnSelectEpisodeListener selectedListener;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // This has to work
        selectedListener = (OnSelectEpisodeListener) activity;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        return inflater.inflate(R.layout.episode_list, container, false);
    }

    @Override
    public void onListItemClick(ListView list, View view, int position, long id) {
        Episode selectedEpisode = (Episode) adapter.getItem(position);

        if (selectedListener != null)
            selectedListener.onEpisodeSelected(selectedEpisode);
        else
            Log.d(getClass().getSimpleName(), "Episode selected, but no listener attached");
    }

    @Override
    public void select(int position) {
        super.select(position);
        // this.selectedEpisode = selectedEpisode;
        //
        // // Episode is available
        // if (currentPodcast != null &&
        // currentPodcast.getEpisodes().contains(selectedEpisode))
        // select(currentPodcast.getEpisodes().indexOf(selectedEpisode));
        // // Episode is not in the current episode list
        // else
        // selectNone();
    }

    // /**
    // * Set the episode list to display and update the UI accordingly.
    // *
    // * @param list List of episodes to display.
    // */
    // public void prepareForPodcast(Podcast podcast) {
    // multiplePodcastsMode = false;
    // resetAndSpin();
    //
    // // Reset internal variables
    // if (selectedListener != null)
    // selectedListener.onNoEpisodeSelected();
    //
    // currentPodcast = podcast;
    //
    // if (!podcast.needsReload()) {
    // episodeList = currentPodcast.getEpisodes();
    // processNewEpisodes();
    // }
    // }
    //
    // public void prepareForAllPodcasts() {
    // multiplePodcastsMode = true;
    // resetAndSpin();
    //
    // // Reset internal variables
    // if (selectedListener != null)
    // selectedListener.onNoEpisodeSelected();
    //
    // currentPodcast = null;
    // episodeList = new ArrayList<Episode>();
    //
    // for (Podcast podcast : data.getPodcastList())
    // if (!podcast.needsReload()) {
    // episodeList.addAll(podcast.getEpisodes());
    // }
    //
    // Collections.sort(episodeList);
    // }

    public void setEpisodes(List<Episode> episodeList) {
        showProgress = false;
        showLoadFailed = false;

        setListAdapter(new EpisodeListAdapter(getActivity(),
                new ArrayList<Episode>(episodeList), selectAll));

        // Update UI
        if (episodeList.isEmpty())
            emptyView.setText(R.string.no_episodes);
        updateUiElementVisibility();
    }

    @Override
    protected void reset() {
        if (emptyView != null)
            emptyView.setText(R.string.no_podcast_selected);

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
