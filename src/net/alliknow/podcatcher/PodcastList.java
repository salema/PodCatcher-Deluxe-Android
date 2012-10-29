/**
 * 
 */
package net.alliknow.podcatcher;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;

import javax.xml.parsers.DocumentBuilderFactory;

import net.alliknow.podcatcher.tags.OPML;
import net.alliknow.podcatcher.types.Podcast;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import android.content.Context;
import android.util.Log;

/**
 * @author kevin
 *
 */
public class PodcastList extends ArrayList<Podcast> {
	
	/** The name of the file we store our saved podcasts in (as OPML) */
	private static final String OPML_FILENAME = "podcasts.opml";
	/** The OPML file encoding */
	private static final String OPML_FILE_ENCODING = "utf8";
	
	private Context context;

	public PodcastList(Context context) {
		super();
		
		this.context = context;
	}
	
	public void load() {
		//this is just for testing
		//if (! Arrays.asList(getActivity().fileList()).contains(OPML_FILENAME))
		writeDummy();
		
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware(true);
			
			Document podcastFile = factory.newDocumentBuilder().parse(context.openFileInput(OPML_FILENAME));
			NodeList podcasts = podcastFile.getElementsByTagName(OPML.OUTLINE);
			
			for (int index = 0; index < podcasts.getLength(); index++) 
				add(new Podcast(podcasts.item(index)));
			
			Collections.sort(this); 
		} catch (Exception e) {
			Log.e(getClass().getSimpleName(), "Cannot load OPML file", e);
		}
	}
	
	public void store() {
		try {			
			BufferedWriter writer = getPodcastFileWriter();
			
			writer.write("<?xml version=\"1.0\" encoding=\"" + OPML_FILE_ENCODING + "\"?>");
			writer.write("<opml version=\"2.0\">");
			writer.write("<body>");
			
			for (Podcast podcast : this) writer.write(podcast.toOpmlString());
			
			writer.write("</body></opml>");
			writer.close();
			
			Log.d(getClass().getSimpleName(), "OPML podcast file written");
		} catch (Exception e) {
			Log.e(getClass().getSimpleName(), "Cannot store podcast OPML file", e);
		}
	}

	private void writeDummy() {
		try {
			BufferedWriter writer = getPodcastFileWriter();
			
			writer.write("<?xml version=\"1.0\" encoding=\"" + OPML_FILE_ENCODING + "\"?>");
			writer.write("<opml version=\"2.0\">");
			writer.write("<body>");
			writer.write("<outline text=\"This American Life\" xmlUrl=\"http://feeds.thisamericanlife.org/talpodcast\"/>");
			writer.write("<outline text=\"Radiolab\" xmlUrl=\"http://feeds.wnyc.org/radiolab\"/>");
			writer.write("<outline text=\"Linux' Outlaws\" xmlUrl=\"http://feeds.feedburner.com/linuxoutlaws\"/>");
			writer.write("<outline text=\"GEO\" xmlUrl=\"http://www.geo.de/GEOaudio/index.xml\"/>");
			writer.write("<outline text=\"Mäuse\" xmlUrl=\"http://podcast.wdr.de/maus.xml\"/>");
			writer.write("</body></opml>");
			writer.close();
			
			Log.d(getClass().getSimpleName(), "Dummy OPML written");
		} catch (Exception e) {
			Log.e(getClass().getSimpleName(), "Cannot write dummy OPML file", e);
		}
	}
	
	private BufferedWriter getPodcastFileWriter() throws FileNotFoundException,	UnsupportedEncodingException {
		FileOutputStream fos = context.openFileOutput(OPML_FILENAME, Context.MODE_PRIVATE);
		
		return new BufferedWriter(new OutputStreamWriter(fos, OPML_FILE_ENCODING));
	}
}
