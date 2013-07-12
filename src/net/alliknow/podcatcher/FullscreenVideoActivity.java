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

package net.alliknow.podcatcher;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.MediaController;

import net.alliknow.podcatcher.listeners.PlayServiceListener;
import net.alliknow.podcatcher.services.PlayEpisodeService;
import net.alliknow.podcatcher.services.PlayEpisodeService.PlayServiceBinder;
import net.alliknow.podcatcher.view.fragments.VideoSurfaceProvider;

/**
 * Show fullscreen video activity.
 */
public class FullscreenVideoActivity extends BaseActivity implements VideoSurfaceProvider,
        PlayServiceListener {

    /** Play service */
    protected PlayEpisodeService service;
    /** The media controller overlay */
    private MediaController controller;

    /** Flag to indicate whether video surface is available */
    private boolean videoSurfaceAvailable = false;
    /** Our video surface holder callback to update availability */
    private VideoCallback videoCallback = new VideoCallback();

    /** The video view */
    private SurfaceView videoView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // We only get to work if fullscreen playback is actually enabled.
        if (selection.isFullscreenEnabled()) {
            setContentView(R.layout.fullscreen_video);

            videoView = (SurfaceView) findViewById(R.id.episode_video);
            videoView.getHolder().addCallback(videoCallback);

            // Attach to play episode service
            Intent intent = new Intent(this, PlayEpisodeService.class);
            bindService(intent, connection, 0);
        }
        // Stop ourselves
        else
            finish();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (controller != null)
            controller.show();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (controller != null)
            controller.show();

        return false;
    }

    @Override
    public void finish() {
        selection.setFullscreenEnabled(false);

        super.finish();
    }

    @Override
    protected void onDestroy() {
        videoView.getHolder().removeCallback(videoCallback);

        // Detach from play service (prevents leaking)
        if (service != null) {
            service.removePlayServiceListener(this);
            unbindService(connection);
        }

        super.onDestroy();
    }

    @Override
    public void onVideoAvailable() {
        // pass
    }

    @Override
    public void onPlaybackStarted() {
        // If we go to the next episode and that one has no video content, close
        // the activity.
        if (service != null && !service.isVideo())
            finish();
        // Update the controller
        else if (controller != null) {
            controller.show();
            attachPrevNextListeners();
        }
    }

    @Override
    public void onPlaybackStateChanged() {
        if (controller != null)
            controller.show();
    }

    @Override
    public void onStopForBuffering() {
        // pass
    }

    @Override
    public void onResumeFromBuffering() {
        // pass
    }

    @Override
    public void onBufferUpdate(int seconds) {
        // pass
    }

    @Override
    public void onPlaybackComplete() {
        // pass
    }

    @Override
    public void onError() {
        // Close activity on error
        finish();
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

        videoView.setLayoutParams(layoutParams);
    }

    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection connection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder serviceBinder) {
            service = ((PlayServiceBinder) serviceBinder).getService();
            // Attach the listeners and provide our surface
            service.setVideoSurfaceProvider(FullscreenVideoActivity.this);
            service.addPlayServiceListener(FullscreenVideoActivity.this);

            // Create the media controller to show when the surface is touched
            controller = new MediaController(FullscreenVideoActivity.this);
            controller.setMediaPlayer(service);
            controller.setAnchorView(videoView);
            attachPrevNextListeners();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            // Nothing to do here
        }
    };

    /** The callback implementation */
    private final class VideoCallback implements SurfaceHolder.Callback {

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            videoSurfaceAvailable = true;

            if (controller != null)
                controller.show();
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

    private final class NextListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            if (service != null)
                service.playNext();
        }
    }

    private final class PrevListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            if (service != null)
                service.seekTo(0);
        }
    }

    private void attachPrevNextListeners() {
        if (controller != null)
            controller.setPrevNextListeners(episodeManager.isPlaylistEmpty() ?
                    null : new NextListener(), new PrevListener());
    }
}
