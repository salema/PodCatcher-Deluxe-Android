
package com.dragontek.mygpoclient;

public class Global {

    public static boolean DEBUG = false;

    public static String HOST = "gpodder.net";
    public static int VERSION = 2;
    public static int TOPLIST_DEFAULT = 25;
    public static String WEBSITE = "http://dragontek.github.com/mygpoclient-java";

    public static String USER_AGENT = String.format("mygpoclient-java/%s (%s)",
            VERSION, WEBSITE);

}
