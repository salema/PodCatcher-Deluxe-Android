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
package net.alliknow.podcatcher;

import net.alliknow.podcatcher.fragments.EpisodeListFragment;
import net.alliknow.podcatcher.listeners.OnSelectEpisodeListener;
import net.alliknow.podcatcher.types.Episode;
import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;

/**
 * @author Kevin Hausmann
 *
 */
public class ShowEpisodeListActivity extends Activity implements OnSelectEpisodeListener {
	private int viewMode;
	
	EpisodeListFragment episodeList;
	/* (non-Javadoc)
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		figureOutViewMode();
		
		if (viewMode > 0) {
			finish();
		} else if (savedInstanceState == null) {
            // During initial setup, plug in the details fragment.
            episodeList = new EpisodeListFragment();
            // episodeList.setArguments(getIntent().getExtras());
            getFragmentManager().beginTransaction().add(android.R.id.content, episodeList).commit();
            
            processIntent();
        }
	}
	
	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		setIntent(intent);
		processIntent();
	}
	
	private void processIntent() {
		Intent intent = getIntent();
		if (intent.getBooleanExtra("progress", false)) episodeList.resetAndSpin();
		if (intent.getBooleanExtra("select",  false)) onEpisodeSelected(null);
	}
	
	private void figureOutViewMode() {
		if (getResources().getConfiguration().smallestScreenWidthDp >= 600) viewMode = 2;
		else if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) viewMode = 0;
		else viewMode = 1;
	}

	/* (non-Javadoc)
	 * @see net.alliknow.podcatcher.listeners.OnSelectEpisodeListener#onEpisodeSelected(net.alliknow.podcatcher.types.Episode)
	 */
	@Override
	public void onEpisodeSelected(Episode selectedEpisode) {
		Intent intent = new Intent();
        intent.setClass(this, ShowEpisodeActivity.class);
        //intent.putExtra("index", index);
        startActivity(intent);
	}

	/* (non-Javadoc)
	 * @see net.alliknow.podcatcher.listeners.OnSelectEpisodeListener#onNoEpisodeSelected()
	 */
	@Override
	public void onNoEpisodeSelected() {
		// TODO Auto-generated method stub
		
	}
}
