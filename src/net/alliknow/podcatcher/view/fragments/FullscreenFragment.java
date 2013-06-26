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

package net.alliknow.podcatcher.view.fragments;

import android.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.ViewGroup;
import android.widget.VideoView;

import net.alliknow.podcatcher.R;

/**
 * Fragment for fullscreen video playback.
 */
public class FullscreenFragment extends DialogFragment {

    /** The episode video view */
    private VideoView videoView;

    /** Flag to indicate whether video surface is available */
    private boolean videoSurfaceAvailable = false;
    /** Our video surface holder callback to update availability */
    private VideoCallback videoCallback = new VideoCallback();

    /** The callback implementation */
    private final class VideoCallback implements SurfaceHolder.Callback {
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            videoSurfaceAvailable = true;
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            // pass
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            videoSurfaceAvailable = false;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setStyle(STYLE_NO_FRAME, android.R.style.Theme_Holo_Light_NoActionBar_Fullscreen);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        // Inflate the layout
        final View layout = inflater.inflate(R.layout.fullscreen_video, container, false);

        // // Get the display dimensions
        // Rect displayRectangle = new Rect();
        // getActivity().getWindow().getDecorView().getWindowVisibleDisplayFrame(displayRectangle);
        //
        // // Adjust the layout minimum height so the dialog always has the same
        // // height and does not bounce around depending on the list content
        // layout.setMinimumHeight((int) (displayRectangle.height() * 0.9f));

        return layout;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        videoView = (VideoView) getView().findViewById(R.id.episode_video);
        videoView.getHolder().addCallback(videoCallback);
    }

    /**
     * @return The surface holder for the video view.
     */
    public SurfaceHolder getSurfaceHolder() {
        return videoView.getHolder();
    }

    /**
     * @return Whether the surface is available for playback.
     */
    public boolean isVideoSurfaceAvailable() {
        return videoSurfaceAvailable;
    }
}
