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
import android.view.MenuItem;

import net.alliknow.podcatcher.model.types.Episode;
import net.alliknow.podcatcher.model.types.Podcast;
import net.alliknow.podcatcher.view.fragments.EpisodeFragment;

/**
 * @author Kevin Hausmann
 */
public class ShowEpisodeActivity extends EpisodeActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        switch (viewMode) {
            case LARGE_PORTRAIT_VIEW:
            case LARGE_LANDSCAPE_VIEW:
            case SMALL_LANDSCAPE_VIEW:
                // In large or landscape layouts we do not need this activity at
                // all, so finish it off
                finish();
                break;
            case SMALL_PORTRAIT_VIEW:
                // Set the content view
                setContentView(R.layout.main);

                // During initial setup, plug in the details fragment.
                if (savedInstanceState == null) {
                    episodeFragment = new EpisodeFragment();
                    getFragmentManager()
                            .beginTransaction()
                            .add(R.id.content, episodeFragment,
                                    getResources().getString(R.string.episode_fragment_tag))
                            .commit();
                }

                // Set fragment members
                findFragments();

                // Enable navigation
                getActionBar().setDisplayHomeAsUpEnabled(true);

                // Set episode in fragment UI
                restoreCurrentEpisodeFromIntent();
                episodeFragment.setEpisode(currentEpisode);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // This is called when the Home (Up) button is pressed
                finish();

                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_right);
    }

    private void restoreCurrentEpisodeFromIntent() {
        // Get URL of podcast to load
        String podcastUrl = getIntent().getExtras().getString(PODCAST_URL_KEY);
        String episodeUrl = getIntent().getExtras().getString(EPISODE_URL_KEY);

        // Find the podcast object
        Podcast selectedPodcast = podcastManager.findPodcastForUrl(podcastUrl);

        // Find and (re)set the current episode to show
        if (selectedPodcast != null) {
            for (Episode episode : selectedPodcast.getEpisodes())
                if (episode.getMediaUrl().toString().equals(episodeUrl))
                    this.currentEpisode = episode;
        }
    }
}
