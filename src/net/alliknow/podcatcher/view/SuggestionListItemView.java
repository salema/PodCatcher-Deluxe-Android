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

package net.alliknow.podcatcher.view;

import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import net.alliknow.podcatcher.R;
import net.alliknow.podcatcher.listeners.OnAddSuggestionListener;
import net.alliknow.podcatcher.model.types.MediaType;
import net.alliknow.podcatcher.model.types.Podcast;

/**
 * A list item view to represent a podcast suggestion.
 */
public class SuggestionListItemView extends RelativeLayout {

    /** Separator for meta data in the UI */
    private static final String METADATA_SEPARATOR = " • ";

    /** The title text view */
    private TextView titleTextView;
    /** The meta information text view */
    private TextView metaTextView;
    /** The add suggestion button */
    private Button addButton;
    /** The description text view */
    private TextView descriptionTextView;

    /**
     * Create a podcast suggestion item list view.
     * 
     * @param context Context for the view to live in.
     * @param attrs View attributes.
     */
    public SuggestionListItemView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        titleTextView = (TextView) findViewById(R.id.suggestion_name);
        metaTextView = (TextView) findViewById(R.id.suggestion_meta);
        addButton = (Button) findViewById(R.id.suggestion_add_button);
        descriptionTextView = (TextView) findViewById(R.id.suggestion_description);
    }

    /**
     * Make the view update all its child to represent input given.
     * 
     * @param suggestion Podcast suggestion to represent.
     * @param listener Call-back to alert when the button is pressed.
     * @param alreadyAdded Whether the suggestion is already added.
     */
    public void show(final Podcast suggestion, final OnAddSuggestionListener listener,
            boolean alreadyAdded) {
        // 1. Set the text to display for title
        titleTextView.setText(suggestion.getName());

        // 2. Set the text to display for classification
        metaTextView.setText(createClassificationLabel(suggestion));

        // 3. Set the text to display for the description
        descriptionTextView.setText(suggestion.getDescription());

        // 4. Find and prepare the add button
        if (alreadyAdded) {
            showCheckmarkInsteadOfButton();
        } else {
            addButton.setEnabled(true);
            addButton.setBackgroundResource(R.drawable.button_green);
            addButton.setText(suggestion.getMediaType().equals(MediaType.AUDIO) ?
                    R.string.suggestion_listen : R.string.suggestion_watch);
            addButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
            addButton.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View view) {
                    showCheckmarkInsteadOfButton();

                    listener.onAddPodcast(suggestion);
                }
            });
        }
    }

    private String createClassificationLabel(final Podcast suggestion) {
        final Resources res = getResources();

        return res.getStringArray(R.array.languages)[suggestion.getLanguage().ordinal()]
                + METADATA_SEPARATOR
                + res.getStringArray(R.array.genres)[suggestion.getGenre().ordinal()]
                + METADATA_SEPARATOR
                + res.getStringArray(R.array.types)[suggestion.getMediaType().ordinal()];
    }

    private void showCheckmarkInsteadOfButton() {
        addButton.setEnabled(false);
        addButton.setBackgroundDrawable(null);
        addButton.setText(null);
        addButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_checkmark, 0);
    }
}
