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

package net.alliknow.podcatcher.model.types;

import net.alliknow.podcatcher.model.EpisodeManager;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;

/**
 * Instances of this type represent additional information on episodes that is
 * not derived from the podcast feed, but from the user's interaction with the
 * episode, such as downloaded files, resume times, old/new status. This should
 * not be used outside the model, use {@link EpisodeManager} instead.
 */
public class EpisodeMetadata {

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
    /** The download manager id for this episode. */
    public Long downloadId;
    /** The absolute local filepath to the downloaded copy of this episode. */
    public String filePath;
    /** The time in millis to resume episode playback at */
    public Integer resumeAt;

    /**
     * @return Whether the metadata is actually need because it has any data.
     */
    public boolean hasData() {
        return downloadId != null ||
                filePath != null ||
                resumeAt != null;
    }

    /**
     * Create an actual episode object from the metadata.
     * 
     * @param episodeUrl URL for the new episode to be identified by.
     * @return An episode object or <code>null</code> if something goes wrong.
     */
    public Episode marshalEpisode(URL episodeUrl) {
        // Create the corresponding podcast
        Podcast podcast;
        try {
            podcast = new Podcast(podcastName, new URL(podcastUrl));
        } catch (MalformedURLException e) {
            return null;
        }

        // Create and return the episode
        return new Episode(podcast, episodeName, episodeUrl, episodePubDate, episodeDescription);
    }
}
