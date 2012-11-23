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
import java.util.List;

import net.alliknow.podcatcher.R;
import net.alliknow.podcatcher.types.Episode;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

/**
 * Adapter class used for the list of episodes
 * 
 * @author Kevin Hausmann
 */
public class EpisodeListAdapter extends PodcatcherBaseListAdapter {

	/** The list our data resides in */
	protected List<Episode> list;
	/** Whether the podcast name should be shown */
	protected boolean showPodcastName = false;
	/** Formatter to use for the episode date */
	protected final DateFormat formatter = DateFormat.getDateInstance(DateFormat.LONG);
	
	/**
	 * Create new adapter
	 * 
	 * @param context The activity
	 * @param episodeList The list of episodes to show in list
	 */
	public EpisodeListAdapter(Context context, List<Episode> episodeList) {
		this(context, episodeList, false);
	}
	
	/**
	 * Create new adapter
	 * 
	 * @param context The activity
	 * @param episodeList The list of episodes to show in list
	 * @param showPodcastName Whether the podcast name should be shown next to the date
	 */
	public EpisodeListAdapter(Context context, List<Episode> episodeList, boolean showPodcastName) {
		super(context);
		
		this.list = episodeList;
		this.showPodcastName = showPodcastName;
	}
	
	@Override
	public int getCount() {
		return list.size();
	}

	@Override
	public Object getItem(int position) {
		return list.get(position);
	}

	@Override
	public long getItemId(int position) {
		return list.get(position).hashCode();
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		convertView = findReturnView(convertView, parent, R.layout.list_item);
		
		setTextAndState(convertView, R.id.list_item_title, list.get(position).getName(), position);
		setTextAndState(convertView, R.id.list_item_caption, createDateText(list.get(position)), position);
				
		return convertView;
	}

	private String createDateText(Episode episode) {
		if (episode.getPubDate() == null && !showPodcastName) return "---";
		else if (episode.getPubDate() == null && showPodcastName) return episode.getPodcastName();
		else if (showPodcastName) return formatter.format(episode.getPubDate()) + " - " + episode.getPodcastName();
		else return formatter.format(episode.getPubDate());
	}
}
