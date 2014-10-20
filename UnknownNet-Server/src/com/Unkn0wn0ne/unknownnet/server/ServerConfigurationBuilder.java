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

import com.Unkn0wn0ne.unknownnet.server.util.Protocol;

public class ServerConfigurationBuilder {
	
	private int serverPort = 4334; 
	private boolean useSSL = false;
	private String protocolVersion = "unknownserver-dev";
	private int maxClients = 1000;
	
	// TCP settings 
	private boolean TCP_NODELAY = true; 
	private int IP_TOS = 10;
	private boolean KEEP_ALIVE = false;
	
	private Protocol protocol = Protocol.TCP;
	
	// UDP settings
	private int authServerPort = 4334;
	
	public ServerConfigurationBuilder() {
		
	}
	
	public ServerConfigurationBuilder(int serverPort, boolean useSSL, String protocolVersion, int maxClients, boolean TCP_NODELAY, int IP_TOS, boolean KEEP_ALIVE, Protocol protocol, int authServerPort) {
		this.serverPort = serverPort;
		this.useSSL = useSSL;
		this.protocolVersion = protocolVersion;
		this.maxClients = maxClients;
		this.TCP_NODELAY = TCP_NODELAY;
		this.KEEP_ALIVE = KEEP_ALIVE;
		this.protocol = protocol;
		this.authServerPort = authServerPort;
	}
	
	public ServerConfigurationBuilder setServerPort(int newPort) {
		this.serverPort = newPort;
		return this;
	}
	
	public ServerConfigurationBuilder setUseSSL(boolean useSSL) {
		this.useSSL = useSSL;
		return this;
	}
	
	public ServerConfigurationBuilder setProtocolVersion(String protocolVersion) {
		this.protocolVersion = protocolVersion;
		return this;
	}
	
	public ServerConfigurationBuilder setMaxClients(int maxClients) {
		this.maxClients = maxClients;
		return this;
	}
	
	public ServerConfigurationBuilder setTCPNoDelay(boolean nodelay) {
		this.TCP_NODELAY = nodelay;
		return this;
	}
	
	public ServerConfigurationBuilder setProtocol(Protocol protocol) {
		this.protocol = protocol;
		return this;
	}
	
	public ServerConfigurationBuilder setAuthServerPort(int port) {
		this.authServerPort = port;
		return this;
	}
	
	public ServerConfigurationBuilder setIPTOS(int iptos) {
		this.IP_TOS = iptos;
		return this;
	}
	
	public ServerConfigurationBuilder setTCPKeepAlive(boolean keepalive) {
		this.KEEP_ALIVE = keepalive;
		return this;
	}
	
	public int getServerPort() {
		return this.serverPort;
	}
	
	public boolean isUsingSSL() {
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
	
	public Protocol getProtocol() {
		return this.protocol;
	}
	
	public int getAuthServerPort() {
		return this.authServerPort;
	}

	public int getIPTOS() {
		return this.IP_TOS;
	}

	public boolean getTCPKeepAlive() {
		return this.KEEP_ALIVE;
	}
}