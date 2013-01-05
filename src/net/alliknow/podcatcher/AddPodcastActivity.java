/** Copyright 2012 Kevin Hausmann
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

import net.alliknow.podcatcher.listeners.OnAddPodcastListener;
import net.alliknow.podcatcher.listeners.OnLoadPodcastListener;
import net.alliknow.podcatcher.model.tasks.Progress;
import net.alliknow.podcatcher.model.tasks.remote.LoadPodcastTask;
import net.alliknow.podcatcher.model.types.Podcast;
import net.alliknow.podcatcher.view.fragments.AddPodcastFragment;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Add a new podcast activity.
 */
public class AddPodcastActivity extends PodcatcherBaseActivity
        implements OnLoadPodcastListener, OnAddPodcastListener {

    /** The podcast load task */
    private LoadPodcastTask loadTask;

    private AddPodcastFragment addPodcastFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPodcastFragment = new AddPodcastFragment();
        addPodcastFragment.show(getFragmentManager(), null);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (loadTask != null)
            loadTask.cancel(true);
    }

    @Override
    public void addPodcast(String podcastUrl) {
        // Try to load the given online resource
        try {
            loadTask = new LoadPodcastTask(this);
            loadTask.preventZippedTransfer(((Podcatcher) getApplication())
                    .isOnFastConnection());
            loadTask.execute(new Podcast(null, new URL(podcastUrl)));
        } catch (MalformedURLException e) {
            onPodcastLoadFailed(null);
        }
    }

    @Override
    public void onPodcastLoadProgress(Podcast podcast, Progress progress) {
        addPodcastFragment.showProgress(progress);
    }

    @Override
    public void onPodcastLoaded(Podcast podcast) {
        // We do not allow empty podcast to be added (TODO Does this make
        // sense?)
        if (podcast.getEpisodes().isEmpty())
            onPodcastLoadFailed(podcast);
        // This is an actual podcast, add it
        else {
            dataManager.addPodcast(podcast);
            // Only if in tablet mode... selectPodcast(newPodcast);
            finish();
        }
    }

    @Override
    public void onPodcastLoadFailed(Podcast podcast) {
        addPodcastFragment.showPodcastLoadFailed();
    }

    @Override
    public void dismiss() {
        finish();
    }

    @Override
    public void showSuggestions() {
        // TODO Auto-generated method stub

    }
}
