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

import java.io.FileOutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;

import net.alliknow.podcatcher.R;
import net.alliknow.podcatcher.adapters.PodcastListAdapter;
import net.alliknow.podcatcher.fragments.AddPodcastFragment.AddPodcastListener;
import net.alliknow.podcatcher.tags.OPML;
import net.alliknow.podcatcher.tasks.LoadPodcastLogoTask;
import net.alliknow.podcatcher.tasks.LoadPodcastLogoTask.PodcastLogoLoader;
import net.alliknow.podcatcher.tasks.LoadPodcastTask;
import net.alliknow.podcatcher.tasks.LoadPodcastTask.PodcastLoader;
import net.alliknow.podcatcher.types.Podcast;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import android.app.Activity;
import android.app.ListFragment;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;

/**
 * List fragment to display the list of podcasts as part of the
 * podcast activity.
 * 
 * @author Kevin Hausmann
 */
public class PodcastListFragment extends ListFragment implements AddPodcastListener, PodcastLoader, PodcastLogoLoader {
	
	/** Container Activity must implement this interface */
    public interface OnPodcastSelectedListener {
    	/**
    	 * Updates the UI to reflect that a podcast has been selected.
    	 * @param selectedPodcast Podcast selected by the user
    	 */
    	public void onPodcastSelected(Podcast selectedPodcast);
    }
    /** The activity we are in (listens to user selection) */ 
    private OnPodcastSelectedListener selectedListener;
    
    /** Container Activity must implement this interface */
    public interface OnPodcastLoadedListener {
    	/**
    	 * Updates the UI to reflect that a podcast has been loaded.
    	 * @param loadedPodcast Podcast loaded
    	 */
    	public void onPodcastLoaded(Podcast loadedPodcast);
    }
    /** The activity we are in (listens to loading complete) */ 
    private OnPodcastLoadedListener loadedListener;
    
	/** The list of podcasts we know */
	private List<Podcast> podcastList = new ArrayList<Podcast>();
	/** Currently selected podcast */
	private Podcast currentPodcast;
	/** Currently show podcast logo */
	private Bitmap currentLogo;
	/** The name of the file we store our saved podcasts in (as OPML) */
	private static String OPML_FILENAME = "podcasts.opml";
	
