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
package net.alliknow.podcatcher.views;

import net.alliknow.podcatcher.R;
import net.alliknow.podcatcher.services.PlayEpisodeService;
import net.alliknow.podcatcher.types.Episode;
import android.content.Context;
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
	
	/** The player divider used when title is shown */
	private ImageView playerDividerView;
	/** Title view showing current episode title */
	private TextView playerTitleView;
	/** The player's seek bar */
	private SeekBar playerSeekBar;
	/** The player main button */
	private Button playerButton;
	/** The error view */
	private TextView playerErrorView;
	
	public Player(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		at = getResources().getString(R.string.at);
		of = getResources().getString(R.string.of);
		
		View view = View.inflate(context, R.layout.player, this);
		playerDividerView = (ImageView) view.findViewById(R.id.player_divider);
		playerTitleView = (TextView) view.findViewById(R.id.player_title);
		playerSeekBar = (SeekBar) view.findViewById(R.id.player_seekbar);
		playerButton = (Button) view.findViewById(R.id.player_button);
		playerErrorView = (TextView) view.findViewById(R.id.player_error);
	}

	@Override
	public void setOnClickListener(OnClickListener listener) {
		playerButton.setOnClickListener(listener);
	}
	
	@Override
	public void setOnLongClickListener(OnLongClickListener listener) {
		playerButton.setOnLongClickListener(listener);
	}
	
	/**
	 * Set a seek bar listener to the players seek bar.
	 * @param listener The listener.
	 */
	public void setOnSeekBarChangeListener(OnSeekBarChangeListener listener) {
		playerSeekBar.setOnSeekBarChangeListener(listener);
	}
	
	/**
	 * Update the player's UI according to the current
	 * state of play. 
	 * @param service The play episode service (should not be <code>null</code>
	 * but will fail gracefully).
	 * @param currentEpisode The episode currently selected (may be <code>null</code>).
	 */
	public void update(PlayEpisodeService service, Episode currentEpisode) {
		if (service != null) {
			playerErrorView.setVisibility(GONE);
			
			playerDividerView.setVisibility(service.isWorkingWith(currentEpisode) ? GONE : VISIBLE);
			playerTitleView.setVisibility(service.isWorkingWith(currentEpisode) ? GONE : VISIBLE);
			playerTitleView.setText(service.getCurrentEpisodeName() + " - " 
						+ service.getCurrentEpisodePodcastName());
				
			updateSeekBar(service);
			updateButton(service);
					
			setVisibility(service.isPrepared() || service.isPreparing() ? VISIBLE : GONE);
		}
	}

	/**
	 * Set the secondary progress shown in seek bar.
	 * @param seconds The progress in seconds.
	 */
	public void setSecondaryProgress(int seconds) {
		playerSeekBar.setSecondaryProgress(seconds);
	}

	/**
	 * Show the player's error view.
	 */
	public void showError() {
		setVisibility(VISIBLE);
		
		playerTitleView.setVisibility(GONE);
		playerButton.setVisibility(GONE);
		playerSeekBar.setVisibility(GONE);
		playerErrorView.setVisibility(VISIBLE);
	}
	
	private void updateSeekBar(PlayEpisodeService service) {
		playerSeekBar.setEnabled(! service.isPreparing());
		
		// We are running and might advance progress
		if (service.isPrepared()) {
			playerSeekBar.setMax(service.getDuration());
			playerSeekBar.setProgress(service.getCurrentPosition());
		} // Reset progress
		else {
			playerSeekBar.setProgress(0);
			playerSeekBar.setSecondaryProgress(0);
		}
	}

	private void updateButton(PlayEpisodeService service) {
		// Update button appearance
		playerButton.setEnabled(! service.isBuffering());
		playerButton.setBackgroundResource(service.isPlaying() ? R.drawable.button_red : R.drawable.button_green);
		playerButton.setCompoundDrawablesWithIntrinsicBounds(
				service.isPlaying() ? R.drawable.ic_media_pause : R.drawable.ic_media_play, 0, 0, 0);
		
		// Update button label
		// Buffering...
		if (service.isBuffering()) {
			playerButton.setText(R.string.buffering);
			playerButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_menu_rotate, 0, 0, 0);
		} // Playing or paused
		else {
			playerButton.setText(service.isPlaying() ? R.string.pause : R.string.resume);
			
			if (service.isPrepared()) {
				final String position = formatTime(service.getCurrentPosition());
				final String duration = formatTime(service.getDuration());
				
				playerButton.setText(playerButton.getText() + " " + at + " " + position + " " + of + " " + duration);
			}
		}
	}

	/**
	 * Format an amount of time.
	 * @param time Amount in seconds to format.
	 * @return The time span as hh:mm:ss with appropriate omissions.
	 */
	private String formatTime(int time) {
		int hours = time / 3600;
		
		int minutes = (time / 60) - 60 * hours;
		int seconds = time % 60;
		
		String minutesString = Player.formatNumber(minutes, hours > 0);
		String secondsString = Player.formatNumber(seconds, true);
		
		if (hours > 0) return hours + ":" + minutesString + ":" + secondsString;
		else return minutesString + ":" + secondsString; 
	}
	
	private static String formatNumber(int number, boolean makeTwoDigits) {
		if (number < 10 && makeTwoDigits) return "0" + number;
		else return number + "";
	}
}
