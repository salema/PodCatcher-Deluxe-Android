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

import android.app.ListFragment;

/**
 * Generic list fragment subclass for podcatcher list fragments.
 * Defines some helpers.
 */
public abstract class PodcatcherListFragment extends ListFragment {

	/**
	 * Smoothly scroll to given position if not currently visible.
	 * @param position Position to scroll to.
	 */
	protected void scrollListView(int position) {
		if (getListView().getFirstVisiblePosition() > position || getListView().getLastVisiblePosition() < position)
				getListView().smoothScrollToPosition(position);
	}
}
