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

import android.app.Activity;
import android.os.Bundle;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import net.alliknow.podcatcher.R;
import net.alliknow.podcatcher.adapters.EpisodeListAdapter;
import net.alliknow.podcatcher.listeners.EpisodeListContextListener;
import net.alliknow.podcatcher.listeners.OnReverseSortingListener;
import net.alliknow.podcatcher.listeners.OnSelectEpisodeListener;
import net.alliknow.podcatcher.listeners.OnToggleFilterListener;
import net.alliknow.podcatcher.model.types.Episode;
import net.alliknow.podcatcher.view.EpisodeListItemView;

import java.util.ArrayList;
import java.util.List;

/**
 * List fragment to display the list of episodes.
 */
public class EpisodeListFragment extends PodcatcherListFragment {

    /** The list of episodes we are currently showing. */
    private List<Episode> currentEpisodeList;

    /** The activity we are in (listens to user selection) */
    private OnSelectEpisodeListener episodeSelectionListener;
    /** The activity we are in (listens to filter toggles) */
    private OnToggleFilterListener filterListener;
    /** The activity we are in (listens to sorting toggles) */
    private OnReverseSortingListener sortingListener;

    /** Flag for show sort menu item state */
    private boolean showSortMenuItem = false;
    /** Flag for the state of the sort menu item */
    private boolean sortMenuItemState = false;
    /** Flag for show filter menu item state */
    private boolean showFilterMenuItem = false;
    /** Flag for the state of the filter menu item */
    private boolean filterMenuItemState = false;
    /** Flag for show filter warning state */
    private boolean showFilterWarning = false;
    /** The number of episodes filtered */
    private int filteredEpisodesCount = 0;
    /** Flag to indicate whether podcast names should be shown for episodes */
    private boolean showPodcastNames = false;

    /** Identifier for the string the empty view shows. */
    private int emptyStringId = R.string.episode_none;

    /** The sort episodes menu bar item */
    private MenuItem sortMenuItem;
    /** The filter episodes menu bar item */
    private MenuItem filterMenuItem;
    /** The filter warning label */
    private TextView filterWarningLabel;
    /** The filter warning label divider */
    private View filterWarningDivider;

