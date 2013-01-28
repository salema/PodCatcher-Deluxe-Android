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

import android.os.Bundle;

import net.alliknow.podcatcher.model.types.Episode;
import net.alliknow.podcatcher.model.types.Podcast;
import net.alliknow.podcatcher.view.fragments.EpisodeListFragment;

import java.util.ArrayList;

/**
 * @author Kevin Hausmann
 */
public class ShowEpisodeListActivity extends EpisodeListActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        System.out.println("Create");

        // Check if we need this activity at all
        if (viewMode != SMALL_PORTRAIT_VIEW) {
            finish();
        } else {
            setContentView(R.layout.main);

            if (savedInstanceState == null) {
                // During initial setup, plug in the episode list fragment.
                episodeListFragment = new EpisodeListFragment();

                getFragmentManager()
                        .beginTransaction()
                        .add(R.id.content, episodeListFragment,
                                getResources().getString(R.string.episode_list_fragment_tag))
                        .commit();
            }
        }
    }

    @Override
    protected void onStart() {
        // TODO Auto-generated method stub
        super.onStart();
        System.out.println("Start");
    }

    @Override
    protected void onResume() {
        super.onResume();
        System.out.println("Resume");

        // Get the load mode
        multiplePodcastsMode = getIntent().getExtras().getBoolean(PodcastActivity.MODE_KEY);

        // Get URL of podcast to load
        String podcastUrl = getIntent().getExtras().getString(PODCAST_URL_KEY);
        currentPodcast = podcastManager.findPodcastForUrl(podcastUrl);

        // Prepare UI
        episodeListFragment.resetAndSpin();

        // We are in select all mode
        if (multiplePodcastsMode) {
            currentEpisodeList = new ArrayList<Episode>();

            for (Podcast podcast : podcastManager.getPodcastList())
                podcastManager.load(podcast);
        } // Single podcast to load
        else {
            // Go load it if found
            if (currentPodcast != null)
                podcastManager.load(currentPodcast);
            else
                episodeListFragment.showLoadFailed();
        }
    }

    @Override
    protected void onPause() {
        // TODO Auto-generated method stub
        super.onPause();

        System.out.println("Pause");
    }

    @Override
    protected void onStop() {
        // TODO Auto-generated method stub
        super.onStop();
        System.out.println("Stop");
    }

    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();

        System.out.println("Destroy");
    }

    @Override
    protected void updatePlayer() {
        super.updatePlayer();

        if (playerFragment != null) {
            playerFragment.setLoadMenuItemVisibility(false, false);
            playerFragment.setPlayerTitleVisibility(true);
        }
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_right);
    }
}
