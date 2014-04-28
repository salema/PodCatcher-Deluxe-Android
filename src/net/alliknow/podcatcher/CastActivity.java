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

package net.alliknow.podcatcher;

import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.MediaRouteActionProvider;
import android.support.v7.app.MediaRouteButton;
import android.support.v7.media.MediaRouteSelector;
import android.support.v7.media.MediaRouter;
import android.support.v7.media.MediaRouter.RouteInfo;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.gms.cast.ApplicationMetadata;
import com.google.android.gms.cast.Cast;
import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.CastMediaControlIntent;
import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.cast.RemoteMediaPlayer;
import com.google.android.gms.cast.RemoteMediaPlayer.MediaChannelResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;

import java.io.IOException;

/**
 * Podcatcher base activity. Defines some common functionality useful for all
 * activities.
 */
public abstract class CastActivity extends BaseActivity implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    protected MediaRouter mediaRouter;
    protected MediaRouteSelector mediaRouteSelector;
    protected CastDevice selectedDevice;
    protected MediaRouter.Callback mediaRouterCallback = new MyMediaRouterCallback();
    protected GoogleApiClient apiClient;
    protected MediaRouteButton mediaRouteButton;

    private class MyMediaRouterCallback extends MediaRouter.Callback {

        @Override
        public void onRouteSelected(MediaRouter router, RouteInfo info) {
            selectedDevice = CastDevice.getFromBundle(info.getExtras());
            String routeId = info.getId();

            Cast.CastOptions.Builder apiOptionsBuilder = Cast.CastOptions
                    .builder(selectedDevice, new MyCastClientListener());

            apiClient = new GoogleApiClient.Builder(CastActivity.this)
                    .addApi(Cast.API, apiOptionsBuilder.build())
                    .addConnectionCallbacks(CastActivity.this)
                    .addOnConnectionFailedListener(CastActivity.this)
                    .build();
            apiClient.connect();
        }

        @Override
        public void onRouteUnselected(MediaRouter router, RouteInfo info) {
            // teardown();
            selectedDevice = null;
        }
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        Log.d("CAST", "App connected");
        try {
            Cast.CastApi.launchApplication(apiClient,
                    CastMediaControlIntent.DEFAULT_MEDIA_RECEIVER_APPLICATION_ID, false)
                    .setResultCallback(
                            new ResultCallback<Cast.ApplicationConnectionResult>() {
                                @Override
                                public void onResult(Cast.ApplicationConnectionResult result) {
                                    Status status = result.getStatus();
                                    if (status.isSuccess()) {
                                        ApplicationMetadata applicationMetadata =
                                                result.getApplicationMetadata();
                                        String sessionId = result.getSessionId();
                                        String applicationStatus = result.getApplicationStatus();
                                        boolean wasLaunched = result.getWasLaunched();
                                        playSample();
                                    } else {
                                        // teardown();
                                    }
                                }
                            });

        } catch (Exception e) {
            Log.e("CAST", "Failed to launch application", e);
        }
    }

    public void playSample() {

        RemoteMediaPlayer player = new RemoteMediaPlayer();
        player.setOnStatusUpdatedListener(new RemoteMediaPlayer.OnStatusUpdatedListener() {

            @Override
            public void onStatusUpdated() {
                Log.d("CAST", "Remote media player status updated");
            }
        });
        player.setOnMetadataUpdatedListener(new RemoteMediaPlayer.OnMetadataUpdatedListener() {

            @Override
            public void onMetadataUpdated() {
                Log.d("CAST", "Remote media player metadata updated");
            }
        });

        try {
            Cast.CastApi.setMessageReceivedCallbacks(apiClient,
                    player.getNamespace(), player);
        } catch (IOException e) {
            Log.e("CAST", "Exception while creating media channel", e);
        }
        player.requestStatus(apiClient)
                .setResultCallback(new ResultCallback<RemoteMediaPlayer.MediaChannelResult>() {
                    @Override
                    public void onResult(MediaChannelResult result) {
                        if (!result.getStatus().isSuccess()) {
                            Log.e("CAST", "Failed to request status.");
                        }
                    }
                });

        MediaMetadata mediaMetadata = new MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE);
        mediaMetadata.putString(MediaMetadata.KEY_TITLE, "My podcast");
        MediaInfo mediaInfo = new MediaInfo.Builder(
                "http://dts.podtrac.com/redirect.mp4/twit.cachefly.net/video/sn/sn0452/sn0452_h264m_1280x720_1872.mp4")
                .setContentType("video/mp4")
                .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                .setMetadata(mediaMetadata)
                .build();
        try {
            player.load(apiClient, mediaInfo, true)
                    .setResultCallback(new ResultCallback<RemoteMediaPlayer.MediaChannelResult>() {
                        @Override
                        public void onResult(MediaChannelResult result) {
                            if (result.getStatus().isSuccess()) {
                                Log.d("CAST", "Media loaded successfully");
                            } else {
                                Log.d("CAST", "Media playback failed");
                            }
                        }
                    });
        } catch (IllegalStateException e) {
            Log.e("CAST", "Problem occurred with media during loading", e);
        } catch (Exception e) {
            Log.e("CAST", "Problem opening media during loading", e);
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.d("CAST", "Connection failed");
    }

    @Override
    public void onConnectionSuspended(int cause) {
        Log.d("CAST", "Connection suspended");
    }

    private class MyCastClientListener extends Cast.Listener {

        @Override
        public void onApplicationStatusChanged() {
            super.onApplicationStatusChanged();

            Log.d("CAST", "App status changed");
        }

        @Override
        public void onApplicationDisconnected(int statusCode) {
            super.onApplicationDisconnected(statusCode);

            Log.d("CAST", "App disconnect");
        }

        @Override
        public void onVolumeChanged() {
            super.onVolumeChanged();

            Log.d("CAST", "Volume changed");
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mediaRouter = MediaRouter.getInstance(getApplicationContext());
        mediaRouteSelector = new MediaRouteSelector.Builder()
                .addControlCategory(
                        CastMediaControlIntent
                                .categoryForCast(CastMediaControlIntent.DEFAULT_MEDIA_RECEIVER_APPLICATION_ID))
                .build();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        MenuItem mediaRouteMenuItem =
                menu.findItem(R.id.media_route_menu_item);
        MediaRouteActionProvider mediaRouteActionProvider =
                (MediaRouteActionProvider)
                MenuItemCompat.getActionProvider(mediaRouteMenuItem);
        mediaRouteActionProvider.setRouteSelector(mediaRouteSelector);

        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();

        mediaRouter.addCallback(mediaRouteSelector, mediaRouterCallback,
                MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN);
    }

    @Override
    protected void onPause() {
        if (isFinishing()) {
            mediaRouter.removeCallback(mediaRouterCallback);
        }

        super.onPause();
    }
}
