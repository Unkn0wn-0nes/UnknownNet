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
import java.io.IOException;

public class InternalPacket6DistributedObjectCreation extends Packet{

	private String objectText = "";
	
	@Override
	public void setVariables(Object... vars) {
		this.objectText = (String)vars[0];
	}

	@Override
	public int getId() {
		return -6;
	}

	@Override
	public void write(DataOutputStream dataStream) throws IOException {
		dataStream.writeUTF(this.objectText);
	}

	@Override
	public void read(DataInputStream dataStream) throws IOException {
		
	}

	@Override
	public PACKET_PRIORITY getPriority() {
		return PACKET_PRIORITY.INTERNAL;
	}

	@Override
	public PACKET_PROTOCOL getProtocol() {
		return PACKET_PROTOCOL.TCP;
	}

	@Override
	public void clearVariables() {
		this.objectText = null;
	}

}
