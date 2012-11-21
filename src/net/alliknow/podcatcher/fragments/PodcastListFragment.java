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

import java.util.Collections;

import net.alliknow.podcatcher.PodcastList;
import net.alliknow.podcatcher.R;
import net.alliknow.podcatcher.adapters.PodcastListAdapter;
import net.alliknow.podcatcher.listeners.OnAddPodcastListener;
import net.alliknow.podcatcher.listeners.OnLoadPodcastListener;
import net.alliknow.podcatcher.listeners.OnLoadPodcastLogoListener;
import net.alliknow.podcatcher.listeners.OnSelectPodcastListener;
import net.alliknow.podcatcher.listeners.OnShowSuggestionsListener;
import net.alliknow.podcatcher.listeners.PodcastListContextListener;
import net.alliknow.podcatcher.tasks.LoadPodcastLogoTask;
import net.alliknow.podcatcher.tasks.LoadPodcastTask;
import net.alliknow.podcatcher.types.Podcast;
import android.app.ListFragment;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ImageView;
import android.widget.ListView;

/**
 * List fragment to display the list of podcasts as part of the
 * podcast activity.
 * 
 * @author Kevin Hausmann
 */
public class PodcastListFragment extends ListFragment implements OnAddPodcastListener, 
	OnShowSuggestionsListener, OnLoadPodcastListener, OnLoadPodcastLogoListener {
	
	/** The activity we are in (listens to user selection) */ 
    private OnSelectPodcastListener selectedListener;
    /** The activity we are in (listens to loading events) */ 
    private OnLoadPodcastListener loadListener;
    
	/** The list of podcasts we know */
	private PodcastList podcastList = new PodcastList();
	/** The list of podcast suggestions */
	private PodcastList podcastSuggestions;
	/** Currently selected podcast */
	private Podcast currentPodcast;
	/** Currently show podcast logo */
	private Bitmap currentLogo;
	/** Flag indicating whether we are in select all mode */
	private boolean selectAll = false;
	
	/** The list adapter */
	private PodcastListAdapter adapter;
	/** Remove podcast menu item */
	private MenuItem selectAllMenuItem;
	/** Remove podcast menu item */
	private MenuItem removeMenuItem;
	/** The logo view */
	private ImageView logoView;
	
	/** The current podcast load task */
	private LoadPodcastTask loadPodcastTask;
	/** The current podcast logo load task */
	private LoadPodcastLogoTask loadPodcastLogoTask;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setRetainInstance(true);
		setHasOptionsMenu(true);
		// Loads podcasts from stored file to this.podcastList
		podcastList.load(getActivity());
		// Maps the podcast list items to the list UI
		adapter = new PodcastListAdapter(getActivity(), podcastList);
		setListAdapter(adapter);
		// If podcast list is empty we show dialog on startup
		if (getListAdapter().isEmpty()) showAddPodcastDialog();
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		
		return inflater.inflate(R.layout.podcast_list, container, false);
	}
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		
		// Load all podcasts? TODO Make this a preference
		//for (Podcast podcast : podcastList)
		//	if (podcast.needsReload()) new LoadPodcastTask(this, true).execute(podcast);
		
		logoView = (ImageView) view.findViewById(R.id.podcast_image);
		
		getListView().setMultiChoiceModeListener(new PodcastListContextListener(this));
		getListView().setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE_MODAL);
		
		if (currentLogo != null) logoView.setImageBitmap(currentLogo);
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.podcast_list, menu);
		
		removeMenuItem = (MenuItem) menu.findItem(R.id.remove_podcast_button);
		selectAllMenuItem = (MenuItem) menu.findItem(R.id.select_all_podcasts_button);
		
		updateMenuItems();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
	    	case R.id.add_podcast_button:
	    		showAddPodcastDialog();
	   
	    		return true;
	    	case R.id.select_all_podcasts_button:
	    		selectAll();
	    		
	    		return true;
	    	case R.id.remove_podcast_button:
	    		getListView().setItemChecked(podcastList.indexOf(currentPodcast), true);
	    		
	    		return true;
	    	default:
	    		return super.onOptionsItemSelected(item);
		}
	}
	
	@Override
	public void onListItemClick(ListView list, View view, int position, long id) {
		Podcast selectedPodcast = podcastList.get(position);
		selectPodcast(selectedPodcast);
	}
	
	/**
	 * @param listener Listener to be alerted on podcast selection
	 */
	public void setPodcastSelectedListener(OnSelectPodcastListener listener) {
		this.selectedListener = listener;
	}

	/**
	 * @param listener Listener to be alerted on podcast load completion
	 */
	public void setPodcastLoadedListener(OnLoadPodcastListener listener) {
		this.loadListener = listener;
	}
	
	@Override
	public void addPodcast(Podcast newPodcast) {
		if (! podcastList.contains(newPodcast)) {
			podcastList.add(newPodcast);
			Collections.sort(podcastList);
			
			podcastList.store(getActivity());
		} else Log.d(getClass().getSimpleName(), "Podcast \"" + newPodcast.getName() + "\" is already in list.");
		
		selectPodcast(newPodcast);
	}
	
	/**
	 * @return The list of podcast currently listed.
	 */
	public PodcastList getPodcastList() {
		return this.podcastList;
	}
	
	private void showAddPodcastDialog() {
		AddPodcastFragment addPodcastFragment = new AddPodcastFragment();
		addPodcastFragment.setTargetFragment(this, 0);
		
		addPodcastFragment.show(getFragmentManager(), null);
	}
	
	@Override
	public void showSuggestions() {
		SuggestionFragment suggestionFragment = new SuggestionFragment();
		suggestionFragment.setTargetFragment(this, 0);
		
		suggestionFragment.show(getFragmentManager(), "suggest_podcast");
	}
	
	/**
	 * @return The list of podcast suggestions cached here
	 */
	public PodcastList getPodcastSuggestions() {
		return podcastSuggestions;
	}

	/**
	 * @param podcastSuggestions The list of podcast suggestions to cache here
	 */
	public void setPodcastSuggestions(PodcastList podcastSuggestions) {
		this.podcastSuggestions = podcastSuggestions;
	}

	private void selectPodcast(Podcast selectedPodcast) {
		this.currentPodcast = selectedPodcast;
		this.selectAll = false;
			
		// Stop loading previous tasks
		cancelCurrentLoadTasks();
					
		// Prepare UI
		adapter.setSelectedPosition(podcastList.indexOf(selectedPodcast));
		scrollListView(podcastList.indexOf(selectedPodcast));
		logoView.setImageResource(R.drawable.default_podcast_logo);
		updateMenuItems();
		
		// Alert parent activity
		if (selectedListener != null) selectedListener.onPodcastSelected(currentPodcast);
		else Log.d(getClass().getSimpleName(), "Podcast selected, but no listener attached");
		
		// Load if too old, otherwise just use previously loaded version
		if (selectedPodcast.needsReload()) {
			// Download podcast RSS feed (async)
			loadPodcastTask = new LoadPodcastTask(this);
			loadPodcastTask.execute(selectedPodcast);
		}
		// Use buffered content
		else onPodcastLoaded(selectedPodcast, false);
	}

	private void selectAll() {
		this.currentPodcast = null;
		this.currentLogo = null;
		this.selectAll = true;
		
		// Stop loading previous tasks
		cancelCurrentLoadTasks();
		
		// Prepare UI
		adapter.setSelectAll();
		logoView.setImageResource(R.drawable.default_podcast_logo);
		updateMenuItems();
		
		// Alert parent activity
		if (selectedListener != null) selectedListener.onAllPodcastsSelected();
		else Log.d(getClass().getSimpleName(), "All podcasts selected, but no listener attached");
		
		for (Podcast podcast : podcastList)
			if (podcast.needsReload()) new LoadPodcastTask(this, true).execute(podcast);
			else onPodcastLoaded(podcast, true);
	}
	
	private void cancelCurrentLoadTasks() {
		if (loadPodcastTask != null) loadPodcastTask.cancel(true);
		if (loadPodcastLogoTask != null) loadPodcastLogoTask.cancel(true);
	}
	
	/**
	 * Check whether there is a podcast currently selected in the list.
	 * @return <code>true</code> if so, <code>false</code> otherwise. 
	 */
	public boolean isPodcastSelected() {
		return currentPodcast != null;
	}
	
	/**
	 * Removes the podcast selected in context mode.
	 */
	public void removeCheckedPodcasts() {
		SparseBooleanArray checkedItems = getListView().getCheckedItemPositions();
		
		// Remove checked podcasts
		for (int index = podcastList.size() - 1; index >= 0; index--)
			if (checkedItems.get(index)) {
				// Reset internal variable if necessary
				if (podcastList.get(index).equals(currentPodcast)) currentPodcast = null;
				// Remove podcast from list
				podcastList.remove(index);
			}
		
		// Update UI (current podcast was deleted)
		if (currentPodcast == null) {
			adapter.setSelectNone();
			logoView.setImageResource(R.drawable.default_podcast_logo);
			currentLogo = null;
			
			if (selectedListener != null) selectedListener.onNoPodcastSelected();
		} // Current podcast has new position
		else adapter.setSelectedPosition(podcastList.indexOf(currentPodcast));
		
		updateListVisibility();
		updateMenuItems();
		
		// Store changed list
		podcastList.store(getActivity());
	}
	
	/**
	 * 
	 */
	private void updateListVisibility() {
		getView().findViewById(android.R.id.empty)
		.setVisibility(podcastList.isEmpty() ? View.VISIBLE : View.GONE);
		getListView().setVisibility(podcastList.isEmpty() ? View.GONE : View.VISIBLE);
	}

	@Override
	public void onPodcastLoadProgress(int progress) {
		if (loadListener != null) loadListener.onPodcastLoadProgress(progress);
	}
	
	/**
	 * Notified by async RSS file loader on completion.
	 * Updates UI to display the podcast's episodes.
	 * @param podcast Podcast RSS feed loaded for
	 */
	@Override
	public void onPodcastLoaded(Podcast podcast, boolean wasBackground) {
		// This will display the number of episodes
		adapter.notifyDataSetChanged();
		
		if (loadListener != null) loadListener.onPodcastLoaded(podcast, wasBackground);
		else Log.d(getClass().getSimpleName(), "Podcast loaded, but no listener attached");
		
		if (! wasBackground) {
			loadPodcastTask = null;
			
			// Download podcast logo
			loadPodcastLogoTask = new LoadPodcastLogoTask(this, logoView.getWidth(), logoView.getHeight());
			loadPodcastLogoTask.execute(podcast);
		}
	}
	
	@Override
	public void onPodcastLoadFailed(Podcast podcast, boolean wasBackground) {
		// Only react if the podcast failed to load that we are actually waiting for
		if (! wasBackground) {
			loadPodcastTask = null;
			
			if (loadListener != null) loadListener.onPodcastLoadFailed(podcast, false);
			else Log.d(getClass().getSimpleName(), "Podcast failed to load, but no listener attached");
		}
			
		Log.w(getClass().getSimpleName(), "Podcast failed to load " + podcast);
	}
	
	@Override
	public void onPodcastLogoLoaded(Bitmap logo) {
		loadPodcastLogoTask = null;
		currentLogo = logo;
		
		logoView.setImageBitmap(logo);
	}
	
	@Override
	public void onPodcastLogoLoadFailed() { /* Just stick with the default logo... */ }
	
	private void updateMenuItems() {
		selectAllMenuItem.setVisible(! selectAll);
		removeMenuItem.setVisible(currentPodcast != null);
	}
	
	private void scrollListView(int position) {
		if (getListView().getFirstVisiblePosition() > position || getListView().getLastVisiblePosition() < position)
				getListView().setSelectionFromTop(position, 0);
	}
}
