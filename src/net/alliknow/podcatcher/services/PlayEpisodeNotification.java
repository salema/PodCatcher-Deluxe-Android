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

package net.alliknow.podcatcher.services;

import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;
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
import net.alliknow.podcatcher.model.types.Episode;
import net.alliknow.podcatcher.model.types.Podcast;

import java.util.HashMap;
import java.util.Map;

/**
 * Helper class for the {@link PlayEpisodeService} to encapsulate the complexity
 * of notifications.
 */
public class PlayEpisodeNotification {

    /** The single instance */
    private static PlayEpisodeNotification instance;
    /** The context the notifications live in */
    private Context context;

    /** The actual intent that brings back the app */
    private final Intent appIntent;
    /** The pending intents for the actions */
    private final PendingIntent stopPendingIntent;
    private final PendingIntent tooglePendingIntent;

    /** Our builder */
    private NotificationCompat.Builder notificationBuilder;
    /** The cache for the scaled bitmaps */
    private Map<String, Bitmap> bitmapCache = new HashMap<String, Bitmap>();

    private PlayEpisodeNotification(Context context) {
        this.context = context;

        // Create all the static intents we need for every build
        appIntent = new Intent(context, PodcastActivity.class)
                .putExtra(EpisodeListActivity.MODE_KEY, ContentMode.SINGLE_PODCAST)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
                        Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        final Intent stopIntent = new Intent(context, PlayEpisodeService.class);
        stopIntent.setAction(PlayEpisodeService.ACTION_STOP);
        stopPendingIntent = PendingIntent.getService(context, 0, stopIntent,
                FLAG_UPDATE_CURRENT);

        final Intent toogleIntent = new Intent(context, PlayEpisodeService.class);
        toogleIntent.setAction(PlayEpisodeService.ACTION_TOGGLE);
        tooglePendingIntent = PendingIntent.getService(context, 0, toogleIntent,
                FLAG_UPDATE_CURRENT);
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

    /**
     * Build a new notification using default values for all but the episode.
     * 
     * @param episode The episode playing.
     * @return The notification to display.
     * @see #build(Episode, boolean, int, int)
     */
    public Notification build(Episode episode) {
        return build(episode, false, 0, 0);
    }

    /**
     * Build a new notification. To update the progress on the notification, use
     * {@link #updateProgress(int, int)} instead.
     * 
     * @param episode The episode playing.
     * @param paused Playback state, <code>true</code> for paused.
     * @param position The current playback progress.
     * @param duration The length of the current episode.
     * @return The notification to display.
     */
    public Notification build(Episode episode, boolean paused, int position, int duration) {
        // Prepare the main intent (leading back to the app)
        appIntent.putExtra(PODCAST_URL_KEY, episode.getPodcast().getUrl());
        appIntent.putExtra(EPISODE_URL_KEY, episode.getMediaUrl());
        final PendingIntent backToAppIntent = PendingIntent.getActivity(context, 0,
                appIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        // Create the notification builder and set values
        notificationBuilder = new NotificationCompat.Builder(context)
                .setContentIntent(backToAppIntent)
                .setTicker(episode.getName())
                .setSmallIcon(R.drawable.ic_stat)
                .setContentTitle(episode.getName())
                .setContentText(episode.getPodcast().getName())
                .setWhen(0)
                .setProgress(duration, position, false)
                .setOngoing(true);

        // Add stop action
        notificationBuilder.addAction(R.drawable.ic_media_stop,
                context.getString(R.string.stop), stopPendingIntent);

        // Add other actions according to playback state
        if (paused)
            notificationBuilder.addAction(R.drawable.ic_media_resume,
                    context.getString(R.string.resume), tooglePendingIntent);
        else
            notificationBuilder.addAction(R.drawable.ic_media_pause,
                    context.getString(R.string.pause), tooglePendingIntent);

        // Apply the notification style
        if (isLargeDevice() && isPodcastLogoAvailable(episode))
            notificationBuilder.setStyle(new NotificationCompat.BigPictureStyle()
                    .bigPicture(episode.getPodcast().getLogo()));
        else if (isPodcastLogoAvailable(episode))
            notificationBuilder.setLargeIcon(getScaledBitmap(episode.getPodcast()));

        return notificationBuilder.build();
    }

    /**
     * Update the last notification build with a new progress and duration and
     * rebuild it leaving all the other data intact. Only call this after having
     * called one of the build() methods before.
     * 
     * @param position The new progrss position.
     * @param duration The length of the current episode.
     * @return The updated notification to display.
     */
    public Notification updateProgress(int position, int duration) {
        notificationBuilder.setProgress(duration, position, false);

        return notificationBuilder.build();
    }

    private boolean isPodcastLogoAvailable(Episode episode) {
        return episode.getPodcast().isLogoCached();
    }

    private boolean isLargeDevice() {
        return context.getResources().getConfiguration().smallestScreenWidthDp >= Podcatcher.MIN_PIXEL_LARGE;
    }

    private Bitmap getScaledBitmap(Podcast podcast) {
        final String cacheKey = podcast.getUrl();

        if (!bitmapCache.containsKey(cacheKey)) {
            final Resources res = context.getResources();
            int height = (int) res.getDimension(android.R.dimen.notification_large_icon_height);
            int width = (int) res.getDimension(android.R.dimen.notification_large_icon_width);

            bitmapCache.put(cacheKey,
                    Bitmap.createScaledBitmap(podcast.getLogo(), width, height, false));
        }

        return bitmapCache.get(cacheKey);
    }
}
