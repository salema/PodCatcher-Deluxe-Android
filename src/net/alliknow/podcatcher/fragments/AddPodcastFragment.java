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

import net.alliknow.podcatcher.Podcatcher;
import net.alliknow.podcatcher.R;
import net.alliknow.podcatcher.listeners.OnAddPodcastListener;
import net.alliknow.podcatcher.listeners.OnLoadPodcastListener;
import net.alliknow.podcatcher.tasks.LoadPodcastTask;
import net.alliknow.podcatcher.tasks.Progress;
import net.alliknow.podcatcher.types.Podcast;
import net.alliknow.podcatcher.views.HorizontalProgressView;
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
	private HorizontalProgressView progressView;
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
		if (Podcatcher.isInDebugMode(getActivity()))
			podcastUrlEditText.setText("richeisen.libsyn.com/rss");
		
		progressView = (HorizontalProgressView) view.findViewById(R.id.add_podcast_progress);
		
		showSuggestionsButton = (Button) view.findViewById(R.id.add_suggestions_button);
		showSuggestionsButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				dismiss();
				
				OnAddPodcastListener listener = (OnAddPodcastListener) getTargetFragment();
				
				// We need the listener to exist
				if (listener == null) Log.w(getClass().getSimpleName(), "Suggestions requested, but no listener attached");
				// We need the listener to provide the required interface
				else if (! (listener instanceof OnAddPodcastListener))
					Log.w(getClass().getSimpleName(), "Suggestions requested, but target fragment does not implement OnAddPodcastListener");
				else listener.showSuggestions();
			}
		});
		
		addPodcastButton = (Button) view.findViewById(R.id.add_podcast_button);
		addPodcastButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				addPodcast();
			}
		});

		// This checks for a potential podcast URL in the clipboard
		// and presets the text field if available
		checkClipboardForPodcastUrl();
	}
	
	@Override
	public void onDismiss(DialogInterface dialog) {
		super.onDismiss(dialog);
		
		if (loadTask != null) loadTask.cancel(true);
	}
	
	private void addPodcast() {
		// Prepare UI
		podcastUrlEditText.setEnabled(false);
		addPodcastButton.setEnabled(false);
		progressView.setVisibility(View.VISIBLE);
		
		// Try to make the input work as a online resource
		String spec = podcastUrlEditText.getText().toString();
		if (! URLUtil.isNetworkUrl(spec)) {
			spec = "http://" + spec;
			podcastUrlEditText.setText(spec);
		}
		
		// Try to load the given online resource
		try {
			loadTask = new LoadPodcastTask(this);
			loadTask.preventZippedTranfer(true);
			loadTask.execute(new Podcast(null, new URL(spec)));
		} catch (MalformedURLException e) {
			onPodcastLoadFailed(null);
		}	
	}
	
	@Override
	public void onPodcastLoadProgress(Podcast podcast, Progress progress) {
		progressView.publishProgress(progress);
	}

	@Override
	public void onPodcastLoaded(Podcast podcast) {
		// Get call back
		OnAddPodcastListener listener = (OnAddPodcastListener) getTargetFragment();
		
		// We do not allow empty podcast to be added (TODO Does this make sense?)
		if (podcast.getEpisodes().isEmpty()) onPodcastLoadFailed(podcast);
		// We need the target fragment to function as our call back
		else if (listener == null || !(listener instanceof OnAddPodcastListener)) {
			Log.w(getClass().getSimpleName(), "Podcast okay, but target fragment is absent or does not implement OnAddPodcastListener");
			onPodcastLoadFailed(podcast);
		} // This is an actual podcast, add it
		else {
			dismiss();
						
			listener.addPodcast(podcast);
		}
	}

	@Override
	public void onPodcastLoadFailed(Podcast podcast) {
		// Show error in the UI
		progressView.showError(R.string.error_podcast_add);
		podcastUrlEditText.setEnabled(true);
		addPodcastButton.setEnabled(true);
	}
	
	private void checkClipboardForPodcastUrl() {
		ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
		
		// Get the value to paste (make this failsafe)
		if (clipboard != null && clipboard.hasPrimaryClip() && clipboard.getPrimaryClip().getItemCount() > 0) {
			CharSequence candidate = clipboard.getPrimaryClip().getItemAt(0).getText();
			
			// Check whether this might be a podcast RSS online resource, if so set text field
			if (URLUtil.isNetworkUrl(candidate.toString())) podcastUrlEditText.setText(candidate);
		}
	}
}
