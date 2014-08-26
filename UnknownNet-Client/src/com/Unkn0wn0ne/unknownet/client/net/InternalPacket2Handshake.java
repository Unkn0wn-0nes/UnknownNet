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
}
