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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import com.Unkn0wn0ne.unknownnet.server.logging.LogType;
import com.Unkn0wn0ne.unknownnet.server.logging.UnknownLogger;

public class ServerGuard {
	private List<String> bannedIPs = new ArrayList<String>();
	
	public void init() {
		UnknownLogger.log(Level.INFO, LogType.DEBUG_INFORMATION, "Internal/UnknownServerGuard: Init");
		loadBannedIps();
	}
	
	private void loadBannedIps() {
		File bannedIps = new File("banned-ips.dat");
		if (!bannedIps.exists()) {
			try {
				bannedIps.createNewFile();
				return;
			} catch (IOException e) {
				UnknownLogger.log(Level.SEVERE, LogType.SECURITY_WARNING, "Internal/UnknownServerGuard: Failed to create banned-ips.dat, an IOException occurred.", e);
				return;
			}
		}
		
		try {
			FileReader fReader = new FileReader(bannedIps);
			BufferedReader reader = new BufferedReader(fReader);
			
			String msg = null;
			try {
				while ((msg = reader.readLine()) != null) {
					synchronized (this.bannedIPs) {
						this.bannedIPs.add(msg);
					}
				}
			} catch (IOException e) {
				UnknownLogger.log(Level.SEVERE, LogType.SECURITY_WARNING, "Internal/UnknownServerGuard: Failed to load banned-ips.dat, an IOException occurred.", e);
			}
			
			try {
				reader.close();
			} catch (IOException e) {
				UnknownLogger.log(Level.SEVERE, LogType.DEBUG_INFORMATION, "Internal/UnknownServerGuard: Failed to close reader, an IOException occurred.", e);
			}
		} catch (FileNotFoundException e) {
			UnknownLogger.log(Level.SEVERE, LogType.SECURITY_WARNING, "Internal/UnknownServerGuard: Failed to load banned-ips.dat, a FileNotFoundException occurred.", e);
		}
	}
	
	public void logSecurityViolation(VIOLATION_TYPE issue, UnknownClient client) {
		if (issue != VIOLATION_TYPE.UNEXPECTED_BEHAVIOR) {
			UnknownLogger.log(Level.SEVERE, LogType.SECURITY_WARNING, "Internal/UnknownServerGuard: Client '" + client.getAddress().getHostAddress() + "' has triggered a security violation and it's connection will now be terminated. Violation Type: " + issue.getType());
			client.eject("A security violation has occurred and your client's connection has been terminated for the network's safety. This action has been recorded.", false);
		} else {
			UnknownLogger.log(Level.SEVERE, LogType.SECURITY_WARNING, "Internal/UnknownServerGuard: Client '" + client.getAddress().getHostAddress() + "' has preformed an action that has caused an unexpected behavior to result.");
		}
	}
	
	
	public boolean verifyClient(UnknownClient client) {
		synchronized (this.bannedIPs) {
			String ip = client.getAddress().getHostAddress();
			if (this.bannedIPs.contains(ip)) {
				UnknownLogger.log(Level.INFO, LogType.SECURITY_WARNING, "Internal/UnknownServerGuard: Banned ip address '" + ip + "' has attempted to connect. Ejecting client.");
				client.eject("Security Violation: You have been banned from this server.", false);
				return false;
			}
		}
		return true;
	}
	
	public void ipBanClient(UnknownClient client) {
		synchronized (this.bannedIPs) {
			String ip = client.getAddress().getHostAddress();
			UnknownLogger.log(Level.INFO, LogType.SECURITY_WARNING, "Internal/UnknownServerGuard: Banning ip '" + ip + "'");
			this.bannedIPs.add(ip);
			client.eject("Security Violation: You have been banned from this server.", false);
			writeBan(ip);
		}
	}

	private void writeBan(String ip) {
		File file = new File("banned-ips.dat");
		try {
			FileWriter fWriter = new FileWriter(file);
			fWriter.append(ip + "\n");
			fWriter.flush();
			fWriter.close();
		} catch (IOException e) {
			UnknownLogger.log(Level.SEVERE, LogType.SECURITY_WARNING,"Internal/UnknownServerGuard: Failed to save ban, an IOException occurred.", e);
			UnknownLogger.log(Level.INFO, LogType.SECURITY_WARNING, "Internal/UnknownServerGuard: Ban will be stored in server memory, but will not remain permanent.");
		}
	}
}
