
package net.alliknow.podcatcher.listeners;

import net.alliknow.podcatcher.model.types.Episode;

import java.util.List;

public interface OnLoadPlaylistListener {

    public void onPlaylistLoaded(List<Episode> playlist);

}
