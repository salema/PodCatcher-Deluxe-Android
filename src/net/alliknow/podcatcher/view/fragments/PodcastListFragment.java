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

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;

import net.alliknow.podcatcher.R;
import net.alliknow.podcatcher.adapters.PodcastListAdapter;
import net.alliknow.podcatcher.listeners.ContextMenuListener;
import net.alliknow.podcatcher.listeners.OnLoadPodcastLogoListener;
import net.alliknow.podcatcher.listeners.OnSelectPodcastListener;
import net.alliknow.podcatcher.model.PodcastManager;
import net.alliknow.podcatcher.model.types.Podcast;
import net.alliknow.podcatcher.model.types.Progress;
import net.alliknow.podcatcher.view.PodcastListItemView;

import java.util.Collections;
import java.util.List;

/**
 * List fragment to display the list of podcasts.
 */
public class PodcastListFragment extends PodcatcherListFragment
        implements AdapterView.OnItemSelectedListener, OnLoadPodcastLogoListener {

    /**
     * The listener call-back to alert on podcast selection
     */
    private OnSelectPodcastListener podcastSelectionListener;
    private ContextMenuListener contextMenuListener;

    /**
     * The list of podcasts currently shown
     */
    private List<Podcast> currentPodcastList;

    /**
     * The podcast add and remove animation duration
     */
    private int addRemoveDuration;

    /**
     * Status flag indicating that our view is created
     */
    private boolean viewCreated = false;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // Make sure our listener is present
        try {
            this.podcastSelectionListener = (OnSelectPodcastListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnSelectPodcastListener");
        }

        this.addRemoveDuration = getResources().getInteger(android.R.integer.config_mediumAnimTime);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

//        setHasOptionsMenu(true);

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

        // Set list choice listener (context action mode)
//        getListView().setMultiChoiceModeListener(new PodcastListContextListener(this));

        // Consider the view created successfully beyond this point
        viewCreated = true;

        // This will make sure we show the right information once the view
        // controls are established (the list might have been set earlier)
        if (currentPodcastList != null)
            setPodcastList(currentPodcastList);

        contextMenuListener = (ContextMenuListener) getActivity();
        getListView().setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                Podcast podcast = PodcastManager.getInstance().getPodcastList().get(position);
                contextMenuListener.onPodcastContextMenuOpen(podcast);
                return false;
            }
        });

        getListView().setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                PodcastListFragment.super.onFocusChange(v, hasFocus);
                if (hasFocus) {
                    contextMenuListener.onPodcastListFocused();
                }
            }
        });
    }

    @Override
    public void onListItemClick(ListView list, View view, int position, long id) {
        Podcast selectedPodcast = (Podcast) adapter.getItem(position);

        // Alert parent activity
        podcastSelectionListener.onPodcastSelected(selectedPodcast);
        contextMenuListener.onPodcastListFocused();

//        onFocusChange(list, true);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        Podcast selectedPodcast = (Podcast) adapter.getItem(position);
        if (!getListView().isInTouchMode()) {
            podcastSelectionListener.onPodcastSelected(selectedPodcast);
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
//        if (!getListView().isInTouchMode()) {
//            podcastSelectionListener.onNoPodcastSelected();
//        }
    }

    @Override
    public void onDestroyView() {
        viewCreated = false;

        super.onDestroyView();
    }

    /**
     * Set the list of podcasts to show in this fragment. You can call this any
     * time and the view will catch up as soon as it is created. This will also
     * reset any selection.
     *
     * @param podcastList List of podcasts to show.
     */
    public void setPodcastList(List<Podcast> podcastList) {
        this.currentPodcastList = podcastList;

        showProgress = false;
        showLoadFailed = false;

        // Reset selection since it might not work with the new list
        selectNone();

        // Maps the podcast list items to the list UI
        // Only update the UI if it has been inflated
        if (viewCreated) {
            if (adapter == null)
                // This also set the member
                setListAdapter(new PodcastListAdapter(getActivity(), podcastList));
            else
                ((PodcastListAdapter) adapter).updateList(podcastList);

            updateUiElementVisibility();
        }
    }

    /**
     * Add a podcast to the list shown. Use this instead of
     * {@link #setPodcastList(List)} if you want a nice, animated addition.
     *
     * @param podcast Podcast to add.
     */
    public void addPodcast(Podcast podcast) {
        currentPodcastList.add(podcast);
        Collections.sort(currentPodcastList);
        ((PodcastListAdapter) adapter).updateList(currentPodcastList);

        final int index = currentPodcastList.indexOf(podcast);

        if (viewCreated) {
            final PodcastListItemView listItemView = (PodcastListItemView) findListItemViewForIndex(index);

            // Is the position visible?
            if (listItemView != null) {
                listItemView.setAlpha(0f);
                listItemView.animate().alpha(1f).setDuration(addRemoveDuration).setListener(null);
            }
        }
    }

    /**
     * Remove a podcast from the list shown. Use this instead of
     * {@link #setPodcastList(List)} if you want a nice, animated removal.
     *
     * @param podcast Podcast to remove.
     */
    public void removePodcast(final Podcast podcast) {
        final int index = currentPodcastList.indexOf(podcast);

        if (viewCreated) {
            final PodcastListItemView listItemView = (PodcastListItemView) findListItemViewForIndex(index);

            // Is the position visible?
            if (listItemView != null)
                listItemView.animate().alpha(0f).setDuration(addRemoveDuration)
                        .setListener(new AnimatorListenerAdapter() {

                            @Override
                            public void onAnimationEnd(Animator animation) {
                                if (currentPodcastList.remove(podcast))
                                    ((PodcastListAdapter) adapter).updateList(currentPodcastList);
                                // Set it back to opaque because the view might
                                // be recycled and we need it to show
                                listItemView.setAlpha(1f);
                            }
                        });
                // Not visible, simply remove the podcast
            else if (currentPodcastList.remove(podcast))
                ((PodcastListAdapter) adapter).updateList(currentPodcastList);
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
            final PodcastListItemView listItemView = (PodcastListItemView) findListItemViewForIndex(position);

            // Is the position visible?
            if (listItemView != null)
                listItemView.updateProgress(progress);
        }
    }

    @Override
    public void onPodcastLogoLoaded(Podcast podcast) {
        ((BaseAdapter) getListAdapter()).notifyDataSetChanged();
    }

    @Override
    public void onPodcastLogoLoadFailed(Podcast podcast) {
        // pass
    }
}
