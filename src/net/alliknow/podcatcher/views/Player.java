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

import net.alliknow.podcatcher.Podcatcher;
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
 *
 * @author Kevin Hausmann
 */
public class Player extends LinearLayout {
	
	final private String at;
	final private String of;
	
	private ImageView playerDividerView;
	private TextView playerTitleView;
	private Button playerButton;
	private SeekBar playerSeekBar;
	private TextView playerErrorView;
	
	public Player(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		
		at = getResources().getString(R.string.at);
		of = getResources().getString(R.string.of);
		
		View view = View.inflate(context, R.layout.player, this);
		playerDividerView = (ImageView) view.findViewById(R.id.player_divider);
		playerTitleView = (TextView) view.findViewById(R.id.player_title);
		playerSeekBar = (SeekBar) view.findViewById(R.id.player_seekbar);
		playerButton = (Button) view.findViewById(R.id.player_button);
		playerErrorView = (TextView) view.findViewById(R.id.player_error);
	}

	public Player(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public Player(Context context) {
		this(context, null);
	}
	
	@Override
	public void setOnClickListener(OnClickListener listener) {
		playerButton.setOnClickListener(listener);
	}
	
	@Override
	public void setOnLongClickListener(OnLongClickListener listener) {
		playerButton.setOnLongClickListener(listener);
	}
	
	public void setOnSeekBarChangeListener(OnSeekBarChangeListener listener) {
		playerSeekBar.setOnSeekBarChangeListener(listener);
	}
	
	public void update(PlayEpisodeService service, Episode currentEpisode) {
		playerErrorView.setVisibility(View.GONE);
		
		playerDividerView.setVisibility(service.isWorkingWith(currentEpisode) ? View.GONE : View.VISIBLE);
		playerTitleView.setVisibility(service.isWorkingWith(currentEpisode) ? View.GONE : View.VISIBLE);
		playerTitleView.setText(service.getCurrentEpisodeName() + " - " 
					+ service.getCurrentEpisodePodcastName());
			
		updateSeekBar(service);
		updateButton(service);
				
		setVisibility(service.isPrepared() || service.isPreparing() ? View.VISIBLE : View.GONE);
	}

	public void setSecondaryProgress(int seconds) {
		playerSeekBar.setSecondaryProgress(seconds);
	}

	public void showError() {
		playerErrorView.setVisibility(View.VISIBLE);
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
		playerButton.setEnabled(! service.isBuffering());
		playerButton.setBackgroundResource(service.isPlaying() ? R.drawable.button_red : R.drawable.button_green);
		playerButton.setCompoundDrawablesWithIntrinsicBounds(
				service.isPlaying() ? R.drawable.ic_media_pause : R.drawable.ic_media_play, 0, 0, 0);
		
		if (service.isBuffering()) {
			playerButton.setText(R.string.buffering);
			playerButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_menu_rotate, 0, 0, 0);
		}
		else {
			playerButton.setText(service.isPlaying() ? R.string.pause : R.string.resume);
			
			if (service.isPrepared()) {
				final String position = Podcatcher.formatTime(service.getCurrentPosition());
				final String duration = Podcatcher.formatTime(service.getDuration());
				
				playerButton.setText(playerButton.getText() + " " + at + " " + position + " " + of + " " + duration);
			}
		}
	}
}
