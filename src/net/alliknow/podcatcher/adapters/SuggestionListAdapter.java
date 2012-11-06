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
import net.alliknow.podcatcher.listeners.OnAddPodcastListener;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

/**
 * @author Kevin Hausmann
 */
public class SuggestionListAdapter extends PodcastListAdapter {

	private OnAddPodcastListener listener;
	
	/**
	 * @param context
	 * @param podcastList
	 */
	public SuggestionListAdapter(Context context, PodcastList podcastList, OnAddPodcastListener listener) {
		super(context, podcastList);
		
		this.listener = listener;
	}
	
	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		convertView = findReturnView(convertView, parent, R.layout.suggestion_list_item);
		
		setText(convertView, R.id.suggestion_name, list.get(position).getName(), position);
		setText(convertView, R.id.suggestion_meta, list.get(position).getLanguage() + " " +
				list.get(position).getGenre() + " " + list.get(position).getMediaType(), position);
		setText(convertView, R.id.suggestion_description, list.get(position).getDescription(), position);
		
		Button addButton = (Button) convertView.findViewById(R.id.add_suggestion_button);
		addButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				listener.addPodcast(list.get(position));
			}
		});
		
		return convertView;
	}
	
	@Override
	protected void setText(View listItem, int viewId, String text, int position) {
		TextView textView = (TextView) listItem.findViewById(viewId);
		textView.setText(text);
	}
}
