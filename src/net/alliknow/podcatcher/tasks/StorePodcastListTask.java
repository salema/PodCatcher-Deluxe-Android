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

import java.io.BufferedWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Date;

import net.alliknow.podcatcher.PodcastList;
import net.alliknow.podcatcher.types.Podcast;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

/**
 * Stores the default podcast list to the filesystem asynchronously.
 */
public class StorePodcastListTask extends AsyncTask<PodcastList, Progress, Void> {

	/** Content of OPML file title tag */
	protected static final String OPML_TITLE = "Simple Podcatcher Podcast file";
	
	/** Our context */
	private final Context context;
	
	/**
	 * Create new task.
	 * @param context Context to read file from.
	 * @param listener Callback to be alerted on completion.
	 */
	public StorePodcastListTask(Context context) {
		this.context = context;
	}
	
	@Override
	protected Void doInBackground(PodcastList... params) {
		PodcastList list = params[0];
		OutputStream fileStream = null; 
		try {
			if (list == null) throw new Exception("Podcast list cannot be null!");
			
			fileStream = context.openFileOutput(PodcastList.OPML_FILENAME, Context.MODE_PRIVATE);
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(fileStream, PodcastList.OPML_FILE_ENCODING));
			
			writer.write("<?xml version=\"1.0\" encoding=\"" + PodcastList.OPML_FILE_ENCODING + "\"?>");
			writer.write("<opml version=\"2.0\">");
			writer.write("<head>");
			writer.write("<title>" + OPML_TITLE + "</title>");
			writer.write("<dateModified>" + new Date().toString() + "</dateModified>");
			writer.write("</head>");
			writer.write("<body>");
			
			for (Podcast podcast : list) {
				String opmlString = podcast.toOpmlString();
				if (opmlString != null) writer.write(opmlString);
			}
			
			writer.write("</body></opml>");
			writer.close();
		} catch (Exception e) {
			Log.e(getClass().getSimpleName(), "Cannot store podcast OPML file", e);
		}
		
		return null;
	}
}
