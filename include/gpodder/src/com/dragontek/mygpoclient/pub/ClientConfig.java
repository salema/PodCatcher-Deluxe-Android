package com.dragontek.mygpoclient.pub;

import java.util.Map;

import com.google.gson.annotations.SerializedName;

public class ClientConfig {
	public Map<String, String> mygpo;
	@SerializedName("mygpo-feedservice")
	public Map<String, String> mygpo_feedservice;
	public long update_timeout;

}
