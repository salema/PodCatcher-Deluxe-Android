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

import net.alliknow.podcatcher.PodcastList;
import net.alliknow.podcatcher.R;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

/**
 * Adapter class used for the list of podcasts.
 * 
 * @author Kevin Hausmann
 */
public class PodcastListAdapter extends PodcatcherBaseListAdapter {

	/** The list our data resides in */
	protected PodcastList list;

	/**
	 * Create new adapter
	 * 
	 * @param context The current context
	 * @param podcastList List of podcasts to wrap (not <code>null</code>)
	 */
	public PodcastListAdapter(Context context, PodcastList podcastList) {
		super(context);
		
		this.list = podcastList;
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
		convertView = findReturnView(convertView, parent, R.layout.podcast_list_item);
		
		int numberOfEpisodes = list.get(position).getEpisodes().size();
		setTextAndState(convertView, R.id.podcast_name, list.get(position).getName(), position);
		setTextAndState(convertView, R.id.podcast_episode_number, getEpisodeNumberText(numberOfEpisodes), position);
		
		convertView.findViewById(R.id.podcast_episode_number)
			.setVisibility(numberOfEpisodes != 0 ? View.VISIBLE : View.GONE);
		
		return convertView;
	}

	private String getEpisodeNumberText(int numberOfEpisodes) {
		return numberOfEpisodes == 1 ? 
				resources.getString(R.string.one_episode) :
				numberOfEpisodes + " " + resources.getString(R.string.episodes);
	}
}
