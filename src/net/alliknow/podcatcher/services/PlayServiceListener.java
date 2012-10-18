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
package net.alliknow.podcatcher.services;

/**
 * Listener interface to implement if you are interested to be alerted
 * by the play episode service on a couple of important events.
 *  
 * @author Kevin Hausmann
 */
public interface PlayServiceListener {

	/**
	 * Called by the service on the listener if an episode is loaded
	 * and ready to play (the service might in fact already have started
	 * playback...)
	 */
	public void onReadyToPlay();

	/**
	 * Called by the service on the listener if an episode is temporarily
	 * stopped for filling the media player's buffer.
	 */
	public void onStopForBuffering();
	
	/**
	 * Called by the service on the listener if an episode was temporarily
	 * stopped for filling the media player's buffer and now resumes.
	 */
	public void onResumeFromBuffering();
	
	/**
	 * Calley by the service on the listener if the media player buffer
	 * state changed.
	 * @param seconds Seconds from the media start currently buffered.
	 */
	public void onBufferUpdate(int seconds);
	
	/**
	 * Called by the service on the listener if an episode finished playing.
	 * The service does not free resources on completion automatically,
	 * you might want to call <code>reset()</code>.
	 */
	public void onPlaybackComplete();
	
	/**
	 * Called by the service on the listener if an episode fails to play
	 * or any other error occurs.
	 */
	public void onError();

}