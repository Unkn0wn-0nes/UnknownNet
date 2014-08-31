package com.Unkn0wn0ne.unknownet.client.net;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class InternalPacket5Hello extends Packet{

	@Override
	public int getId() {
		return -5;
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
