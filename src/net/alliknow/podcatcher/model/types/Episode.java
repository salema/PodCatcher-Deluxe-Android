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

package net.alliknow.podcatcher.model.types;

import android.util.Log;

import net.alliknow.podcatcher.model.ParserUtils;
import net.alliknow.podcatcher.model.tags.RSS;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * The episode type. Each episode represents an item from a podcast's RSS/XML
 * feed. Episodes are created when the podcast is loaded (parsed), you should
 * have no need to create instances yourself.
 */
public class Episode implements Comparable<Episode> {

    /** The podcast this episode is part of */
    private Podcast podcast;

    /** This episode title */
    private String name;
    /** The episode's online location */
    private URL mediaUrl;
    /** The episode's release date */
    private Date pubDate;
    /** The episode duration */
    private String duration;
    /** The episode's description */
    private String description;
    /** The episode's long content description */
    private String content;

    /**
     * Create a new episode.
     * 
     * @param podcast Podcast this episode belongs to. Cannot be
     *            <code>null</code>.
     */
    public Episode(Podcast podcast) {
        if (podcast == null)
            throw new NullPointerException("Episode can not have null as the podcast instance!");

        this.podcast = podcast;
    }

    /**
     * @return The owning podcast. This will not be <code>null</code>.
     */
    public Podcast getPodcast() {
        return podcast;
    }

    /**
     * @return The episode's title.
     */
    public String getName() {
        return name;
    }

    /**
     * @return The media content online location.
     */
    public URL getMediaUrl() {
        return mediaUrl;
    }

    /**
     * @return The publication date for this episode.
     */
    public Date getPubDate() {
        if (pubDate == null)
            return null;
        else
            return new Date(pubDate.getTime());
    }

    /**
     * @return The episode's duration as given by the podcast feed. This might
     *         not be available and therefore <code>null</code>.
     */
    public String getDuration() {
        return duration;
    }

    /**
     * @return The description for this episode (if any). Might be
     *         <code>null</code>.
     */
    public String getDescription() {
        return description;
    }

    /**
     * @return The long content description for this episode from the
     *         content:encoded tag (if any). Might be <code>null</code>.
     */
    public String getLongDescription() {
        return content;
    }

    @Override
    public String toString() {
        return getName();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        else if (!(o instanceof Episode))
            return false;

        Episode other = (Episode) o;

        if (mediaUrl == null || other.getMediaUrl() == null)
            return false;
        else
            return mediaUrl.toString().equals(((Episode) o).getMediaUrl().toString());
    }

    @Override
    public int hashCode() {
        return mediaUrl == null ? 0 : mediaUrl.toString().hashCode();
    }

    @Override
    public int compareTo(Episode another) {
        if (pubDate == null || another == null || another.getPubDate() == null)
            return 0;
        else
            return -1 * pubDate.compareTo(another.getPubDate());
    }

    /**
     * Read data from an item node in the RSS/XML podcast file and use it to set
     * this episode's fields.
     * 
     * @param parser Podcast file parser, set to the start tag of the item to
     *            read.
     * @throws XmlPullParserException On parsing problems.
     * @throws IOException On I/O problems.
     */
    void parse(XmlPullParser parser) throws XmlPullParserException, IOException {
        // Make sure we start at item tag
        parser.require(XmlPullParser.START_TAG, "", RSS.ITEM);

        // Look at all start tags of this item
        while (parser.nextTag() == XmlPullParser.START_TAG) {
            final String tagName = parser.getName();

            // Episode title
            if (tagName.equalsIgnoreCase(RSS.TITLE))
                name = parser.nextText().trim();
            // Episode media URL
            else if (tagName.equalsIgnoreCase(RSS.ENCLOSURE)) {
                mediaUrl = createMediaUrl(parser.getAttributeValue("", RSS.URL));
                parser.nextText();
            }
            // Episode publication date (2 options)
            else if (tagName.equalsIgnoreCase(RSS.DATE) && pubDate == null)
                pubDate = parsePubDate(parser.nextText());
            else if (tagName.equalsIgnoreCase(RSS.PUBDATE) && pubDate == null)
                pubDate = parsePubDate(parser.nextText());
            // Episode duration
            else if (tagName.equalsIgnoreCase(RSS.DURATION))
                duration = parseDuration(parser.nextText());
            // Episode description
            else if (tagName.equalsIgnoreCase(RSS.DESCRIPTION))
                description = parser.nextText();
            else if (isContentEncodedTag(parser))
                content = parser.nextText();
            // Unneeded node, skip...
            else
                ParserUtils.skipSubTree(parser);
        }

        // Make sure we end at item tag
        parser.require(XmlPullParser.END_TAG, "", RSS.ITEM);
    }

    private boolean isContentEncodedTag(XmlPullParser parser) {
        return RSS.CONTENT_ENCODED.equals(parser.getName()) &&
                RSS.CONTENT_NAMESPACE.equals(parser.getNamespace(parser.getPrefix()));
    }

    private URL createMediaUrl(String url) {
        try {
            return new URL(url);
        } catch (MalformedURLException e) {
            Log.e(getClass().getSimpleName(), "Episode has invalid URL", e);
        }

        return null;
    }

    private Date parsePubDate(String value) {
        try {
            // RSS/XML files use this format for dates
            DateFormat formatter =
                    new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.ENGLISH);

            return formatter.parse(value);
        } catch (ParseException e) {
            Log.w(getClass().getSimpleName(), "Episode has invalid publication date", e);
        }

        return null;
    }

    private String parseDuration(String durationString) {
        try {
            // Do not allow something like 0:00
            if (Integer.parseInt(durationString.replace(':', '0')) == 0)
                return null;

            final int duration = Integer.parseInt(durationString);
            return duration / 60 + ":" + duration % 60;
        } catch (NumberFormatException e) {
            return durationString;
        }
    }
}
