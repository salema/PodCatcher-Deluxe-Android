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

import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.AbsListView.MultiChoiceModeListener;

import net.alliknow.podcatcher.R;
import net.alliknow.podcatcher.model.EpisodeManager;
import net.alliknow.podcatcher.model.types.Episode;
import net.alliknow.podcatcher.view.adapters.EpisodeListAdapter;
import net.alliknow.podcatcher.view.fragments.EpisodeListFragment;

/**
 * Listener for the episode list context mode.
 */
public class EpisodeListContextListener implements MultiChoiceModeListener {

    /** The owning fragment */
    private EpisodeListFragment fragment;
    /** The episode manager handle */
    private final EpisodeManager episodeManager;

    /** The mark new menu item */
    private MenuItem newMenuItem;
    /** The mark old menu item */
    private MenuItem oldMenuItem;

    /** The download menu item */
    private MenuItem downloadMenuItem;
    /** The delete menu item */
    private MenuItem deleteMenuItem;

    /**
     * Flag to indicate whether the mode should do potentially expensive UI
     * updates when a list item is checked
     */
    private boolean updateUi = true;

    /**
     * Create new listener for the episode list context mode.
     * 
     * @param fragment The episode list fragment to call back to.
     */
    public EpisodeListContextListener(EpisodeListFragment fragment) {
        this.fragment = fragment;

        episodeManager = EpisodeManager.getInstance();
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        MenuInflater inflater = mode.getMenuInflater();
        inflater.inflate(R.menu.episode_list_context, menu);

        newMenuItem = menu.findItem(R.id.episode_new_contextmenuitem);
        oldMenuItem = menu.findItem(R.id.episode_old_contextmenuitem);
        downloadMenuItem = menu.findItem(R.id.episode_download_contextmenuitem);
        deleteMenuItem = menu.findItem(R.id.episode_remove_contextmenuitem);

        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        update(mode);

        return true;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        boolean markNew = false;
        boolean download = false;
        boolean append = false;

        SparseBooleanArray checkedItems = fragment.getListView().getCheckedItemPositions();

        switch (item.getItemId()) {
            case R.id.episode_new_contextmenuitem:
                markNew = true;
                // No break here, code blow should run
            case R.id.episode_old_contextmenuitem:
                for (int position = 0; position < fragment.getListAdapter().getCount(); position++)
                    if (checkedItems.get(position)) {
                        Episode episode = (Episode) fragment.getListAdapter().getItem(position);

                        episodeManager.setState(episode, !markNew);
                    }

                // Action picked, so close the CAB
                mode.finish();
                return true;
            case R.id.episode_download_contextmenuitem:
                download = true;
                // No break here, code blow should run
            case R.id.episode_remove_contextmenuitem:
                for (int position = 0; position < fragment.getListAdapter().getCount(); position++)
                    if (checkedItems.get(position)) {
                        Episode episode = (Episode) fragment.getListAdapter().getItem(position);

                        if (download)
                            episodeManager.download(episode);
                        else
                            episodeManager.deleteDownload(episode);
                    }

                // Action picked, so close the CAB
                mode.finish();
                return true;
            case R.id.episode_select_all_contextmenuitem:
                // Disable expensive UI updates
                updateUi = false;
                for (int index = 0; index < fragment.getListAdapter().getCount(); index++)
                    fragment.getListView().setItemChecked(index, true);

                // Re-enable UI updates
                updateUi = true;
                update(mode);
                return true;
            case R.id.episode_add_to_playlist_contextmenuitem:
                append = true;
                // No break here, code blow should run
            case R.id.episode_remove_from_playlist_contextmenuitem:
                for (int position = 0; position < fragment.getListAdapter().getCount(); position++)
                    if (checkedItems.get(position)) {
                        Episode episode = (Episode) fragment.getListAdapter().getItem(position);

                        if (append)
                            episodeManager.appendToPlaylist(episode);
                        else
                            episodeManager.removeFromPlaylist(episode);
                    }

                // Action picked, so close the CAB
                mode.finish();
                return true;
            default:
                return false;
        }
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        ((EpisodeListAdapter) fragment.getListAdapter()).setCheckedPositions(null);
    }

    @Override
    public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
        update(mode);
    }

    private void update(ActionMode mode) {
        if (updateUi) {
            updateMenuItems();

            // Let list adapter know which items to mark checked (row color)
            ((EpisodeListAdapter) fragment.getListAdapter()).setCheckedPositions(
                    fragment.getListView().getCheckedItemPositions());

            // Update the mode title text
            int checkedItemCount = fragment.getListView().getCheckedItemCount();
            String newTitle = fragment.getString(R.string.no_episode_selected);

            if (checkedItemCount == 1)
                newTitle = fragment.getString(R.string.one_episode);
            else if (checkedItemCount > 1)
                newTitle = checkedItemCount + " "
                        + fragment.getString(R.string.episodes);

            mode.setTitle(newTitle);
        }
    }

    private void updateMenuItems() {
        // Make all menu items invisible
        newMenuItem.setVisible(false);
        oldMenuItem.setVisible(false);
        downloadMenuItem.setVisible(false);
        deleteMenuItem.setVisible(false);

        SparseBooleanArray checkedItems = fragment.getListView().getCheckedItemPositions();

        // Check which option apply to current selection and make corresponding
        // menu items visible
        for (int position = 0; position < fragment.getListAdapter().getCount(); position++) {
            if (checkedItems.get(position)) {
                Episode episode = (Episode) fragment.getListAdapter().getItem(position);

                if (episodeManager.getState(episode))
                    newMenuItem.setVisible(true);
                else
                    oldMenuItem.setVisible(true);

                if (episodeManager.isDownloaded(episode) || episodeManager.isDownloading(episode))
                    deleteMenuItem.setVisible(true);
                else
                    downloadMenuItem.setVisible(true);
            }

            // Break out of the loop if all menu items are visible
            if (newMenuItem.isVisible() && oldMenuItem.isVisible()
                    && downloadMenuItem.isVisible() && deleteMenuItem.isVisible())
                break;
        }
    }
}
