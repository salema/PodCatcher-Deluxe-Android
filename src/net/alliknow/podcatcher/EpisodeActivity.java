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

import android.app.FragmentTransaction;
import android.content.Intent;

import net.alliknow.podcatcher.listeners.OnSelectEpisodeListener;
import net.alliknow.podcatcher.model.types.Episode;
import net.alliknow.podcatcher.view.fragments.EpisodeFragment;
import net.alliknow.podcatcher.view.fragments.EpisodeListFragment;

/**
 * Show episode activity.
 */
public class EpisodeActivity extends BaseActivity implements OnSelectEpisodeListener {

    /** Key used to store podcast URL in intent or bundle */
    public static final String PODCAST_URL_KEY = "podcast_url";
    /** Key used to store episode URL in intent or bundle */
    public static final String EPISODE_URL_KEY = "episode_url";

    /** The episode currently selected and displayed */
    protected Episode currentEpisode;

    @Override
    public void onEpisodeSelected(Episode selectedEpisode) {
        this.currentEpisode = selectedEpisode;

        switch (viewMode) {
            case LARGE_PORTRAIT_VIEW:
            case LARGE_LANDSCAPE_VIEW:
                // Set episode in episode fragment
                findEpisodeFragment().setEpisode(selectedEpisode);
                // Make sure selection matches in list fragment
                // findEpisodeListFragment().selectEpisode(selectedEpisode);
                break;
            case SMALL_LANDSCAPE_VIEW:
                // Find, and if not already done create, episode fragment
                EpisodeFragment episodeFragment = findEpisodeFragment();
                if (episodeFragment == null)
                    episodeFragment = new EpisodeFragment();
                // Add the fragment to the UI, placing the list fragment
                FragmentTransaction transaction = getFragmentManager().beginTransaction();
                transaction.replace(R.id.content, episodeFragment, episodeFragmentTag);
                transaction.addToBackStack(null);
                transaction.commit();
                // Set the episode
                episodeFragment.setEpisode(selectedEpisode);
                break;
            case SMALL_PORTRAIT_VIEW:
                // Send intent to open episode as a new activity
                Intent intent = new Intent(this, ShowEpisodeActivity.class);
                intent.putExtra(PODCAST_URL_KEY, selectedEpisode.getPodcastUrl());
                intent.putExtra(EPISODE_URL_KEY, selectedEpisode.getMediaUrl().toExternalForm());

                startActivity(intent);
        }
    }

    @Override
    public void onNoEpisodeSelected() {
        this.currentEpisode = null;

        // If there is a episode fragment, reset it
        EpisodeListFragment episodeListFragment = findEpisodeListFragment();
        if (episodeListFragment != null)
            episodeListFragment.selectNone();
    }
}
