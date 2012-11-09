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
import net.alliknow.podcatcher.listeners.OnAddPodcastListener;
import net.alliknow.podcatcher.listeners.OnLoadSuggestionListener;
import net.alliknow.podcatcher.tasks.LoadPodcastTask;
import net.alliknow.podcatcher.tasks.LoadSuggestionsTask;
import net.alliknow.podcatcher.types.Genre;
import net.alliknow.podcatcher.types.Language;
import net.alliknow.podcatcher.types.MediaType;
import net.alliknow.podcatcher.types.Podcast;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ListView;
import android.widget.ProgressBar;
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
	private static final String SUGGESTION_MAIL_SUBJECT = "A proposal for a podcast suggestion in the PodCatcher apps";
	
	/** The add podcast listener */
	private OnAddPodcastListener listener;
	/** The suggestions load task */
	private LoadSuggestionsTask loadTask;
	/** The list of suggestions */
	private PodcastList suggestionList;
	/** The list of podcasts already added */
	private PodcastList podcastList;
	
	/** The language filter */
	private Spinner languageFilter;
	/** The genre filter */
	private Spinner genreFilter;
	/** The media type filter */
	private Spinner mediaTypeFilter;
	/** The progress view */
	private View progressView;
	/** The progress bar */
	private ProgressBar progressBar;
	/** The progress bar text */
	private TextView progressTextView;
	/** The suggestions list view */
	private ListView suggestionsListView;
	/** The no suggestions view */
	private TextView noSuggestionsView;
	/** The send a suggestion view */
	private TextView sendSuggestionView;
	
	/** Caches for filter selection */
	private int languageFilterSelection;
	private int genreFilterSelection;
	private int mediaTypeFilterSelection;
	
	/** The listener to update the list on filter change */
	private OnItemSelectedListener selectionListener = new OnItemSelectedListener() {

		@Override
		public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
			updateList();
			storeFilterSelection();
		}

		@Override
		public void onNothingSelected(AdapterView<?> parent) {
			updateList();
			storeFilterSelection();
		}
	};
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setRetainInstance(true);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		
		return inflater.inflate(R.layout.suggestions, container, false);
	}
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		initUi(view);
		
		// Suggestion list has not been loaded before
		if (suggestionList == null || suggestionList.isEmpty()) {
			setInitialFilterSelection();
			
			loadTask = new LoadSuggestionsTask(this);
			loadTask.execute((Void)null);
		} // List was loaded before
		else {
			restoreFilterSelection();
			updateList();
			
			progressView.setVisibility(View.GONE);
			suggestionsListView.setVisibility(View.VISIBLE);
		}
	}
	
	@Override
	public void onDismiss(DialogInterface dialog) {
		super.onDismiss(dialog);
		
		if (loadTask != null) loadTask.cancel(true);
	}
	
	@Override
	public void onDestroyView() {
		// This is a work around to prevent to dialog
		// from being dismissed on configuration change
		if (getDialog() != null && getRetainInstance())
			getDialog().setDismissMessage(null);
		
		super.onDestroyView();
	}
	
	/**
	 * Add a listener to be notified if a new podcast was selected and loaded.
	 * Overwrites any current listener.
	 * @param listener The listener
	 */
	public void setAddPodcastListener(OnAddPodcastListener listener) {
		this.listener = listener;
	}
	
	/**
	 * Set the list of currently already listed podcasts.
	 * These will be filtered from the suggestions.
	 * @param currentList The list of podcasts.
	 */
	public void setCurrentPodcasts(PodcastList currentList) {
		this.podcastList = currentList;
	}
	
	@Override
	public void onSuggestionsLoadProgress(int progress) {
		if (progress == LoadPodcastTask.PROGRESS_CONNECT) 
			progressTextView.setText(getResources().getString(R.string.connect));
		else if (progress == LoadPodcastTask.PROGRESS_LOAD)
			progressTextView.setText(getResources().getString(R.string.load));
		else if (progress >= 0 && progress <= 100) progressTextView.setText(progress + "%");
		else if (progress == LoadPodcastTask.PROGRESS_PARSE)
			progressTextView.setText(getResources().getString(R.string.parse));
		else progressTextView.setText(getResources().getString(R.string.load));
	}

	@Override
	public void onSuggestionsLoaded(PodcastList suggestions) {
		suggestionList = suggestions;
		updateList();
				
		progressView.setVisibility(View.GONE);
		suggestionsListView.setVisibility(View.VISIBLE);
	}

	@Override
	public void onSuggestionsLoadFailed() {
		progressBar.setVisibility(View.GONE);
		
		progressTextView.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
		progressTextView.setText(R.string.error_suggestions_load);
	}
	
	private void setInitialFilterSelection() {
		Locale current = getActivity().getResources().getConfiguration().locale;
		languageFilterSelection = current.getLanguage().equalsIgnoreCase("de") ? 2 : 1;
		
		genreFilterSelection = 0;
		mediaTypeFilterSelection = 1;
		
		restoreFilterSelection();
	}
	
	private void updateList() {
		if (suggestionList != null && !suggestionList.isEmpty()) {
			PodcastList filteredSuggestionList = new PodcastList();
			
			for (Podcast suggestion : suggestionList)
				if (podcastList == null || !podcastList.contains(suggestion) &&
						matchesFilter(suggestion)) filteredSuggestionList.add(suggestion);
			
			suggestionsListView.setAdapter(new SuggestionListAdapter(getActivity(), filteredSuggestionList, listener));
			
			if (filteredSuggestionList.isEmpty()) {
				suggestionsListView.setVisibility(View.GONE);
				noSuggestionsView.setVisibility(View.VISIBLE);
			} else {
				noSuggestionsView.setVisibility(View.GONE);
				suggestionsListView.setVisibility(View.VISIBLE);
			}	
		}
	}

	private boolean matchesFilter(Podcast suggestion) {
		return (languageFilter.getSelectedItemPosition() == 0 || 
			((Language)languageFilter.getSelectedItem()).equals(suggestion.getLanguage())) &&
			(genreFilter.getSelectedItemPosition() == 0 || 
			((Genre)genreFilter.getSelectedItem()).equals(suggestion.getGenre())) &&
			(mediaTypeFilter.getSelectedItemPosition() == 0 || 
			((MediaType)mediaTypeFilter.getSelectedItem()).equals(suggestion.getMediaType()));
	}
	
	private void storeFilterSelection() {
		languageFilterSelection = languageFilter.getSelectedItemPosition();
		genreFilterSelection = genreFilter.getSelectedItemPosition();
		mediaTypeFilterSelection = mediaTypeFilter.getSelectedItemPosition();
	}
	
	private void restoreFilterSelection() {
		languageFilter.setSelection(languageFilterSelection);
		genreFilter.setSelection(genreFilterSelection);
		mediaTypeFilter.setSelection(mediaTypeFilterSelection);
	}
		
	private void initUi(View view) {
		getDialog().setTitle(R.string.suggested_podcasts);
				
		languageFilter = (Spinner) getView().findViewById(R.id.select_language);
		languageFilter.setAdapter(new LanguageSpinnerAdapter(getActivity()));
		languageFilter.setOnItemSelectedListener(selectionListener);
		
		genreFilter = (Spinner) getView().findViewById(R.id.select_genre);
		genreFilter.setAdapter(new GenreSpinnerAdapter(getActivity()));
		genreFilter.setOnItemSelectedListener(selectionListener);
		
		mediaTypeFilter = (Spinner) getView().findViewById(R.id.select_type);
		mediaTypeFilter.setAdapter(new MediaTypeSpinnerAdapter(getActivity()));
		mediaTypeFilter.setOnItemSelectedListener(selectionListener);
		
		progressView = getView().findViewById(R.id.suggestion_list_progress);
		progressBar = (ProgressBar) getView().findViewById(R.id.suggestion_list_progress_bar);
		progressTextView = (TextView) getView().findViewById(R.id.suggestion_list_progress_text);
		progressTextView.setTextColor(getResources().getColor(android.R.color.black));
		progressTextView.setText(null);
		
		suggestionsListView = (ListView) view.findViewById(R.id.suggested_podcasts);
		noSuggestionsView = (TextView) view.findViewById(R.id.no_suggestions);
		
		sendSuggestionView = (TextView) view.findViewById(R.id.send_suggestion);
		sendSuggestionView.setText(Html.fromHtml("<a href=\"mailto:" + SUGGESTION_MAIL_ADDRESS +
				"?subject=" + SUGGESTION_MAIL_SUBJECT + "\">" +
				getResources().getString(R.string.send_suggestion) + "</a>"));
		sendSuggestionView.setMovementMethod(LinkMovementMethod.getInstance());
	}
}