    /** Status flag indicating that our view is created */
    private boolean viewCreated = false;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // Make sure our listener is present
        try {
            this.episodeSelectionListener = (OnSelectEpisodeListener) activity;
            this.filterListener = (OnToggleFilterListener) activity;
            this.sortingListener = (OnReverseSortingListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnSelectEpisodeListener, " +
                    "OnFilterToggleListener, and OnReverseSortingListener");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        return inflater.inflate(R.layout.episode_list, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        filterWarningLabel = (TextView) view.findViewById(R.id.filtered_warning);
        filterWarningDivider = view.findViewById(R.id.warning_divider);

        filterWarningLabel.setBackgroundColor(themeColor);
        filterWarningDivider.setBackgroundColor(themeColor);

        viewCreated = true;

        // Set list choice listener (context action mode)
        getListView().setMultiChoiceModeListener(new EpisodeListContextListener(this));

        // This will make sure we show the right information once the view
        // controls are established (the list might have been set earlier)
        if (currentEpisodeList != null) {
            setEpisodeList(currentEpisodeList);
            setFilterWarning(showFilterWarning, filteredEpisodesCount);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        inflater.inflate(R.menu.episode_list, menu);

        sortMenuItem = menu.findItem(R.id.sort_menuitem);
        setSortMenuItemVisibility(showSortMenuItem, sortMenuItemState);

        filterMenuItem = menu.findItem(R.id.filter_menuitem);
        setFilterMenuItemVisibility(showFilterMenuItem, filterMenuItemState);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.sort_menuitem:
                // Tell activity to re-order the list
                sortingListener.onReverseOrder();

                return true;
            case R.id.filter_menuitem:
                // Tell activity to toggle the filter
                filterListener.onToggleFilter();

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onListItemClick(ListView list, View view, int position, long id) {
        // Find selected episode and alert listener
        Episode selectedEpisode = (Episode) adapter.getItem(position);
        episodeSelectionListener.onEpisodeSelected(selectedEpisode);
    }

    @Override
    public void onDestroyView() {
        viewCreated = false;

        super.onDestroyView();
    }

    /**
     * Set the list of episodes to show in this fragment. You can call this any
     * time and the view will catch up as soon as it is created.
     * 
     * @param episodeList List of episodes to show.
     */
    public void setEpisodeList(List<Episode> episodeList) {
        this.currentEpisodeList = episodeList;

        showProgress = false;
        showLoadFailed = false;

        // Update UI
        if (viewCreated) {
            // We need to store any currently check items here because
            // setting the adapter will override their status and the
            // relevant positions in the list might change
            List<Episode> checkedEpisodes = getCheckedEpisodes();
            // Clear all checked states here
            getListView().clearChoices();

            if (adapter == null)
                // This also set the member
                setListAdapter(new EpisodeListAdapter(getActivity(), episodeList));
            else
                ((EpisodeListAdapter) adapter).updateList(episodeList);

            // Update adapter setting
            ((EpisodeListAdapter) adapter).setShowPodcastNames(showPodcastNames);

            // Restore checked items
            if (checkedEpisodes != null)
                for (Episode episode : checkedEpisodes) {
                    final int newPosition = currentEpisodeList.indexOf(episode);

                    if (newPosition >= 0)
                        getListView().setItemChecked(newPosition, true);
                }

            // Update other UI elements
            if (episodeList.isEmpty())
                emptyView.setText(emptyStringId);

            // Make sure to match selection state
            if (selectAll)
                selectAll();
            else if (selectedPosition >= 0 && selectedPosition < episodeList.size())
                select(selectedPosition);
            else
                selectNone();
        }
    }

    /**
     * Set whether the fragment should show the sort icon. You can call this any
     * time and can expect it to happen on fragment resume at the latest. You
     * also have to set the sort icon state, <code>true</code> for "reverse" and
     * <code>false</code> for "normal" (i.e. latest first).
     * 
     * @param show Whether to show the sort menu item.
     * @param reverse State of the sort menu item (reverse / normal)
     */
    public void setSortMenuItemVisibility(boolean show, boolean reverse) {
        this.showSortMenuItem = show;
        this.sortMenuItemState = reverse;

        // Only do it right away if resumed and menu item is available,
        // otherwise onResume or the menu creation callback will call us.
        if (sortMenuItem != null) {
            sortMenuItem.setVisible(showSortMenuItem);
            sortMenuItem.setIcon(sortMenuItemState ?
                    R.drawable.ic_menu_sort_reverse : R.drawable.ic_menu_sort);
        }
    }

    /**
     * Set whether the fragment should show the filter icon. You can call this
     * any time and can expect it to happen on fragment resume at the latest.
     * You also have to set the filter icon state, <code>true</code> for
     * "new only" and <code>false</code> for "show all".
     * 
     * @param show Whether to show the filter menu item.
     * @param filter State of the filter menu item (new / all)
     */
    public void setFilterMenuItemVisibility(boolean show, boolean filter) {
        this.showFilterMenuItem = show;
        this.filterMenuItemState = filter;

        // Only do it right away if resumed and menu item is available,
        // otherwise onResume or the menu creation callback will call us.
        if (filterMenuItem != null) {
            filterMenuItem.setVisible(showFilterMenuItem);
            filterMenuItem.setTitle(filterMenuItemState ?
                    R.string.episodes_filter_all : R.string.episodes_filter_new);
            filterMenuItem.setIcon(filterMenuItemState ?
                    R.drawable.ic_menu_filter_back : R.drawable.ic_menu_filter);
        }
    }

    /**
     * Configure whether the fragment should show the filter warning. This will
     * only work once the view is created.
     * 
     * @param show Whether to show the warning at all.
     * @param count The amount of episodes filtered.
     */
    public void setFilterWarning(boolean show, int count) {
        this.showFilterWarning = show;
        this.filteredEpisodesCount = count;

        if (viewCreated) {
            filterWarningLabel.setVisibility(show ? View.VISIBLE : View.GONE);
            filterWarningDivider.setVisibility(show ? View.VISIBLE : View.GONE);
            filterWarningLabel.setText(getResources().getQuantityString(
                    R.plurals.episodes_filtered, count, count));
        }
    }

    /**
     * Update the progress information for the episode at the given position to
     * reflect the percentage of given. Does nothing if the episode is off
     * screen.
     * 
     * @param position The position of the episode in the current list.
     * @param percent The percentage value to show.
     */
    public void showProgress(int position, int percent) {
        // To prevent this if we are not ready to handle progress update
        // e.g. on app termination
        if (viewCreated) {
            // Adjust the position relative to list scroll state
            final int firstVisiblePosition = getListView().getFirstVisiblePosition();
            final EpisodeListItemView listItemView =
                    (EpisodeListItemView) getListView().getChildAt(position - firstVisiblePosition);
            // Is the position visible?
            if (listItemView != null)
                listItemView.updateProgress(percent);
        }
    }

    @Override
    public void setThemeColors(int color, int variantColor) {
        super.setThemeColors(color, variantColor);

        if (viewCreated) {
            filterWarningLabel.setBackgroundColor(color);
            filterWarningDivider.setBackgroundColor(color);
        }
    }

    /**
     * Set whether the fragment should show the podcast name for each episode
     * item. Change will be reflected upon next call of
     * {@link #setEpisodeList(List)}
     * 
     * @param show Whether to show the podcast names.
     */
    public void setShowPodcastNames(boolean show) {
        this.showPodcastNames = show;
    }

    /**
     * Define which text label the list's empty view shows. Will only have an
     * effect if you call {@link #setEpisodeList(List)} with an empty list
     * afterwards.
     * 
     * @param id The empty string resource identifier.
     */
    public void setEmptyStringId(int id) {
        this.emptyStringId = id;
    }

    @Override
    protected void reset() {
        if (viewCreated) {
            emptyView.setText(R.string.podcast_none_selected);
            setFilterWarning(false, 0);
        }

        currentEpisodeList = null;
        showPodcastNames = false;

        super.reset();
    }

    /**
     * Show error view.
     */
    @Override
    public void showLoadFailed() {
        if (viewCreated)
            progressView.showError(R.string.podcast_load_error);

        super.showLoadFailed();
    }

    private List<Episode> getCheckedEpisodes() {
        List<Episode> result = null;

        SparseBooleanArray checkedItems = getListView().getCheckedItemPositions();
        if (checkedItems.size() > 0) {
            result = new ArrayList<Episode>();

            for (int position = 0; position < getListAdapter().getCount(); position++)
                if (checkedItems.get(position))
                    result.add((Episode) getListAdapter().getItem(position));
        }

        return result;
    }
}
