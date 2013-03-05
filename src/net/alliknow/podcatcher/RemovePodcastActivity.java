/** Copyright 2012, 2013 Kevin Hausmann
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

import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

/**
 * Non-UI activity to remove podcasts.
 */
public class RemovePodcastActivity extends BaseActivity {

    /** Key to find the podcast positions under */
    public static final String PODCAST_POSITION_LIST_KEY = "position_list_key";

    @Override
    protected void onStart() {
        super.onStart();

        // Get the list of positions to remove
        List<Integer> positions = getIntent().getIntegerArrayListExtra(PODCAST_POSITION_LIST_KEY);

        // If list is there, remove podcasts at given positions
        if (positions != null) {
            // Make sure positions are ordered lowest to highest
            Collections.sort(positions);

            // We need to iterate backwards, so positions are not screwed up
            ListIterator<Integer> li = positions.listIterator(positions.size());

            // Remove podcasts starting with the last one
            while (li.hasPrevious())
                podcastManager.remove(li.previous());
        }

        // Make sure we stop here
        finish();
    }
}
