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

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

/**
 * Defines some util methods
 * 
 * @author Kevin Hausmann
 */
public class Podcatcher {
	
	/**
	 * Checks whether the app is in debug mode
	 * @param context Activity context
	 * @return true when in debug
	 */
	public static boolean isInDebugMode(Context context) {
		boolean debuggable = false;
		 
	    PackageManager manager = context.getPackageManager();
	    try
	    {
	        ApplicationInfo info = manager.getApplicationInfo(context.getPackageName(), 0);
	        debuggable = (0 != (info.flags &= ApplicationInfo.FLAG_DEBUGGABLE));
	    }
	    catch(Exception e) {}
	     
	    return debuggable;
	}
	
	/**
	 * Format an amount of time.
	 * @param time Amount in seconds to format
	 * @return The time span as hh:mm:ss with appropriate omissions
	 */
	public static String formatTime(int time) {
		int hours = Math.round(time / 3600);
		
		int minutes = Math.round((time / 60) - 60 * hours);
		int seconds = Math.round(time % 60);
		
		String minutesString = formatNumber(minutes, hours > 0);
		String secondsString = formatNumber(seconds, true);
		
		if (hours > 0) return hours + ":" + minutesString + ":" + secondsString;
		else return minutesString + ":" + secondsString; 
	}
	
	private static String formatNumber(int number, boolean makeTwoDigits) {
		if (number < 10 && makeTwoDigits) return "0" + number;
		else return number + "";
	}
}
