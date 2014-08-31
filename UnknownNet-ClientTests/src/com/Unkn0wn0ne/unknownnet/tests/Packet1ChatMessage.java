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
package com.Unkn0wn0ne.unknownnet.tests;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import com.Unkn0wn0ne.unknownet.client.net.Packet;

public class Packet1ChatMessage extends Packet{

	private String smsg, rmsg;
	
	@Override
	public int getId() {
		return 1;
	}

	@Override
	public void write(DataOutputStream dataStream) throws IOException {
		dataStream.writeUTF(smsg);
	}

	@Override
	public void read(DataInputStream dataStream) throws IOException {
		this.rmsg = dataStream.readUTF();
	}

	@Override
	public void setVariables(Object... vars) {
		this.smsg = (String)vars[0];
	}

	@Override
	public PACKET_PRIORITY getPriority() {
		return PACKET_PRIORITY.HIGH;
	}
	
	public String getMessage() {
		return this.rmsg;
	}

}