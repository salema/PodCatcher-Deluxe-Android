/** Copyright 2012 Kevin Hausmann
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

import java.util.List;

import net.alliknow.podcatcher.adapters.EpisodeListAdapter;
import net.alliknow.podcatcher.types.Episode;
import net.alliknow.podcatcher.types.Podcast;
import android.app.Activity;
import android.app.ListFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

/**
 * List fragment to display the list of episodes as part of the
 * podcast activity.
 * 
 * @author Kevin Hausmann
 */
public class EpisodeListFragment extends ListFragment {

	/** The list of episode showing */
	private List<Episode> episodeList;
	
	/** Container Activity must implement this interface */
    public interface OnEpisodeSelectedListener {
    	/**
    	 * Updates the UI to reflect that a podcast has been selected.
    	 * @param selectedPodcast Podcast selected by the user
    	 */
    	public void onEpisodeSelected(Episode selectedEpisode);
    }
    /** The activity we are in (listens to user selection) */ 
    private OnEpisodeSelectedListener listener;
    
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		
		return inflater.inflate(R.layout.episode_list, container, false);
	}
	
	@Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
       
        try {
            listener = (OnEpisodeSelectedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnEpisodeSelectedListener");
        }
    }

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		Episode selectedEpisode = this.episodeList.get(position);
		listener.onEpisodeSelected(selectedEpisode);
	}
	
	public void setPodcast(Podcast podcast) {
		getView().findViewById(R.id.episode_list_progress).setVisibility(View.GONE);
		this.episodeList = podcast.getEpisodes();
		setListAdapter(new EpisodeListAdapter(getActivity(), this.episodeList));
		getListView().setVisibility(View.VISIBLE);
	}

	public void clearAndSpin() {
		getListView().setVisibility(View.GONE);
		getView().findViewById(android.R.id.empty).setVisibility(View.GONE);
		getView().findViewById(R.id.episode_list_progress).setVisibility(View.VISIBLE);
	}
}
