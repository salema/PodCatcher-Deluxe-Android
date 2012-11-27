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
package net.alliknow.podcatcher.listeners;

import net.alliknow.podcatcher.R;
import net.alliknow.podcatcher.adapters.PodcastListAdapter;
import net.alliknow.podcatcher.fragments.PodcastListFragment;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.AbsListView.MultiChoiceModeListener;

/**
 * Listener for the podcast list context mode.
 */
public class PodcastListContextListener implements MultiChoiceModeListener {

	/** The owning fragment */
	private PodcastListFragment fragment;
	
	/**
	 * Create new listener for the podcast list context mode.
	 * @param fragment The podcast list fragment to call back to.
	 */
	public PodcastListContextListener(PodcastListFragment fragment) {
		this.fragment = fragment;
	}
	
	@Override
	public boolean onCreateActionMode(ActionMode mode, Menu menu) {
		MenuInflater inflater = mode.getMenuInflater();
		inflater.inflate(R.menu.podcast_list_context, menu);
        
		return true;
	}

	@Override
	public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
		return false;
	}

	@Override
	public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
		switch (item.getItemId()) {
        	case R.id.podcast_remove_contextmenuitem:
        		fragment.removeCheckedPodcasts();
        		mode.finish(); // Action picked, so close the CAB
        		return true;
        	default:
        		return false;
		}
	}

	@Override
	public void onDestroyActionMode(ActionMode mode) {}

	@Override
	public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
		// Let list adapter know which items to mark checked
		((PodcastListAdapter) fragment.getListAdapter()).setCheckedPositions(
			fragment.getListView().getCheckedItemPositions());
		
		// Set the context menu title
		int checkedItemCount = fragment.getListView().getCheckedItemCount();
		mode.setTitle(checkedItemCount == 1 ? fragment.getResources().getString(R.string.one_podcast_selected) :
			checkedItemCount + " " + fragment.getResources().getString(R.string.podcasts_selected));
	}
}
