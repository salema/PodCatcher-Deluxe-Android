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

import java.io.FileOutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;

import net.alliknow.podcatcher.tags.OPML;
import net.alliknow.podcatcher.types.Podcast;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import android.app.Activity;
import android.app.ListFragment;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleAdapter;

/**
 * List fragment to display the list of podcasts as part of the
 * podcast activity.
 * 
 * @author Kevin Hausmann
 */
public class PodcastListFragment extends ListFragment {
	
	/** Container Activity must implement this interface */
    public interface OnPodcastSelectedListener {
    	/**
    	 * Updates the UI to reflect that a podcast has been selected.
    	 * @param selectedPodcast Podcast selected by the user
    	 */
    	public void onPodcastSelected(Podcast selectedPodcast);
    }
    /** The activity we are in (listens to user selection) */ 
    private OnPodcastSelectedListener listener;
    
	/** The list of podcasts we know */
	private List<Podcast> podcastList = new ArrayList<Podcast>();
	/** The name of the file we store our saved podcasts in (as OPML) */
	private static String OPML_FILENAME = "podcasts.opml";
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Loads podcasts from stored file to this.podcastList
		this.loadPodcastList();
		// Maps the podcast list items to the list UI
		this.createAndSetListAdapter();
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		
		return inflater.inflate(R.layout.podcast_list, container, false);
	}
	
	@Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
       
        try {
            listener = (OnPodcastSelectedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnPodcastSelectedListener");
        }
    }
	
	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		Podcast selectedPodcast = this.podcastList.get(position);
		listener.onPodcastSelected(selectedPodcast);
	}
	
	public void setPodcastLogo(Bitmap logo) {
		ImageView logoView = (ImageView) getView().findViewById(R.id.podcastImage);
		logoView.setImageBitmap(logo);
	}
	
	private void createAndSetListAdapter() {
		final String podcastName = "podcastName"; 
		
		// create the UI mapping
		String[] from = new String[] { podcastName };
		int[] to = new int[] { R.id.podcastName };

		// prepare the list of all records
		List<HashMap<String, String>> fillMaps = new ArrayList<HashMap<String, String>>();
		for (Podcast podcast : podcastList) {
			HashMap<String, String> map = new HashMap<String, String>();
			
			map.put(podcastName, podcast.getName());
			
			fillMaps.add(map);
		}

		// fill in the layout
		SimpleAdapter adapter = new SimpleAdapter(this.getActivity(), fillMaps, R.layout.podcast_list_item, from, to);
		setListAdapter(adapter);
	}
	
	private void loadPodcastList() {
		//this is just for testing
		if (! Arrays.asList(this.getActivity().fileList()).contains(OPML_FILENAME)) this.writeDummyPodcastList();
		
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
		} catch (Exception e) {
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
