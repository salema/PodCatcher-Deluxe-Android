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

import static net.alliknow.podcatcher.BaseActivity.PODCAST_POSITION_LIST_KEY;

import android.content.Intent;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.AbsListView.MultiChoiceModeListener;

import net.alliknow.podcatcher.ExportOpmlActivity;
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
        update(mode);

        return true;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        // Get the checked positions
        SparseBooleanArray checkedItems = fragment.getListView().getCheckedItemPositions();
        ArrayList<Integer> positions = new ArrayList<Integer>();

        // Prepare list of podcast positions to send to the triggered activity
        for (int index = 0; index < fragment.getListView().getCount(); index++)
            if (checkedItems.get(index))
                positions.add(index);

        switch (item.getItemId()) {
            case R.id.podcast_remove_contextmenuitem:
                // Prepare deletion activity
                Intent remove = new Intent(fragment.getActivity(), RemovePodcastActivity.class);
                remove.putIntegerArrayListExtra(PODCAST_POSITION_LIST_KEY, positions);

                // Go remove podcasts
                fragment.startActivity(remove);

                // Action picked, so close the CAB
                mode.finish();
                return true;
            case R.id.opml_export_contextmenuitem:
                // Prepare export activity
                Intent export = new Intent(fragment.getActivity(), ExportOpmlActivity.class);
                export.putIntegerArrayListExtra(PODCAST_POSITION_LIST_KEY, positions);

                // Go export podcasts
                fragment.startActivity(export);

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
        update(mode);
    }

    private void update(ActionMode mode) {
        // Let list adapter know which items to mark checked (row color)
        ((PodcastListAdapter) fragment.getListAdapter()).setCheckedPositions(
                fragment.getListView().getCheckedItemPositions());

        // Update the mode title text
        final int checkedItemCount = fragment.getListView().getCheckedItemCount();
        mode.setTitle(fragment.getResources()
                .getQuantityString(R.plurals.podcasts, checkedItemCount, checkedItemCount));
    }
}
