package com.Unkn0wn0ne.unknownnet.server;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class ServerGuard {

	private Logger logger = Logger.getLogger("UnknownNet");
	
	
	public void logSecurityViolation(VIOLATION_TYPE issue, UnknownClient client) {
		if (issue != VIOLATION_TYPE.UNEXPECTED_BEHAVIOR) {
			logger.severe("Internal/ServerGuard: Client '" + client.getAddress().getHostAddress() + "' has triggered a security violation and it's connection will now be terminated. Violation Type: " + issue.getType());
			client.eject("A security violation has occurred and your client's connection has been terminated for the network's safety. This action has been recorded.", false);
		} else {
			logger.warning("Internal/ServerGuard: Client '" + client.getAddress().getHostAddress() + "' has preformed an action that has caused an unexpected behavior to result.");
		}
	}
	
	private List<String> bannedIPs = new ArrayList<String>();
	
	public boolean verifyClient(UnknownClient client) {
		synchronized (this.bannedIPs) {
			String ip = client.getAddress().getHostAddress();
			if (this.bannedIPs.contains(ip)) {
				logger.warning("Internal/ServerSecurityManager: Banned ip address '" + ip + "' has attempted to connect. Ejecting client.");
				client.eject("Security Violation: You have been banned from this server.", false);
				return false;
			}
		}
		return true;
	}
	
	public void ipBanClient(UnknownClient client) {
		synchronized (this.bannedIPs) {
			String ip = client.getAddress().getHostAddress();
			logger.info("Internal/ServerSecurityManager: Banning ip '" + ip + "'");
			this.bannedIPs.add(ip);
			client.eject("Security Violation: You have been banned from this server.", false);
		}
	}
}
