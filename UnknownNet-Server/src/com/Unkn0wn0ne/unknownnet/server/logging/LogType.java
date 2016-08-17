package com.Unkn0wn0ne.unknownnet.server.logging;

public enum LogType {

	/**
	 * CORE
	 * The core functions of the UnknownNet system. Server initialization and shutdown messages are logged here along with basic system information.
	 * These messages cannot be filtered for sanity reasons. 
	 */
	CORE("[Core] "),
	
	/**
	 * NETWORKING
	 * Used to detail networking related events. This includes client join, leaving, and ejection.
	 * These messages can be filtered.
	 */
	NETWORKING("[Networking] "),
	
	/**
	 * CRITICIAL_ERROR
	 * Events logged under 'CRITICAL_ERROR' usually detail uncaught exceptions or fatal server exceptions that prevent proper functioning.
	 * These messages cannot be filtered for sanity reasons.
	 */
	CRITICAL_ERROR("[Critical-Error] "),
	
	/**
	 * DEBUG_INFORMATION
	 * Events logged under 'DEBUG_INFORMATION' are usually UnknownNet messages containing various information about the server. This can also be used for the implementations to log debug messages
	 * These messages can be filtered.
	 */
	DEBUG_INFORMATION("[Debug-Information] "),
	
	/**
	 * SECURITY_WARNING
	 * Events logged under "SECURITY_WARNING" are usually protocol violations or clients attempting to violate security sandboxes or restrictions imposed upon them.
	 * An example of this type of message would be a client attempting to access administrative functions of the server when it is not authorized to do so.
	 * These messages cannot be filtered for security reasons.
	 */
	SECURITY_WARNING("[Security-Warning] "),
	
	/**
	 * IMPLEMENTATION
	 * Other messages used in the server implementation. These messages have a [Implementation] tag appended to them.
	 * These messages can be filtered.
	 */
	IMPLEMENTATION("[Implementation] ");
	
	private String prefix;
	
	LogType(String prefix) {
		this.prefix = prefix;
	}
	
	public String getPrefix() {
		return this.prefix;
	}
}
