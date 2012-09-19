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
import net.alliknow.podcatcher.tasks.LoadPodcastTask;
import net.alliknow.podcatcher.tasks.LoadPodcastTask.PodcastLoader;
import net.alliknow.podcatcher.types.Podcast;
import android.app.DialogFragment;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.URLUtil;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

/**
 * A dialog to let the user add a podcast.
 * 
 * @author Kevin Hausmann
 */
public class AddPodcastFragment extends DialogFragment implements PodcastLoader {

	/**
     * Interface definition for a callback to be invoked when a podcast is added.
     */
	public interface AddPodcastListener {
		
		/**
		 * Called on listener when podcast is added.
		 * @param newPodcast Podcast to add.
		 */
		void addPodcast(Podcast newPodcast);
	}

	/** The add podcast listener */
	private AddPodcastListener listener;
	
	/** The podcast URL text field */
	private EditText podcastUrlEditText;
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
		
		podcastUrlEditText = (EditText)view.findViewById(R.id.podcast_url);
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
		
		errorView = (TextView)view.findViewById(R.id.add_podcast_error);
		
		addPodcastButton = (Button)view.findViewById(R.id.add_podcast_button);
		addPodcastButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				addPodcast();
			}
		});
	
		checkClipboardForPodcastUrl();
		updateButtonEnablement();
	}
	
	public void setAddPodcastListener(AddPodcastListener listener) {
		this.listener = listener;
	}
	
	private void addPodcast() {
		addPodcastButton.setEnabled(false);
		errorView.setVisibility(View.GONE);
		
		String spec = podcastUrlEditText.getText().toString();
		
		try {
			new LoadPodcastTask(this).execute(new Podcast(null, new URL(spec)));
		} catch (MalformedURLException e) {
			onPodcastLoadFailed(null);
		}	
	}

	@Override
	public void onPodcastLoaded(Podcast podcast) {
		listener.addPodcast(podcast);
		
		dismiss();
		errorView.setVisibility(View.GONE);
		podcastUrlEditText.setText(null);
		updateButtonEnablement();
	}

	@Override
	public void onPodcastLoadFailed(Podcast podcast) {
		errorView.setVisibility(View.VISIBLE);
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
		return URLUtil.isNetworkUrl(candidate.toString());
	}
}
