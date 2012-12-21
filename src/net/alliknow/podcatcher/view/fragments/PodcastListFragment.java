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
package net.alliknow.podcatcher.view.fragments;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import java.util.List;

import net.alliknow.podcatcher.Podcatcher;
import net.alliknow.podcatcher.R;
import net.alliknow.podcatcher.listeners.OnAddPodcastListener;
import net.alliknow.podcatcher.listeners.OnLoadPodcastListListener;
import net.alliknow.podcatcher.listeners.OnLoadPodcastListener;
import net.alliknow.podcatcher.listeners.OnLoadPodcastLogoListener;
import net.alliknow.podcatcher.listeners.OnSelectPodcastListener;
import net.alliknow.podcatcher.listeners.OnShowSuggestionsListener;
import net.alliknow.podcatcher.listeners.PodcastListContextListener;
import net.alliknow.podcatcher.model.PodcastManager;
import net.alliknow.podcatcher.model.tasks.Progress;
import net.alliknow.podcatcher.model.types.Podcast;
import net.alliknow.podcatcher.view.HorizontalProgressView;
import net.alliknow.podcatcher.view.adapters.PodcastListAdapter;
import android.app.Activity;
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
 */
public class PodcastListFragment extends PodcatcherListFragment implements OnAddPodcastListener, 
	OnShowSuggestionsListener, OnLoadPodcastListener, OnLoadPodcastLogoListener, OnLoadPodcastListListener {

	/** The podcast data container */
	private PodcastManager data;
	/** Currently selected podcast */
	private Podcast currentPodcast;
	
	/** The activity we are in (listens to user selection) */ 
    private OnSelectPodcastListener selectedListener;
    	
    /** The context mode listener */
    private PodcastListContextListener contextListener = new PodcastListContextListener(this);
    
	/** Remove podcast menu item */
	private MenuItem selectAllMenuItem;
	/** Remove podcast menu item */
	private MenuItem removeMenuItem;
	/** The logo view */
	private ImageView logoView;
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		
		// Link to podcast data singleton
		data = ((Podcatcher) activity.getApplication()).getModel();
		
		// Make sure we can react on data changes
		data.addLoadPodcastListListener(this);
    	data.addLoadPodcastListener(this);
    	data.addLoadPodcastLogoListener(this);
    			
		// We need this to work...
		selectedListener = (OnSelectPodcastListener) activity;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setHasOptionsMenu(true);
		// Make the UI show to be working once it is up
		showProgress = true;
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		
		return inflater.inflate(R.layout.podcast_list, container, false);
	}
	
	@Override
	public void onPodcastListLoaded(List<Podcast> podcastList) {
		this.showProgress = false;
		
		// Maps the podcast list items to the list UI
		setListAdapter(new PodcastListAdapter(getActivity(), podcastList));
		
		// Only update the UI if it has been inflated
		if (isResumed()) {
			updateUiElementVisibility();
			
			// If podcast list is empty we show dialog on startup
			if (podcastList.isEmpty()) showAddPodcastDialog();
		}
	}
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		// Load all podcasts? TODO Make this a preference
		//for (Podcast podcast : podcastList)
		//	if (podcast.needsReload()) new LoadPodcastTask(this).execute(podcast);
		
		logoView = (ImageView) view.findViewById(R.id.podcast_image);
		
		getListView().setMultiChoiceModeListener(contextListener);
		getListView().setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE_MODAL);
		
		List<Podcast> podcastList = data.getPodcastList();
		if (podcastList != null) onPodcastListLoaded(podcastList);
				
		//if (currentPodcast != null) logoView.setImageBitmap(currentPodcast.getLogo());
		
		super.onViewCreated(view, savedInstanceState);
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.podcast_list, menu);
		
		removeMenuItem = (MenuItem) menu.findItem(R.id.podcast_remove_menuitem);
		selectAllMenuItem = (MenuItem) menu.findItem(R.id.podcast_select_all_menuitem);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
	    	case R.id.podcast_add_menuitem:
	    		showAddPodcastDialog();
	   
	    		return true;
	    	case R.id.podcast_select_all_menuitem:
	    		selectAll();
	    		
	    		return true;
	    	case R.id.podcast_remove_menuitem:
	    		getListView().setItemChecked(data.indexOf(currentPodcast), true);
	    		
	    		return true;
	    	default:
	    		return super.onOptionsItemSelected(item);
		}
	}
	
	@Override
	public void onListItemClick(ListView list, View view, int position, long id) {
		Podcast selectedPodcast = data.get(position);
		selectPodcast(selectedPodcast);
	}
	
	@Override
	public void onDetach() {
		super.onDetach();
		
		// Prevent leaking of listeners
		data.removeLoadPodcastListListener(this);
    	data.removeLoadPodcastListener(this);
    	data.removeLoadPodcastLogoListener(this);
		
    	// Do not leak activity
		selectedListener = null;
	}
	
	@Override
	public void addPodcast(Podcast newPodcast) {
		// Notify data fragment
		data.addPodcast(newPodcast);
		// Update the list
		setListAdapter(new PodcastListAdapter(getActivity(), data.getPodcastList()));
		
		// Only if in tablet mode... selectPodcast(newPodcast);
	}
	
	@Override
	public List<Podcast> getPodcastList() {
		return data.getPodcastList();
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
		
		suggestionFragment.show(getFragmentManager(), null);
	}
	
	@Override
	public List<Podcast> getPodcastSuggestions() {
		return data.getPodcastSuggestions();
	}

	@Override
	public void setPodcastSuggestions(List<Podcast> podcastSuggestions) {
		data.setPodcastSuggestions(podcastSuggestions);
	}

	private void selectPodcast(Podcast selectedPodcast) {
		super.selectItem(data.indexOf(selectedPodcast));
		this.currentPodcast = selectedPodcast;
					
		// Stop loading previous tasks
		data.cancelAllLoadTasks();
					
		// Prepare UI
		if (currentPodcast.getLogo() == null)
			logoView.setImageResource(R.drawable.default_podcast_logo);
		else logoView.setImageBitmap(currentPodcast.getLogo());
		updateUiElementVisibility();
		
		// Alert parent activity
		if (selectedListener != null) selectedListener.onPodcastSelected(currentPodcast);
		else Log.d(getClass().getSimpleName(), "Podcast selected, but no listener attached");
		
		// Load if too old, load podcast
		if (currentPodcast.needsReload()) data.load(currentPodcast);
		// Use buffered content
		else onPodcastLoaded(selectedPodcast);
	}

	@Override
	public void selectAll() {
		super.selectAll();
		this.currentPodcast = null;
		
		// Stop loading previous tasks
		data.cancelAllLoadTasks();
		
		// Alert parent activity
		if (selectedListener != null) selectedListener.onAllPodcastsSelected();
		else Log.d(getClass().getSimpleName(), "All podcasts selected, but no listener attached");
				
		// Prepare UI
		logoView.setImageResource(R.drawable.default_podcast_logo);
		updateUiElementVisibility();
				
		// Load all podcasts
		for (Podcast podcast : data.getPodcastList())
			if (podcast.needsReload()) {
				// Otherwise progress will not show
				podcast.resetEpisodes();
				
				data.load(podcast);
			} else onPodcastLoaded(podcast);
	}
	
	@Override
	public void selectNone() {
		super.selectNone();
		
		logoView.setImageResource(R.drawable.default_podcast_logo);
		
		if (selectedListener != null) selectedListener.onNoPodcastSelected();
	}	
	
	/**
	 * Check whether there is a podcast currently selected in the list.
	 * @return <code>true</code> if so, <code>false</code> otherwise. 
	 */
	public boolean isPodcastSelected() {
		return currentPodcast != null || selectAll;
	}
	
	/**
	 * Removes the podcast selected in context mode.
	 */
	public void removeCheckedPodcasts() {
		SparseBooleanArray checkedItems = getListView().getCheckedItemPositions();
		
		// Remove checked podcasts
		for (int index = data.size() - 1; index >= 0; index--)
			if (checkedItems.get(index)) {
				// Reset internal variable if necessary
				if (data.get(index).equals(currentPodcast)) currentPodcast = null;
				// Remove podcast from list
				data.remove(index);
			}
		
		// Update UI (current podcast was deleted)
		if (!selectAll && currentPodcast == null) selectNone();	
		// Current podcast has new position
		else if (!selectAll) adapter.setSelectedPosition(data.indexOf(currentPodcast));
		
		updateUiElementVisibility();
	}
	
	@Override
	public void onPodcastLoadProgress(Podcast podcast, Progress progress) {
		// To prevent this if we are not ready to handle progress update
		// e.g. on app termination
		if (isResumed()) {
			View listItemView = getListView().getChildAt(data.indexOf(podcast));
			if (listItemView != null)
				((HorizontalProgressView)listItemView.findViewById(R.id.list_item_progress))
					.publishProgress(progress);
		}
	}
	
	/**
	 * Notified by async RSS file loader on completion.
	 * Updates UI to display the podcast's episodes.
	 * @param podcast Podcast RSS feed was loaded for.
	 */
	@Override
	public void onPodcastLoaded(Podcast podcast) {
		// This will display the number of episodes
		adapter.notifyDataSetChanged();
		
		// Load logo if this is the podcast we are waiting for and not cached
		if (podcast.equals(currentPodcast) && currentPodcast.getLogo() == null) 
			data.loadLogo(currentPodcast, logoView.getWidth(), logoView.getHeight());
	}
	
	@Override
	public void onPodcastLoadFailed(Podcast podcast) {
		// This will update the list view
		adapter.notifyDataSetChanged();
				
		Log.w(getClass().getSimpleName(), "Podcast failed to load " + podcast);
	}
	
	@Override
	public void onPodcastLogoLoaded(Podcast podcast, Bitmap logo) {
		// Cache the result in podcast object
		if (podcast.equals(currentPodcast))	currentPodcast.setLogo(logo);
		
		logoView.setImageBitmap(logo);
	}
	
	@Override
	public void onPodcastLogoLoadFailed(Podcast podcast) { 
		// pass
	}
	
	@Override
	protected void updateUiElementVisibility() {
		super.updateUiElementVisibility();
		
		if (isResumed()) {
			logoView.setVisibility(selectAll ? GONE : VISIBLE);
			
			// Menu items might be late to load
			if (selectAllMenuItem != null)
				selectAllMenuItem.setVisible(data.getPodcastList() != null && data.size() > 1 && !selectAll);
			if (removeMenuItem != null)
				removeMenuItem.setVisible(currentPodcast != null);
		}
	}
}
