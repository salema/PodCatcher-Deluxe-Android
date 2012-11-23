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

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import android.os.AsyncTask;

/**
 * Abstract super class for file download tasks.
 * 
 * @author Kevin Hausmann
 */
public abstract class LoadRemoteFileTask<Params, Result> extends AsyncTask<Params, Progress, Result> {
	
	/** The connection timeout */
	protected static final int CONNECT_TIMEOUT = 8000;
	/** The read timeout */
	protected static final int READ_TIMEOUT = 60000;
	
	/** Whether we prevent gzipping on server side */
	protected boolean preventZippedTranfer = false;
	/** Store whether loading failed */
	protected boolean failed = false;
	
	/**
	 * Whether the load task should prevent server side
	 * zipping of transfered file (improves progress information).
	 * @param prevent The flag (default is <code>false</code>).
	 */
	public void preventZippedTranfer(boolean prevent) {
		this.preventZippedTranfer = prevent;
	}
	
	/**
	 * Download the file and return it as a byte array.
	 * Will feed <code>publishProgress</code>.
	 * 
	 * @param remote URL connection to load from.
	 * @return The file content.
	 * @throws IOException If something goes wrong.
	 */
	protected byte[] loadFile(URL remote) throws IOException {
		return loadFile(remote, -1);
	}
	
	/**
	 * Download the file and return it as a byte array.
	 * Will feed <code>publishProgress</code>.
	 * 
	 * @param remote URL connection to load from.
	 * @param limit Maximum size (in bytes) for the file to load.
	 * @return The file content.
	 * @throws IOException If something goes wrong.
	 */
	protected byte[] loadFile(URL remote, int limit) throws IOException {
		HttpURLConnection connection = (HttpURLConnection) remote.openConnection();
		connection.setConnectTimeout(CONNECT_TIMEOUT);
		connection.setReadTimeout(READ_TIMEOUT);
		if (preventZippedTranfer) connection.setRequestProperty("Accept-Encoding", "identity");
		
		// TODO allow for password protected feeds 
		// String userpass = username + ":" + password;
		// String basicAuth = "Basic " + DatatypeCon.encode(userpass.getBytes()));
		// connection.setRequestProperty ("Authorization", basicAuth);
		
		InputStream remoteStream = null;
		ByteArrayOutputStream result = null;
		
		try {
			// Open stream and check whether we know its length
			remoteStream = new BufferedInputStream(connection.getInputStream());
			boolean sendLoadProgress = connection.getContentLength() > 0;
			
			// Create the byte buffer to write to
			result = new ByteArrayOutputStream();
			publishProgress(Progress.LOAD);
			
			byte[] buffer = new byte[1024];
			int bytesRead = 0;
			int totalBytes = 0;
			// Read stream and report progress (if possible)
			while((bytesRead = remoteStream.read(buffer)) > 0) {
				if (isCancelled()) return null;
				
				totalBytes += bytesRead;
				if (limit >= 0 && totalBytes > limit) return null;
				
				result.write(buffer, 0, bytesRead);
							  
				if (sendLoadProgress)
					publishProgress(new Progress(totalBytes, connection.getContentLength()));
			}
			
			// Return result as a byte array
			return result.toByteArray();
		} finally {
			// Close the streams
			if (remoteStream != null) remoteStream.close();
			if (result != null) result.close();
			// Disconnect
			connection.disconnect();
		}
	}
}
