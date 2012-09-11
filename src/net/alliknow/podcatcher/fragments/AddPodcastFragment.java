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

import java.net.MalformedURLException;
import java.net.URL;

import net.alliknow.podcatcher.R;
import net.alliknow.podcatcher.types.Podcast;
import android.app.DialogFragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

/**
 * A dialog to let an user add a podcast.
 * 
 * @author Kevin Hausmann
 */
public class AddPodcastFragment extends DialogFragment {

	/**
     * Interface definition for a callback to be invoked when a podcast is added.
     */
	public interface AddPodcastListener {
		
		/**
		 * Called on listener when podcast is added.
		 * @param newPodcast Podcast to add.
		 */
		void addPodcast(Podcast newPodcast);
	}

	/** The add podcast listener */
	private AddPodcastListener listener;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		
		return inflater.inflate(R.layout.add_podcast, container, false);
	}
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		getDialog().setTitle(R.string.add_podcast);
		Button add = (Button)view.findViewById(R.id.add_podcast_button);
		add.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				addPodcast("http://downloads.bbc.co.uk/podcasts/worldservice/bizdaily/rss.xml");
			}
		});
	}
	
	public void setAddPodcastListener(AddPodcastListener listener) {
		this.listener = listener;
	}
	
	private void addPodcast(String string) {
		dismiss();
		Log.d("Add Podcast", string);
		try {
			listener.addPodcast(new Podcast("Test", new URL(string)));
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
