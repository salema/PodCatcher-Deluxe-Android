/** Copyright 2012 Kevin Hausmann
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

package net.alliknow.podcatcher.view;

import net.alliknow.podcatcher.R;
import net.alliknow.podcatcher.listeners.OnReturnToPlayingEpisodeListener;
import net.alliknow.podcatcher.model.types.Episode;
import net.alliknow.podcatcher.services.PlayEpisodeService;
import android.content.Context;
import android.text.Html;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

/**
 * A sophisticated player view.
 */
public class Player extends LinearLayout {

    /** String resource needed for button label */
    final private String at;
    /** String resource needed for button label */
    final private String of;

    /** The listener for the title click */
    private OnReturnToPlayingEpisodeListener returnListener;

    /** The player divider used when title is shown */
    private ImageView dividerView;
    /** Title view showing current episode title */
    private TextView titleView;
    /** The player's seek bar */
    private SeekBar seekBar;
    /** The player main button */
    private Button button;
    /** The error view */
    private TextView errorView;

    public Player(Context context, AttributeSet attrs) {
        super(context, attrs);

        at = getResources().getString(R.string.at);
        of = getResources().getString(R.string.of);

        View view = View.inflate(context, R.layout.player, this);
        dividerView = (ImageView) view.findViewById(R.id.player_divider);
        titleView = (TextView) view.findViewById(R.id.player_title);
        seekBar = (SeekBar) view.findViewById(R.id.player_seekbar);
        button = (Button) view.findViewById(R.id.player_button);
        errorView = (TextView) view.findViewById(R.id.player_error);

        titleView.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                returnListener.onReturnToPlayingEpisode();
            }
        });
    }

    @Override
    public void setOnClickListener(OnClickListener listener) {
        button.setOnClickListener(listener);
    }

    @Override
    public void setOnLongClickListener(OnLongClickListener listener) {
        button.setOnLongClickListener(listener);
    }

    /**
     * Set a seek bar listener to the players seek bar.
     * 
     * @param listener The listener.
     */
    public void setOnSeekBarChangeListener(OnSeekBarChangeListener listener) {
        seekBar.setOnSeekBarChangeListener(listener);
    }

    /**
     * Set a call-back to be alerted if the user wants to return to the
     * currently playing episode.
     * 
     * @param listener The listener.
     */
    public void setOnReturnToPlayingEpisodeListener(OnReturnToPlayingEpisodeListener listener) {
        this.returnListener = listener;
    }

    /**
     * Update the player's UI according to the current state of play.
     * 
     * @param service The play episode service (should not be <code>null</code>
     *            but will fail gracefully).
     * @param currentEpisode The episode currently selected (may be
     *            <code>null</code>).
     */
    public void update(PlayEpisodeService service, Episode currentEpisode) {
        if (service != null) {
            errorView.setVisibility(GONE);

            dividerView.setVisibility(service.isWorkingWith(currentEpisode) ? GONE : VISIBLE);
            titleView.setVisibility(service.isWorkingWith(currentEpisode) ? GONE : VISIBLE);
            titleView.setText(Html.fromHtml("<a href=\"\">" + service.getCurrentEpisodeName()
                    + " - "
                    + service.getCurrentEpisodePodcastName() + "</a>"));

            updateSeekBar(service);
            updateButton(service);

            setVisibility(service.isPrepared() || service.isPreparing() ? VISIBLE : GONE);
        }
    }

    /**
     * Set the secondary progress shown in seek bar.
     * 
     * @param seconds The progress in seconds.
     */
    public void setSecondaryProgress(int seconds) {
        seekBar.setSecondaryProgress(seconds);
    }

    /**
     * Show the player's error view.
     */
    public void showError() {
        setVisibility(VISIBLE);

        titleView.setVisibility(GONE);
        button.setVisibility(GONE);
        seekBar.setVisibility(GONE);
        errorView.setVisibility(VISIBLE);
    }

    private void updateSeekBar(PlayEpisodeService service) {
        seekBar.setEnabled(!service.isPreparing());

        // We are running and might advance progress
        if (service.isPrepared()) {
            seekBar.setMax(service.getDuration());
            seekBar.setProgress(service.getCurrentPosition());
        } // Reset progress
        else {
            seekBar.setProgress(0);
            seekBar.setSecondaryProgress(0);
        }
    }

    private void updateButton(PlayEpisodeService service) {
        // Update button appearance
        button.setEnabled(!service.isBuffering());
        button.setBackgroundResource(service.isPlaying() ? R.drawable.button_red
                : R.drawable.button_green);
        button.setCompoundDrawablesWithIntrinsicBounds(
                service.isPlaying() ? R.drawable.ic_media_pause : R.drawable.ic_media_play, 0, 0, 0);

        // Update button label
        // Buffering...
        if (service.isBuffering()) {
            button.setText(R.string.buffering);
            button.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_menu_rotate, 0, 0, 0);
        } // Playing or paused
        else {
            button.setText(service.isPlaying() ? R.string.pause : R.string.resume);

            if (service.isPrepared()) {
                final String position = formatTime(service.getCurrentPosition());
                final String duration = formatTime(service.getDuration());

                button.setText(button.getText() + " " + at + " " + position + " " + of + " "
                        + duration);
            }
        }
    }

    /**
     * Format an amount of time.
     * 
     * @param time Amount in seconds to format.
     * @return The time span as hh:mm:ss with appropriate omissions.
     */
    private String formatTime(int time) {
        int hours = time / 3600;

        int minutes = (time / 60) - 60 * hours;
        int seconds = time % 60;

        String minutesString = Player.formatNumber(minutes, hours > 0);
        String secondsString = Player.formatNumber(seconds, true);

        if (hours > 0)
            return hours + ":" + minutesString + ":" + secondsString;
        else
            return minutesString + ":" + secondsString;
    }

    private static String formatNumber(int number, boolean makeTwoDigits) {
        if (number < 10 && makeTwoDigits)
            return "0" + number;
        else
            return number + "";
    }
}
