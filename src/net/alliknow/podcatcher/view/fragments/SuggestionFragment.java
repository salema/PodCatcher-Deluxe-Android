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

package net.alliknow.podcatcher.view.fragments;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import net.alliknow.podcatcher.R;
import net.alliknow.podcatcher.listeners.OnAddSuggestionListener;
import net.alliknow.podcatcher.model.types.Genre;
import net.alliknow.podcatcher.model.types.Language;
import net.alliknow.podcatcher.model.types.MediaType;
import net.alliknow.podcatcher.model.types.Podcast;
import net.alliknow.podcatcher.model.types.Progress;
import net.alliknow.podcatcher.view.ProgressView;
import net.alliknow.podcatcher.view.adapters.GenreSpinnerAdapter;
import net.alliknow.podcatcher.view.adapters.LanguageSpinnerAdapter;
import net.alliknow.podcatcher.view.adapters.MediaTypeSpinnerAdapter;
import net.alliknow.podcatcher.view.adapters.SuggestionListAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Fragment to show podcast suggestions.
 */
public class SuggestionFragment extends DialogFragment {

    /** The filter wildcard */
    public static final String FILTER_WILDCARD = "ALL";

    /** Mail address to send new suggestions to */
    private static final String SUGGESTION_MAIL_ADDRESS = "suggestion@podcatcher-deluxe.com";
    /** Subject for mail with new suggestions */
    private static final String SUGGESTION_MAIL_SUBJECT = "A proposal for a podcast suggestion in the Podcatcher Android apps";

    /** The call back we work on */
    private OnAddSuggestionListener listener;
    /** The list of suggestions to show */
    private List<Podcast> suggestionList;

    /** The language filter */
    private Spinner languageFilter;
    /** The genre filter */
    private Spinner genreFilter;
    /** The media type filter */
    private Spinner mediaTypeFilter;
    /** The progress view */
    private ProgressView progressView;
    /** The suggestions list view */
    private ListView suggestionsListView;
    /** The no suggestions view */
    private TextView noSuggestionsView;
    /** The send a suggestion view */
    private TextView sendSuggestionView;

    /** The listener to update the list on filter change */
    private final OnItemSelectedListener selectionListener = new OnItemSelectedListener() {

        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            updateList();
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
            updateList();
        }
    };

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // Make sure our listener is present
        try {
            this.listener = (OnAddSuggestionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnAddSuggestionListener");
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        return inflater.inflate(R.layout.suggestions, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        getDialog().setTitle(R.string.suggested_podcasts);

        languageFilter = (Spinner) view.findViewById(R.id.suggestion_language_select);
        languageFilter.setAdapter(new LanguageSpinnerAdapter(getActivity()));
        languageFilter.setOnItemSelectedListener(selectionListener);

        genreFilter = (Spinner) view.findViewById(R.id.suggestion_genre_select);
        genreFilter.setAdapter(new GenreSpinnerAdapter(getActivity()));
        genreFilter.setOnItemSelectedListener(selectionListener);

        mediaTypeFilter = (Spinner) view.findViewById(R.id.suggestion_type_select);
        mediaTypeFilter.setAdapter(new MediaTypeSpinnerAdapter(getActivity()));
        mediaTypeFilter.setOnItemSelectedListener(selectionListener);

        progressView = (ProgressView) view.findViewById(R.id.suggestion_list_progress);

        suggestionsListView = (ListView) view.findViewById(R.id.suggestion_podcasts);
        noSuggestionsView = (TextView) view.findViewById(R.id.suggestion_none);

        sendSuggestionView = (TextView) view.findViewById(R.id.suggestion_send);
        sendSuggestionView.setText(Html.fromHtml("<a href=\"mailto:" + SUGGESTION_MAIL_ADDRESS +
                "?subject=" + SUGGESTION_MAIL_SUBJECT + "\">" +
                getResources().getString(R.string.send_suggestion) + "</a>"));
        sendSuggestionView.setMovementMethod(LinkMovementMethod.getInstance());

        restoreFilters(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();

        // The list might have changed while we were paused
        updateList();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt(Language.class.getSimpleName(), languageFilter.getSelectedItemPosition());
        outState.putInt(Genre.class.getSimpleName(), genreFilter.getSelectedItemPosition());
        outState.putInt(MediaType.class.getSimpleName(), mediaTypeFilter.getSelectedItemPosition());
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        // Make sure the parent activity knows when we are closing
        if (listener instanceof OnCancelListener)
            ((OnCancelListener) listener).onCancel(dialog);
    }

    /**
     * Set list of suggestions to show and update the UI.
     * 
     * @param suggestions Podcasts to show.
     */
    public void setList(List<Podcast> suggestions) {
        // Set the list to show
        this.suggestionList = suggestions;

        // Filter list and update UI (if ready)
        if (isResumed())
            updateList();
    }

    /**
     * Show load suggestions progress.
     * 
     * @param progress Progress information to give.
     */
    public void showLoadProgress(Progress progress) {
        progressView.publishProgress(progress);
    }

    /**
     * Show load failed for podcast suggestions.
     */
    public void showLoadFailed() {
        progressView.showError(R.string.error_suggestions_load);
    }

    private void restoreFilters(Bundle savedInstanceState) {
        // Coming from configuration change
        if (savedInstanceState != null) {
            languageFilter.setSelection(savedInstanceState.getInt(Language.class.getSimpleName()));
            genreFilter.setSelection(savedInstanceState.getInt(Genre.class.getSimpleName()));
            mediaTypeFilter
                    .setSelection(savedInstanceState.getInt(MediaType.class.getSimpleName()));
        } // Initial opening of the dialog
        else
            setInitialFilterSelection();
    }

    private void setInitialFilterSelection() {
        // Set according to locale
        Locale current = getActivity().getResources().getConfiguration().locale;
        languageFilter.setSelection(current.getLanguage().equalsIgnoreCase("de") ? 2 : 1);
        // Set to "all"
        genreFilter.setSelection(0);
        // Set to audio, since this is an audio version
        mediaTypeFilter.setSelection(1);
    }

    private void updateList() {
        // Filter the suggestion list
        if (suggestionList != null) {
            // Resulting list
            List<Podcast> filteredSuggestionList = new ArrayList<Podcast>();
            // Do filter!
            for (Podcast suggestion : suggestionList)
                if (matchesFilter(suggestion))
                    filteredSuggestionList.add(suggestion);

            // Set filtered list
            suggestionsListView.setAdapter(new SuggestionListAdapter(getActivity(),
                    filteredSuggestionList, listener));
            // Update UI
            if (filteredSuggestionList.isEmpty()) {
                suggestionsListView.setVisibility(GONE);
                noSuggestionsView.setVisibility(VISIBLE);
            }
            else {
                noSuggestionsView.setVisibility(GONE);
                suggestionsListView.setVisibility(VISIBLE);
            }

            progressView.setVisibility(GONE);
        }
    }

    /**
     * Checks whether the given podcast matches the filter selection.
     * 
     * @param suggestion Podcast to check.
     * @return <code>true</code> if the podcast fits.
     */
    private boolean matchesFilter(Podcast suggestion) {
        return (languageFilter.getSelectedItemPosition() == 0 ||
                ((Language) languageFilter.getSelectedItem()).equals(suggestion.getLanguage())) &&
                (genreFilter.getSelectedItemPosition() == 0 ||
                ((Genre) genreFilter.getSelectedItem()).equals(suggestion.getGenre())) &&
                (mediaTypeFilter.getSelectedItemPosition() == 0 ||
                ((MediaType) mediaTypeFilter.getSelectedItem()).equals(suggestion.getMediaType()));
    }
}
