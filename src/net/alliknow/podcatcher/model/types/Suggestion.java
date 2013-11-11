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

package net.alliknow.podcatcher.model.types;

import java.net.URL;

/**
 * The podcast suggestion type. Extends {@link Podcast} for a few fields
 * specific to suggestions.
 */
public class Suggestion extends Podcast {

    /** Whether the podcast is featured */
    private boolean isFeatured = false;

    /**
     * Create new suggestion. See {@link Podcast} for details.
     * 
     * @param name The name to show.
     * @param url The URL to load feed from.
     */
    public Suggestion(String name, URL url) {
        super(name, url);
    }

    /**
     * @return Whether this suggestion is featured.
     */
    public boolean isFeatured() {
        return isFeatured;
    }

    /**
     * @param isFeatured What to set the flag to.
     */
    public void setFeatured(boolean isFeatured) {
        this.isFeatured = isFeatured;
    }
}
