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
import net.alliknow.podcatcher.tasks.Progress;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

/**
 * A sophisticated progress view.
 *
 * @author Kevin Hausmann
 */
public class ProgressView extends LinearLayout {
	
	/** The progress bar */
	protected ProgressBar progressBar;
	/** The progress bar text */
	protected TextView progressTextView;
	
	public ProgressView(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		inflate(context);
	}
	
	/**
	 * Inflate the view's layout, override to change in subclass.
	 * @param context Context view lives in.
	 */
	protected void inflate(Context context) {
		View view = View.inflate(context, R.layout.progress, this);
		
		progressBar = (ProgressBar) view.findViewById(R.id.progress_bar);
		progressTextView = (TextView) view.findViewById(R.id.progress_text);
	}
		
	/**
	 * Show a textual progress information. Beyond actual
	 * percentages this also works with flags from load tasks.
	 * @param progress Progress to visualize.
	 * @see <code>Progress</code>
	 */
	public void publishProgress(Progress progress) {
		progressBar.setVisibility(View.VISIBLE);
		progressTextView.setTextColor(getResources().getColor(R.color.text_secondary));
		
		if (progress.equals(Progress.CONNECT))
			progressTextView.setText(getResources().getString(R.string.connect));
		else if (progress.equals(Progress.LOAD))
			progressTextView.setText(getResources().getString(R.string.load));
		else if (progress.equals(Progress.PARSE))
			progressTextView.setText(getResources().getString(R.string.parse));
		else if (progress.getPercentDone() >= 0 && progress.getPercentDone() <= 100) 
			progressTextView.setText(progress.getPercentDone() + "%");
		else progressTextView.setText(getResources().getString(R.string.load));
	}
	
	/**
	 * Show an error and abort progress.
	 * @param errorId Resource id for error message.
	 */
	public void showError(int errorId) {
		progressBar.setVisibility(View.GONE);
		
		progressTextView.setTextColor(getResources().getColor(R.color.text_error));
		progressTextView.setText(errorId);
	}

	/**
	 * Reset to initial UI state.
	 */
	public void reset() {
		progressBar.setVisibility(View.VISIBLE);
		progressTextView.setText(null);
		progressTextView.setTextColor(getResources().getColor(R.color.text_secondary));
	}
}