	/** The current podcast load task */
	private LoadPodcastTask loadPodcastTask;
	/** The current podcast logo load task */
	private LoadPodcastLogoTask loadPodcastLogoTask;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		this.setRetainInstance(true);
		this.setHasOptionsMenu(true);
		// Loads podcasts from stored file to this.podcastList
		this.loadPodcastList();
		// Maps the podcast list items to the list UI
		setListAdapter(new PodcastListAdapter(getActivity(), podcastList));
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		
		return inflater.inflate(R.layout.podcast_list, container, false);
	}
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		
		if (this.currentLogo != null) setPodcastLogo(this.currentLogo);
	}
	
	@Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
       
        try {
            selectedListener = (OnPodcastSelectedListener) activity;
            loadedListener = (OnPodcastLoadedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnPodcastSelectedListener");
        }
    }
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.podcast_list_menu, menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.add_podcast_button) {
			AddPodcastFragment fragment = new AddPodcastFragment();
			fragment.setAddPodcastListener(this);
			fragment.show(getFragmentManager(), "add_podcast");
		}
		
		return item.getItemId() == R.id.add_podcast_button;
	}
	
	@Override
	public void onListItemClick(ListView list, View view, int position, long id) {
		Podcast selectedPodcast = this.podcastList.get(position);
		
		// Is this a new selection ?
		if (this.currentPodcast == null || !this.currentPodcast.equals(selectedPodcast)) {
			this.currentPodcast = selectedPodcast;
			
			// Prepare UI
			((PodcastListAdapter) getListAdapter()).setSelectedPosition(position);
			setPodcastLogo(BitmapFactory.decodeResource(getActivity().getResources(), R.drawable.default_podcast_logo));
			selectedListener.onPodcastSelected(selectedPodcast);
			
			// Stopp loading previous tasks
			if (this.loadPodcastTask != null) this.loadPodcastTask.cancel(true);
			if (this.loadPodcastLogoTask != null) this.loadPodcastLogoTask.cancel(true);
						
			// Load if too old, otherwise just use previously loaded version
			if (selectedPodcast.needsReload()) {
				// Download podcast RSS feed (async)
				this.loadPodcastTask = new LoadPodcastTask(this);
				this.loadPodcastTask.execute(selectedPodcast);	
			}
			// Use buffered content
			else this.onPodcastLoaded(selectedPodcast);
		}
	}
	
	/**
	 * Notified by async RSS file loader on completion.
	 * Updates UI to display the podcast's episodes.
	 * @param podcast Podcast RSS feed loaded for
	 */
	@Override
	public void onPodcastLoaded(Podcast podcast) {
		this.loadPodcastTask = null;
		
		// TODO Handle the case where the fragment is not attached because the activity is recreated
		loadedListener.onPodcastLoaded(podcast);
		
		// Download podcast logo
		if (podcast.getLogoUrl() != null) {
			this.loadPodcastLogoTask = new LoadPodcastLogoTask(this);
			this.loadPodcastLogoTask.execute(podcast);
		} else Log.i("Logo", "No logo for podcast " + podcast);
	}
	
	@Override
	public void onPodcastLogoLoaded(Bitmap logo) {
		this.loadPodcastLogoTask = null;
		this.currentLogo = logo;
		
		setPodcastLogo(logo);
	}
	
	@Override
	public void onPodcastLoadFailed(Podcast podcast) {
		this.loadPodcastTask = null;
		
		Log.w("podcast", "Podcast failed to load " + podcast);
	}
	
	@Override
	public void onPodcastLogoLoadFailed() {}
	
	@Override
	public void addPodcast(Podcast newPodcast) {
		if (! podcastList.contains(newPodcast)) {
			podcastList.add(newPodcast);
			Collections.sort(podcastList);
			
			setListAdapter(new PodcastListAdapter(getActivity(), podcastList));
		} else Log.d("Add podcast", "Podcast \"" + newPodcast.getName() + "\" is already in list.");
	}
	
	@Override
	public void onDetach() {
		super.onDetach();
		
		storePodcastList();
	}
	
	private void setPodcastLogo(Bitmap logo) {
		ImageView logoView = (ImageView) getView().findViewById(R.id.podcast_image);
		logoView.setImageBitmap(logo);
	}
	
	private void loadPodcastList() {
		//this is just for testing
		/*if (! Arrays.asList(this.getActivity().fileList()).contains(OPML_FILENAME))*/ this.writeDummyPodcastList();
		
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware(true);
			
			Document podcastFile = factory.newDocumentBuilder().parse(this.getActivity().openFileInput(OPML_FILENAME));
			NodeList podcasts = podcastFile.getElementsByTagName(OPML.OUTLINE);
			
			for (int index = 0; index < podcasts.getLength(); index++) {
				String name = podcasts.item(index).getAttributes().getNamedItem(OPML.TEXT).getNodeValue();
				String url = podcasts.item(index).getAttributes().getNamedItem(OPML.XMLURL).getNodeValue();
				
				podcastList.add(new Podcast(name, new URL(url)));
			}
			
			Collections.sort(podcastList); 
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void storePodcastList() {
		try {
			FileOutputStream fos = this.getActivity().openFileOutput(OPML_FILENAME, Context.MODE_PRIVATE);
			fos.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>".getBytes());
			fos.write("<opml version=\"2.0\">".getBytes());
			fos.write("<body>".getBytes());
			
			for (Podcast podcast : podcastList) {
				String outline = "<outline text=\"" + podcast.getName() + "\" xmlUrl=\"" + podcast.getUrl() + "\" />";
				fos.write(outline.getBytes());
			}
			
			fos.write("</body></opml>".getBytes());
			fos.close();
			
			Log.d("File", "OPML podcast file written");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void writeDummyPodcastList() {
		try {
			FileOutputStream fos = this.getActivity().openFileOutput(OPML_FILENAME, Context.MODE_PRIVATE);
			fos.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>".getBytes());
			fos.write("<opml version=\"2.0\">".getBytes());
			fos.write("<body>".getBytes());
			fos.write("<outline text=\"This American Life\" xmlUrl=\"http://feeds.thisamericanlife.org/talpodcast\"/>".getBytes());
			fos.write("<outline text=\"Radiolab\" xmlUrl=\"http://feeds.wnyc.org/radiolab\"/>".getBytes());
			fos.write("<outline text=\"Linux Outlaws\" xmlUrl=\"http://feeds.feedburner.com/linuxoutlaws\"/>".getBytes());
			fos.write("</body></opml>".getBytes());
			fos.close();
			
			Log.d("File", "Dummy OPML written");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
