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

package net.alliknow.podcatcher.view.fragments;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.app.Activity;
import android.app.ListFragment;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListAdapter;
import android.widget.TextView;

import net.alliknow.podcatcher.R;
import net.alliknow.podcatcher.listeners.OnSelectPodcastListener;
import net.alliknow.podcatcher.model.types.Progress;
import net.alliknow.podcatcher.view.ProgressView;
import net.alliknow.podcatcher.view.adapters.PodcatcherBaseListAdapter;

/**
 * Generic list fragment sub-class for podcatcher list fragments. Defines some
 * helpers and common functionality.
 */
public abstract class PodcatcherListFragment extends ListFragment {

    /** The activity we are in (listens to user selection) */
    protected OnSelectPodcastListener contentSelectionListener;

    /** The list adapter */
    protected PodcatcherBaseListAdapter adapter;

    /** The empty view */
    protected TextView emptyView;
    /** The progress bar */
    protected ProgressView progressView;

    /** Flags for internal state: show progress */
    protected boolean showProgress = false;
    /** Flags for internal state: show error */
    protected boolean showLoadFailed = false;
    /** Flags for internal state: select all */
    protected boolean selectAll = false;
    /** Member to keep track of current selection */
    protected int selectedPosition = -1;

    /** Status flag indicating that our view is created */
    private boolean viewCreated = false;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // Make sure our listener is present
        try {
            this.contentSelectionListener = (OnSelectPodcastListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnSelectPodcastListener");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        emptyView = (TextView) getView().findViewById(android.R.id.empty);
        progressView = (ProgressView) getView().findViewById(R.id.progress);

        viewCreated = true;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Prevent the menu item to be added multiple times if more then on
        // sub-class fragment is showing
        if (menu.findItem(R.id.podcast_select_all_menuitem) == null)
            inflater.inflate(R.menu.content_modes, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.podcast_select_all_menuitem:
                contentSelectionListener.onAllPodcastsSelected();

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        updateUiElementVisibility();
    }

    @Override
    public void onDestroyView() {
        viewCreated = false;

        super.onDestroyView();
    }

    @Override
    public void setListAdapter(ListAdapter adapter) {
        this.adapter = (PodcatcherBaseListAdapter) adapter;

        super.setListAdapter(adapter);
    }

    /**
     * Select an item.
     * 
     * @param position Index of item to select.
     */
    public void select(int position) {
        selectAll = false;
        selectedPosition = position;

        if (adapter != null && !showProgress) {
            adapter.setSelectedPosition(position);
            getListView().smoothScrollToPosition(position);
        }

        updateUiElementVisibility();
    }

    /**
     * Select all items.
     */
    public void selectAll() {
        selectAll = true;
        selectedPosition = -1;

        if (adapter != null && !showProgress)
            adapter.setSelectAll();

        updateUiElementVisibility();
    }

    /**
     * Unselect selected item (if any).
     */
    public void selectNone() {
        selectAll = false;
        selectedPosition = -1;

        if (adapter != null && !showProgress)
            adapter.setSelectNone();

        updateUiElementVisibility();
    }

    /**
     * Refresh the list and its views.
     */
    public void refresh() {
        if (adapter != null)
            adapter.notifyDataSetChanged();
    }

    /**
     * Reset the UI to initial state.
     */
    public void resetUi() {
        reset();

        updateUiElementVisibility();
    }

    /**
     * Show the UI to be working.
     */
    public void resetAndSpin() {
        reset();
        // Show progress should be set to make UI switch
        // to show progress as soon as it is created
        showProgress = true;

        updateUiElementVisibility();
    }

    /**
     * Reset the fragments state. Sub-classes should extends this. Will be
     * called on public reset.
     * 
     * @see #resetUi()
     * @see #resetAndSpin()
     */
    protected void reset() {
        showProgress = false;
        showLoadFailed = false;
        selectAll = false;
        selectedPosition = -1;

        setListAdapter(null);
        if (viewCreated)
            progressView.reset();
    }

    /**
     * Update UI with load progress.
     * 
     * @param progress Amount loaded or flag from load task.
     */
    public void showProgress(Progress progress) {
        // Only show this if we are visible
        if (viewCreated)
            progressView.publishProgress(progress);
    }

    /**
     * Show error view.
     */
    public void showLoadFailed() {
        showProgress = false;
        showLoadFailed = true;
        selectAll = false;

        updateUiElementVisibility();
    }

    /**
     * Use the internal state variables to determine wanted UI state.
     * Sub-classes might want to extend this.
     */
    protected void updateUiElementVisibility() {
        if (viewCreated) {
            // Progress view is displaying information
            if (showProgress || showLoadFailed) {
                emptyView.setVisibility(GONE);
                getListView().setVisibility(GONE);
                progressView.setVisibility(VISIBLE);
            } // Show the episode list or the empty view
            else {
                boolean itemsAvailable = getListAdapter() != null && !getListAdapter().isEmpty();

                emptyView.setVisibility(itemsAvailable ? GONE : VISIBLE);
                getListView().setVisibility(itemsAvailable ? VISIBLE : GONE);
                progressView.setVisibility(GONE);
            }
        }
    }
}
