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

import java.util.List;

/**
 * List fragment to display the list of episodes.
 */
public class EpisodeListFragment extends PodcatcherListFragment {

    /** The list of episodes we are currently showing. */
    private List<Episode> currentEpisodeList;

    /** The activity we are in (listens to user selection) */
    private OnSelectEpisodeListener episodeSelectionListener;

    /** Flag to indicate whether podcast names should be shown for episodes */
    private boolean showPodcastNames = false;

    /** Status flag indicating that our view is created */
    private boolean viewCreated = false;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // Make sure our listener is present
        try {
            this.episodeSelectionListener = (OnSelectEpisodeListener) activity;
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
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewCreated = true;

        // This will make sure we show the right information once the view
        // controls are established (the list might have been set earlier)
        if (currentEpisodeList != null)
            setEpisodeList(currentEpisodeList, true);
    }

    @Override
    public void onListItemClick(ListView list, View view, int position, long id) {
        // Find selected episode and alert listener
        Episode selectedEpisode = (Episode) adapter.getItem(position);
        episodeSelectionListener.onEpisodeSelected(selectedEpisode);
    }

    @Override
    public void onDestroyView() {
        viewCreated = false;

        super.onDestroyView();
    }

    /**
     * Set the list of episodes to show in this fragment. You can call this any
     * time and the view will catch up as soon as it is created. Only has any
     * effect if the list given is not <code>null</code> and different from the
     * episode list currently displayed.
     * 
     * @param episodeList List of episodes to show.
     */
    public void setEpisodeList(List<Episode> episodeList) {
        setEpisodeList(episodeList, false);
    }

    private void setEpisodeList(List<Episode> episodeList, boolean forceReload) {
        if (forceReload || (episodeList != null && !episodeList.equals(currentEpisodeList))) {

            this.currentEpisodeList = episodeList;

            showProgress = false;
            showLoadFailed = false;

            // Update UI
            if (viewCreated) {
                // Update the list
                EpisodeListAdapter adapter = new EpisodeListAdapter(getActivity(), episodeList);
                adapter.setShowPodcastNames(showPodcastNames);

                setListAdapter(adapter);

                // Update other UI elements
                if (episodeList.isEmpty())
                    emptyView.setText(R.string.episode_none);

                // Make sure to match selection state
                if (selectAll)
                    selectAll();
                else if (selectedPosition >= 0 && selectedPosition < episodeList.size())
                    select(selectedPosition);
                else
                    selectNone();
            }
        }
    }

    /**
     * Set whether the fragment should show the podcast name for each episode
     * item. Change will be reflected upon next call of
     * {@link #setEpisodeList(List)}
     * 
     * @param show Whether to show the podcast names.
     */
    public void setShowPodcastNames(boolean show) {
        this.showPodcastNames = show;
    }

    @Override
    protected void reset() {
        if (viewCreated)
            emptyView.setText(R.string.podcast_none_selected);

        currentEpisodeList = null;
        showPodcastNames = false;

        super.reset();
    }

    /**
     * Show error view.
     */
    @Override
    public void showLoadFailed() {
        if (viewCreated)
            progressView.showError(R.string.podcast_load_error);

        super.showLoadFailed();
    }
}
