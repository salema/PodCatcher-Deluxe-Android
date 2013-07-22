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

package net.alliknow.podcatcher.model;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

/**
 * Utility class to support podcast XML/RSS parsing.
 */
public class ParserUtils {

    /**
     * Skip the entire sub tree the given parser is currently pointing at.
     * 
     * @param parser Parser to advance.
     * @throws XmlPullParserException On parsing problems.
     * @throws IOException On I/O trouble.
     */
    public static void skipSubTree(XmlPullParser parser) throws XmlPullParserException, IOException {
        // We need to see a start tag next. The tag and any sub-tree it might
        // have will be skipped.
        parser.require(XmlPullParser.START_TAG, null, null);

        int level = 1;
        // Continue parsing and increase/decrease the level
        while (level > 0) {
            int eventType = parser.next();
            if (eventType == XmlPullParser.END_TAG) {
                --level;
            } else if (eventType == XmlPullParser.START_TAG) {
                ++level;
            }
        }

        // We are back to the original level, behind the start tag given and any
        // sub-tree that might have been there. Return.
    }

    /**
     * Format an amount of time.
     * 
     * @param time Amount in seconds to format.
     * @return The time span as hh:mm:ss with appropriate omissions.
     */
    public static String formatTime(int time) {
        int hours = time / 3600;

        int minutes = (time / 60) - 60 * hours;
        int seconds = time % 60;

        String minutesString = formatNumber(minutes, hours > 0);
        String secondsString = formatNumber(seconds, true);

        if (hours > 0)
            return hours + ":" + minutesString + ":" + secondsString;
        else
            return minutesString + ":" + secondsString;
    }

    private static String formatNumber(int number, boolean makeTwoDigits) {
        if (number < 10 && makeTwoDigits)
            return "0" + number;
        else
            return number + "";
    }
}
