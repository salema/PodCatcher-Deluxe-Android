package net.alliknow.podcatcher.view;

import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import net.alliknow.podcatcher.PodcastActivity;
import net.alliknow.podcatcher.R;
import net.alliknow.podcatcher.listeners.PlaybackListener;
import net.alliknow.podcatcher.model.types.Episode;
import net.alliknow.podcatcher.services.PlayEpisodeService;

public class ControllerView extends RelativeLayout implements PlaybackListener {

    ImageButton btnRewind;
    ImageButton btnPlay;
    ImageButton btnPause;
    ImageButton btnStop;
    ImageButton btnForward;

    PodcastActivity activity;
    PlayEpisodeService service;

//    PlayEpisodeService mService;
//    Episode mEpisode;

    public ControllerView(Context context) {
        super(context);
        this.activity = (PodcastActivity) context;
    }

    public ControllerView(Context context, AttributeSet set) {
        super(context, set);
        this.activity = (PodcastActivity) context;
    }

    public void connectToService(PlayEpisodeService service) {
        this.service = service;
        service.addPlayBackListener(this);
        togglePlayPauseButton(service.isPlaying());
        setEnabled(true);
    }

    public void disconnectFromService() {
        this.service = null;
        setEnabled(false);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        View.inflate(getContext(), R.layout.controller_view, this);
        initialize();
    }

    private void initialize() {
        btnRewind = (ImageButton) findViewById(R.id.btn_rewind);
        btnPlay = (ImageButton) findViewById(R.id.btn_play);
        btnPause = (ImageButton) findViewById(R.id.btn_pause);
        btnStop = (ImageButton) findViewById(R.id.btn_stop);
        btnForward = (ImageButton) findViewById(R.id.btn_forward);

        btnRewind.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
//                if (mService != null) {
//                    mService.rewind();
//                }
                getContext().startService(new Intent(PlayEpisodeService.ACTION_REWIND));
            }
        });

        btnPlay.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
//                if (mService != null && mEpisode != null) {
//                    mService.playEpisode(mEpisode);
//                }
//                getContext().startService(new Intent(PlayEpisodeService.ACTION_PLAY));
                if (service.isPrepared()) {
                    getContext().startService(new Intent(PlayEpisodeService.ACTION_PLAY));
                } else {
                    activity.onToggleLoad();
                }
            }
        });

        btnPause.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                getContext().startService(new Intent(PlayEpisodeService.ACTION_PAUSE));
            }
        });

        btnStop.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                getContext().startService(new Intent(PlayEpisodeService.ACTION_STOP));
            }
        });

        btnForward.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                getContext().startService(new Intent(PlayEpisodeService.ACTION_FORWARD));
            }
        });
    }

    public void togglePlayPauseButton(boolean isPlaying) {
        if (isPlaying) {
            // show stop button
            btnPlay.setVisibility(INVISIBLE);
            btnPause.setVisibility(VISIBLE);
            btnPause.requestFocus();
        } else {
            // show play button
            btnPlay.setVisibility(VISIBLE);
            btnPause.setVisibility(INVISIBLE);
            btnPlay.requestFocus();
        }
    }

    @Override
    protected void onFocusChanged(boolean gainFocus, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
        if (gainFocus) {
            if (btnPlay.getVisibility() == VISIBLE) {
                btnPlay.requestFocus();
            } else {
                btnPause.requestFocus();
            }
        }
    }

    @Override
    public void onPlay() {
        togglePlayPauseButton(true);
    }

    @Override
    public void onPause() {
        togglePlayPauseButton(false);
    }

    @Override
    public void onStop() {
        togglePlayPauseButton(false);
    }

    @Override
    public void onUpdateProgress(int progress) {
        // pass
    }

    @Override
    public void onSetNewEpisode(Episode episode, int duration) {
        // pass
    }

    @Override
    public void onNothingSet() {
        // pass
    }
}
