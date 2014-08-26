package com.Unkn0wn0ne.unknownnet.server.net;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;


public class InternalPacket4AdministrativeAction extends Packet{

	@Override
	public int getId() {
		return -4;
	}

	@Override
	public void write(DataOutputStream dataStream) throws IOException {
		
	}

	@Override
	public void read(DataInputStream dataStream) throws IOException {
		
	}

	@Override
	public void setVariables(Object... vars) {
		
	}

	@Override
	public PACKET_PRIORITY getPriority() {
		return PACKET_PRIORITY.INTERNAL;
	}

}
