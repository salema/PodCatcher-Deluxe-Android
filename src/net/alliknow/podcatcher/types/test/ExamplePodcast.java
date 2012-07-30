package net.alliknow.podcatcher.types.test;

import java.net.MalformedURLException;
import java.net.URL;

public enum ExamplePodcast {
	RADIOLAB("http://feeds.wnyc.org/radiolab"),
	THISAMERICANLIFE("http://feeds.thisamericanlife.org/talpodcast"),
	LINUXOUTLAWS("http://feeds.feedburner.com/linuxoutlaws");
	
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
