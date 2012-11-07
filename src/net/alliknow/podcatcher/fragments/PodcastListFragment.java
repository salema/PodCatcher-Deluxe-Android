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
import net.alliknow.podcatcher.listeners.PodcastListContextListener;
import net.alliknow.podcatcher.tasks.LoadPodcastLogoTask;
import net.alliknow.podcatcher.tasks.LoadPodcastTask;
import net.alliknow.podcatcher.types.Podcast;
import android.app.ListFragment;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
public class PodcastListFragment extends ListFragment implements OnAddPodcastListener, OnLoadPodcastListener, OnLoadPodcastLogoListener {
	
	/** The add podcast fragment to use */
	private AddPodcastFragment addPodcastFragment = new AddPodcastFragment();
	/** The show suggestions fragment to use */
	private SuggestionFragment suggestionFragment = new SuggestionFragment();
	/** The activity we are in (listens to user selection) */ 
    private OnSelectPodcastListener selectedListener;
    /** The activity we are in (listens to loading events) */ 
    private OnLoadPodcastListener loadListener;
    
	/** The list of podcasts we know */
	private PodcastList podcastList = new PodcastList();
	/** Currently selected podcast */
	private Podcast currentPodcast;
	/** Currently show podcast logo */
	private Bitmap currentLogo;
	
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
		setListAdapter(new PodcastListAdapter(getActivity(), podcastList));
		// Make sure we are alerted if a new podcast is added
		addPodcastFragment.setAddPodcastListener(this);
		suggestionFragment.setAddPodcastListener(this);
		// If podcast list is empty we show dialog on startup
		if (getListAdapter().isEmpty()) addPodcastFragment.show(getFragmentManager(), "add_podcast");
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		
		return inflater.inflate(R.layout.podcast_list, container, false);
	}
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		
		// Load all podcast TODO Make this a perference
		//for (Podcast podcast : podcastList)
		//	if (podcast.needsReload()) new LoadPodcastTask(this, true).execute(podcast);
		
		getListView().setMultiChoiceModeListener(new PodcastListContextListener(this));
		getListView().setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE_MODAL);
		
		if (currentLogo != null) setPodcastLogo(currentLogo);
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.podcast_list_menu, menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.add_podcast_button) 
			addPodcastFragment.show(getFragmentManager(), "add_podcast");
				
		return item.getItemId() == R.id.add_podcast_button;
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
			
			setListAdapter(new PodcastListAdapter(getActivity(), podcastList));
		} else Log.d(getClass().getSimpleName(), "Podcast \"" + newPodcast.getName() + "\" is already in list.");
		
		selectPodcast(newPodcast);
		podcastList.store(getActivity());
	}
	
	@Override
	public void showSuggestions() {
		suggestionFragment.setCurrentPodcasts(podcastList);
		suggestionFragment.show(getFragmentManager(), "suggest_podcast");
	}
	
	private void selectPodcast(Podcast selectedPodcast) {
		// Is this a valid selection (in podcast list and new)?
		if (podcastList.contains(selectedPodcast) && (currentPodcast == null || !currentPodcast.equals(selectedPodcast))) {
			currentPodcast = selectedPodcast;
			
			// Stop loading previous tasks
			if (loadPodcastTask != null) loadPodcastTask.cancel(true);
			if (loadPodcastLogoTask != null) loadPodcastLogoTask.cancel(true);
						
			// Prepare UI
			((PodcastListAdapter) getListAdapter()).setSelectedPosition(podcastList.indexOf(selectedPodcast));
			setPodcastLogo(BitmapFactory.decodeResource(getActivity().getResources(), R.drawable.default_podcast_logo));
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
	}
	
	/**
	 * Removes the podcast selected in context mode.
	 */
	public void removeCheckedPodcasts() {
		SparseBooleanArray checkedItems = getListView().getCheckedItemPositions();
		
		for (int index = podcastList.size() - 1; index >= 0; index--)
			if (checkedItems.get(index)) podcastList.remove(index);
				
		setListAdapter(new PodcastListAdapter(getActivity(), podcastList));
		podcastList.store(getActivity());
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
		((PodcastListAdapter) getListAdapter()).notifyDataSetChanged();
		
		if (! wasBackground) {
			loadPodcastTask = null;
						
			if (loadListener != null) loadListener.onPodcastLoaded(podcast, false);
			else Log.d(getClass().getSimpleName(), "Podcast loaded, but no listener attached");
			
			// Download podcast logo
			if (isAdded() && podcast.getLogoUrl() != null) {
				loadPodcastLogoTask = new LoadPodcastLogoTask(this);
				loadPodcastLogoTask.execute(podcast);
			} else Log.d(getClass().getSimpleName(), "Not attached or no logo for podcast " + podcast);
		}
	}
	
	@Override
	public void onPodcastLoadFailed(Podcast podcast, boolean wasBackground) {
		// Only react if the podcast failed to load that we are actually waiting for
		if (currentPodcast.equals(podcast) && !wasBackground) {
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
		
		if (isAdded()) setPodcastLogo(logo);
	}
	
	@Override
	public void onPodcastLogoLoadFailed() {}
	
	private void setPodcastLogo(Bitmap logo) {
		ImageView logoView = (ImageView) getView().findViewById(R.id.podcast_image);
		logoView.setImageBitmap(logo);
	}
}
