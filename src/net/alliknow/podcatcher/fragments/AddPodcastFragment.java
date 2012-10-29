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
import net.alliknow.podcatcher.listeners.AddPodcastListener;
import net.alliknow.podcatcher.listeners.PodcastLoadListener;
import net.alliknow.podcatcher.tasks.LoadPodcastTask;
import net.alliknow.podcatcher.types.Podcast;
import android.app.DialogFragment;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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
public class AddPodcastFragment extends DialogFragment implements PodcastLoadListener {

	/** The add podcast listener */
	private AddPodcastListener listener;
	/** The podcast load task */
	private LoadPodcastTask loadTask;
	
	/** The podcast URL text field */
	private EditText podcastUrlEditText;
	/** The progress view */
	private ProgressBar progressView;
	/** The error text view */
	private TextView errorView;
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
		podcastUrlEditText.addTextChangedListener(new TextWatcher() {
			
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				updateButtonEnablement();
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,	int after) {}

			@Override
			public void afterTextChanged(Editable s) {}
		});
		podcastUrlEditText.setOnEditorActionListener(new OnEditorActionListener() {
			
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				boolean handled = false;
		        
				if (actionId == EditorInfo.IME_ACTION_GO) {
		            addPodcast();
		            handled = true;
		        }
		        
		        return handled;
			}
		});
		
		progressView = (ProgressBar) view.findViewById(R.id.add_podcast_progress);
		errorView = (TextView) view.findViewById(R.id.add_podcast_error);
		
		addPodcastButton = (Button) view.findViewById(R.id.add_podcast_button);
		addPodcastButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				addPodcast();
			}
		});
	
		checkClipboardForPodcastUrl();
		updateButtonEnablement();
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
	public void setAddPodcastListener(AddPodcastListener listener) {
		this.listener = listener;
	}
	
	private void addPodcast() {
		podcastUrlEditText.setEnabled(false);
		progressView.setVisibility(View.VISIBLE);
		errorView.setVisibility(View.GONE);
		
		String spec = podcastUrlEditText.getText().toString();
		if (! URLUtil.isNetworkUrl(spec.toString())) {
			spec = "http://" + spec;
			podcastUrlEditText.setText(spec);
		}
		// Need to do this here because text might have changed above
		addPodcastButton.setEnabled(false);
		
		try {
			new LoadPodcastTask(this).execute(new Podcast(null, new URL(spec)));
		} catch (MalformedURLException e) {
			onPodcastLoadFailed(null);
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
	public void onPodcastLoaded(Podcast podcast) {
		if (! podcast.getEpisodes().isEmpty()) {
			dismiss();
			
			if (listener != null) listener.addPodcast(podcast);
			else Log.d(getClass().getSimpleName(), "Podcast loaded, but no listener attached");
			
			errorView.setVisibility(View.GONE);
			progressView.setVisibility(View.GONE);
			podcastUrlEditText.setText(null);
			podcastUrlEditText.setEnabled(true);
			updateButtonEnablement();
		} else onPodcastLoadFailed(podcast);
	}

	@Override
	public void onPodcastLoadFailed(Podcast podcast) {
		progressView.setVisibility(View.GONE);
		errorView.setVisibility(View.VISIBLE);
		podcastUrlEditText.setEnabled(true);
		updateButtonEnablement();
	}
	
	private void updateButtonEnablement() {
		addPodcastButton.setEnabled(isValidPodcastUrl(podcastUrlEditText.getText()));
	}
	
	private void checkClipboardForPodcastUrl() {
		ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
		
		if (clipboard.hasPrimaryClip()) {
			CharSequence candidate = clipboard.getPrimaryClip().getItemAt(0).getText();
			
			if (isValidPodcastUrl(candidate)) podcastUrlEditText.setText(candidate);
		}
	}
	
	private boolean isValidPodcastUrl(CharSequence candidate) {
		return URLUtil.isNetworkUrl(candidate.toString()) ||
				(candidate.length() > 5 && candidate.toString().contains("."));
	}
}
