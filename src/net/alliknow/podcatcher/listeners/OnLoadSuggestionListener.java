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
package net.alliknow.podcatcher.listeners;

import net.alliknow.podcatcher.PodcastList;

/**
 * Interface definition for a callback to be invoked when podcast suggestions are loaded.
 */
public interface OnLoadSuggestionListener {
	
	/**
	 * Called on progress update.
	 * @param progress Percent of suggestions JSON file loaded 
	 * or flag from <code>LoadRemoteFileTask</code>.
	 * Note that this only works if the http connection
	 * reports its content length correctly. Otherwise 
	 * (and this happens in the wild out there) percent might be >100.
	 */
	public void onSuggestionsLoadProgress(int progress);
	
	/**
	 * Called on completion.
	 * @param suggestions Podcast suggestions loaded.
	 */
	public void onSuggestionsLoaded(PodcastList suggestions);
	
	/**
	 * Called when loading the suggestions failed.
	 */
	public void onSuggestionsLoadFailed();
}