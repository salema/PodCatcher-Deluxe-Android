/** Copyright 2012-2014 Kevin Hausmann
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

package net.alliknow.podcatcher.model.types;

import net.alliknow.podcatcher.model.EpisodeManager;
import net.alliknow.podcatcher.model.PodcastManager;

import java.util.Date;

/**
 * Instances of this type represent additional information on episodes that is
 * not derived from the podcast feed, but from the user's interaction with the
 * episode, such as downloaded files, resume times, old/new status. This should
 * not be used outside the model, use {@link EpisodeManager} instead.
 */
public class EpisodeMetadata {

    /** The download manager id for this episode. */
    public Long downloadId;
    /** The absolute local filepath to the downloaded copy of this episode. */
    public String filePath;
    /** The time in millis to resume episode playback at */
    public Integer resumeAt;
    /** The state information (old/new) for the episode */
    public Boolean isOld;
    /** The playlist position for the episode */
    public Integer playlistPosition;

    /**
     * Extra information that is only valid when the app runs and is not saved.
     */
    /** The progress made downloading the episode */
    public int downloadProgress = -1;

    /**
     * Extra information to make it possible to actually display an episode not
     * available from any podcast. It is not essential for the metadata record
     * and is only needed if the episode is downloaded.
     */
    /** The name of the podcast this episode belongs to */
    public String podcastName;
    /** The URL of the podcast this episode belongs to */
    public String podcastUrl;
    /** The episode name for this metadata */
    public String episodeName;
    /** The episode publication date for this metadata */
    public Date episodePubDate;
    /** The episode description for this metadata */
    public String episodeDescription;

    /**
     * @return Whether the metadata is actually need because it has any data.
     */
    public boolean hasData() {
        return downloadId != null ||
                filePath != null ||
                resumeAt != null ||
                isOld != null ||
                playlistPosition != null;
    }

    /**
     * @return Whether the metadata has no information other then for the
     *         episode's state, i.e. <code>isOld</code> and/or
     *         <code>resumeAt</code>.
     */
    public boolean hasOnlyStateData() {
        return downloadId == null &&
                filePath == null &&
                playlistPosition == null;
    }

    /**
     * Create an actual episode object from the metadata.
     * 
     * @param episodeUrl URL for the new episode to be identified by.
     * @return An episode object or <code>null</code> if something goes wrong.
     */
    public Episode marshalEpisode(String episodeUrl) {
        PodcastManager manager = PodcastManager.getInstance();

        // Try to get episode from the podcast manager
        Episode result = manager.findEpisodeForUrl(episodeUrl, podcastUrl);
        // No luck, create episode
        if (result == null) {
            // Try to get podcast from the podcast manager
            Podcast podcast = manager.findPodcastForUrl(podcastUrl);
            // No luck, create podcast
            if (podcast == null)
                podcast = new Podcast(podcastName, podcastUrl);

            // Create the episode
            result = new Episode(podcast, episodeName, episodeUrl, episodePubDate,
                    episodeDescription);
        }

        return result;
    }
}
