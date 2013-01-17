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

import net.alliknow.podcatcher.listeners.OnLoadPodcastListener;
import net.alliknow.podcatcher.listeners.OnSelectEpisodeListener;
import net.alliknow.podcatcher.model.tasks.Progress;
import net.alliknow.podcatcher.model.types.Episode;
import net.alliknow.podcatcher.model.types.Podcast;
import net.alliknow.podcatcher.view.fragments.EpisodeListFragment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 
 */
public class ShowEpisodeListActivity extends PodcatcherBaseActivity implements
        OnLoadPodcastListener, OnSelectEpisodeListener {

    /** Key to give wanted podcast url in intent */
    public static final String PODCAST_URL_KEY = "podcast_url";

    /** Flag to indicate whether we are in multiple podcast mode */
    private boolean multiplePodcastsMode = false;

    /** The podcast we are showing episodes for */
    private Podcast selectedPodcast;
    private List<Episode> currentEpisodeList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check if we need this activity at all
        if (viewMode != SMALL_PORTRAIT_VIEW) {
            finish();
        } else {
            podcastManager.addLoadPodcastListener(this);

            if (savedInstanceState == null)
                // During initial setup, plug in the episode list fragment.
                getFragmentManager()
                        .beginTransaction()
                        .add(android.R.id.content, new EpisodeListFragment(),
                                episodeListFragmentTag)
                        .commit();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Prepare UI
        findEpisodeListFragment().resetAndSpin();

        // Get the load mode
        multiplePodcastsMode = getIntent().getExtras().getBoolean(PodcastActivity.MODE_KEY);

        // We are in select all mode
        if (multiplePodcastsMode) {
            currentEpisodeList = new ArrayList<Episode>();

            for (Podcast podcast : podcastManager.getPodcastList())
                podcastManager.load(podcast);
        } // Single podcast to load
        else {
            // Get URL of podcast to load
            String podcastUrl = getIntent().getExtras().getString(PODCAST_URL_KEY);

            // Find the podcast object
            for (Podcast podcast : podcastManager.getPodcastList())
                if (podcast.getUrl().toString().equals(podcastUrl))
                    this.selectedPodcast = podcast;

            // Go load it if found
            if (selectedPodcast != null)
                podcastManager.load(selectedPodcast);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        podcastManager.removeLoadPodcastListener(this);
    }

    @Override
    public void onPodcastLoadProgress(Podcast podcast, Progress progress) {
        if (!multiplePodcastsMode && podcast.equals(selectedPodcast))
            findEpisodeListFragment().showProgress(progress);
    }

    @Override
    public void onPodcastLoaded(Podcast podcast) {
        if (multiplePodcastsMode) {
            // TODO decide on this: episodeList.addAll(list.subList(0,
            // list.size() > 100 ? 100 : list.size() - 1));
            if (podcast.getEpisodes().size() > 0) {
                currentEpisodeList.addAll(podcast.getEpisodes());
                Collections.sort(currentEpisodeList);

                findEpisodeListFragment().setEpisodes(currentEpisodeList);
            }
        }
        else if (podcast.equals(selectedPodcast)) {
            currentEpisodeList = podcast.getEpisodes();
            findEpisodeListFragment().setEpisodes(currentEpisodeList);
        }
    }

    @Override
    public void onPodcastLoadFailed(Podcast podcast) {
        // TODO What happens in multiple podcast mode?
        if (!multiplePodcastsMode)
            findEpisodeListFragment().showLoadFailed();
    }

    @Override
    public void onEpisodeSelected(Episode selectedEpisode) {
        Intent intent = new Intent();
        intent.setClass(this, ShowEpisodeActivity.class);
        // intent.putExtra("index", index);
        startActivity(intent);
    }

    @Override
    public void onNoEpisodeSelected() {
        // TODO Auto-generated method stub
    }
}
