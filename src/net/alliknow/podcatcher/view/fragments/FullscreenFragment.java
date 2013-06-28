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

import android.app.Activity;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.MediaController;
import android.widget.MediaController.MediaPlayerControl;
import android.widget.VideoView;

import net.alliknow.podcatcher.R;

/**
 * Fragment for fullscreen video playback.
 */
public class FullscreenFragment extends DialogFragment implements VideoSurfaceProvider {

    /** The activity we live in */
    private OnCancelListener listener;
    /** The media controller to use in overlay */
    private MediaPlayerControl control;
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

    /**
     * Make dialog aware of the media controller to use. Needs to be set in
     * order to enable overlay controls for the video.
     * 
     * @param control The control to use.
     */
    public void setMediaPlayerControl(MediaPlayerControl control) {
        this.control = control;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // Make sure our listener is present
        try {
            this.listener = (OnCancelListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnCancelListener");
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setStyle(STYLE_NO_FRAME, android.R.style.Theme_Holo_Light_NoActionBar_Fullscreen);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fullscreen_video, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        videoView = (VideoView) getView().findViewById(R.id.episode_video);
        videoView.getHolder().addCallback(videoCallback);

        if (control != null) {
            Log.i("VIDEO", "Controller added");
            final MediaController controller = new MediaController(getActivity());
            controller.setMediaPlayer(control);
            controller.setAnchorView(videoView);

            videoView.setMediaController(controller);
        }
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        // Make sure the parent activity knows when we are closing
        listener.onCancel(dialog);
    }

    @Override
    public SurfaceHolder getVideoSurface() {
        return videoView.getHolder();
    }

    @Override
    public boolean isVideoSurfaceAvailable() {
        return videoSurfaceAvailable;
    }

    @Override
    public void adjustToVideoSize(int width, int height) {
        LayoutParams layoutParams = videoView.getLayoutParams();

        layoutParams.height = (int) (((float) height / (float) width) *
                (float) videoView.getWidth());
        Log.i(getClass().getSimpleName(), "Video view height set to " + height);

        videoView.setLayoutParams(layoutParams);
    }
}
