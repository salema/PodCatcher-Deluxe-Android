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
import net.alliknow.podcatcher.types.Podcast;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

/**
 * Adapter for suggestion list.
 * 
 * @author Kevin Hausmann
 */
public class SuggestionListAdapter extends PodcastListAdapter {
	
	/** Owner for button call backs */
	protected OnAddPodcastListener listener;
	/** Separator for meta data in the UI */
	private static final String METADATA_SEPARATOR = " ● ";
	
	/**
	 * Create new adapter
	 * 
	 * @param context The current context
	 * @param podcastList List of podcasts (suggestions) to wrap
	 * @param listener Call back for the add button to attach
	 */
	public SuggestionListAdapter(Context context, PodcastList podcastList, OnAddPodcastListener listener) {
		super(context, podcastList);
		
		this.listener = listener;
	}
	
	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		convertView = findReturnView(convertView, parent, R.layout.suggestion_list_item);
		final Podcast suggestion = list.get(position);
		
		setText(convertView, R.id.suggestion_name, suggestion.getName());
		setText(convertView, R.id.suggestion_meta,
				getResourceString(suggestion.getLanguage()) + METADATA_SEPARATOR +
				getResourceString(suggestion.getGenre()) + METADATA_SEPARATOR +
				getResourceString(suggestion.getMediaType()));
		setText(convertView, R.id.suggestion_description, suggestion.getDescription());
		
		Button addButton = (Button) convertView.findViewById(R.id.add_suggestion_button);
		addButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View view) {
				listener.addPodcast(suggestion);
				view.setEnabled(false);
			}
		});
		
		return convertView;
	}
}
