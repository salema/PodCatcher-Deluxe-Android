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
import android.widget.ProgressBar;
import android.widget.TextView;

/**
 * A sophisticated horizontal progress view.
 *
 * @author Kevin Hausmann
 */
public class HorizontalProgressView extends ProgressView {
	
	public HorizontalProgressView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public HorizontalProgressView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public HorizontalProgressView(Context context) {
		this(context, null);
	}
	
	@Override
	protected void inflate(Context context) {
		View view = View.inflate(context, R.layout.progress_horizontal, this);
		
		progressBar = (ProgressBar) view.findViewById(R.id.progress_bar);
		progressTextView = (TextView) view.findViewById(R.id.progress_text);
	}
	
	@Override
	public void publishProgress(Progress progress) {
		super.publishProgress(progress);
		
		if (progress.getPercentDone() >= 0 && progress.getPercentDone() <= 100) {
			progressBar.setIndeterminate(false);
			progressBar.setProgress(progress.getPercentDone());
		} else progressBar.setIndeterminate(true);
	}
}
