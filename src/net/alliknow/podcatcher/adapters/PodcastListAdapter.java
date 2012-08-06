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

import java.util.List;

import net.alliknow.podcatcher.R;
import net.alliknow.podcatcher.types.Podcast;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

/**
 * Adapter class used for the list of podcasts
 * 
 * @author Kevin Hausmann
 */
public class PodcastListAdapter extends BaseAdapter {

	/** The list our date resides in */
	private List<Podcast> list;
	/** Inflater for new views */
	private LayoutInflater inflater;
	
	/**
	 * Create new adapter
	 * @param context The current context
	 * @param podcastList List of podcasts to wrap
	 */
	public PodcastListAdapter(Context context, List<Podcast> podcastList) {
		this.inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
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
		return list.get(position).getUrl().hashCode();
	}
	
	@Override
	public boolean hasStableIds() {
		return true;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if (convertView == null) 
			convertView = inflater.inflate(R.layout.podcast_list_item, parent, false);
		
		((TextView) convertView).setText(this.list.get(position).getName());
		
		return convertView;
	}
}
