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

package net.alliknow.podcatcher.listeners;

import android.content.Intent;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.AbsListView.MultiChoiceModeListener;

import net.alliknow.podcatcher.R;
import net.alliknow.podcatcher.RemovePodcastActivity;
import net.alliknow.podcatcher.view.adapters.PodcastListAdapter;
import net.alliknow.podcatcher.view.fragments.PodcastListFragment;

import java.util.ArrayList;

/**
 * Listener for the podcast list context mode.
 */
public class PodcastListContextListener implements MultiChoiceModeListener {

    /** The owning fragment */
    private PodcastListFragment fragment;

    /**
     * Create new listener for the podcast list context mode.
     * 
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
        setTitle(mode);

        return true;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        switch (item.getItemId()) {
            case R.id.podcast_remove_contextmenuitem:
                // Get the checked positions
                SparseBooleanArray checkedItems = fragment.getListView().getCheckedItemPositions();
                ArrayList<Integer> positions = new ArrayList<Integer>();

                // Prepare list of podcast positions to remove
                for (int index = 0; index < fragment.getListView().getCount(); index++)
                    if (checkedItems.get(index))
                        positions.add(index);

                // Prepare deletion activity
                Intent intent = new Intent(fragment.getActivity(), RemovePodcastActivity.class);
                intent.putIntegerArrayListExtra(RemovePodcastActivity.PODCAST_POSITION_LIST_KEY,
                        positions);

                // Go remove podcasts
                fragment.startActivity(intent);

                // Action picked, so close the CAB
                mode.finish();
                return true;
            default:
                return false;
        }
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        ((PodcastListAdapter) fragment.getListAdapter()).setCheckedPositions(null);
    }

    @Override
    public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
        // Let list adapter know which items to mark checked
        ((PodcastListAdapter) fragment.getListAdapter()).setCheckedPositions(
                fragment.getListView().getCheckedItemPositions());

        setTitle(mode);
    }

    private void setTitle(ActionMode mode) {
        int checkedItemCount = fragment.getListView().getCheckedItemCount();
        mode.setTitle(checkedItemCount == 1 ?
                fragment.getResources().getString(R.string.one_podcast_selected) :
                checkedItemCount + " "
                        + fragment.getResources().getString(R.string.podcasts_selected));
    }
}
