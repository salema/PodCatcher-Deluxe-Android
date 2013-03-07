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
import android.util.Log;

import net.alliknow.podcatcher.listeners.OnLoadEpisodeMetadataListener;
import net.alliknow.podcatcher.model.EpisodeManager;
import net.alliknow.podcatcher.model.tags.METADATA;
import net.alliknow.podcatcher.model.types.EpisodeMetadata;
import net.alliknow.podcatcher.model.types.Progress;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Load the episode metadata from the file system.
 */
public class LoadEpisodeMetadataTask extends AsyncTask<Void, Progress, Map<URL, EpisodeMetadata>> {

    /** Our context */
    private Context context;
    /** The listener callback */
    private OnLoadEpisodeMetadataListener listener;

    /** Member to measure performance */
    private Date startTime;

    /**
     * Create new task.
     * 
     * @param context Context to read file from (not <code>null</code>).
     * @param listener Callback to be alerted on completion. Could be
     *            <code>null</code>, but then nobody would ever know that this
     *            task finished.
     * @see EpisodeManager#METADATA_FILENAME
     */
    public LoadEpisodeMetadataTask(Context context, OnLoadEpisodeMetadataListener listener) {
        this.context = context;
        this.listener = listener;
    }

    @Override
    protected Map<URL, EpisodeMetadata> doInBackground(Void... params) {
        // Record start time
        this.startTime = new Date();

        // Create resulting data structure and file stream
        Map<URL, EpisodeMetadata> result = new HashMap<URL, EpisodeMetadata>();
        InputStream fileStream = null;

        try {
            // 1. Build parser
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            // Create the parser to use
            XmlPullParser parser = factory.newPullParser();

            // 2. Open default podcast file
            fileStream = context.openFileInput(EpisodeManager.METADATA_FILENAME);
            parser.setInput(fileStream, StoreFileTask.FILE_ENCODING);

            // 3. Parse the OPML file
            int eventType = parser.next();

            // Read complete document
            while (eventType != XmlPullParser.END_DOCUMENT) {
                // We only need start tags here
                if (eventType == XmlPullParser.START_TAG) {
                    String tagName = parser.getName();

                    // Metadata found
                    if (tagName.equalsIgnoreCase(METADATA.METADATA)) {
                        URL key = new URL(parser.getAttributeValue(null, METADATA.EPISODE_URL));
                        EpisodeMetadata metadata = readMetadata(parser);

                        result.put(key, metadata);
                    }

                }

                // Done, get next parsing event
                eventType = parser.next();
            }
        } catch (Exception e) {
            Log.e(getClass().getSimpleName(), "Load failed for episode metadata!", e);
        } finally {
            // Make sure we close the file stream
            if (fileStream != null)
                try {
                    fileStream.close();
                } catch (IOException e) {
                    /* Nothing we can do here */
                    Log.w(getClass().getSimpleName(),
                            "Failed to close episode metadata file stream!", e);
                }
        }

        return result;
    }

    @Override
    protected void onPostExecute(Map<URL, EpisodeMetadata> result) {
        Log.d(getClass().getSimpleName(), "Read " + result.size() + " metadata records in "
                + (new Date().getTime() - startTime.getTime()) + "ms.");

        if (listener != null)
            listener.onEpisodeMetadataLoaded(result);
        else
            Log.w(getClass().getSimpleName(), "Episode metadata loaded, but no listener attached");
    }

    private EpisodeMetadata readMetadata(XmlPullParser parser)
            throws XmlPullParserException, IOException {

        // Create the resulting metadata record
        EpisodeMetadata result = new EpisodeMetadata();

        // Parse the metadata information
        int eventType = parser.next();

        // Read till the end of the metadata tag is reached
        while (!(eventType == XmlPullParser.END_TAG && parser.getName().equalsIgnoreCase(
                METADATA.METADATA))) {

            // We only need start tags here
            if (eventType == XmlPullParser.START_TAG) {
                String tagName = parser.getName();

                // Metadata detail found
                if (tagName.equalsIgnoreCase(METADATA.DOWNLOAD_ID))
                    result.downloadId = Long.parseLong(parser.nextText());
                else if (tagName.equalsIgnoreCase(METADATA.LOCAL_FILE_PATH))
                    result.filePath = parser.nextText();
            }

            // Done, get next parsing event
            eventType = parser.next();
        }

        return result;
    }
}
