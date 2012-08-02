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
package net.alliknow.podcatcher.adapters;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.alliknow.podcatcher.R;
import net.alliknow.podcatcher.types.Podcast;
import android.content.Context;
import android.widget.SimpleAdapter;

/**
 * Adapter class used for the list of podcasts
 * 
 * @author Kevin Hausmann
 */
public class PodcastListAdapter extends SimpleAdapter {
	
	/** Map key for UI element referal */
	private static String PODCAST_NAME = "podcast_name";
	private static String PODCAST_EPISODE_COUNT = "podcast_episode_count";
	
	/** Create the actual UI mapping */
	private static String[] FROM = new String[] { PODCAST_NAME, PODCAST_EPISODE_COUNT };
	private static int[] TO = new int[] { R.id.podcast_name, R.id.podcast_episode_count };

	/**
	 * Create new adapter
	 * 
	 * @param context The activity
	 * @param podcastList The list of podcasts to show in list
	 */
	public PodcastListAdapter(Context context, List<Podcast> podcastList) {
		super(context, fillMaps(context, podcastList), R.layout.podcast_list_item, FROM, TO);
	}

	private static List<? extends Map<String, ?>> fillMaps(Context context, List<Podcast> podcastList) {
		// prepare the list of maps for all podcasts
		List<HashMap<String, String>> podcastMaps = new ArrayList<HashMap<String, String>>();
		for (Podcast podcast : podcastList) {
			HashMap<String, String> podcastMap = new HashMap<String, String>();
			
			podcastMap.put(PODCAST_NAME, podcast.getName());
			//podcastMap.put(PODCAST_EPISODE_COUNT, podcast.getEpisodes().size() + " " + 
			//		context.getResources().getText(R.string.episodes).toString());
			
			podcastMaps.add(podcastMap);
		}

		return podcastMaps;
	}
}
