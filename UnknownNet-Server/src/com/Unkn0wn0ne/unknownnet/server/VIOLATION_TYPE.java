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

/**
 * Enum listing the possible protocol / security / integrity violations a client can triggered. Used for server security purposes. For more information see {@link ServerGuard}
 * @author Unkn0wn0ne
 */
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
    
    /**
     * Gets the violation type
     * @return The violation type.
     */
    public String getType() {
    	return type;
    }
}
