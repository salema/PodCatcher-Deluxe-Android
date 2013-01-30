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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import net.alliknow.podcatcher.model.tags.RSS;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.graphics.Bitmap;
import android.util.Log;

/**
 * The podcast type.
 */
public class Podcast implements Comparable<Podcast> {

    /**
     * The minimum time podcast content is buffered (in milliseconds). If older,
     * we need to reload.
     */
    public static final int TIME_TO_LIFE = 30 * 60 * 1000;

    /** Name of the podcast */
    private String name;
    /** Location of the podcast's RSS file */
    private URL url;
    /** Podcast's description */
    private String description;
    /** Broadcast language */
    private Language language;
    /** Podcast genre */
    private Genre genre;
    /** Podcast media type */
    private MediaType mediaType;
    /** The podcasts list of episodes */
    private List<Episode> episodes = new ArrayList<Episode>();
    /** The podcast's image (logo) location */
    private URL logoUrl;
    /** The cached logo bitmap */
    private Bitmap logo;
    /** The point in time when the RSS file as last been set */
    private Date updated;
    /** The encoding of the loaded file */
    private String encoding;
    /** Whether this podcast is currently loading */
    private boolean loading;

    /**
     * Create a new podcast by name and RSS file location. The name will not be
     * read from the file, but remains as given (unless you give
     * <code>null</code> as the name). All other data on the podcast will only
     * be available after <code>parse()</code> was called.
     * 
     * @param name The podcast's name, if you give <code>null</code> the name
     *            will be read from the RSS file (if set afterwards).
     * @param url The location of the podcast's RSS file.
     * @see parse
     */
    public Podcast(String name, URL url) {
        this.name = name;
        this.url = url;
    }

    /**
     * @return The podcast's name.
     */
    public String getName() {
        return name;
    }

    /**
     * @return The podcast's online location.
     */
    public URL getUrl() {
        return url;
    }

    /**
     * @return The description.
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param description The description to set.
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * @return The language.
     */
    public Language getLanguage() {
        return language;
    }

    /**
     * @param language The language to set.
     */
    public void setLanguage(Language language) {
        this.language = language;
    }

    /**
     * @return The genre.
     */
    public Genre getGenre() {
        return genre;
    }

    /**
     * @param genre The genre to set.
     */
    public void setGenre(Genre genre) {
        this.genre = genre;
    }

    /**
     * @return The mediaType.
     */
    public MediaType getMediaType() {
        return mediaType;
    }

    /**
     * @param mediaType The mediaType to set.
     */
    public void setMediaType(MediaType mediaType) {
        this.mediaType = mediaType;
    }

    /**
     * Find and return all episodes for this podcast. Will never return
     * <code>null</code> but an empty list when encountering problems. Set the
     * RSS file before expecting any results.
     * 
     * @return The list of episodes as listed in the feed.
     * @see parse
     */
    public List<Episode> getEpisodes() {
        return episodes;
    }

    /**
     * Replace current episode list with an empty one.
     */
    public void resetEpisodes() {
        episodes.clear();
    }

    /**
     * Find and return the podcast's image location (logo). Only works after RSS
     * file is set.
     * 
     * @return URL pointing at the logo location.
     * @see parse
     */
    public URL getLogoUrl() {
        return logoUrl;
    }

    /**
     * Get a cached logo for this podcast.
     * 
     * @return The cached logo if it was previously set using
     *         <code>setLogo()</code>, <code>null</code> otherwise.
     */
    public Bitmap getLogo() {
        return logo;
    }

    /**
     * Cache the podcast given.
     * 
     * @param logo Logo to use for this podcast.
     */
    public void setLogo(Bitmap logo) {
        this.logo = logo;
    }

    /**
     * The podcast's encoding.
     * 
     * @return Get the input encoding for the podcast file loaded. This may be
     *         <code>null</code>, if unknown.
     */
    public String getEncoding() {
        return encoding;
    }

