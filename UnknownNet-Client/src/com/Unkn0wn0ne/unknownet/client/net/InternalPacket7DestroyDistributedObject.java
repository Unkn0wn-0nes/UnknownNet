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

public class InternalPacket7DestroyDistributedObject extends Packet{

	private long id;
	private int clientZone = -1;
	
	@Override
	public void setVariables(Object... vars) {
		
	}

	@Override
	public void clearVariables() {
		this.id = 0;
	}

	@Override
	public int getId() {
		return -7;
	}

	@Override
	public void write(DataOutputStream dataStream) throws IOException {
		
	}

	@Override
	public void read(DataInputStream dataStream) throws IOException {
		this.id = dataStream.readLong();
		this.clientZone = dataStream.readInt();
	}

	@Override
	public PACKET_PRIORITY getPriority() {
		return PACKET_PRIORITY.INTERNAL;
	}

	@Override
	public PACKET_PROTOCOL getProtocol() {
		return PACKET_PROTOCOL.TCP;
	}
	
	public long getDObjectId() {
		return this.id;
	}
	
	public int getClientZoneId() {
		return this.clientZone;
	}
}
