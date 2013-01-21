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

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import net.alliknow.podcatcher.R;
import net.alliknow.podcatcher.listeners.PlayerListener;
import net.alliknow.podcatcher.model.types.Episode;
import net.alliknow.podcatcher.services.PlayEpisodeService;

/**
 * 
 */
public class PlayerFragment extends Fragment {

    /** String resource needed for button label */
    private String at;
    /** String resource needed for button label */
    private String of;

    /** The listener for the title click */
    private PlayerListener listener;

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

    /** The load episode menu bar item */
    private MenuItem loadMenuItem;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // Make sure our listener is present
        try {
            this.listener = (PlayerListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement PlayerListener");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.player, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        at = getResources().getString(R.string.at);
        of = getResources().getString(R.string.of);

        dividerView = (ImageView) view.findViewById(R.id.player_divider);
        titleView = (TextView) view.findViewById(R.id.player_title);
        titleView.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                listener.onReturnToPlayingEpisode();
            }
        });

        seekBar = (SeekBar) view.findViewById(R.id.player_seekbar);
        seekBar.setOnSeekBarChangeListener(listener);

        button = (Button) view.findViewById(R.id.player_button);
        button.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                listener.onTogglePlay();
            }
        });
        button.setOnLongClickListener(new OnLongClickListener() {

            @Override
            public boolean onLongClick(View v) {
                listener.onUnloadEpisode();

                return true;
            }
        });

        errorView = (TextView) view.findViewById(R.id.player_error);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.episode, menu);

        loadMenuItem = menu.findItem(R.id.episode_load_menuitem);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.episode_load_menuitem:

                listener.onLoadEpisode();

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
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

            // Menu item might be late to load
            if (loadMenuItem != null) {
                loadMenuItem.setVisible(true); // currentEpisode != null &&
                                               // service != null);

                if (loadMenuItem.isVisible()) {
                    loadMenuItem.setTitle(service.isWorkingWith(currentEpisode) ? R.string.stop
                            : R.string.play);
                    loadMenuItem
                            .setIcon(service.isWorkingWith(currentEpisode) ? R.drawable.ic_media_stop
                                    : R.drawable.ic_media_play);
                }
            }

            errorView.setVisibility(GONE);

            dividerView.setVisibility(service.isWorkingWith(currentEpisode) ? GONE : VISIBLE);
            titleView.setVisibility(service.isWorkingWith(currentEpisode) ? GONE : VISIBLE);
            titleView.setText(Html.fromHtml("<a href=\"\">" + service.getCurrentEpisodeName()
                    + " - "
                    + service.getCurrentEpisodePodcastName() + "</a>"));

            updateSeekBar(service);
            updateButton(service);
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

        String minutesString = formatNumber(minutes, hours > 0);
        String secondsString = formatNumber(seconds, true);

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
