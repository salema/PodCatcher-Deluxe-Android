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

package net.alliknow.podcatcher;

import net.alliknow.podcatcher.model.types.Episode;

/**
 * Show episode activity.
 */
public class EpisodeActivity extends BaseActivity {

    /** Key used to store podcast URL in intent or bundle */
    public static final String PODCAST_URL_KEY = "podcast_url";
    /** Key used to store episode URL in intent or bundle */
    public static final String EPISODE_URL_KEY = "episode_url";

    /** The episode currently selected and displayed */
    protected Episode currentEpisode;

}
