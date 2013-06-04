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

package net.alliknow.podcatcher.model.tasks;

import android.content.Context;
import android.os.AsyncTask;
import android.text.Html;
import android.util.Log;

import net.alliknow.podcatcher.listeners.OnLoadPodcastListListener;
import net.alliknow.podcatcher.model.PodcastManager;
import net.alliknow.podcatcher.model.tags.OPML;
import net.alliknow.podcatcher.model.types.Podcast;
import net.alliknow.podcatcher.model.types.Progress;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Loads the default podcast list from the file system asynchronously. On
 * failure, an empty podcast list is returned.
 */
public class LoadPodcastListTask extends AsyncTask<Void, Progress, List<Podcast>> {

    /** Our context */
    private Context context;
    /** The listener callback */
    private OnLoadPodcastListListener listener;

    /** Member to measure performance */
    private Date startTime;

    /** The file that we read from. */
    protected File importFile;

    /**
     * Create new task.
     * 
     * @param context Context to read file from (not <code>null</code>).
     * @param listener Callback to be alerted on completion. Could be
     *            <code>null</code>, but then nobody would ever know that this
     *            task finished.
     * @see PodcastManager#OPML_FILENAME
     */
    public LoadPodcastListTask(Context context, OnLoadPodcastListListener listener) {
        this.context = context;
        this.listener = listener;
    }

    /**
     * Define where the task should read the podcast OPML file from. Not setting
     * this (or given <code>null</code> here) will result in the file being read
     * from the private app directory.
     * 
     * @param location The location to read from.
     */
    public void setCustomLocation(File opmlFile) {
        this.importFile = opmlFile;
    }

    @Override
    protected List<Podcast> doInBackground(Void... params) {
        // Record start time
        this.startTime = new Date();

        // Create resulting data structure and file stream
        List<Podcast> result = new ArrayList<Podcast>();
        InputStream fileStream = null;

        try {
            // 1. Open the podcast file
            if (importFile == null)
                fileStream = context.openFileInput(PodcastManager.OPML_FILENAME);
            else
                fileStream = new FileInputStream(importFile);

            // 2. Build parser
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            // Create the parser to use
            XmlPullParser parser = factory.newPullParser();

            // 3. Parse the OPML file
            parser.setInput(fileStream, PodcastManager.OPML_FILE_ENCODING);
            int eventType = parser.next();

            // Read complete document
            while (eventType != XmlPullParser.END_DOCUMENT) {
                // We only need start tags here
                if (eventType == XmlPullParser.START_TAG) {
                    String tagName = parser.getName();

                    // Podcast found
                    if (tagName.equalsIgnoreCase(OPML.OUTLINE))
                        result.add(createPodcast(parser));
                }

                // Done, get next parsing event
                eventType = parser.next();
            }

            // 4. Sort and tidy up!
            while (result.remove(null))
                ;
            Collections.sort(result);
        } catch (Exception e) {
            Log.e(getClass().getSimpleName(), "Load failed for podcast list!", e);
        } finally {
            // Make sure we close the file stream
            if (fileStream != null)
                try {
                    fileStream.close();
                } catch (IOException e) {
                    /* Nothing we can do here */
                    Log.w(getClass().getSimpleName(), "Failed to close podcast file stream!", e);
                }
        }

        return result;
    }

    @Override
    protected void onPostExecute(List<Podcast> result) {
        Log.i(getClass().getSimpleName(), "Added " + result.size() + " podcast(s) to list in "
                + (new Date().getTime() - startTime.getTime()) + "ms.");

        if (listener != null)
            listener.onPodcastListLoaded(result);
        else
            Log.w(getClass().getSimpleName(), "Podcast list loaded, but no listener attached");
    }

    /**
     * Read podcast information from the given parser and create a new podcast
     * object for it.
     * 
     * @param parser Parser to read from. Has to be set to the OPML outline
     *            start tag.
     * @return A new Podcast instance with name and URL set. If any error
     *         occurs, <code>null</code> is returned.
     */
    private Podcast createPodcast(XmlPullParser parser) {
        try {
            // Make sure we start at item tag
            parser.require(XmlPullParser.START_TAG, "", OPML.OUTLINE);
            // Get the podcast name
            String name = parser.getAttributeValue("", OPML.TEXT);
            // Make sure podcast name looks good
            if (name.equals("null"))
                name = null;
            else
                name = Html.fromHtml(name).toString();
            // Get and parse podcast url
            URL url = new URL(parser.getAttributeValue("", OPML.XMLURL));
            // Create the podcast
            return new Podcast(name, url);
        } catch (MalformedURLException e) {
            Log.w(getClass().getSimpleName(), "OPML outline has bad URL!", e);
        } catch (XmlPullParserException e) {
            Log.w(getClass().getSimpleName(), "OPML outline not parsable!", e);
        } catch (IOException e) { /* pass */
        }

        return null;
    }
}