    /**
     * Set the RSS file parser representing this podcast. This is were the
     * object gets its information from. Many of its methods will not return
     * valid results unless this method was called. Calling this method also
     * resets all information read earlier.
     * 
     * @param parser Parser used to read the RSS/XML file.
     * @throws IOException If we encounter problems read the file.
     * @throws XmlPullParserException On parsing errors.
     */
    public void parse(XmlPullParser parser) throws XmlPullParserException, IOException {
        // Reset state
        resetEpisodes();
        updated = new Date();
        encoding = parser.getInputEncoding();

        // Start parsing
        int eventType = parser.next();

        // Read complete document
        while (eventType != XmlPullParser.END_DOCUMENT) {
            // We only need start tags here
            if (eventType == XmlPullParser.START_TAG) {
                String tagName = parser.getName();

                // Podcast name found
                if (tagName.equalsIgnoreCase(RSS.TITLE))
                    loadName(parser);
                // Image found
                else if (tagName.equalsIgnoreCase(RSS.IMAGE))
                    loadImage(parser);
                // Thumbnail found
                else if (tagName.equalsIgnoreCase(RSS.THUMBNAIL))
                    loadThumbnail(parser);
                // Episode found
                else if (tagName.equalsIgnoreCase(RSS.ITEM))
                    loadEpisode(parser);
            }

            // Done, get next parsing event
            eventType = parser.next();
        }
    }

    /**
     * Whether the podcast content is old enough to need reloading. This relates
     * to the time that <code>parse</code> has last been called on this object
     * and has nothing to do with the updating of the podcast RSS file on the
     * provider's server.
     * 
     * @return <code>true</code> iff time to live expired or the podcast has
     *         never been loaded.
     */
    public boolean needsReload() {
        // Has never been loaded
        if (updated == null)
            return true;
        // Check age
        else
            return new Date().getTime() - updated.getTime() > TIME_TO_LIFE;
    }

    /**
     * Check whether the podcast is currently in the loading state. This only
     * works if somebody set the flag via <code>setLoading()</code>.
     * 
     * @return The flag.
     */
    public boolean isLoading() {
        return loading;
    }

    /**
     * Set the loading flag.
     * 
     * @param loading The flag to set.
     */
    public void setLoading(boolean loading) {
        this.loading = loading;
    }

    /**
     * @return Whether this podcast has an non-empty name and an URL.
     */
    public boolean hasNameAndUrl() {
        return name != null && name.length() > 0 && url != null;
    }

    @Override
    public String toString() {
        if (name == null)
            return "Unnamed podcast";
        if (url == null)
            return name;
        else
            return name + " at " + url.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        else if (!(o instanceof Podcast))
            return false;

        Podcast other = (Podcast) o;

        if (url == null || other.getUrl() == null)
            return false;
        else
            return url.toString().equals(((Podcast) o).getUrl().toString());
    }

    @Override
    public int hashCode() {
        return url == null ? 0 : url.toString().hashCode();
    }

    @Override
    public int compareTo(Podcast another) {
        if (name == null || another == null || another.getName() == null)
            return 0;
        else
            return getName().compareToIgnoreCase(another.getName());
    }

    private void loadName(XmlPullParser parser) throws XmlPullParserException, IOException {
        // Only update the name if not set
        if (name == null)
            name = parser.nextText();
    }

    private void loadImage(XmlPullParser parser) throws XmlPullParserException, IOException {
        // HREF attribute used?
        if (parser.getAttributeValue("", RSS.HREF) != null)
            logoUrl = createLogoUrl(parser.getAttributeValue("", RSS.HREF));
        // URL tag used!
        else {
            // Make sure we start at image tag
            parser.require(XmlPullParser.START_TAG, "", RSS.IMAGE);

            // Look at all start tags of this image
            while (parser.nextTag() == XmlPullParser.START_TAG) {
                // URL tag found
                if (parser.getName().equalsIgnoreCase(RSS.URL))
                    logoUrl = createLogoUrl(parser.nextText());
                // Unneeded node, skip...
                else
                    ParserUtils.skipSubTree(parser);
            }

            // Make sure we end at image tag
            parser.require(XmlPullParser.END_TAG, "", RSS.IMAGE);
        }
    }

    private void loadThumbnail(XmlPullParser parser) {
        // Some podcasts use thumbnails instead of images
        logoUrl = createLogoUrl(parser.getAttributeValue("", RSS.URL));
    }

    private URL createLogoUrl(String nodeValue) {
        try {
            return new URL(nodeValue);
        } catch (MalformedURLException e) {
            Log.e(getClass().getSimpleName(), "Podcast has invalid logo URL", e);
        }

        return null;
    }

    private void loadEpisode(XmlPullParser parser) throws XmlPullParserException, IOException {
        // Create episode and parse the data
        Episode newEpisode = new Episode(this);
        newEpisode.parse(parser);

        // Only add if there is some actual content to play
        if (newEpisode.getMediaUrl() != null)
            episodes.add(newEpisode);
    }
}
