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

import net.alliknow.podcatcher.fragments.EpisodeFragment;
import android.os.Bundle;

/**
 * 
 */
public class ShowEpisodeActivity extends PodcatcherBaseActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// Check if we need this activity at all
		if (viewMode != SMALL_PORTRAIT_VIEW) {
			finish();
		} else if (savedInstanceState == null) {
            // During initial setup, plug in the details fragment.
            EpisodeFragment episode = new EpisodeFragment();
            // episodeList.setArguments(getIntent().getExtras());
            getFragmentManager().beginTransaction().add(android.R.id.content, episode, episodeFragmentTag).commit();
        }
	}
}
