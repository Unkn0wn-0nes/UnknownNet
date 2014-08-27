package com.Unkn0wn0ne.unknownet.client.net;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;


public class InternalPacket1Kick extends Packet{

	private String msg = "";
	
	@Override
	public int getId() {
		return -1;
	}

	@Override
	public void write(DataOutputStream dataStream) throws IOException {
		dataStream.writeUTF(this.msg);
	}

	@Override
	public void read(DataInputStream dataStream) throws IOException {
		this.msg = dataStream.readUTF();
	}

	@Override
	public void setVariables(Object... vars) {
		this.msg = (String)vars[0];
	}

	@Override
	public PACKET_PRIORITY getPriority() {
		return PACKET_PRIORITY.INTERNAL;
	}

	public String getReason() {
		return this.msg;
	}

}
