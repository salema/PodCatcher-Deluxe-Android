package net.alliknow.podcatcher.listeners;

import net.alliknow.podcatcher.model.types.Episode;

public interface PlaybackListener {

    void onPlay();
    void onPause();
    void onStop();
    void onUpdateProgress(int progress);
    void onSetNewEpisode(Episode episode, int duration);
    void onNothingSet();

}