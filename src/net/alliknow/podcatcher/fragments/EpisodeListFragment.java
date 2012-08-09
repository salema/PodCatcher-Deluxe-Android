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
package net.alliknow.podcatcher.fragments;

import java.util.List;

import net.alliknow.podcatcher.Podcatcher;
import net.alliknow.podcatcher.R;
import net.alliknow.podcatcher.adapters.EpisodeListAdapter;
import net.alliknow.podcatcher.types.Episode;
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
	
	private boolean showProgress = false;
	
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
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	setRetainInstance(true);
    }
    
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		
		return inflater.inflate(R.layout.episode_list, container, false);
	}
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		
		if (this.showProgress) clearAndSpin();
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
	public void onListItemClick(ListView list, View view, int position, long id) {
		Episode selectedEpisode = this.episodeList.get(position);
		((EpisodeListAdapter) getListAdapter()).setSelectedPosition(position);
		
		listener.onEpisodeSelected(selectedEpisode);
	}
	
	/**
	 * Set the episode list to display and update the UI accordingly
	 * @param list List of episodes to display
	 */
	public void setEpisodeList(List<Episode> list) {
		getView().findViewById(R.id.episode_list_progress).setVisibility(View.GONE);
		
		this.showProgress = false;
		this.episodeList = list;
		setListAdapter(new EpisodeListAdapter(getActivity(), this.episodeList));
		getListView().setVisibility(View.VISIBLE);
	}

	/**
	 * Show the UI to be working
	 */
	public void clearAndSpin() {
		this.showProgress = true;
		getListView().setVisibility(View.GONE);
		getView().findViewById(android.R.id.empty).setVisibility(View.GONE);
		if (! Podcatcher.isInDebugMode(getActivity()))
				getView().findViewById(R.id.episode_list_progress).setVisibility(View.VISIBLE);
	}
}
