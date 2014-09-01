/** Copyright 2014 Unkn0wn0ne

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License. **/
package com.Unkn0wn0ne.unknownnet.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.util.Properties;
import java.util.logging.Logger;

public class ConfigurationManager {

	private Logger logger = Logger.getLogger("UnknownNet");
	private int serverPort = 4334; 
	private boolean useSSL = false;
	private String protocolVersion = "unknownserver-dev";
	private int maxClients = 1000;
	
	// TCP settings 
	private boolean TCP_NODELAY = true; 
	private int IP_TOS = 0x10;
	private boolean KEEP_ALIVE = true; // Note: Temporary, I just have to patch the client
	
	private String protocol = "UDP";
	
	// UDP settings
	private int authServerPort = 4333;
	
	public ConfigurationManager() {
		File config = new File("unknownserver.properties");
		if (!config.exists()) {
			logger.severe("Internal/ConfigurationManager: unknownserver.properties does not exist, server will create this and run with the default settings");
			logger.severe("Internal/ConfigurationManager: These defaults could preset security and/or functionality risks depending on your implementation. UnknownNet-ServerGuard will be activated to monitor suspicious activity.");
			generateConfig(config);
		} else {
			loadConfig(config);
		}
	}
	
	private void loadConfig(File config) {
		logger.info("Internal/ConfigurationManager: Loading configuration file...");
		try {
			Properties sProps = new Properties();
			sProps.load(new FileInputStream(config));
			
			this.serverPort = Integer.parseInt(sProps.getProperty("server.port", "4334"));
			this.protocolVersion = sProps.getProperty("server.protocolversion", "unknownserver-dev");
			this.useSSL = Boolean.parseBoolean(sProps.getProperty("server.useSSL", "false"));
			this.maxClients = Integer.parseInt(sProps.getProperty("server.maxClients", "1000"));
			this.protocol = sProps.getProperty(sProps.getProperty("server.protocol", "TCP"));
			
			this.TCP_NODELAY = Boolean.parseBoolean(sProps.getProperty("tcp.nodelay", "true"));
			this.IP_TOS = Integer.parseInt(sProps.getProperty("tcp.iptos", "0x10"));
			this.KEEP_ALIVE = Boolean.parseBoolean(sProps.getProperty("tcp.keepalive", "true"));
			
			this.authServerPort = Integer.parseInt(sProps.getProperty("udp.authport", "4333"));
			logger.info("Internal/ConfigurationManager: Successfully loaded configuration file");
		} catch (Exception e) {
			logger.severe("Internal/ConfigurationManager: Failed to load configuration file, an Exception has occurred. Using defaults");
			e.printStackTrace();
		}
	}

	private void generateConfig(File config) {
		try {
			config.createNewFile();
			
			FileWriter fWriter = new FileWriter(config);
			fWriter.write("# UnknownNet Server Configuration Files. \n");
			fWriter.write("# These where automatically generated. Feel free to modify them to suit your needs. \n");
			
			fWriter.write("# General Settings \n");
			fWriter.write("server.port=4334 \n");
			fWriter.write("server.protocolversion=unknownserver-dev \n");
			fWriter.write("server.useSSL=false \n");
			fWriter.write("server.maxClients=1000 \n");
			fWriter.write("server.protocol=TCP \n");
			
			fWriter.write("# TCP Connection Specific Settings \n");
			fWriter.write("tcp.nodelay=true \n");
			fWriter.write("tcp.iptos=0x10 \n");
			fWriter.write("tcp.keepalive=true");
			
			fWriter.write("# UDP specific settings \n");
			fWriter.write("udp.authport=4333 \n");
			
			fWriter.write("# END OF CONFIGURATION FILE. \n");
			fWriter.flush();
			fWriter.close();
		} catch (Exception e) {
			logger.severe("Internal/ConfigurationManager: Failed to generate config file. An Exception has occurred.");
			e.printStackTrace();
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
	
	public String getProtocol() {
		return this.protocol;
	}
	
	public int getAuthServerPort() {
		return this.authServerPort;
	}
}
