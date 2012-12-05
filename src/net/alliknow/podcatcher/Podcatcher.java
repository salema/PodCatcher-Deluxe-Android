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
package net.alliknow.podcatcher;

import static android.content.Context.CONNECTIVITY_SERVICE;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.List;

import net.alliknow.podcatcher.types.Podcast;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.text.Html;
import android.util.Log;

/**
 * Defines some util methods.
 */
public class Podcatcher {
	
	/** The name of the file we store our saved podcasts in (as OPML) */
	public static final String OPML_FILENAME = "podcasts.opml";
	/** The OPML file encoding */
	public static final String OPML_FILE_ENCODING = "utf8";
	
	/**
	 * Clear list.
	 * Add a small number of sample podcast to the list for testing.
	 * Sort list.
	 * @param list List to fill.
	 */
	public static void putSamplePodcasts(List<Podcast> list) {
		list.clear();
		
		list.add(createPodcast("This American Life", "http://feeds.thisamericanlife.org/talpodcast"));
		list.add(createPodcast("Radiolab", "http://feeds.wnyc.org/radiolab"));
		list.add(createPodcast("Linux' Outlaws", "http://feeds.feedburner.com/linuxoutlaws"));
		list.add(createPodcast("GEO", "http://www.geo.de/GEOaudio/index.xml"));
		list.add(createPodcast("Mäuse", "http://podcast.wdr.de/maus.xml"));
		list.add(createPodcast("D&uuml;de", "http://feeds.feedburner.com/UhhYeahDude"));
		list.add(createPodcast("neo", "http://www.zdf.de/ZDFmediathek/podcast/1446344?view=podcast"));
		
		// Remove null elements if accidentially create and added above
		while (list.remove(null));
		
		Collections.sort(list);
	}
	
	private static Podcast createPodcast(String name, String url) {
		try {
			return new Podcast(Html.fromHtml(name).toString(), new URL(url));
		} catch (MalformedURLException e) {
			Log.e("Podcatcher", "Cannot add sample podcast: " + name, e);
			return null;
		}
	}
	
	/**
	 * Checks whether the device is currently on a fast
	 * network (such as wifi) as opposed to a mobile network. 
	 * @return <code>true</code> iff we have fast
	 * (and potentially free) Internet access.
	 * @param context Context to check.
	 */
	public static boolean isOnFastConnection(Context context) {
		if (context == null) return false;
		
		ConnectivityManager manager = (ConnectivityManager) context.getSystemService(CONNECTIVITY_SERVICE);
		NetworkInfo activeNetwork = manager.getActiveNetworkInfo();
		
		if (activeNetwork == null) return false;
		else switch (activeNetwork.getType()) {
			case ConnectivityManager.TYPE_ETHERNET:
			case ConnectivityManager.TYPE_WIFI:
			case ConnectivityManager.TYPE_WIMAX:
				return true;
			default: 
				return false;
		}
	}
	
	/**
	 * Checks whether the app is in debug mode.
	 * @param context Activity context to check.
	 * @return <code>true</code> iff in debug.
	 */
	public static boolean isInDebugMode(Context context) {
		boolean debug = false;
		 
	    PackageManager manager = context.getPackageManager();
	    try
	    {
	        ApplicationInfo info = manager.getApplicationInfo(context.getPackageName(), 0);
	        debug = (0 != (info.flags &= ApplicationInfo.FLAG_DEBUGGABLE));
	    }
	    catch(Exception e) {}
	     
	    return debug;
	}
	
	/**
	 * Skip the entire sub tree the given parser is
	 * currently pointing at.
	 * @param parser Parser to advance.
	 * @throws XmlPullParserException On parsing problems.
	 * @throws IOException On I/O trouble.
	 */
	public static void skipSubTree(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, null, null);
        
        int level = 1;
        while (level > 0) {
            int eventType = parser.next();
            if (eventType == XmlPullParser.END_TAG) {
                --level;
            } else if (eventType == XmlPullParser.START_TAG) {
                ++level;
            }
        }
    }
}
