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
package net.alliknow.podcatcher.types;

import java.net.URL;

/**
 * The episode type.
 * 
 * @author Kevin Hausmann
 */
public class Episode {

	private String name;
	private URL mediaUrl;
	
	public Episode(String name) {
		this.name = name;
	}
	
	public String getName() {
		return name;
	}
	
	public URL getMediaUrl() {
		return mediaUrl;
	}
	
	@Override
	public String toString() {
		return getName();
	}
	
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof Episode)) return false;
		else return this.mediaUrl.equals(((Episode) o).getMediaUrl());
	}
}
