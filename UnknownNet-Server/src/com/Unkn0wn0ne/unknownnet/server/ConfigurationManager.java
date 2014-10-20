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

import com.Unkn0wn0ne.unknownnet.server.util.Protocol;

public class ConfigurationManager {

	private Logger logger = Logger.getLogger("UnknownNet");
	
	private boolean useFileSystem = false;
	private ServerConfigurationBuilder config = null;
	
	public void load() {
		if (!useFileSystem && config == null) {
			this.logger.warning("Internal/ConfigurationManager: useFileSystem is false, but supplied configuration is null. Using defaults.");
			this.config = new ServerConfigurationBuilder();
			return;
		} else if (this.useFileSystem && config != null) {
			this.logger.warning("Internal/ConfigurationManager: useFileSystem is true, but there is a supplied configuration. Using supplied config");
			return;
		} else if (!this.useFileSystem && config != null) {
			this.logger.info("Internal/ConfigurationManager: Using supplied configuration.");
			return;
		} 
		
		this.config = new ServerConfigurationBuilder();
		this.logger.info("Internal/ConfigurationManager: Using file sytem for configuration information.");
		
		File fConfig = new File("unknownserver.properties");
		if (!fConfig.exists()) {
			logger.severe("Internal/ConfigurationManager: unknownserver.properties does not exist, server will create this and run with the default settings");
			logger.severe("Internal/ConfigurationManager: These defaults could preset security and/or functionality risks depending on your implementation. UnknownNet-ServerGuard will be activated to monitor suspicious activity.");
			generateConfig(fConfig);
		} else {
			loadConfig(fConfig);
		}
	}
	

	private void loadConfig(File fConfig) {
		logger.info("Internal/ConfigurationManager: Loading configuration file...");
		try {
			Properties sProps = new Properties();
			sProps.load(new FileInputStream(fConfig));
			
			this.config.setServerPort(Integer.parseInt(sProps.getProperty("server.port", "4334").trim()))
			 .setProtocolVersion(sProps.getProperty("server.protocolversion", "unknownserver-dev").trim())
			 .setUseSSL(Boolean.parseBoolean(sProps.getProperty("server.useSSL", "false").trim()))
			 .setMaxClients(Integer.parseInt(sProps.getProperty("server.maxClients", "1000").trim()))
			 .setProtocol(Protocol.valueOf(sProps.getProperty("server.protocol", "TCP").trim()))
			 .setTCPNoDelay(Boolean.parseBoolean(sProps.getProperty("tcp.nodelay", "true").trim()))
			 .setIPTOS(Integer.parseInt(sProps.getProperty("tcp.iptos", "10").trim()))
			 .setTCPKeepAlive(Boolean.parseBoolean(sProps.getProperty("tcp.keepalive", "true").trim()))
			 .setAuthServerPort(Integer.parseInt(sProps.getProperty("udp.authport", "4333").trim()));
			logger.info("Internal/ConfigurationManager: Successfully loaded configuration file");
		} catch (Exception e) {
			logger.severe("Internal/ConfigurationManager: Failed to load configuration file, an Exception has occurred. Using defaults");
			e.printStackTrace();
		}
	}

	private void generateConfig(File fConfig) {
		try {
			fConfig.createNewFile();
			
			FileWriter fWriter = new FileWriter(fConfig);
			fWriter.write("# UnknownNet Server Configuration Files.\n");
			fWriter.write("# These where automatically generated. Feel free to modify them to suit your needs. \n");
			
			fWriter.write("# General Settings\n");
			fWriter.write("# server.port - Port the server's client acceptor runs on. \n");
			fWriter.write("# server.protocolversion - A unique string that both your client and server must have to verify that the protocol is up to date. \n");
			fWriter.write("# server.useSSL - Use SSL over TCP to protect user information and help prevent data stream manipulation. \n");
			fWriter.write("# server.maxClients - The number of clients that can be concurrently connected to the server. \n");
			fWriter.write("# server.protocol - The protocol that the server will be using. This can ethier be TCP or UDP or dualstack. \n");
			
			fWriter.write("server.port=4334\n");
			fWriter.write("server.protocolversion=unknownserver-dev\n");
			fWriter.write("server.useSSL=false\n");
			fWriter.write("server.maxClients=1000\n");
			fWriter.write("server.protocol=TCP\n");
			
			fWriter.write("# TCP Connection Specific Settings\n");
			fWriter.write("tcp.nodelay=true\n");
			fWriter.write("tcp.iptos=10\n");
			fWriter.write("tcp.keepalive=false\n");
			
			fWriter.write("# UDP specific settings\n");
			fWriter.write("udp.authport=4334\n");
			
			fWriter.write("# END OF CONFIGURATION FILE.\n");
			fWriter.flush();
			fWriter.close();
		} catch (Exception e) {
			logger.severe("Internal/ConfigurationManager: Failed to generate config file. An Exception has occurred.");
			e.printStackTrace();
		}
	}

	public int getServerPort() {
		return this.config.getServerPort();
	}
	
	public boolean useSSL() {
		return this.config.isUsingSSL();
	}
	
	public String getProtocolVersion() {
		return this.config.getProtocolVersion();
	}
	
	
	public int getMaxClients() {
		return this.config.getMaxClients();
	}
	
	public boolean getTCPNoDelay() {
		return this.config.getTCPNoDelay();
	}
	
	public int getTrafficClass() {
		return this.config.getIPTOS();
	}
	
	public boolean getKeepAlive() {
		return this.config.getTCPKeepAlive();
	}
	
	public String getProtocol() {
		return this.config.getProtocol().getProtocol();
	}
	
	public int getAuthServerPort() {
		return this.config.getAuthServerPort();
	}

	public void setConfiguration(ServerConfigurationBuilder config) {
		this.config = config;
	}

	public void setUseFileSystem(boolean useFileSystem) {
		this.useFileSystem = useFileSystem;
	}
}
