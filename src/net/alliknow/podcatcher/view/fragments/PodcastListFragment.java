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
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;

import net.alliknow.podcatcher.AddPodcastActivity;
import net.alliknow.podcatcher.R;
import net.alliknow.podcatcher.listeners.OnSelectPodcastListener;
import net.alliknow.podcatcher.listeners.PodcastListContextListener;
import net.alliknow.podcatcher.model.tasks.Progress;
import net.alliknow.podcatcher.model.types.Podcast;
import net.alliknow.podcatcher.view.HorizontalProgressView;
import net.alliknow.podcatcher.view.adapters.PodcastListAdapter;

import java.util.List;

/**
 * List fragment to display the list of podcasts.
 */
public class PodcastListFragment extends PodcatcherListFragment {

    /** The list of podcasts currently shown */
    private List<Podcast> currentPodcastList;

    /** The activity we are in (listens to user selection) */
    private OnSelectPodcastListener selectionListener;

    /** Remove podcast menu item */
    private MenuItem selectAllMenuItem;
    /** Remove podcast menu item */
    private MenuItem removeMenuItem;

    /** The logo view */
    private ImageView logoView;
    /** The current logo view mode */
    private LogoViewMode mode = LogoViewMode.SMALL;

    /** The options available for the logo view */
    public enum LogoViewMode {
        SMALL, LARGE
    };

    /** Status flag indicating that our view is created */
    private boolean viewCreated = false;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // Make sure our listener is present
        try {
            this.selectionListener = (OnSelectPodcastListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnSelectPodcastListener");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);

        // Make the UI show to be working once it is up
        showProgress = true;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        return inflater.inflate(R.layout.podcast_list, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        logoView = (ImageView) view.findViewById(R.id.podcast_image);
        getListView().setMultiChoiceModeListener(new PodcastListContextListener(this));

        viewCreated = true;

        // This will make sure we show the right information once the view
        // controls are established (the list might have been set earlier)
        if (currentPodcastList != null)
            setPodcastList(currentPodcastList);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.podcast_list, menu);

        removeMenuItem = (MenuItem) menu.findItem(R.id.podcast_remove_menuitem);
        selectAllMenuItem = (MenuItem) menu.findItem(R.id.podcast_select_all_menuitem);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.podcast_add_menuitem:
                startActivity(new Intent(getActivity(), AddPodcastActivity.class));

                return true;
            case R.id.podcast_select_all_menuitem:
                selectionListener.onAllPodcastsSelected();

                return true;
            case R.id.podcast_remove_menuitem:
                getListView().setItemChecked(adapter.getSelectedPosition(), true);

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onListItemClick(ListView list, View view, int position, long id) {
        Podcast selectedPodcast = (Podcast) adapter.getItem(position);

        // Alert parent activity
        selectionListener.onPodcastSelected(selectedPodcast);
    }

    @Override
    public void onDestroyView() {
        viewCreated = false;

        super.onDestroyView();
    }

    public void setPodcastList(List<Podcast> podcastList) {
        this.currentPodcastList = podcastList;

        this.showProgress = false;

        // Maps the podcast list items to the list UI
        // Only update the UI if it has been inflated
        if (viewCreated) {
            setListAdapter(new PodcastListAdapter(getActivity(), podcastList));

            // Make sure to match selection state
            if (selectAll)
                selectAll();
            else if (adapter.getSelectedPosition() >= 0)
                select(adapter.getSelectedPosition());
            else
                selectNone();

            updateUiElementVisibility();
        }
    }

    @Override
    public void select(int position) {
        super.select(position);

        if (viewCreated) {
            Podcast selectedPodcast = (Podcast) adapter.getItem(position);

            // Prepare UI
            if (selectedPodcast.getLogo() == null)
                logoView.setImageResource(R.drawable.default_podcast_logo);
            else
                logoView.setImageBitmap(selectedPodcast.getLogo());

            updateUiElementVisibility();
        }
    }

    @Override
    public void selectAll() {
        super.selectAll();

        // Prepare UI
        if (viewCreated) {
            logoView.setImageResource(R.drawable.default_podcast_logo);
            updateUiElementVisibility();
        }
    }

    @Override
    public void selectNone() {
        super.selectNone();

        if (viewCreated) {
            logoView.setImageResource(R.drawable.default_podcast_logo);
        }
    }

    /**
     * Show progress for a certain position in the podcast list. Progress will
     * ignored if the item is not visible.
     * 
     * @param position Position in list to show progress for.
     * @param progress Progress information to show.
     */
    public void showProgress(int position, Progress progress) {
        // To prevent this if we are not ready to handle progress update
        // e.g. on app termination
        if (viewCreated) {
            View listItemView = getListView().getChildAt(position);
            if (listItemView != null)
                ((HorizontalProgressView) listItemView.findViewById(R.id.list_item_progress))
                        .publishProgress(progress);
        }
    }

    public void setLogoVisibility(LogoViewMode mode) {
        this.mode = mode;

        updateUiElementVisibility();
    }

    public void showLogo(Bitmap logo) {
        if (viewCreated)
            logoView.setImageBitmap(logo);
    }

    public int getLogoViewWidth() {
        return viewCreated ? logoView.getWidth() : 100;
    }

    public int getLogoViewHeight() {
        return viewCreated ? logoView.getHeight() : 100;
    }

    @Override
    protected void updateUiElementVisibility() {
        super.updateUiElementVisibility();

        if (viewCreated) {
            logoView.setVisibility(mode.equals(LogoViewMode.SMALL) ? GONE : VISIBLE);

            // Menu items might be late to load
            if (selectAllMenuItem != null)
                selectAllMenuItem.setVisible(adapter != null && adapter.getCount() > 1
                        && !selectAll);
            if (removeMenuItem != null)
                removeMenuItem.setVisible(adapter != null && adapter.getSelectedPosition() >= 0);
        }
    }
}
