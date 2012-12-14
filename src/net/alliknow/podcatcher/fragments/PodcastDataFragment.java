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

import static net.alliknow.podcatcher.Podcatcher.isOnFastConnection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.alliknow.podcatcher.Podcatcher;
import net.alliknow.podcatcher.listeners.OnAddPodcastListener;
import net.alliknow.podcatcher.listeners.OnLoadPodcastListListener;
import net.alliknow.podcatcher.listeners.OnLoadPodcastListener;
import net.alliknow.podcatcher.listeners.OnLoadPodcastLogoListener;
import net.alliknow.podcatcher.listeners.OnShowSuggestionsListener;
import net.alliknow.podcatcher.tasks.LoadPodcastListTask;
import net.alliknow.podcatcher.tasks.Progress;
import net.alliknow.podcatcher.tasks.StorePodcastListTask;
import net.alliknow.podcatcher.tasks.remote.LoadPodcastLogoTask;
import net.alliknow.podcatcher.tasks.remote.LoadPodcastTask;
import net.alliknow.podcatcher.types.Podcast;
import android.app.Fragment;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;

/**
 *
 */
public class PodcastDataFragment extends Fragment implements OnAddPodcastListener, 
OnShowSuggestionsListener, OnLoadPodcastListener, OnLoadPodcastLogoListener, OnLoadPodcastListListener {

	/** The list of podcasts we know */
	private List<Podcast> podcastList = new ArrayList<Podcast>();
	/** The list of podcast suggestions */
	private List<Podcast> podcastSuggestions;
	
	/** The current podcast load tasks */
	private Map<Podcast, LoadPodcastTask> loadPodcastTasks = new HashMap<Podcast, LoadPodcastTask>();
	/** The current podcast logo load tasks */
	private Map<Podcast, LoadPodcastLogoTask> loadPodcastLogoTasks = new HashMap<Podcast, LoadPodcastLogoTask>();
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setRetainInstance(true);
		
		// Load podcasts from stored file
		LoadPodcastListTask loadListTask = new LoadPodcastListTask(getActivity(), this);
		loadListTask.execute((Void)null);
	}
	
	@Override
	public void onPodcastListLoaded(List<Podcast> podcastList) {
		this.podcastList = podcastList;
		
		if (Podcatcher.isInDebugMode(getActivity())) Podcatcher.putSamplePodcasts(podcastList);
		
		((OnLoadPodcastListListener) getTargetFragment()).onPodcastListLoaded(podcastList);
	}
	
	public int indexOf(Podcast podcast) {
		if (podcastList == null) return -1;
		else return podcastList.indexOf(podcast);
	}
	
	public Podcast get(int position) {
		return podcastList.get(position);
	}

	@Override
	public void onPodcastLogoLoaded(Podcast podcast, Bitmap logo) {
		loadPodcastLogoTasks.remove(podcast);
		
		((OnLoadPodcastLogoListener) getTargetFragment()).onPodcastLogoLoaded(podcast, logo);
	}

	@Override
	public void onPodcastLogoLoadFailed(Podcast podcast) {
		loadPodcastLogoTasks.remove(podcast);
		
		((OnLoadPodcastLogoListener) getTargetFragment()).onPodcastLogoLoadFailed(podcast);
	}

	@Override
	public void onPodcastLoadProgress(Podcast podcast, Progress progress) {
		((OnLoadPodcastListener) getTargetFragment()).onPodcastLoadProgress(podcast, progress);
	}

	@Override
	public void onPodcastLoaded(Podcast podcast) {
		// Remove from the map of loading task
		loadPodcastTasks.remove(podcast);
		
		((OnLoadPodcastListener) getTargetFragment()).onPodcastLoaded(podcast);
	}

	@Override
	public void onPodcastLoadFailed(Podcast podcast) {
		// Remove from the map of loading task
		loadPodcastTasks.remove(podcast);
		
		((OnLoadPodcastListener) getTargetFragment()).onPodcastLoadFailed(podcast);
	}

	@Override
	public List<Podcast> getPodcastSuggestions() {
		return podcastSuggestions;
	}

	@Override
	public void setPodcastSuggestions(List<Podcast> suggestions) {
		this.podcastSuggestions = suggestions;
	}

	@Override
	public List<Podcast> getPodcastList() {
		return this.podcastList;
	}

	@Override
	public void addPodcast(Podcast newPodcast) {
		if (! podcastList.contains(newPodcast)) {
			podcastList.add(newPodcast);
			Collections.sort(podcastList);			
			new StorePodcastListTask(getActivity()).execute(podcastList);
		} else Log.d(getClass().getSimpleName(), "Podcast \"" + newPodcast.getName() + "\" is already in list.");
	}

	@Override
	public void showSuggestions() {
		// TODO Auto-generated method stub
		
	}
	
	public void cancelAllLoadTasks() {
		for (LoadPodcastTask task : loadPodcastTasks.values()) task.cancel(true);
		for (LoadPodcastLogoTask task : loadPodcastLogoTasks.values()) task.cancel(true);
		
		loadPodcastTasks.clear();
		loadPodcastLogoTasks.clear();
	}

	public void load(Podcast podcast) {
		// Download podcast RSS feed (async)
		LoadPodcastTask loadPodcastTask = new LoadPodcastTask(this);
		loadPodcastTask.preventZippedTransfer(isOnFastConnection(getActivity()));
		loadPodcastTask.execute(podcast);
		
		loadPodcastTasks.put(podcast, loadPodcastTask);
	}
	
	public void loadLogo(Podcast podcast, int width, int height) {
		LoadPodcastLogoTask loadPodcastLogoTask = new LoadPodcastLogoTask(this, width, height);
		loadPodcastLogoTask.setLoadLimit(isOnFastConnection(getActivity()) ? 
				LoadPodcastLogoTask.MAX_LOGO_SIZE_WIFI : LoadPodcastLogoTask.MAX_LOGO_SIZE_MOBILE);
		loadPodcastLogoTask.execute(podcast);
		
		loadPodcastLogoTasks.put(podcast, loadPodcastLogoTask);
	}

	public int size() {
		if (podcastList == null) return 0;
		else return podcastList.size();
	}

	public void remove(int index) {
		podcastList.remove(index);
		
		// Store changed list
		new StorePodcastListTask(getActivity()).execute(podcastList);
	}

	public boolean contains(Podcast podcast) {
		if (podcastList == null) return false;
		else return podcastList.contains(podcast);
	}
}
