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
package net.alliknow.podcatcher.views;

import net.alliknow.podcatcher.R;
import net.alliknow.podcatcher.tasks.LoadPodcastTask;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

/**
 *
 * @author Kevin Hausmann
 */
public class ProgressView extends LinearLayout {
	
	/** The progress bar */
	private ProgressBar progressBar;
	/** The progress bar text */
	private TextView progressTextView;
	
	public ProgressView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		
		View view = View.inflate(context, R.layout.progress_view, this);
		progressBar = (ProgressBar) view.findViewById(R.id.progress_bar);
		progressTextView = (TextView) view.findViewById(R.id.progress_text);
	}

	public ProgressView(Context context, AttributeSet attrs) {
		this(context, null, 0);
	}

	public ProgressView(Context context) {
		this(context, null);
	}
	
	public void publishProgress(int progress) {
		if (progress == LoadPodcastTask.PROGRESS_CONNECT) 
			progressTextView.setText(getResources().getString(R.string.connect));
		else if (progress == LoadPodcastTask.PROGRESS_LOAD)
			progressTextView.setText(getResources().getString(R.string.load));
		else if (progress >= 0 && progress <= 100) progressTextView.setText(progress + "%");
		else if (progress == LoadPodcastTask.PROGRESS_PARSE)
			progressTextView.setText(getResources().getString(R.string.parse));
		else progressTextView.setText(getResources().getString(R.string.load));
	}
	
	public void showError(int errorId) {
		progressBar.setVisibility(View.GONE);
		
		progressTextView.setTextColor(getResources().getColor(R.color.text_error));
		progressTextView.setText(errorId);
	}
}
