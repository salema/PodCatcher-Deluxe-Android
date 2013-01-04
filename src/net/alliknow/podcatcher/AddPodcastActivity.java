
package net.alliknow.podcatcher;

import net.alliknow.podcatcher.listeners.OnAddPodcastListener;
import net.alliknow.podcatcher.model.types.Podcast;

public class AddPodcastActivity extends PodcatcherBaseActivity
        implements OnAddPodcastListener {

    @Override
    public void addPodcast(Podcast newPodcast) {
        // Notify data fragment
        dataManager.addPodcast(newPodcast);
        // Update the list
        // setListAdapter(new PodcastListAdapter(getActivity(),
        // data.getPodcastList()));

        // Only if in tablet mode... selectPodcast(newPodcast);
    }

    @Override
    public void showSuggestions() {
        // TODO Auto-generated method stub

    }
}
