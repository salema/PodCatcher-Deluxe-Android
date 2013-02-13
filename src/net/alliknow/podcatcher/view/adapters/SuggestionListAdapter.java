/** Copyright 2012, 2013 Kevin Hausmann
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

package net.alliknow.podcatcher.view.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import net.alliknow.podcatcher.R;
import net.alliknow.podcatcher.listeners.OnAddSuggestionListener;
import net.alliknow.podcatcher.model.types.Podcast;

import java.util.List;

/**
 * Adapter for suggestion list.
 */
public class SuggestionListAdapter extends PodcastListAdapter {

    /** Owner for button call backs */
    protected final OnAddSuggestionListener listener;
    /** Separator for meta data in the UI */
    private static final String METADATA_SEPARATOR = " ● ";

    /**
     * Create new adapter.
     * 
     * @param context The current context.
     * @param podcastList List of podcasts (suggestions) to wrap.
     * @param listener Call back for the add button to attach.
     */
    public SuggestionListAdapter(Context context, List<Podcast> podcastList,
            OnAddSuggestionListener listener) {
        super(context, podcastList);

        this.listener = listener;
    }

    @Override
    public View getView(int position, View itemView, ViewGroup parent) {
        // Create the return view (this should not be recycled)
        itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.suggestion_list_item, parent, false);

        // Find suggestion to represent
        final Podcast suggestion = list.get(position);

        // Set the text to display for title
        setText(itemView, R.id.suggestion_name, suggestion.getName());
        // Set the text to display for classification
        setText(itemView, R.id.suggestion_meta,
                getResourceString(itemView.getResources(), suggestion.getLanguage())
                        + METADATA_SEPARATOR +
                        getResourceString(itemView.getResources(), suggestion.getGenre())
                        + METADATA_SEPARATOR +
                        getResourceString(itemView.getResources(), suggestion.getMediaType()));
        // Set the text to display for the description
        setText(itemView, R.id.suggestion_description, suggestion.getDescription());

        // Find and prepare the add button
        final Button addButton = (Button) itemView.findViewById(R.id.suggestion_add_button);
        addButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                addButton.setEnabled(false);
                addButton.setBackgroundDrawable(null);
                addButton.setText(null);
                addButton.setCompoundDrawablesWithIntrinsicBounds(0, 0,
                        R.drawable.ic_checkmark_light, 0);

                listener.onAddPodcast(suggestion);
            }
        });

        return itemView;
    }
}
