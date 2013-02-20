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

package net.alliknow.podcatcher.model.tasks.remote;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import net.alliknow.podcatcher.listeners.OnLoadPodcastLogoListener;
import net.alliknow.podcatcher.model.types.Podcast;
import net.alliknow.podcatcher.model.types.Progress;

/**
 * An async task to load a podcast logo. Implement
 * {@link OnLoadPodcastLogoListener} to be alerted on completion or failure.
 */
public class LoadPodcastLogoTask extends LoadRemoteFileTask<Podcast, Bitmap> {

    /** Maximum byte size for the logo to load on wifi */
    public static final int MAX_LOGO_SIZE_WIFI = 250000;
    /** Maximum byte size for the logo to load on mobile connection */
    public static final int MAX_LOGO_SIZE_MOBILE = 100000;

    /** Owner */
    private OnLoadPodcastLogoListener listener;
    /** Podcast currently loading */
    private Podcast podcast;

    /** Dimensions we decode the logo image file to (saves memory in places) */
    protected final int requestedWidth;
    /** The height */
    protected final int requestedHeight;

    /**
     * Create new task.
     * 
     * @param listener Callback to be alerted on progress and completion. This
     *            will not be leaked if you keep a handle on this task, but set
     *            to <code>null</code> after execution.
     * @param requestedWidth Width to sample result image to.
     * @param requestedHeight Height to sample result image to.
     */
    public LoadPodcastLogoTask(OnLoadPodcastLogoListener listener, int requestedWidth,
            int requestedHeight) {
        this.listener = listener;

        this.requestedWidth = requestedWidth;
        this.requestedHeight = requestedHeight;
    }

    @Override
    protected Bitmap doInBackground(Podcast... podcasts) {
        this.podcast = podcasts[0];

        try {
            // 1. Get logo data
            byte[] logo = loadFile(podcast.getLogoUrl());

            // 2. Decode and sample the result
            if (!isCancelled())
                return decodeAndSampleBitmap(logo);
        } catch (Exception e) {
            failed = true;

            Log.w(getClass().getSimpleName(), "Logo failed to load for podcast \"" + podcasts[0]
                    + "\" with " + "logo URL " + podcasts[0].getLogoUrl(), e);
        } finally {
            publishProgress(Progress.DONE);
        }

        return null;
    }

    @Override
    protected void onPostExecute(Bitmap result) {
        // Background task failed to complete
        if (failed || result == null) {
            if (listener != null)
                listener.onPodcastLogoLoadFailed(podcast);
            else
                Log.w(getClass().getSimpleName(),
                        "Podcast logo loading failed, but no listener attached");
        } // Podcast logo was loaded
        else {
            if (listener != null)
                listener.onPodcastLogoLoaded(podcast, result);
            else
                Log.w(getClass().getSimpleName(), "Podcast logo loaded, but no listener attached");
        }
    }

    /**
     * Create a memory-efficient bitmap at the correct size needed for the
     * application. If the bitmap is larger than width or height given at
     * {@link #LoadPodcastLogoTask(OnLoadPodcastLogoListener, int, int)}, it
     * will be sample down.
     * 
     * @param data Bitmap data loaded from the internet.
     * @return The decoded and sampled bitmap.
     */
    protected Bitmap decodeAndSampleBitmap(byte[] data) {
        final BitmapFactory.Options options = new BitmapFactory.Options();

        // If requested dimensions are meaningful, calculate sample size
        if (requestedHeight > 0 && requestedWidth > 0) {
            // Decode with inJustDecodeBounds=true to check dimensions
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(data, 0, data.length, options);

            // Calculate inSampleSize
            options.inSampleSize = calculateInSampleSize(options);

            // Decode bitmap with inSampleSize set
            options.inJustDecodeBounds = false;
        }

        return BitmapFactory.decodeByteArray(data, 0, data.length, options);
    }

    /**
     * Calculate the sample size for the image.
     * 
     * @param options Bitmap options to work with.
     * @return The sample size.
     */
    protected int calculateInSampleSize(BitmapFactory.Options options) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > requestedHeight || width > requestedWidth) {
            if (width > height) {
                inSampleSize = Math.round((float) height / (float) requestedHeight);
            } else {
                inSampleSize = Math.round((float) width / (float) requestedWidth);
            }
        }
        return inSampleSize;
    }
}
