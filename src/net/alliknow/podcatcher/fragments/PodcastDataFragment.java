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

import static net.alliknow.podcatcher.Podcatcher.isInDebugMode;
import static net.alliknow.podcatcher.Podcatcher.isOnFastConnection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.alliknow.podcatcher.Podcatcher;
import net.alliknow.podcatcher.listeners.OnLoadPodcastListListener;
import net.alliknow.podcatcher.listeners.OnLoadPodcastListener;
import net.alliknow.podcatcher.listeners.OnLoadPodcastLogoListener;
import net.alliknow.podcatcher.tasks.LoadPodcastListTask;
import net.alliknow.podcatcher.tasks.Progress;
import net.alliknow.podcatcher.tasks.StorePodcastListTask;
import net.alliknow.podcatcher.tasks.remote.LoadPodcastLogoTask;
import net.alliknow.podcatcher.tasks.remote.LoadPodcastTask;
import net.alliknow.podcatcher.types.Podcast;
import android.app.Activity;
import android.app.Fragment;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;

/**
 * The podcast data fragment.
 * This holds and keeps all our data and is retained
 * across configuration changes. 
 */
public class PodcastDataFragment extends Fragment implements 
	OnLoadPodcastListener, OnLoadPodcastLogoListener, OnLoadPodcastListListener {

	/** Call-back types the target fragment has to implement */
	private OnLoadPodcastListListener loadListListener;
	private OnLoadPodcastListener loadPodcastListener;
	private OnLoadPodcastLogoListener loadLogoListener;
	
	/** The list of podcasts we know */
	private List<Podcast> podcastList;
	/** The list of podcast suggestions */
	private List<Podcast> podcastSuggestions;
	
	/** The current podcast load tasks */
	private Map<Podcast, LoadPodcastTask> loadPodcastTasks = new HashMap<Podcast, LoadPodcastTask>();
	/** The current podcast logo load tasks */
	private Map<Podcast, LoadPodcastLogoTask> loadPodcastLogoTasks = new HashMap<Podcast, LoadPodcastLogoTask>();
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		
		// Load podcasts from stored file if not available,
		// i.e. this is running the first time.
		if (podcastList == null) {
			LoadPodcastListTask loadListTask = new LoadPodcastListTask(getActivity(), this);
			loadListTask.execute((Void)null);
		}
		
		// Init call-backs
		// This should always work, no point in catching exceptions here...
		loadListListener = (OnLoadPodcastListListener) getTargetFragment();
		loadPodcastListener = (OnLoadPodcastListener) getTargetFragment();
		loadLogoListener = (OnLoadPodcastLogoListener) getTargetFragment();
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// This fragment is keep in config changes,
		// since it holds all our data.
		setRetainInstance(true);
	}
	
	@Override
	public void onDetach() {
		// Make sure not to leak target fragment
		loadListListener = null;
		loadPodcastListener = null;
		loadLogoListener = null;
		
		super.onDetach();
	}
	
	@Override
	public void onDestroy() {
		// App went down, free resources.
		cancelAllLoadTasks();
		
		super.onDestroy();
	}
	
	@Override
	public void onPodcastListLoaded(List<Podcast> podcastList) {
		this.podcastList = podcastList;
		
		if (isInDebugMode(getActivity())) Podcatcher.putSamplePodcasts(podcastList);
		
		// Alert target UI fragment
		if (loadListListener != null) loadListListener.onPodcastListLoaded(podcastList);
		else Log.d(getClass().getSimpleName(), "Podcast list loaded, but no listener set.");
	}
	
	/**
	 * Load data for given podcast from its URL.
	 * This is an async load, so this method will return
	 * immediately. Implement the appropriate call-back
	 * to monitor the load process and to get its result.
	 * @param podcast Podcast to load.
	 */
	public void load(Podcast podcast) {
		// Download podcast RSS feed (async)
		LoadPodcastTask loadPodcastTask = new LoadPodcastTask(this);
		loadPodcastTask.preventZippedTransfer(isOnFastConnection(getActivity()));
		loadPodcastTask.execute(podcast);
		
		loadPodcastTasks.put(podcast, loadPodcastTask);
	}

	@Override
	public void onPodcastLoadProgress(Podcast podcast, Progress progress) {
		if (loadPodcastListener != null) loadPodcastListener.onPodcastLoadProgress(podcast, progress);
	}

	@Override
	public void onPodcastLoaded(Podcast podcast) {
		// Remove from the map of loading task
		loadPodcastTasks.remove(podcast);
		
		if (loadPodcastListener != null) loadPodcastListener.onPodcastLoaded(podcast);
		else Log.d(getClass().getSimpleName(), "Podcast loaded, but no listener attached.");
	}

	@Override
	public void onPodcastLoadFailed(Podcast podcast) {
		// Remove from the map of loading task
		loadPodcastTasks.remove(podcast);
		
		if (loadPodcastListener != null) loadPodcastListener.onPodcastLoadFailed(podcast);
		else Log.d(getClass().getSimpleName(), "Podcast failed to load, but no listener set.");
	}
	
	/**
	 * Load logo for given podcast from its URL.
	 * This is an async load, so this method will return
	 * immediately. Implement the appropriate call-back
	 * to monitor the load process and to get its result.
	 * @param podcast Podcast to load logo for.
	 * @param width Width the logo can be sampled down to.
	 * Give zero (0) to disable sampling.
	 * @param height Height the logo can be sampled down to.
	 */
	public void loadLogo(Podcast podcast, int width, int height) {
		LoadPodcastLogoTask loadPodcastLogoTask = new LoadPodcastLogoTask(this, width, height);
		loadPodcastLogoTask.setLoadLimit(isOnFastConnection(getActivity()) ? 
				LoadPodcastLogoTask.MAX_LOGO_SIZE_WIFI : LoadPodcastLogoTask.MAX_LOGO_SIZE_MOBILE);
		loadPodcastLogoTask.execute(podcast);
		
		loadPodcastLogoTasks.put(podcast, loadPodcastLogoTask);
	}
	
	@Override
	public void onPodcastLogoLoaded(Podcast podcast, Bitmap logo) {
		loadPodcastLogoTasks.remove(podcast);
		
		if (loadLogoListener != null) loadLogoListener.onPodcastLogoLoaded(podcast, logo);
		else Log.d(getClass().getSimpleName(), "Podcast logo loaded, but no listener set.");
	}

	@Override
	public void onPodcastLogoLoadFailed(Podcast podcast) {
		loadPodcastLogoTasks.remove(podcast);
		
		if (loadLogoListener != null) loadLogoListener.onPodcastLogoLoadFailed(podcast);
		else Log.d(getClass().getSimpleName(), "Podcast logo failed to load, but no listener set.");
	}
	
	public List<Podcast> getPodcastList() {
		if (podcastList == null) return null;
		// return copy in order to make sure
		// nobody changes this list on us.
		else return new ArrayList<Podcast>(podcastList);
	}
	
	public List<Podcast> getPodcastSuggestions() {
		if (podcastSuggestions == null) return null;
		// return copy in order to make sure
		// nobody changes this list on us.
		else return new ArrayList<Podcast>(podcastSuggestions);
	}

	public void setPodcastSuggestions(List<Podcast> suggestions) {
		this.podcastSuggestions = suggestions;
	}
	
	@SuppressWarnings("unchecked")
	public void addPodcast(Podcast newPodcast) {
		if (! podcastList.contains(newPodcast)) {
			podcastList.add(newPodcast);
			Collections.sort(podcastList);
			
			// Store changed list
			new StorePodcastListTask(getActivity()).execute(podcastList);
		} else Log.d(getClass().getSimpleName(), "Podcast \"" + newPodcast.getName() + "\" is already in list.");
	}
	
	@SuppressWarnings("unchecked")
	public void remove(int index) {
		if (index >= 0 && index < podcastList.size()) {
			podcastList.remove(index);
		
			// Store changed list
			new StorePodcastListTask(getActivity()).execute(podcastList);
		}
	}

	public int indexOf(Podcast podcast) {
		if (podcastList == null) return -1;
		else return podcastList.indexOf(podcast);
	}
	
	public Podcast get(int position) {
		if (podcastList == null) return null;
		else if (position < 0 || position >= podcastList.size()) return null;
		else return podcastList.get(position);
	}
	
	public int size() {
		if (podcastList == null) return 0;
		else return podcastList.size();
	}

	public boolean contains(Podcast podcast) {
		if (podcastList == null) return false;
		else return podcastList.contains(podcast);
	}
	
	public void cancelAllLoadTasks() {
		for (LoadPodcastTask task : loadPodcastTasks.values()) task.cancel(true);
		for (LoadPodcastLogoTask task : loadPodcastLogoTasks.values()) task.cancel(true);
		
		loadPodcastTasks.clear();
		loadPodcastLogoTasks.clear();
	}
}
