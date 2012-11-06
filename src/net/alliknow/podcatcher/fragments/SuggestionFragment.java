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
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
	/** The send a suggestion view */
	private TextView sendSuggestionView;
	
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
		getDialog().setTitle("Suggested Podcasts");
		
		languageFilter = (Spinner) getView().findViewById(R.id.select_language);
		languageFilter.setAdapter(new LanguageSpinnerAdapter(getActivity()));
		languageFilter.setSelection(0);
		
		genreFilter = (Spinner) getView().findViewById(R.id.select_genre);
		genreFilter.setAdapter(new GenreSpinnerAdapter(getActivity()));
		genreFilter.setSelection(0);
		
		mediaTypeFilter = (Spinner) getView().findViewById(R.id.select_type);
		mediaTypeFilter.setAdapter(new MediaTypeSpinnerAdapter(getActivity()));
		mediaTypeFilter.setSelection(0);
		
		progressView = getView().findViewById(R.id.suggestion_list_progress);
		progressBar = (ProgressBar) getView().findViewById(R.id.suggestion_list_progress_bar);
		progressTextView = (TextView) getView().findViewById(R.id.suggestion_list_progress_text);
		
		suggestionsListView = (ListView) view.findViewById(R.id.suggested_podcasts);
		
		sendSuggestionView = (TextView) view.findViewById(R.id.send_suggestion);
		sendSuggestionView.setText(Html.fromHtml("<a href=\"mailto:" + SUGGESTION_MAIL_ADDRESS +
				"?subject=" + SUGGESTION_MAIL_SUBJECT + "\">" +
				getResources().getString(R.string.send_suggestion) + "</a>"));
		sendSuggestionView.setMovementMethod(LinkMovementMethod.getInstance());
		
		if (suggestionList == null || suggestionList.isEmpty()) new LoadSuggestionsTask(this).execute((Void)null);
		else {
			suggestionsListView.setAdapter(new SuggestionListAdapter(getActivity(), suggestionList, listener));
			
			progressView.setVisibility(View.GONE);
			suggestionsListView.setVisibility(View.VISIBLE);
		}
	}
	
	@Override
	public void onDismiss(DialogInterface dialog) {
		super.onDismiss(dialog);
		
		if (loadTask != null) loadTask.cancel(true);
	}
	
	/**
	 * Add a listener to be notified if a new podcast was selected and loaded.
	 * Overwrites any current listener.
	 * @param listener The listener
	 */
	public void setAddPodcastListener(OnAddPodcastListener listener) {
		this.listener = listener;
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
		suggestionsListView.setAdapter(new SuggestionListAdapter(getActivity(), suggestionList, listener));
		
		progressView.setVisibility(View.GONE);
		suggestionsListView.setVisibility(View.VISIBLE);
	}

	@Override
	public void onSuggestionsLoadFailed() {
		progressBar.setVisibility(View.GONE);
		
		progressTextView.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
		progressTextView.setText("Cannot load suggestions!");
	}
}
