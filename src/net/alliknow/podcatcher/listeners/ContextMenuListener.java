package net.alliknow.podcatcher.listeners;

import net.alliknow.podcatcher.model.types.Episode;
import net.alliknow.podcatcher.model.types.Podcast;

public interface ContextMenuListener {

    void onEpisodeContextMenuOpen(Episode episode);
    void onEpisodeContextMenuClose();

    void onPodcastContextMenuOpen(Podcast podcast);
    void onPodcastContextMenuClose();

    void onPodcastListFocused();

}
