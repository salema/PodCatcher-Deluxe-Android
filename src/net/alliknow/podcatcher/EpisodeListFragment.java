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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import net.alliknow.podcatcher.types.Episode;
import net.alliknow.podcatcher.types.Podcast;
import android.app.ListFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SimpleAdapter;

/**
 * List fragment to display the list of episodes as part of the
 * podcast activity.
 * 
 * @author Kevin Hausmann
 */
public class EpisodeListFragment extends ListFragment {

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		
		return inflater.inflate(R.layout.episode_list, container, false);
	}
	
	public void setPodcast(Podcast podcast) {
		final String episodeName = "episodeName"; 
		
		// create the UI mapping
		String[] from = new String[] { episodeName };
		int[] to = new int[] { R.id.episodeName };

		// prepare the list of all records
		List<HashMap<String, String>> fillMaps = new ArrayList<HashMap<String, String>>();
		for (Episode episode : podcast.getEpisodes()) {
			HashMap<String, String> map = new HashMap<String, String>();
			
			map.put(episodeName, episode.getName());
			
			fillMaps.add(map);
		}

		// fill in the layout
		SimpleAdapter adapter = new SimpleAdapter(this.getActivity(), fillMaps, R.layout.episode_list_item, from, to);
		setListAdapter(adapter);
	}
}
