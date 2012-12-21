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

import java.util.List;

import net.alliknow.podcatcher.model.types.Podcast;

/**
 * Interface definition for a callback to be invoked when podcast suggestions are requested.
 */
public interface OnShowSuggestionsListener extends OnAddPodcastListener {

	/**
	 * Get the podcast suggestions from cache. You can return 
	 * <code>null</code> and make suggestions be reloaded from 
	 * the internets.
	 * @return A cached list of podcast suggestions.
	 * @see <code>setPodcastSuggestions</code>
	 */
	public List<Podcast> getPodcastSuggestions();

	/**
	 * Writes the loaded podcast suggestions to a cache you might
	 * provide.
	 * @param suggestions Podcast suggestions to cache.
	 * @see <code>getPodcastSuggestions</code>
	 */
	public void setPodcastSuggestions(List<Podcast> suggestions);

	/**
	 * Get the list of currently already added podcast
	 * to be excluded from suggestions.
	 * @return The (possibly empty) list of podcasts already subscribed to.
	 */
	public List<Podcast> getPodcastList();
}
