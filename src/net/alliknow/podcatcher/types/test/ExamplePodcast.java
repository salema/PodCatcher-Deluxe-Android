package net.alliknow.podcatcher.types.test;

import java.net.MalformedURLException;
import java.net.URL;

public enum ExamplePodcast {
	SCAMSCHOOL("http://revision3.com/scamschool/feed/MP4-Large"),
	RADIOLAB("http://feeds.wnyc.org/radiolab"),
	THISAMERICANLIFE("http://feeds.thisamericanlife.org/talpodcast"),
	LINUXOUTLAWS("http://feeds.feedburner.com/linuxoutlaws"),
	NASAEDGE("http://www.nasa.gov/rss/NASAcast_vodcast.rss"),
	OLAFSCHUBERT("http://www.sputnik.de/podcast/serve.php?typ=rss&podcastid=9"),
	MAUS("http://podcast.wdr.de/maus.xml"),
	DAILYBACON("http://downloads.bbc.co.uk/podcasts/fivelive/dailybacon/rss.xml"),
	GREENCAST("http://www.greenpeace-berlin.de/fileadmin/podcast/greencast.xml"),
	NERDIST("http://nerdist.libsyn.com/rss");
	
	
	private String url;
	
	private ExamplePodcast(String url) {
		this.url = url;
	}
	
	public URL getURL() {
		try {
			return new URL(this.url);
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
}
