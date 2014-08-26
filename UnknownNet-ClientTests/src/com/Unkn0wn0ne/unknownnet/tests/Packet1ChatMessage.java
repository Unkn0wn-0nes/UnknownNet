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