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
package com.Unkn0wn0ne.unknownet.client.net;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class InternalPacket2Handshake extends Packet{

	private String protocolVersion;
	private boolean response = false;
	private String[] loginParams = null;
	
	@Override
	public int getId() {
		return -2;
	}

	@Override
	public void write(DataOutputStream dataStream) throws IOException {
		dataStream.writeUTF(this.protocolVersion);
		if (loginParams == null) {
			dataStream.writeInt(-1);
		} else {
			dataStream.writeInt(this.loginParams.length);
			for (String s : this.loginParams) {
				dataStream.writeUTF(s);
			}
		}
	}

	@Override
	public void read(DataInputStream dataStream) throws IOException {
		this.response = dataStream.readBoolean();
	}

	@Override
	public void setVariables(Object... vars) {
		this.protocolVersion = (String)vars[0];
		if (((Boolean)vars[1]) == true) {
			this.loginParams = (String[])vars[2];
		} else { 
			this.loginParams = null;
		}
	}

	@Override
	public PACKET_PRIORITY getPriority() {
		return PACKET_PRIORITY.INTERNAL;
	}

	public boolean getResponse() {
		return this.response;
	}

	@Override
	public PACKET_PROTOCOL getProtocol() {
		return PACKET_PROTOCOL.TCP;
	}

	@Override
	public void clearVariables() {
		this.protocolVersion = null;
		this.response = false;
		this.loginParams = null;
	}
}
