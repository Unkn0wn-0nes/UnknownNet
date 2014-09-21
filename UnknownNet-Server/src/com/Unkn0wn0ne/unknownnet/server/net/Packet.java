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
import java.io.IOException;

import com.Unkn0wn0ne.unknownnet.server.util.PoolableObject;


public abstract class Packet implements PoolableObject{
	
	private int numClients = 1;
	
	public abstract int getId();
	
	public abstract void write(DataOutputStream dataStream) throws IOException;
	
	public abstract void read(DataInputStream dataStream) throws IOException;
	
	public abstract PACKET_PRIORITY getPriority();
	
	public abstract PACKET_PROTOCOL getProtocol();
	
	public void _write(DataOutputStream dataStream) throws IOException{
		dataStream.writeInt(this.getId());
		this.write(dataStream);
	}
	
	@Override
	public void resetVariables() {
		this.numClients = 1;
		this.clearVariables();
	}
	
	/**
	 * Called to reset the packet's variables before it is reinserted into the pool. UnknownNet manages this automatically.
	 */
	public abstract void clearVariables();
	
	public enum PACKET_PROTOCOL {
		TCP,
		
		UDP;
	}
	
	public enum PACKET_PRIORITY {
		
		INTERNAL,
		
		HIGH,
		
		NORMAL;
	}
	
	/**
	 * If you are sending a packet to more than one client, you must specify how many clients you are sending it to.
	 * @param numClients The number of clients you are sending the packet to
	 */
	public void setRecipentCount(int numClients) {
		synchronized (this) {
			this.numClients = numClients;
		}
	}
	
	/**
	 * @return The number of clients you are sending the packet to
	 */
	public int getRecipentCount() {
		synchronized (this) {
			return this.numClients;
		}
	}
}
