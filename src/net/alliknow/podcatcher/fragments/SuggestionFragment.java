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
package net.alliknow.podcatcher.fragments;

import java.util.Locale;

import net.alliknow.podcatcher.PodcastList;
import net.alliknow.podcatcher.R;
import net.alliknow.podcatcher.adapters.GenreSpinnerAdapter;
import net.alliknow.podcatcher.adapters.LanguageSpinnerAdapter;
import net.alliknow.podcatcher.adapters.MediaTypeSpinnerAdapter;
import net.alliknow.podcatcher.adapters.SuggestionListAdapter;
import net.alliknow.podcatcher.listeners.OnLoadSuggestionListener;
import net.alliknow.podcatcher.listeners.OnShowSuggestionsListener;
import net.alliknow.podcatcher.tasks.LoadSuggestionsTask;
import net.alliknow.podcatcher.types.Genre;
import net.alliknow.podcatcher.types.Language;
import net.alliknow.podcatcher.types.MediaType;
import net.alliknow.podcatcher.types.Podcast;
import net.alliknow.podcatcher.views.ProgressView;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

/**
 * Fragment to show podcast suggestions.
 * 
 * @author Kevin Hausmann
 */
public class SuggestionFragment extends DialogFragment implements OnLoadSuggestionListener {

	/** The filter wildcard */
	public static final String FILTER_WILDCARD = "ALL";
	
	/** Mail address to send new suggestions to */
	private static final String SUGGESTION_MAIL_ADDRESS = "suggestion@podcatcher-deluxe.com";
	/** Subject for mail with new suggestions */
	private static final String SUGGESTION_MAIL_SUBJECT = "A proposal for a podcast suggestion in the Podcatcher apps";
	
