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
import android.widget.TextView;

/**
 * Adapter class used for the list of episodes
 * 
 * @author Kevin Hausmann
 */
public class EpisodeListAdapter extends PodcatcherBaseAdapter {

	/** The list our date resides in */
	private List<Episode> list;
	/** Formatter to use for the episode date */
	private final DateFormat formatter = DateFormat.getDateInstance(DateFormat.LONG);
	
	/**
	 * Create new adapter
	 * 
	 * @param context The activity
	 * @param episodeList The list of episodes to show in list
	 */
	public EpisodeListAdapter(Context context, List<Episode> episodeList) {
		super(context);
		this.list = episodeList;
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
		return list.get(position).getMediaUrl().hashCode();
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if (convertView == null) 
			convertView = inflater.inflate(R.layout.episode_list_item, parent, false);
		
		TextView nameView = (TextView) convertView.findViewById(R.id.episode_name);
		nameView.setText(this.list.get(position).getName());
		setBackground(position, nameView);
		
		TextView dateView = (TextView) convertView.findViewById(R.id.episode_date);
		dateView.setText(formatter.format(this.list.get(position).getPubDate()));
		setBackground(position, dateView);		
		
		return convertView;
	}
}
