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

import android.content.Intent;
import android.os.Bundle;
import android.os.StrictMode;

import net.alliknow.podcatcher.listeners.OnChangePodcastListListener;
import net.alliknow.podcatcher.listeners.OnLoadPodcastListListener;
import net.alliknow.podcatcher.model.types.Podcast;
import net.alliknow.podcatcher.view.fragments.EpisodeListFragment;

import java.util.List;

/**
 * Our main activity class. Works as the main controller. Depending on the view
 * state, other activities cooperate.
 */
public class PodcastActivity extends EpisodeListActivity implements
        OnLoadPodcastListListener, OnChangePodcastListListener {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Enable strict mode when on debug
        if (((Podcatcher) getApplication()).isInDebugMode())
            StrictMode.enableDefaults();

        // Register as listener to the podcast data manager
        podcastManager.addLoadPodcastListListener(this);
        podcastManager.addChangePodcastListListener(this);

        // Inflate the main content view (depends on view mode)
        setContentView(R.layout.main);

        // On small screens in landscape mode, add the episode list fragment
        if (viewMode == SMALL_LANDSCAPE_VIEW && findEpisodeListFragment() == null)
            getFragmentManager().beginTransaction()
                    .add(R.id.content, new EpisodeListFragment(), episodeListFragmentTag).commit();

        // Load all podcasts? TODO Make this a preference
    }

    @Override
    protected void onResume() {
        // Check if podcast list is available - if so, set it
        List<Podcast> podcastList = podcastManager.getPodcastList();
        if (podcastList != null)
            onPodcastListLoaded(podcastList);

        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Unregister the listeners
        podcastManager.removeLoadPodcastListListener(this);
        podcastManager.removeChangePodcastListListener(this);
    }

    @Override
    public void onPodcastListLoaded(List<Podcast> podcastList) {
        // Make podcast list show
        findPodcastListFragment().setPodcastList(podcastList);

        // If podcast list is empty we show dialog on startup
        if (podcastList.isEmpty())
            startActivity(new Intent(this, AddPodcastActivity.class));
    }

    @Override
    public void onPodcastAdded(Podcast podcast) {
        // There is nothing more to do here since we are paused
        // the selection will be picked up on resume.
        this.currentPodcast = podcast;

        // Update podcast list
        findPodcastListFragment().setPodcastList(podcastManager.getPodcastList());
    }

    @Override
    public void onPodcastRemoved(Podcast podcast) {
        if (podcast.equals(currentPodcast))
            this.currentPodcast = null;
    }
}
