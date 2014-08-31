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
	
	private String protocol = "UDP";
	
	// UDP settings
	private int authServerPort = 4333;
	
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
	
	public String getProtocol() {
		return this.protocol;
	}
	
	public int getAuthServerPort() {
		return this.authServerPort;
	}
}
