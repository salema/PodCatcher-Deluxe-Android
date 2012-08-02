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

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.alliknow.podcatcher.R;
import net.alliknow.podcatcher.types.Episode;
import android.content.Context;
import android.widget.SimpleAdapter;

/**
 * Adapter class used for the list of episodes
 * 
 * @author Kevin Hausmann
 */
public class EpisodeListAdapter extends SimpleAdapter {

	/** Map key for UI element referal */
	private static String EPISODE_NAME = "episode_name";
	private static String EPISODE_DATE = "episode_date";
	
	/** Create the actual UI mapping */
	private static String[] FROM = new String[] { EPISODE_NAME, EPISODE_DATE };
	private static int[] TO = new int[] { R.id.episode_name, R.id.episode_date };
	
	/**
	 * Create new adapter
	 * 
	 * @param context The activity
	 * @param episodeList The list of episodes to show in list
	 */
	public EpisodeListAdapter(Context context, List<Episode> episodeList) {
		super(context, fillMaps(context, episodeList), R.layout.episode_list_item, FROM, TO);
	}

	private static List<? extends Map<String, ?>> fillMaps(Context context,	List<Episode> episodeList) {
		DateFormat formatter = DateFormat.getDateInstance(DateFormat.LONG);
		
		// prepare the list of maps for all episodes
		List<HashMap<String, String>> episodeMaps = new ArrayList<HashMap<String, String>>();
		for (Episode episode : episodeList) {
			HashMap<String, String> episodeMap = new HashMap<String, String>();
			
			episodeMap.put(EPISODE_NAME, episode.getName());
			episodeMap.put(EPISODE_DATE, formatter.format(episode.getPubDate()));
			
			episodeMaps.add(episodeMap);
		}
		
		return episodeMaps;
	}
}
