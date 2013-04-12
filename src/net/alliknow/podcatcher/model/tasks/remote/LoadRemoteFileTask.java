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

package net.alliknow.podcatcher.model.tasks.remote;

import android.os.AsyncTask;
import android.util.Log;

import net.alliknow.podcatcher.model.types.Progress;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Abstract super class for file download tasks.
 * 
 * @param <Params> Params as defined by {@link AsyncTask}
 * @param <Result> Result as defined by {@link AsyncTask}
 * @see AsyncTask
 */
public abstract class LoadRemoteFileTask<Params, Result> extends
        AsyncTask<Params, Progress, Result> {

    /** The connection timeout */
    protected static final int CONNECT_TIMEOUT = 8000;
    /** The read timeout */
    protected static final int READ_TIMEOUT = 60000;

    /** A file size limit in bytes for the download */
    protected int loadLimit = -1;
    /** Whether we prevent zipping on server side */
    protected boolean preventZippedTransfer = false;

    /**
     * Set a load limit for the actual download of the file. The default is a
     * negative number, turning off the limit evaluation. If positive and
     * reached {@link #loadFile(URL)} below will return <code>null</code>
     * immediately.
     * 
     * @param limit The limit to set in bytes.
     */
    public void setLoadLimit(int limit) {
        this.loadLimit = limit;
    }

    /**
     * Whether the load task should prevent server side zipping of transfered
     * file (improves progress information).
     * 
     * @param prevent The flag (default is <code>false</code>).
     */
    public void preventZippedTransfer(boolean prevent) {
        this.preventZippedTransfer = prevent;
    }

    /**
     * Download the file and return it as a byte array. Will feed
     * {@link #publishProgress(Object...)}.
     * 
     * @param remote URL connection to load from.
     * @return The file content.
     * @throws IOException If something goes wrong.
     */
    protected byte[] loadFile(URL remote) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) remote.openConnection();
        connection.setConnectTimeout(CONNECT_TIMEOUT);
        connection.setReadTimeout(READ_TIMEOUT);
        if (preventZippedTransfer)
            connection.setRequestProperty("Accept-Encoding", "identity");

        // TODO allow for password protected feeds
        // String userpass = username + ":" + password;
        // String basicAuth = "Basic " +
        // DatatypeCon.encode(userpass.getBytes()));
        // connection.setRequestProperty ("Authorization", basicAuth);

        BufferedInputStream bufferedRemoteStream = null;
        ByteArrayOutputStream result = null;

        try {
            // 1. Open stream and check whether we know its length
            bufferedRemoteStream = new BufferedInputStream(connection.getInputStream());
            // Check whether we should abort load since we have a load limit set
            // and the content length is higher.
            final int contentLength = connection.getContentLength();
            if (loadLimit >= 0 && contentLength != -1 && contentLength > loadLimit)
                throw new IOException("Load limit exceeded (content length reported by remote is "
                        + contentLength + " bytes, limit was " + loadLimit + " bytes)!");
            // Check whether we could calculate the percentage of completion
            boolean sendLoadProgress = contentLength > 0;

            // 2. Create the byte buffer to write to
            result = new ByteArrayOutputStream();
            publishProgress(Progress.LOAD);

            byte[] buffer = new byte[1024];
            int bytesRead = 0;
            int totalBytes = 0;

            // 3. Read stream and report progress (if possible)
            while ((bytesRead = bufferedRemoteStream.read(buffer)) > 0) {
                if (isCancelled())
                    return null;

                totalBytes += bytesRead;
                if (loadLimit >= 0 && totalBytes > loadLimit)
                    throw new IOException("Load limit exceeded (read " + totalBytes +
                            " bytes, limit was " + loadLimit + " bytes)!");

                result.write(buffer, 0, bytesRead);

                if (sendLoadProgress)
                    publishProgress(new Progress(totalBytes, connection.getContentLength()));
            }

            // 4. Return result as a byte array
            return result.toByteArray();
        } finally {
            // Close the streams
            // To remote
            if (bufferedRemoteStream != null)
                try {
                    bufferedRemoteStream.close();
                } catch (Exception e) {
                    Log.w(getClass().getSimpleName(), "Failed to close remote stream", e);
                }

            // To the local byte array
            if (result != null)
                try {
                    result.close();
                } catch (Exception e) {
                    Log.w(getClass().getSimpleName(), "Failed to close local output stream", e);
                }

            // Disconnect
            connection.disconnect();
        }
    }
}
