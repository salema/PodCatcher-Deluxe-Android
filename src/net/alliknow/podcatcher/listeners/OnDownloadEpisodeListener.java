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

package net.alliknow.podcatcher.listeners;

import net.alliknow.podcatcher.model.types.Episode;

/**
 * Interface for the controller to implement when the user requests an episode
 * to be downloaded locally.
 */
public interface OnDownloadEpisodeListener {

    /**
     * Start/stop the download for the current episode.
     */
    public void onToggleDownload();

    /**
     * Called on the listener once a download finished successfully.
     */
    public void onDownloadSuccess();

    /**
     * Called on the listener if a download failed.
     */
    public void onDownloadFailed();

    /**
     * Called on the listener if a download is removed.
     * 
     * @param episode Episode download is removed for.
     */
    public void onDownloadDeleted(Episode episode);
}
