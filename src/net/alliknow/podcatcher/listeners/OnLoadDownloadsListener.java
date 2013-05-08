
package net.alliknow.podcatcher.listeners;

import net.alliknow.podcatcher.model.types.Episode;

import java.util.List;

public interface OnLoadDownloadsListener {

    public void onDownloadsLoaded(List<Episode> downloads);

}