	/** The call back we work on */
	private OnShowSuggestionsListener listener;
	/** The suggestions load task */
	private LoadSuggestionsTask loadTask;
	
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
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		
		return inflater.inflate(R.layout.suggestions, container, false);
	}
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		initUi(view);
		restoreFilters(savedInstanceState);
		
		// No listener
		if (! assureListenerPresent()) onSuggestionsLoadFailed();
		// Suggestion list has not been loaded before
		else if (listener.getPodcastSuggestions() == null) {
			loadTask = new LoadSuggestionsTask(this);
			loadTask.execute((Void)null);
		} // List was loaded before
		else {
			updateList();
		}
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		
		outState.putInt(Language.class.getSimpleName(), languageFilter.getSelectedItemPosition());
		outState.putInt(Genre.class.getSimpleName(), genreFilter.getSelectedItemPosition());
		outState.putInt(MediaType.class.getSimpleName(), mediaTypeFilter.getSelectedItemPosition());
	}
	
	@Override
	public void onDismiss(DialogInterface dialog) {
		super.onDismiss(dialog);
		
		if (loadTask != null) loadTask.cancel(true);
	}
	
	@Override
	public void onSuggestionsLoadProgress(int progress) {
		progressView.publishProgress(progress);
	}

	@Override
	public void onSuggestionsLoaded(PodcastList suggestions) {
		// Cache the suggestions list in the podcast list fragment which will be retained
		listener.setPodcastSuggestions(suggestions);
		
		// Filter list and update UI
		updateList();
	}

	@Override
	public void onSuggestionsLoadFailed() {
		progressView.showError(R.string.error_suggestions_load);
	}
	
	/**
	 * Make sure we have a traget fragment that implement the right call back.
	 * @return <code>true</code> if so.
	 */
	private boolean assureListenerPresent() {
		this.listener = (OnShowSuggestionsListener) getTargetFragment();
		// We need the target fragment to provide the required interface 
		if (listener == null || !(listener instanceof OnShowSuggestionsListener)) {
			Log.w(getClass().getSimpleName(), "Suggestion dialog cannot open, target fragment is null or does not implement OnShowSuggestionsListener");
			return false;
		} else return true;		
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
	
	private void restoreFilters(Bundle savedInstanceState) {
		if (savedInstanceState != null) {
			languageFilter.setSelection(savedInstanceState.getInt(Language.class.getSimpleName()));
			genreFilter.setSelection(savedInstanceState.getInt(Genre.class.getSimpleName()));
			mediaTypeFilter.setSelection(savedInstanceState.getInt(MediaType.class.getSimpleName()));
		} else setInitialFilterSelection();
	}

	private void updateList() {
		PodcastList suggestionList = listener.getPodcastSuggestions();
		// Filter the suggestion list
		if (suggestionList != null) {
			// Currently already  existing podcasts (to be filtered out)
			PodcastList podcastList = listener.getPodcastList();
			// Resulting list
			PodcastList filteredSuggestionList = new PodcastList();
			// Do filter!
			for (Podcast suggestion : suggestionList)
				if (podcastList == null || !podcastList.contains(suggestion) &&
						matchesFilter(suggestion)) filteredSuggestionList.add(suggestion);
			
			// Set filtered list
			suggestionsListView.setAdapter(new SuggestionListAdapter(getActivity(), filteredSuggestionList, listener));
			// Update UI
			if (filteredSuggestionList.isEmpty()) {
				suggestionsListView.setVisibility(View.GONE);
				noSuggestionsView.setVisibility(View.VISIBLE);
			}
			else {
				noSuggestionsView.setVisibility(View.GONE);
				suggestionsListView.setVisibility(View.VISIBLE);
			}
			
			progressView.setVisibility(View.GONE);
		// Just in case the suggestion list has not been cached...
		} // else onSuggestionsLoadFailed();
	}

	/**
	 * Checks whether the given podcast matches the filter selection
	 * @param suggestion Podcast to check
	 * @return <code>true</code> if the podcast fits
	 */
	private boolean matchesFilter(Podcast suggestion) {
		return (languageFilter.getSelectedItemPosition() == 0 || 
			((Language)languageFilter.getSelectedItem()).equals(suggestion.getLanguage())) &&
			(genreFilter.getSelectedItemPosition() == 0 || 
			((Genre)genreFilter.getSelectedItem()).equals(suggestion.getGenre())) &&
			(mediaTypeFilter.getSelectedItemPosition() == 0 || 
			((MediaType)mediaTypeFilter.getSelectedItem()).equals(suggestion.getMediaType()));
	}
	
	private void initUi(View view) {
		getDialog().setTitle(R.string.suggested_podcasts);
				
		languageFilter = (Spinner) view.findViewById(R.id.select_language);
		languageFilter.setAdapter(new LanguageSpinnerAdapter(getActivity()));
		languageFilter.setOnItemSelectedListener(selectionListener);
		
		genreFilter = (Spinner) view.findViewById(R.id.select_genre);
		genreFilter.setAdapter(new GenreSpinnerAdapter(getActivity()));
		genreFilter.setOnItemSelectedListener(selectionListener);
		
		mediaTypeFilter = (Spinner) view.findViewById(R.id.select_type);
		mediaTypeFilter.setAdapter(new MediaTypeSpinnerAdapter(getActivity()));
		mediaTypeFilter.setOnItemSelectedListener(selectionListener);
		
		progressView = (ProgressView) view.findViewById(R.id.suggestion_list_progress);
		
		suggestionsListView = (ListView) view.findViewById(R.id.suggested_podcasts);
		noSuggestionsView = (TextView) view.findViewById(R.id.no_suggestions);
		
		sendSuggestionView = (TextView) view.findViewById(R.id.send_suggestion);
		sendSuggestionView.setText(Html.fromHtml("<a href=\"mailto:" + SUGGESTION_MAIL_ADDRESS +
				"?subject=" + SUGGESTION_MAIL_SUBJECT + "\">" +
				getResources().getString(R.string.send_suggestion) + "</a>"));
		sendSuggestionView.setMovementMethod(LinkMovementMethod.getInstance());
	}
}
