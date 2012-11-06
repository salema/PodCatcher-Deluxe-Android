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

import net.alliknow.podcatcher.PodcastList;
import net.alliknow.podcatcher.R;
import net.alliknow.podcatcher.adapters.SuggestionListAdapter;
import net.alliknow.podcatcher.listeners.OnAddPodcastListener;
import net.alliknow.podcatcher.listeners.OnLoadSuggestionListener;
import net.alliknow.podcatcher.tasks.LoadSuggestionsTask;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

/**
 * Fragment to show podcast suggestions.
 * 
 * @author Kevin Hausmann
 */
public class SuggestionFragment extends DialogFragment implements OnLoadSuggestionListener {

	/** The add podcast listener */
	private OnAddPodcastListener listener;
	/** The suggestions load task */
	private LoadSuggestionsTask loadTask;
	/** The list of suggestions */
	private PodcastList suggestionList;
	
	/** The suggestions list view */
	private ListView suggestionsListView;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		
		return inflater.inflate(R.layout.suggestions, container, false);
	}
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		getDialog().setTitle("Suggested Podcasts");
		
		suggestionsListView = (ListView) view.findViewById(R.id.suggested_podcasts);
		
		if (suggestionList == null || suggestionList.isEmpty()) new LoadSuggestionsTask(this).execute((Void)null);
		else suggestionsListView.setAdapter(new SuggestionListAdapter(getActivity(), suggestionList, listener));
	}
	
	@Override
	public void onDismiss(DialogInterface dialog) {
		super.onDismiss(dialog);
		
		if (loadTask != null) loadTask.cancel(true);
	}
	
	/**
	 * Add a listener to be notified if a new podcast was selected and loaded.
	 * Overwrites any current listener.
	 * @param listener The listener
	 */
	public void setAddPodcastListener(OnAddPodcastListener listener) {
		this.listener = listener;
	}
	
	@Override
	public void onSuggestionsLoadProgress(int progress) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onSuggestionsLoaded(PodcastList suggestions) {
		suggestionList = suggestions;
		suggestionsListView.setAdapter(new SuggestionListAdapter(getActivity(), suggestionList, listener));
	}

	@Override
	public void onSuggestionsLoadFailed() {
		// TODO Auto-generated method stub
	}
}
