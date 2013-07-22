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

package net.alliknow.podcatcher.services;

import static android.media.MediaMetadataRetriever.METADATA_KEY_ARTIST;
import static android.media.MediaMetadataRetriever.METADATA_KEY_DATE;
import static android.media.MediaMetadataRetriever.METADATA_KEY_DURATION;
import static android.media.MediaMetadataRetriever.METADATA_KEY_TITLE;
import static android.media.RemoteControlClient.MetadataEditor.BITMAP_KEY_ARTWORK;

import android.app.PendingIntent;
import android.media.RemoteControlClient;

import net.alliknow.podcatcher.model.types.Episode;
import net.alliknow.podcatcher.view.Utils;

/**
 * Our remote control client used to provide playback information to a remote
 * control that might be present and able to display some episode metadata.
 */
public class PodcatcherRCClient extends RemoteControlClient {

    /** The supported transport modes for the remote control */
    private static final int SUPPORTED_TRANSPORTS = FLAG_KEY_MEDIA_PLAY_PAUSE
            | FLAG_KEY_MEDIA_PAUSE | FLAG_KEY_MEDIA_PLAY | FLAG_KEY_MEDIA_STOP
            | FLAG_KEY_MEDIA_PREVIOUS | FLAG_KEY_MEDIA_REWIND | FLAG_KEY_MEDIA_FAST_FORWARD;

    /**
     * Create the remote control client.
     * 
     * @param mediaButtonIntent The pending intent to use for the media buttons.
     * @param episode The episode to get metadata from.
     */
    public PodcatcherRCClient(PendingIntent mediaButtonIntent, Episode episode) {
        super(mediaButtonIntent);

        setTransportControlFlags(SUPPORTED_TRANSPORTS);
        setMetadata(episode);
    }

    private void setMetadata(Episode episode) {
        if (episode != null) {
            MetadataEditor editor = editMetadata(true);

            editor.putString(METADATA_KEY_TITLE, episode.getName());
            editor.putString(METADATA_KEY_DATE, Utils.getRelativePubDate(episode));
            editor.putLong(METADATA_KEY_DURATION, episode.getDuration() * 1000);

            if (episode.getPodcast() != null) {
                editor.putString(METADATA_KEY_ARTIST, episode.getPodcast().getName());

                if (episode.getPodcast().getLogo() != null)
                    editor.putBitmap(BITMAP_KEY_ARTWORK, episode.getPodcast().getLogo());
            }

            editor.apply();
        }
    }
}
