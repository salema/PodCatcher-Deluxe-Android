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

import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.os.Bundle;

import net.alliknow.podcatcher.listeners.OnAddPodcastListener;
import net.alliknow.podcatcher.listeners.OnLoadPodcastListener;
import net.alliknow.podcatcher.model.tasks.Progress;
import net.alliknow.podcatcher.model.types.Podcast;
import net.alliknow.podcatcher.view.fragments.AddPodcastFragment;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Add new podcast(s) activity.
 */
public class AddPodcastActivity extends PodcatcherBaseActivity
        implements OnLoadPodcastListener, OnAddPodcastListener, OnCancelListener {

    /** The tag we identify our add podcast fragment with */
    private static final String ADD_PODCAST_FRAGMENT_TAG = "add_podcast";

    /** The fragment containing the add URL UI */
    private AddPodcastFragment addPodcastFragment;

    /** Key to find current load url under */
    private static final String LOADING_URL_KEY = "LOADING_URL";
    /** The URL of the podcast we are currently loading (if any) */
    private String currentLoadUrl;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Listen to podcast load events to update UI
        podcastManager.addLoadPodcastListener(this);

        // If we are coming from a config change, we need to know whether there
        // is currently a podcast loading.
        if (savedInstanceState != null)
            currentLoadUrl = savedInstanceState.getString(LOADING_URL_KEY);
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Try to find existing fragment
        addPodcastFragment = (AddPodcastFragment) getFragmentManager().findFragmentByTag(
                ADD_PODCAST_FRAGMENT_TAG);

        // No fragment found, create it
        if (addPodcastFragment == null) {
            addPodcastFragment = new AddPodcastFragment();

            // Show the fragment
            addPodcastFragment.show(getFragmentManager(), ADD_PODCAST_FRAGMENT_TAG);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // Make sure we know which podcast we are loading (if any)
        outState.putString(LOADING_URL_KEY, currentLoadUrl);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Unregister from data manager
        podcastManager.removeLoadPodcastListener(this);
    }

    @Override
    public void addPodcast(String podcastUrl) {
        // Try to load the given online resource
        try {
            // TODO Handle the case were given podcast is already added
            currentLoadUrl = podcastUrl;
            podcastManager.load(new Podcast(null, new URL(podcastUrl)));
        } catch (MalformedURLException e) {
            onPodcastLoadFailed(null);
        }
    }

    @Override
    public void onPodcastLoadProgress(Podcast podcast, Progress progress) {
        if (isCurrentlyLoadingPodcast(podcast))
            addPodcastFragment.showProgress(progress);
    }

    @Override
    public void onPodcastLoaded(Podcast podcast) {
        if (isCurrentlyLoadingPodcast(podcast)) {
            // Reset current load url
            currentLoadUrl = null;

            // We do not allow empty podcast to be added (TODO Does this make
            // sense?)
            if (podcast.getEpisodes().isEmpty())
                onPodcastLoadFailed(podcast);
            // This is an actual podcast, add it
            else {
                podcastManager.addPodcast(podcast);
                finish();
            }
        }
    }

    @Override
    public void onPodcastLoadFailed(Podcast podcast) {
        if (isCurrentlyLoadingPodcast(podcast)) {
            // Reset current load url
            currentLoadUrl = null;

            // Show failed UI
            addPodcastFragment.showPodcastLoadFailed();
        }
    }

    @Override
    public void showSuggestions() {
        addPodcastFragment.dismiss();
        finish();

        // TODO what happens if we are currently loading?
        startActivity(new Intent(this, AddSuggestionActivity.class));
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        // TODO Cancel the load task in podcast manager if running
        finish();
    }

    private boolean isCurrentlyLoadingPodcast(Podcast podcast) {
        return podcast.getUrl().toExternalForm().equalsIgnoreCase(currentLoadUrl);
    }
}
