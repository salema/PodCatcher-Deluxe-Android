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

import java.net.MalformedURLException;
import java.net.URL;

import net.alliknow.podcatcher.R;
import net.alliknow.podcatcher.listeners.OnAddPodcastListener;
import net.alliknow.podcatcher.listeners.OnLoadPodcastListener;
import net.alliknow.podcatcher.tasks.LoadPodcastTask;
import net.alliknow.podcatcher.types.Podcast;
import android.app.DialogFragment;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.webkit.URLUtil;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

/**
 * A dialog to let the user add a podcast.
 * 
 * @author Kevin Hausmann
 */
public class AddPodcastFragment extends DialogFragment implements OnLoadPodcastListener {

	/** The podcast load task */
	protected LoadPodcastTask loadTask;
	
	/** The podcast URL text field */
	private EditText podcastUrlEditText;
	/** The progress view */
	private ProgressBar progressView;
	/** The error text view */
	private TextView errorView;
	/** The show suggestions button */
	private Button showSuggestionsButton;
	/** The add podcast button */
	private Button addPodcastButton;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		
		return inflater.inflate(R.layout.add_podcast, container, false);
	}
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		getDialog().setTitle(R.string.add_podcast);
		
		podcastUrlEditText = (EditText) view.findViewById(R.id.podcast_url);
		podcastUrlEditText.setOnEditorActionListener(new OnEditorActionListener() {
			
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				switch (actionId) {
			        case EditorInfo.IME_ACTION_GO:
			        	addPodcast();
			            
			   			return true;
			        default:
			            return false;
				}
			}
		});
		
		progressView = (ProgressBar) view.findViewById(R.id.add_podcast_progress);
		errorView = (TextView) view.findViewById(R.id.add_podcast_error);
		
		showSuggestionsButton = (Button) view.findViewById(R.id.add_suggestions_button);
		showSuggestionsButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				dismiss();
				
				OnAddPodcastListener listener = (OnAddPodcastListener) getTargetFragment();
				
				if (listener != null) listener.showSuggestions();
				else Log.d(getClass().getSimpleName(), "Suggestions requested, but no listener attached");
			}
		});
		
		addPodcastButton = (Button) view.findViewById(R.id.add_podcast_button);
		addPodcastButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				addPodcast();
			}
		});
	
		checkClipboardForPodcastUrl();
	}
	
	@Override
	public void onDismiss(DialogInterface dialog) {
		super.onDismiss(dialog);
		
		if (loadTask != null) loadTask.cancel(true);
	}
	
	private void addPodcast() {
		podcastUrlEditText.setEnabled(false);
		addPodcastButton.setEnabled(false);
		progressView.setVisibility(View.VISIBLE);
		errorView.setVisibility(View.GONE);
		
		String spec = podcastUrlEditText.getText().toString();
		if (! URLUtil.isNetworkUrl(spec.toString())) {
			spec = "http://" + spec;
			podcastUrlEditText.setText(spec);
		}
				
		try {
			loadTask = new LoadPodcastTask(this);
			loadTask.execute(new Podcast(null, new URL(spec)));
		} catch (MalformedURLException e) {
			onPodcastLoadFailed(null, false);
		}	
	}
	
	@Override
	public void onPodcastLoadProgress(int percent) {
		if (percent >= 0 && percent <= 100) {
			progressView.setIndeterminate(false);
			progressView.setProgress(percent);
		} else progressView.setIndeterminate(true);
	}

	@Override
	public void onPodcastLoaded(Podcast podcast, boolean wasBackground) {
		if (podcast.getEpisodes().isEmpty()) onPodcastLoadFailed(podcast, wasBackground);
		else {
			dismiss();
			
			OnAddPodcastListener listener = (OnAddPodcastListener) getTargetFragment();
			
			if (listener != null) listener.addPodcast(podcast);
			else Log.d(getClass().getSimpleName(), "Podcast loaded, but no listener attached");
		}
	}

	@Override
	public void onPodcastLoadFailed(Podcast podcast, boolean wasBackground) {
		progressView.setVisibility(View.GONE);
		errorView.setVisibility(View.VISIBLE);
		podcastUrlEditText.setEnabled(true);
		addPodcastButton.setEnabled(true);
	}
	
	private void checkClipboardForPodcastUrl() {
		ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
		
		if (clipboard.hasPrimaryClip()) {
			CharSequence candidate = clipboard.getPrimaryClip().getItemAt(0).getText();
			
			if (URLUtil.isNetworkUrl(candidate.toString())) podcastUrlEditText.setText(candidate);
		}
	}
}
