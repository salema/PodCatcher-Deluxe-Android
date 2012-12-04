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
package net.alliknow.podcatcher.tasks;

import android.os.AsyncTask;

/**
 *
 */
public abstract class PodcastListTask<Params, Result> extends AsyncTask<Params, Progress, Result> {

	/** The name of the file we store our saved podcasts in (as OPML) */
	protected static final String OPML_FILENAME = "podcasts.opml";
	/** The OPML file encoding */
	protected static final String OPML_FILE_ENCODING = "utf8";
	/** Content of OPML file title tag */
	protected static final String OPML_TITLE = "Simple Podcatcher Podcast file";
}
