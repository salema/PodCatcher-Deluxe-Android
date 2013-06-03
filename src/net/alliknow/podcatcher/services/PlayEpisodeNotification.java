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

import static net.alliknow.podcatcher.EpisodeActivity.EPISODE_URL_KEY;
import static net.alliknow.podcatcher.EpisodeListActivity.PODCAST_URL_KEY;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

import net.alliknow.podcatcher.BaseActivity.ContentMode;
import net.alliknow.podcatcher.EpisodeListActivity;
import net.alliknow.podcatcher.PodcastActivity;
import net.alliknow.podcatcher.R;
import net.alliknow.podcatcher.model.types.Episode;

/**
 * Helper class for the {@link PlayEpisodeService} to encapsulate the complexity
 * of notifications.
 */
public class PlayEpisodeNotification {
    /** The single instance */
    private static PlayEpisodeNotification instance;

    private Context context;

    private final Intent appIntent;
    private final PendingIntent backToAppIntent;

    private final PendingIntent pauseIntent;
    private final PendingIntent nextIntent;

    private PlayEpisodeNotification(Context context) {
        this.context = context;

        appIntent = new Intent(context, PodcastActivity.class)
                .putExtra(EpisodeListActivity.MODE_KEY, ContentMode.SINGLE_PODCAST)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
                        Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        backToAppIntent = PendingIntent.getActivity(context, 0,
                appIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        pauseIntent = PendingIntent.getService(context, 0,
                new Intent(PlayEpisodeService.ACTION_TOGGLE), PendingIntent.FLAG_UPDATE_CURRENT);

        nextIntent = PendingIntent.getService(context, 0,
                new Intent(PlayEpisodeService.ACTION_SKIP), PendingIntent.FLAG_UPDATE_CURRENT);
    }

    /**
     * Get the single instance representing the service notification.
     * 
     * @param context The context notifications should life in.
     * @return The single instance.
     */
    public static PlayEpisodeNotification getInstance(Context context) {
        if (instance == null)
            instance = new PlayEpisodeNotification(context);

        return instance;
    }

    public Notification build(Episode episode) {
        return build(episode, 0, 0);
    }

    public Notification build(Episode episode, int position, int duration) {
        appIntent.putExtra(PODCAST_URL_KEY, episode.getPodcast().getUrl().toString());
        appIntent.putExtra(EPISODE_URL_KEY, episode.getMediaUrl().toString());

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context)
                .setContentIntent(backToAppIntent)
                .setTicker(episode.getName())
                .setSmallIcon(R.drawable.ic_stat)
                .setContentTitle(episode.getName())
                .setContentText(episode.getPodcast().getName())
                .setContentInfo(context.getString(R.string.app_name))
                .setWhen(0)
                .setProgress(duration, position, false)
                .setOngoing(true)
                .addAction(R.drawable.ic_media_pause, null, pauseIntent)
                // TODO Make sure this listens to playlist changes
                .addAction(R.drawable.ic_media_next, null, nextIntent);

        if (episode.getPodcast().getLogo() == null)
            return notificationBuilder.build();
        else
            return new NotificationCompat.BigPictureStyle(notificationBuilder).bigPicture(
                    episode.getPodcast().getLogo()).build();
    }
}
