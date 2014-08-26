package com.Unkn0wn0ne.unknownnet.server;

public enum VIOLATION_TYPE {
	/**
	 * A client sent violated the implementation or internal protocol. 
	 * Possible occurrences: 
	 * A client sent a packet that it was not supposed to have sent (i.e. sending a login packet after client has been logged in)
	 * A client sent a malformed/invalid packet
	 * A client sent a packet that was not registered on the server
	 * This will result in the termination of the client's connection
	 */
	PROTOCOL("Protocol"),
	
	/**
	 * A client or bug in the server implementation triggered some form of unexpected behavior, most likely a bug. This just typically puts the server guard into a watching state where it logs things more intensively 
	 */
	UNEXPECTED_BEHAVIOR("Unexpected Behavior"),
	
	/**
	 * A client or bug in the server implementation triggered a system error/exception. This increases the logging of the server and runs some basic sanity checks to attempt to see if the server is still functional. If a possibly rogue client caused this issue, it will be kicked with a security violation
	 */
	SYSTEM_ERROR_TRIGRERED("System Error Triggered"),
	
	/**
	 * A client or bug in the server implementation triggered some security issue. If a client is responsible for the security issue, it's connection will be terminated and will be logged
	 * A possible reason for this being triggered is when a client attempted to access administrative privileges when it did not have access
	 */
	SECURITY_ISSUE("Security Issue");
	
	private String type;
	
    VIOLATION_TYPE(String type) {
		this.type = type;
	}
    
    public String getType() {
    	return type;
    }
}