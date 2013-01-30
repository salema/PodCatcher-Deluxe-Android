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
import android.text.TextUtils;
import android.util.Log;

import net.alliknow.podcatcher.Podcatcher;
import net.alliknow.podcatcher.R;
import net.alliknow.podcatcher.model.PodcastManager;
import net.alliknow.podcatcher.model.tags.OPML;
import net.alliknow.podcatcher.model.types.Podcast;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Date;
import java.util.List;

/**
 * Stores the default podcast list to the filesystem asynchronously.
 */
public class StorePodcastListTask extends AsyncTask<List<Podcast>, Progress, Void> {

    /** Our context */
    protected Context context;

    /** Content of OPML file title tag */
    protected String opmlFileTitle = "podcast file";
    /** The indent char */
    private static final char INDENT = ' ';

    /**
     * Create new task.
     * 
     * @param context Context to get file handle from. This will not be leaked
     *            if you keep a handle on this task, but set to
     *            <code>null</code> after execution.
     * @see Podcatcher.OPML_FILENAME
     */
    public StorePodcastListTask(Context context) {
        this.context = context;

        opmlFileTitle = context.getResources().getString(R.string.app_name) + " " + opmlFileTitle;
    }

    @Override
    protected Void doInBackground(List<Podcast>... params) {
        List<Podcast> list = params[0];

        OutputStream fileStream = null;

        try {
            if (list == null)
                throw new Exception("Podcast list cannot be null!");
            // Open the file and get a writer
            fileStream = context.openFileOutput(PodcastManager.OPML_FILENAME, Context.MODE_PRIVATE);
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(fileStream,
                    PodcastManager.OPML_FILE_ENCODING));
            // Write new file content
            writeHeader(writer);
            for (Podcast podcast : list)
                writePodcast(writer, podcast);
            writeFooter(writer);
            // Tidy up
            writer.close();
        } catch (Exception e) {
            Log.e(getClass().getSimpleName(), "Cannot store podcast OPML file", e);
        } finally {
            // Make sure we do not leak the context
            this.context = null;
            // Make sure we close the file stream
            if (fileStream != null)
                try {
                    fileStream.close();
                } catch (IOException e) { /* pass... */
                }
        }

        return null;
    }

    private void writePodcast(BufferedWriter writer, Podcast podcast) throws IOException {
        if (podcast.hasNameAndUrl()) {
            String opmlString = "<" + OPML.OUTLINE + " " + OPML.TEXT + "=\"" +
                    TextUtils.htmlEncode(podcast.getName()) + "\" " +
                    OPML.TYPE + "=\"" + OPML.RSS_TYPE + "\" " +
                    OPML.XMLURL + "=\"" + podcast.getUrl() + "\"/>";

            writeLine(writer, 2, opmlString);
        }
    }

    private void writeHeader(BufferedWriter writer) throws IOException {
        writeLine(writer, 0, "<?xml version=\"1.0\" encoding=\""
                + PodcastManager.OPML_FILE_ENCODING + "\"?>");
        writeLine(writer, 0, "<opml version=\"2.0\">");
        writeLine(writer, 1, "<head>");
        writeLine(writer, 2, "<title>" + opmlFileTitle + "</title>");
        writeLine(writer, 2, "<dateModified>" + new Date().toString() + "</dateModified>");
        writeLine(writer, 1, "</head>");
        writeLine(writer, 1, "<body>");
    }

    private void writeFooter(BufferedWriter writer) throws IOException {
        writeLine(writer, 1, "</body>");
        writeLine(writer, 0, "</opml>");
    }

    private void writeLine(BufferedWriter writer, int level, String line) throws IOException {
        for (int i = 0; i < level * 2; i++)
            writer.write(INDENT);

        writer.write(line);
        writer.newLine();
    }
}
