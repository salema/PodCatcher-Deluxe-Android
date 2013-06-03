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
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.support.v4.app.NotificationCompat;

import net.alliknow.podcatcher.BaseActivity.ContentMode;
import net.alliknow.podcatcher.EpisodeListActivity;
import net.alliknow.podcatcher.PodcastActivity;
import net.alliknow.podcatcher.Podcatcher;
import net.alliknow.podcatcher.R;
import net.alliknow.podcatcher.model.EpisodeManager;
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

    private final PendingIntent tooglePlayIntent;
    private final PendingIntent playNextIntent;

    private NotificationCompat.Builder notificationBuilder;

    private PlayEpisodeNotification(Context context) {
        this.context = context;

        appIntent = new Intent(context, PodcastActivity.class)
                .putExtra(EpisodeListActivity.MODE_KEY, ContentMode.SINGLE_PODCAST)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
                        Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        Intent toogleIntent = new Intent(context, PlayEpisodeService.class);
        toogleIntent.setAction(PlayEpisodeService.ACTION_TOGGLE);
        tooglePlayIntent = PendingIntent.getService(context, 0,
                toogleIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent nextIntent = new Intent(context, PlayEpisodeService.class);
        nextIntent.setAction(PlayEpisodeService.ACTION_SKIP);
        playNextIntent = PendingIntent.getService(context, 0,
                nextIntent, PendingIntent.FLAG_UPDATE_CURRENT);
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
        return build(episode, false, 0, 0);
    }

    public Notification build(Episode episode, boolean paused, int position, int duration) {
        appIntent.putExtra(PODCAST_URL_KEY, episode.getPodcast().getUrl().toString());
        appIntent.putExtra(EPISODE_URL_KEY, episode.getMediaUrl().toString());

        final PendingIntent backToAppIntent = PendingIntent.getActivity(context, 0,
                appIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        notificationBuilder = new NotificationCompat.Builder(context)
                .setContentIntent(backToAppIntent)
                .setTicker(episode.getName())
                .setSmallIcon(R.drawable.ic_stat)
                .setContentTitle(episode.getName())
                .setContentText(episode.getPodcast().getName())
                .setWhen(0)
                .setProgress(duration, position, false)
                .setOngoing(true);

        if (paused)
            notificationBuilder.addAction(R.drawable.ic_media_play, "Play", tooglePlayIntent);
        else
            notificationBuilder.addAction(R.drawable.ic_media_pause, "Pause", tooglePlayIntent);

        if (!EpisodeManager.getInstance().isPlaylistEmpty())
            notificationBuilder.addAction(R.drawable.ic_media_next, "Next", playNextIntent);

        if (isLargeDevice() && isPodcastLogoAvailable(episode)) {
            notificationBuilder.setStyle(new NotificationCompat.BigPictureStyle()
                    .bigPicture(episode.getPodcast().getLogo()));
        } else if (isPodcastLogoAvailable(episode)) {
            Resources res = context.getResources();
            int height = (int) res.getDimension(android.R.dimen.notification_large_icon_height);
            int width = (int) res.getDimension(android.R.dimen.notification_large_icon_width);
            Bitmap scaled = Bitmap.createScaledBitmap(episode.getPodcast().getLogo(), width,
                    height, false);

            notificationBuilder.setLargeIcon(scaled);
        }

        return notificationBuilder.build();
    }

    public Notification updateProgress(int position, int duration) {
        notificationBuilder.setProgress(duration, position, false);

        return notificationBuilder.build();
    }

    private boolean isPodcastLogoAvailable(Episode episode) {
        return episode.getPodcast().getLogo() != null;
    }

    private boolean isLargeDevice() {
        return context.getResources().getConfiguration().smallestScreenWidthDp >= Podcatcher.MIN_PIXEL_LARGE;
    }
}
