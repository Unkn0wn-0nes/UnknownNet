package com.Unkn0wn0ne.unknownnet.server;

import java.io.File;
import java.util.logging.Logger;

public class ConfigurationManager {

	private Logger logger = Logger.getLogger("UnknownNet");
	private int serverPort = 4334; 
	private boolean useSSL = false;
	private String protocolVersion = "unknownserver-dev";
	private boolean useServerGuard = true;
	private int maxClients = 1000;
	
	// TCP settings 
	private boolean TCP_NODELAY = true; 
	private int IP_TOS = 0x10;
	private boolean KEEP_ALIVE = true; // Note: Temporary, I just have to patch the client
	
	public ConfigurationManager() {
		File config = new File("unknownserver.properties");
		if (!config.exists()) {
			logger.severe("Internal/ConfigurationManager: unknownserver.properties does not exist, server will create this and run with the default settings");
			logger.severe("Internal/ConfigurationManager: These defaults could preset security and/or functionality risks depending on your implementation. UnknownNet-ServerGuard will be activated to monitor suspicious activity.");
		}
	}
	
	public int getServerPort() {
		return this.serverPort;
	}
	
	public boolean useSSL() {
		return this.useSSL;
	}
	
	public String getProtocolVersion() {
		return this.protocolVersion;
	}
	
	public boolean useServerGuard() {
		return this.useServerGuard;
	}
	
	public int getMaxClients() {
		return this.maxClients;
	}
	
	public boolean getTCPNoDelay() {
		return this.TCP_NODELAY;
	}
	
	public int getTrafficClass() {
		return this.IP_TOS;
	}
	
	public boolean getKeepAlive() {
		return this.KEEP_ALIVE;
	}
}
