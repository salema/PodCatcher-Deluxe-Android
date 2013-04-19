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
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import net.alliknow.podcatcher.R;
import net.alliknow.podcatcher.listeners.PlayerListener;
import net.alliknow.podcatcher.model.types.Episode;

/**
 * The player fragment.
 */
public class PlayerFragment extends Fragment {

    /** The listener for the title click */
    private PlayerListener listener;

    /** Flag for the show load menu item state */
    private boolean showLoadMenuItem = false;
    /** Flag for the state of the load menu item */
    private boolean loadMenuItemState = true;
    /** Flag for the show player state */
    private boolean showPlayer = false;
    /** Flag for the show player title state */
    private boolean showPlayerTitle = false;
    /** Flag for the show player seek bar state */
    private boolean showPlayerSeekbar = true;
    /** Flag for the show error view state */
    private boolean showError = false;

    /** Status flag indicating that our view is created */
    private boolean viewCreated = false;

    /** The load episode menu bar item */
    private MenuItem loadMenuItem;

    /** Title view showing current episode title */
    private TextView titleView;
    /** The player's seek bar */
    private SeekBar seekBar;
    /** The player main button */
    private Button button;
    /** The error view */
    private TextView errorView;

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

        errorView = (TextView) view.findViewById(R.id.player_error);

        viewCreated = true;
    }

    @Override
    public void onResume() {
        super.onResume();

        setLoadMenuItemVisibility(showLoadMenuItem, loadMenuItemState);
        setPlayerVisibilility(showPlayer);
        setPlayerTitleVisibility(showPlayerTitle);
        setPlayerSeekbarVisibility(showPlayerSeekbar);
        setErrorViewVisibility(showError);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.player, menu);

        loadMenuItem = menu.findItem(R.id.episode_load_menuitem);
        setLoadMenuItemVisibility(showLoadMenuItem, loadMenuItemState);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.episode_load_menuitem:
                // Tell activity to load/unload the current episode
                listener.onToggleLoad();

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onDestroyView() {
        viewCreated = false;

        super.onDestroyView();
    }

    /**
     * Set whether the fragment should show the load menu item. You can call
     * this any time and can expect it to happen on menu creation at the latest.
     * You also have to set the load menu state, <code>true</code> for "Play" /
     * "Load" and <code>false</code> for "Stop" / "Unload".
     * 
     * @param show Whether to show the load menu item.
     * @param load State of the load menu item (load / unload)
     */
    public void setLoadMenuItemVisibility(boolean show, boolean load) {
        this.showLoadMenuItem = show;
        this.loadMenuItemState = load;

        // Only do it right away if resumed and menu item is available,
        // otherwise onResume or the menu creation callback will call us.
        if (isResumed() && loadMenuItem != null) {
            loadMenuItem.setVisible(show);

            loadMenuItem.setTitle(load ? R.string.play : R.string.stop);
            loadMenuItem.setIcon(load ? R.drawable.ic_media_play : R.drawable.ic_media_stop);
        }
    }

    /**
     * Set whether the fragment should show the player title view. You can call
     * this any time and can expect it to happen on resume at the latest. This
     * only makes a difference if the player itself is visible.
     * 
     * @param show Whether to show the player title view.
     */
    public void setPlayerTitleVisibility(boolean show) {
        this.showPlayerTitle = show;

        // Only do it right away if resumed, otherwise onResume will call us.
        if (isResumed())
            titleView.setVisibility(show ? VISIBLE : GONE);
    }

    /**
     * Set whether the fragment should show the player seek bar view. You can
     * call this any time and can expect it to happen on resume at the latest.
     * This only makes a difference if the player itself is visible.
     * 
     * @param show Whether to show the player title view.
     */
    public void setPlayerSeekbarVisibility(boolean show) {
        this.showPlayerSeekbar = show;

        // Only do it right away if resumed, otherwise onResume will call us.
        if (isResumed())
            seekBar.setVisibility(show ? VISIBLE : GONE);
    }

    /**
     * Update the player title view to show name and link to the given episode.
     * 
     * @param playingEpisode Episode to show link to.
     */
    public void updatePlayerTitle(Episode playingEpisode) {
        // We can only do this after the fragment's widgets are created
        if (viewCreated && playingEpisode != null)
            titleView.setText(Html.fromHtml("<a href=\"\">" + playingEpisode.getName() + " - "
                    + playingEpisode.getPodcast().getName() + "</a>"));
    }

    /**
     * Set whether the fragment should show the player view at all. You can call
     * this any time and can expect it to happen on resume at the latest.
     * 
     * @param show Whether to show the player view.
     */
    public void setPlayerVisibilility(boolean show) {
        this.showPlayer = show;

        // Only do it right away if resumed, otherwise onResume will call us.
        if (isResumed())
            getView().setVisibility(show ? VISIBLE : GONE);
    }

    /**
     * Update the player seek bar to show current progress.
     * 
     * @param enabled Whether the seek bar is enabled.
     * @param max Max value of the seek bar.
     * @param progress Progress to set.
     */
    public void updateSeekBar(boolean enabled, int max, int progress) {
        // We can only do this after the fragment's widgets are created
        if (viewCreated) {
            seekBar.setEnabled(enabled);

            seekBar.setMax(max);
            seekBar.setProgress(progress);
        }
    }

    /**
     * Update the player seek bar's secondary progress.
     * 
     * @param secondaryProgress 2ndary progress to set.
     */
    public void updateSeekBarSecondaryProgress(int secondaryProgress) {
        // We can only do this after the fragment's widgets are created
        if (viewCreated)
            seekBar.setSecondaryProgress(secondaryProgress);
    }

    /**
     * Update the player button to show current state and progress.
     * 
     * @param buffering Whether the player is currently buffering.
     * @param playing Whether the player is currently playing.
     * @param duration Full duration of current episode.
     * @param position Player position in current episode.
     */
    public void updateButton(boolean buffering, boolean playing, int duration, int position) {
        // We can only do this after the fragment's widgets are created
        if (viewCreated) {
            // Update button appearance
            button.setEnabled(!buffering);

            // Buffering...
            if (buffering) {
                button.setText(R.string.buffering);
                button.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_menu_rotate, 0, 0, 0);
            } // Playing or paused
            else {
                button.setText(playing ? R.string.pause : R.string.resume);
                button.setBackgroundResource(playing ?
                        R.drawable.button_red : R.drawable.button_green);
                button.setCompoundDrawablesWithIntrinsicBounds(playing ?
                        R.drawable.ic_media_pause : R.drawable.ic_media_play, 0, 0, 0);

                final String formattedPosition = formatTime(position);
                final String formattedDuration = formatTime(duration);

                button.setText(button.getText() + " " + getString(R.string.at) + " " +
                        formattedPosition + " " + getString(R.string.of) + " " + formattedDuration);
            }
        }
    }

    /**
     * Set whether the fragment should show the error view. You can call this
     * any time and can expect it to happen on resume at the latest.
     * 
     * @param show Whether to show the player view.
     */
    public void setErrorViewVisibility(boolean show) {
        this.showError = show;

        // Only do it right away if resumed, otherwise onResume will call us.
        if (isResumed()) {
            titleView.setVisibility(show ? GONE : VISIBLE);
            button.setVisibility(show ? GONE : VISIBLE);
            seekBar.setVisibility(show ? GONE : VISIBLE);
            errorView.setVisibility(show ? VISIBLE : GONE);
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
