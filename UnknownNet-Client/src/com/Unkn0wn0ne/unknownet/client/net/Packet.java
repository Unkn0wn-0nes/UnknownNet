package com.Unkn0wn0ne.unknownet.client.net;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public abstract class Packet {
	
	public abstract int getId();
	
	public abstract void write(DataOutputStream dataStream) throws IOException;
	
	public abstract void read(DataInputStream dataStream) throws IOException;
	
	public abstract void setVariables(Object... vars);
	
	public abstract PACKET_PRIORITY getPriority();
	
	public void _write(DataOutputStream dataStream) throws IOException{
		dataStream.writeInt(this.getId());
		this.write(dataStream);
	}
	
	public enum PACKET_PRIORITY {
		
		INTERNAL,
		
		HIGH,
		
		NORMAL;
	}
}
