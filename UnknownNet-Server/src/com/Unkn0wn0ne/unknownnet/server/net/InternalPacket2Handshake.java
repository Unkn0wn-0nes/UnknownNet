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
package com.Unkn0wn0ne.unknownnet.server.net;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;


public class InternalPacket2Handshake extends Packet{

	private boolean accepted = false;
	private String protocolVersion = "unknownserver-dev";
	private String[] loginParams = null;
	
	@Override
	public int getId() {
		return -2;
	}

	@Override
	public void write(DataOutputStream dataStream) throws IOException {
		dataStream.writeBoolean(this.accepted);
	}

	@Override
	public void read(DataInputStream dataStream) throws IOException {
		this.protocolVersion = dataStream.readUTF();
		int loginData = dataStream.readInt();
		
		switch (loginData) {
		case -1: {
			break;
		}
		default: {
			this.loginParams = new String[loginData];
			for (int x = 0; x < loginData; x++) {
				this.loginParams[x] = dataStream.readUTF();
			}
			break;
		}
		}
	}

	@Override
	public void setVariables(Object... vars) {
		this.accepted = (Boolean)vars[0];
	}

	@Override
	public PACKET_PRIORITY getPriority() {
		return PACKET_PRIORITY.INTERNAL;
	}

	public String getVersion() {
		return this.protocolVersion;
	}
	
	public String[] getLoginData() {
		return this.loginParams;
	}

	@Override
	public PACKET_PROTOCOL getProtocol() {
		return PACKET_PROTOCOL.TCP;
	}

}
